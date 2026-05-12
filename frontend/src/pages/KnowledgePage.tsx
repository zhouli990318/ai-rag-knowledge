import { useEffect, useState } from 'react';
import {
  Box, Typography, Button, IconButton, Grid, LinearProgress,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField,
} from '@mui/material';
import {
  AddOutlined as Add, DeleteOutlined as Delete, UploadOutlined as Upload,
  GitHub, AutorenewOutlined as Autorenew, SearchOutlined as Search,
  FolderOpenOutlined as FolderOpen, DescriptionOutlined as Description,
  EditOutlined as Edit,
  ChevronRightOutlined as ChevronRight,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { knowledgeApi } from '@/entities/knowledge/api/knowledgeApi';
import type { CreateKnowledgeBaseRequest, UpdateKnowledgeBaseRequest } from '@/entities/knowledge/api/knowledgeApi';
import type { KnowledgeBase, KbDocument, SearchResult } from '@/entities/knowledge/model/types';
import { useSnackbar } from 'notistack';
import { useDropzone } from 'react-dropzone';
import { InkBadge, InkSearchBar, InkActionSheet, InkEmptyState } from '@/shared/ui/ink';
import { motion, AnimatePresence } from 'framer-motion';
import { useInk } from '@/shared/theme/ThemeProvider';
import { kbGradients, ui } from '@/shared/theme/semanticColors';
import { CreateKnowledgeBaseDialog, defaultKnowledgeFormState, GitImportDialog } from '@/widgets/knowledge';
import type { KnowledgeFormState } from '@/widgets/knowledge';

// Generate a gradient from KB name
function nameGradient(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return kbGradients[Math.abs(hash) % kbGradients.length];
}

function statusType(s: string) {
  switch (s) { case 'INDEXED': return 'success' as const; case 'PROCESSING': return 'warning' as const; case 'FAILED': return 'error' as const; default: return 'default' as const; }
}

function toKnowledgeRequest(payload: KnowledgeFormState): CreateKnowledgeBaseRequest {
  return {
    name: payload.name.trim(),
    description: payload.description.trim(),
    chunkStrategy: {
      type: payload.chunkType,
      chunkSize: payload.chunkSize,
      chunkOverlap: payload.chunkOverlap,
      semanticThreshold: payload.semanticThreshold,
      childChunkSize: payload.childChunkSize,
      windowSize: payload.windowSize,
      enableParentChild: payload.enableParentChild,
    },
    retrievalConfig: {
      topK: payload.retrievalTopK,
      similarityThreshold: payload.similarityThreshold,
      filterExpression: payload.filterExpression.trim() || null,
      retrievalMode: payload.retrievalMode,
      keywordWeight: payload.keywordWeight,
      vectorWeight: payload.vectorWeight,
      rerankerEnabled: payload.rerankerEnabled,
      rerankerTopK: payload.rerankerTopK,
      windowSize: payload.windowSize,
    },
  };
}

function toKnowledgeFormState(knowledgeBase: KnowledgeBase): KnowledgeFormState {
  return {
    name: knowledgeBase.name,
    description: knowledgeBase.description ?? '',
    chunkType: knowledgeBase.chunkStrategy.type,
    chunkSize: knowledgeBase.chunkStrategy.chunkSize,
    chunkOverlap: knowledgeBase.chunkStrategy.chunkOverlap,
    semanticThreshold: knowledgeBase.chunkStrategy.semanticThreshold,
    childChunkSize: knowledgeBase.chunkStrategy.childChunkSize,
    windowSize: knowledgeBase.chunkStrategy.windowSize,
    enableParentChild: knowledgeBase.chunkStrategy.enableParentChild,
    retrievalTopK: knowledgeBase.retrievalConfig.topK,
    similarityThreshold: knowledgeBase.retrievalConfig.similarityThreshold,
    retrievalMode: knowledgeBase.retrievalConfig.retrievalMode,
    filterExpression: knowledgeBase.retrievalConfig.filterExpression ?? '',
    keywordWeight: knowledgeBase.retrievalConfig.keywordWeight,
    vectorWeight: knowledgeBase.retrievalConfig.vectorWeight,
    rerankerEnabled: knowledgeBase.retrievalConfig.rerankerEnabled,
    rerankerTopK: knowledgeBase.retrievalConfig.rerankerTopK,
  };
}

export default function KnowledgePage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchFilterExpression, setSearchFilterExpression] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [gitOpen, setGitOpen] = useState(false);
  const [rebuildOpen, setRebuildOpen] = useState(false);
  const [actionSheetKb, setActionSheetKb] = useState<KnowledgeBase | null>(null);

  const { data: kbs = [], isLoading } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });
  const { data: documents = [], refetch: refetchDocs } = useQuery({
    queryKey: ['kb-docs', selectedKb?.id],
    queryFn: () => knowledgeApi.listDocuments(selectedKb!.id),
    enabled: !!selectedKb,
  });

  const createMutation = useMutation({
    mutationFn: knowledgeApi.create,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] }); setCreateOpen(false); enqueueSnackbar('创建成功', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '创建失败', { variant: 'error' }),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateKnowledgeBaseRequest }) => knowledgeApi.update(id, data),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] });
      setSelectedKb(saved);
      setEditOpen(false);
      enqueueSnackbar('保存成功', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '保存失败', { variant: 'error' }),
  });
  const deleteMutation = useMutation({
    mutationFn: knowledgeApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] }); setSelectedKb(null); enqueueSnackbar('删除成功', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
  const uploadMutation = useMutation({
    mutationFn: (file: File) => knowledgeApi.uploadDocument(selectedKb!.id, file),
    onSuccess: () => { refetchDocs(); enqueueSnackbar('上传成功', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || e?.message || '上传失败', { variant: 'error' }),
  });
  const deleteDocMutation = useMutation({
    mutationFn: (docId: number) => knowledgeApi.deleteDocument(selectedKb!.id, docId),
    onSuccess: () => { refetchDocs(); enqueueSnackbar('文档已删除', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
  const gitImportMutation = useMutation({
    mutationFn: (url: string) => knowledgeApi.importGit(selectedKb!.id, { repoUrl: url }),
    onSuccess: () => { setGitOpen(false); refetchDocs(); enqueueSnackbar('导入任务已提交', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '导入失败', { variant: 'error' }),
  });
  const rebuildMutation = useMutation({
    mutationFn: (kbId: number) => knowledgeApi.rebuildVectors(kbId),
    onSuccess: (msg) => {
      setRebuildOpen(false); refetchDocs();
      queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] });
      enqueueSnackbar(msg || '向量库重建完成', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || e?.message || '重建失败', { variant: 'error' }),
  });

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (files) => files.forEach((f) => uploadMutation.mutate(f)),
    accept: { 'application/pdf': ['.pdf'], 'text/*': ['.txt', '.md', '.csv'], 'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'] },
  });

  const handleSearch = async () => {
    if (!selectedKb || !searchQuery.trim()) return;
    try {
      const results = await knowledgeApi.search(
        selectedKb.id,
        searchQuery,
        undefined,
        searchFilterExpression.trim() || undefined,
      );
      setSearchResults(results);
    } catch (e: any) {
      enqueueSnackbar(e?.response?.data?.message || '搜索失败', { variant: 'error' });
    }
  };

  const handleCreate = (payload: KnowledgeFormState) => {
    if (!payload.name.trim()) { enqueueSnackbar('请输入名称', { variant: 'warning' }); return; }
    createMutation.mutate(toKnowledgeRequest(payload));
  };

  const handleUpdate = (payload: KnowledgeFormState) => {
    if (!selectedKb) return;
    if (!payload.name.trim()) { enqueueSnackbar('请输入名称', { variant: 'warning' }); return; }
    updateMutation.mutate({ id: selectedKb.id, data: toKnowledgeRequest(payload) });
  };

  const openCreateDialog = () => setCreateOpen(true);

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 }, pt: { xs: 7, md: 7 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5" sx={{ fontFamily: '"Noto Serif SC", serif' }}>知识库</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog} sx={{
          borderRadius: 4,
        }}>
          新建
        </Button>
      </Box>

      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {/* KB Cards */}
      {kbs.length === 0 && !isLoading ? (
        <InkEmptyState
          icon={<FolderOpen />}
          title="暂无知识库"
          subtitle="创建你的第一个知识库，让 AI 拥有专属记忆"
          description="支持文档上传、Git 仓库导入和向量检索"
          action={{ label: '新建知识库', onClick: openCreateDialog }}
        />
      ) : (
        <Grid container spacing={2} sx={{ mb: 2.5 }}>
          {kbs.map((kb: KnowledgeBase) => {
            const isActive = selectedKb?.id === kb.id;
            return (
              <Grid key={kb.id} item xs={12} sm={6} md={4} lg={3}>
                <motion.div whileTap={{ scale: 0.97 }}>
                  <Box
                    onClick={() => setSelectedKb(kb)}
                    onContextMenu={(e) => { e.preventDefault(); setActionSheetKb(kb); }}
                    sx={{
                      cursor: 'pointer',
                      borderRadius: 4,
                      overflow: 'hidden',
                      border: isActive ? `1.5px solid rgba(200,75,49,0.5)` : `1px solid ${ui.tableBorder}`,
                      backgroundColor: ui.cardBg,
                      backdropFilter: 'blur(12px)',
                      WebkitBackdropFilter: 'blur(12px)',
                      boxShadow: isActive
                        ? '0 4px 20px rgba(200,75,49,0.12), 0 0 0 1px rgba(200,75,49,0.08)'
                        : '0 2px 12px rgba(0,0,0,0.04)',
                      transition: 'all 300ms cubic-bezier(0.25,0.46,0.45,0.94)',
                      '&:hover': { transform: 'translateY(-3px)', borderColor: 'rgba(224,221,216,0.8)', boxShadow: '0 6px 24px rgba(0,0,0,0.08)' },
                    }}
                  >
                    {/* Gradient top */}
                    <Box sx={{ height: 72, background: nameGradient(kb.name), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <FolderOpen sx={{ fontSize: 32, color: 'rgba(255,255,255,0.8)' }} />
                    </Box>
                    {/* Content */}
                    <Box sx={{ p: 2 }}>
                      <Typography sx={{ fontSize: 17, fontWeight: 600, mb: 0.25 }}>{kb.name}</Typography>
                      <Typography sx={{
                        fontSize: 13, color: 'text.secondary', lineHeight: 1.35,
                        overflow: 'hidden', textOverflow: 'ellipsis',
                        display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
                        mb: 1, minHeight: 35,
                      }}>
                        {kb.description || '暂无描述'}
                      </Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <InkBadge label={`${kb.documentCount} 文档`} status="info" />
                        <InkBadge label={kb.active ? '活跃' : '停用'} status={kb.active ? 'success' : 'default'} />
                      </Box>
                    </Box>
                  </Box>
                </motion.div>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Detail panel */}
      <AnimatePresence>
        {selectedKb && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 16 }}
            transition={{ duration: 0.25 }}
          >
            <Box sx={{
              backgroundColor: '#f7f2e6',
              backdropFilter: 'blur(12px)',
              WebkitBackdropFilter: 'blur(12px)',
              borderRadius: 4, p: 2.5,
              border: '1px solid rgba(224,221,216,0.5)',
              boxShadow: '0 2px 16px rgba(0,0,0,0.04)',
            }}>
              {/* Detail header */}
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
                <Box>
                  <Typography variant="h6">{selectedKb.name}</Typography>
                  <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{selectedKb.description}</Typography>
                  <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                    <InkBadge label={`分块 ${selectedKb.chunkStrategy.type}`} status="default" />
                    <InkBadge label={`模式 ${selectedKb.retrievalConfig.retrievalMode}`} status="info" />
                    <InkBadge label={`阈值 ${selectedKb.retrievalConfig.similarityThreshold}`} status="warning" />
                    {selectedKb.chunkStrategy.enableParentChild && (
                      <InkBadge label={`父子块 ${selectedKb.chunkStrategy.childChunkSize}/${selectedKb.chunkStrategy.windowSize}`} status="success" />
                    )}
                    {selectedKb.chunkStrategy.type === 'SEMANTIC' && (
                      <InkBadge label={`语义阈值 ${selectedKb.chunkStrategy.semanticThreshold}`} status="warning" />
                    )}
                    {selectedKb.retrievalConfig.rerankerEnabled && (
                      <InkBadge label={`Reranker ${selectedKb.retrievalConfig.rerankerTopK}`} status="success" />
                    )}
                    {selectedKb.retrievalConfig.retrievalMode === 'HYBRID' && (
                      <>
                        <InkBadge label={`关键词 ${selectedKb.retrievalConfig.keywordWeight}`} status="default" />
                        <InkBadge label={`向量 ${selectedKb.retrievalConfig.vectorWeight}`} status="default" />
                      </>
                    )}
                  </Box>
                </Box>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button size="small" variant="outlined" startIcon={<Edit />} onClick={() => setEditOpen(true)} sx={{ borderRadius: 4 }}>编辑配置</Button>
                  <Button size="small" variant="outlined" startIcon={<GitHub />} onClick={() => setGitOpen(true)} sx={{ borderRadius: 4 }}>Git 导入</Button>
                  <Button size="small" variant="outlined" color="secondary" startIcon={<Autorenew />} onClick={() => setRebuildOpen(true)} disabled={rebuildMutation.isPending} sx={{ borderRadius: 4 }}>重建向量</Button>
                  <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(selectedKb.id)}><Delete /></IconButton>
                </Box>
              </Box>

              {/* Upload zone */}
              <Box {...getRootProps()} sx={{
                border: `2px dashed ${isDragActive ? ui.accentRed : ui.border}`,
                borderRadius: 2, p: 4, textAlign: 'center', mb: 2.5, cursor: 'pointer',
                backgroundColor: isDragActive ? 'rgba(200,75,49,0.04)' : 'transparent',
                transition: 'all 200ms',
              }}>
                <input {...getInputProps()} />
                <Upload sx={{ fontSize: 36, color: isDragActive ? ui.accentRed : 'text.disabled', mb: 0.5 }} />
                <Typography sx={{ fontSize: 15, color: isDragActive ? ui.accentRed : 'text.secondary' }}>
                  拖拽文件到此处或点击上传
                </Typography>
                <Typography sx={{ fontSize: 12, color: 'text.secondary', mt: 0.5 }}>支持 PDF, TXT, MD, DOCX</Typography>
              </Box>
              {uploadMutation.isPending && <LinearProgress sx={{ mb: 2 }} />}
              {rebuildMutation.isPending && <LinearProgress color="secondary" sx={{ mb: 2 }} />}

              {/* Search */}
              <Box sx={{ mb: 2.5 }}>
                <InkSearchBar value={searchQuery} onChange={setSearchQuery} onSearch={handleSearch} placeholder="搜索知识库内容..." />
                <TextField
                  fullWidth
                  size="small"
                  sx={{ mt: 1 }}
                  label="动态过滤（可选）"
                  value={searchFilterExpression}
                  onChange={(event) => setSearchFilterExpression(event.target.value)}
                  placeholder="file_type IN (md,pdf)"
                  helperText="支持 file_name = README.md、file_type IN (md,pdf)、document_id = 12；多个条件用 AND"
                />
              </Box>
              {searchResults.length > 0 && (
                <Box sx={{
                  backgroundColor: ui.hoverBg,
                  borderRadius: 2, p: 2, mb: 2.5, maxHeight: 240, overflow: 'auto',
                  border: `1px solid ${ui.border}`,
                }}>
                  {searchResults.map((r, i) => (
                    <Box key={i} sx={{
                      py: 1.5,
                      borderBottom: i < searchResults.length - 1 ? `0.5px solid ${ui.border}` : 'none',
                    }}>
                      <Typography sx={{ fontSize: 14, mb: 0.5, lineHeight: 1.4 }}>{r.content}</Typography>
                      <Typography sx={{ fontSize: 11, color: 'text.secondary' }}>{JSON.stringify(r.metadata)}</Typography>
                    </Box>
                  ))}
                </Box>
              )}

              {/* Documents list (iOS grouped list style) */}
              <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: 0.5, mb: 1 }}>
                文档 ({documents.length})
              </Typography>
              <Box sx={{
                backgroundColor: ui.hoverBg,
                borderRadius: 2, overflow: 'hidden',
                border: `1px solid ${ui.border}`,
              }}>
                {documents.length === 0 ? (
                  <Box sx={{ py: 4, textAlign: 'center' }}>
                    <Typography sx={{ fontSize: 15, color: 'text.secondary' }}>暂无文档</Typography>
                  </Box>
                ) : (
                  documents.map((d: KbDocument, i: number) => (
                    <Box key={d.id} sx={{
                      display: 'flex', alignItems: 'center', gap: 1.5,
                      px: 2, py: 1.5,
                      borderBottom: i < documents.length - 1 ? `0.5px solid ${ui.border}` : 'none',
                    }}>
                      <Description sx={{ fontSize: 20, color: 'text.secondary' }} />
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography sx={{ fontSize: 15, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {d.fileName}
                        </Typography>
                        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>
                          {d.fileType} · {(d.fileSize / 1024).toFixed(1)} KB · {d.chunkCount} 分块
                        </Typography>
                      </Box>
                      <InkBadge label={d.status} status={statusType(d.status)} dot />
                      <IconButton size="small" onClick={() => deleteDocMutation.mutate(d.id)} sx={{ color: 'text.secondary', '&:hover': { color: ui.accentRed } }}>
                        <Delete sx={{ fontSize: 18 }} />
                      </IconButton>
                    </Box>
                  ))
                )}
              </Box>
            </Box>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Action sheet for long-press on card */}
      <InkActionSheet
        open={!!actionSheetKb}
        onClose={() => setActionSheetKb(null)}
        title={actionSheetKb?.name}
        actions={[
          { label: '编辑配置', onClick: () => { if (actionSheetKb) { setSelectedKb(actionSheetKb); setEditOpen(true); } }, color: 'primary' },
          { label: '重建向量库', onClick: () => actionSheetKb && rebuildMutation.mutate(actionSheetKb.id), color: 'primary' },
          { label: '删除', onClick: () => actionSheetKb && deleteMutation.mutate(actionSheetKb.id), color: 'error' },
        ]}
      />

      <CreateKnowledgeBaseDialog
        open={createOpen}
        loading={createMutation.isPending}
        title="新建知识库"
        submitLabel="创建"
        initialValue={defaultKnowledgeFormState}
        onClose={() => setCreateOpen(false)}
        onSubmit={handleCreate}
      />

      <CreateKnowledgeBaseDialog
        open={editOpen}
        loading={updateMutation.isPending}
        title="编辑知识库配置"
        submitLabel="保存"
        initialValue={selectedKb ? toKnowledgeFormState(selectedKb) : defaultKnowledgeFormState}
        onClose={() => setEditOpen(false)}
        onSubmit={handleUpdate}
      />

      <GitImportDialog
        open={gitOpen}
        loading={gitImportMutation.isPending}
        onClose={() => setGitOpen(false)}
        onSubmit={(gitRepoUrl) => gitImportMutation.mutate(gitRepoUrl)}
      />

      {/* Rebuild confirm */}
      <Dialog open={rebuildOpen} onClose={() => !rebuildMutation.isPending && setRebuildOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>重建向量库</DialogTitle>
        <DialogContent>
          <Typography sx={{ fontSize: 15, color: 'text.secondary' }}>
            将删除当前知识库已有向量，并基于已保存的文本分片重新向量化。不会删除文档记录。
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRebuildOpen(false)} disabled={rebuildMutation.isPending}>取消</Button>
          <Button variant="contained" color="secondary" onClick={() => selectedKb && rebuildMutation.mutate(selectedKb.id)} disabled={rebuildMutation.isPending} sx={{ borderRadius: 4 }}>
            开始重建
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
