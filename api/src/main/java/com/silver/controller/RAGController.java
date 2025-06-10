package com.silver.controller;

import com.silver.application.RAGApplication;
import com.silver.shared.core.dto.Response;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ai/v1/rag/")
public class RAGController {

    @Resource
    private RAGApplication ragApplication;

    @GetMapping("query_rag_tag_list")
    public Response<List<String>> queryRagTagList() {
        return ragApplication.queryRagTagList();
    }

    @PostMapping("file/upload")
    public Response<String> uploadFile(@RequestParam("ragTag") String ragTag,
                                       @RequestParam("file") List<MultipartFile> files) {
        return ragApplication.uploadFile(ragTag, files);
    }

    @PostMapping("analyze_git_repository")
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl,
                                                 @RequestParam("userName") String userName,
                                                 @RequestParam("token") String token) {
        return ragApplication.analyzeGitRepository(repoUrl, userName, token);
    }

}
