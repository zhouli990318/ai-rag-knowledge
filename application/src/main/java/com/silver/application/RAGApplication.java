package com.silver.application;

import com.silver.shared.core.dto.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class RAGApplication {

    private static final String LOCAL_CLONE_DIR_PREFIX = "git-cloned-repo-";
    private static final ReentrantLock lock = new ReentrantLock(); // 防止并发冲突

    private final RedissonClient redissonClient;

    private final PgVectorStore pgVectorStore;

    private final TokenTextSplitter tokenTextSplitter;

    public RAGApplication(RedissonClient redissonClient, PgVectorStore pgVectorStore,
                          TokenTextSplitter tokenTextSplitter) {
        this.redissonClient = redissonClient;
        this.pgVectorStore = pgVectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("ragTag");
        return Response.<List<String>>builder().code("200").info("调用成功").data(elements).build();
    }

    public Response<String> uploadFile(String ragTag, List<MultipartFile> files) {
        log.info("上传知识库开始 {}", ragTag);
        for (MultipartFile file : files) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();
            List<Document> documentSplitterList = tokenTextSplitter.split(documents);

            documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
            documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));

            pgVectorStore.accept(documentSplitterList);

            RList<String> elements = redissonClient.getList("ragTag");
            if (!elements.contains(ragTag)) {
                elements.add(ragTag);
            }
        }

        log.info("上传知识库完成 {}", ragTag);
        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    public Response<String> analyzeGitRepository(String repoUrl, String userName, String token) {
        File localRepoDir = null;

        try {
            String projectName = extractProjectName(repoUrl);

            // 使用唯一目录名避免并发冲突
            localRepoDir = createUniqueCloneDirectory();
            log.info("开始克隆仓库到本地路径: {}", localRepoDir.getAbsolutePath());

            // 克隆仓库
            cloneGitRepository(repoUrl, userName, token, localRepoDir);

            // 遍历文件并上传知识库
            processFilesRecursively(localRepoDir.toPath(), projectName);

            // 清理本地仓库
            cleanupLocalDirectory(localRepoDir);

            // 更新 Redis 标签列表
            updateRedisTag(projectName);

            log.info("仓库解析完成: {}", repoUrl);
            return Response.<String>builder().code("0000").info("调用成功").build();

        } catch (Exception e) {
            log.error("分析 Git 仓库失败：{}", repoUrl, e);
            return Response.<String>builder().code("9999").info("调用失败：" + e.getMessage()).build();
        } finally {
            if (localRepoDir != null && localRepoDir.exists()) {
                try {
                    FileUtils.deleteDirectory(localRepoDir);
                } catch (IOException e) {
                    log.warn("清理临时目录失败: {}", localRepoDir.getAbsolutePath(), e);
                }
            }
            lock.unlock(); // 确保释放锁
        }
    }

    private void cloneGitRepository(String repoUrl, String userName, String token, File localRepoDir)
            throws GitAPIException, IOException {
        try (Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localRepoDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                .call()) {
            log.info("仓库克隆成功: {}", repoUrl);
        }
    }

    private void processFilesRecursively(Path rootPath, String projectName) throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (shouldSkipFile(file)) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    log.debug("{} 正在处理文件: {}", projectName, file.getFileName());
                    TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                    List<Document> documents = reader.get();

                    documents.forEach(doc -> doc.getMetadata().put("knowledge", projectName));

                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                    pgVectorStore.accept(documentSplitterList);

                } catch (Exception e) {
                    log.error("无法处理文件 {}: {}", file.getFileName(), e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("访问文件失败: {} - 原因: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean shouldSkipFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                fileName.endsWith(".gif") || fileName.endsWith(".exe") ||
                fileName.endsWith(".zip") || fileName.endsWith(".jar");
    }

    private File createUniqueCloneDirectory() throws IOException {
        lock.lock(); // 加锁防止并发写入相同目录
        cleanupLocalDirectory(new File(LOCAL_CLONE_DIR_PREFIX));
        File tempDir = Files.createTempDirectory(LOCAL_CLONE_DIR_PREFIX).toFile();
        log.debug("创建临时克隆目录: {}", tempDir.getAbsolutePath());
        return tempDir;
    }

    private void cleanupLocalDirectory(File dir) throws IOException {
        if (dir != null && dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
    }

    private void updateRedisTag(String projectName) {
        RList<String> tagList = redissonClient.getList("ragTag");
        if (!tagList.contains(projectName)) {
            tagList.add(projectName);
        }
    }

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String lastPart = parts.length > 0 ? parts[parts.length - 1] : "";
        int dotIndex = lastPart.indexOf('.');
        return dotIndex > 0 ? lastPart.substring(0, dotIndex) : lastPart;
    }
}
