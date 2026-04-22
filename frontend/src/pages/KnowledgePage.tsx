import { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, IconButton, Grid, LinearProgress,
  useTheme, Slide, Skeleton,
} from '@mui/material';
import {
  Add, Delete, Upload, GitHub, Autorenew, Search,
  FolderOpen, Description, ChevronRight,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { knowledgeApi } from '../api/knowledgeApi';
import { KnowledgeBase, KbDocument, SearchResult } from '../api/types';
import { useSnackbar } from 'notistack';
import { useDropzone } from 'react-dropzone';
import { IOSStatusBadge, IOSSearchBar, IOSActionSheet, IOSEmptyState } from '../components/ios';
import { motion, AnimatePresence } from 'framer-motion';

// Generate a gradient from KB name
function nameGradient(name: string): string {
  const gradients = [
    'linear-gradient(135deg, #007AFF, #5856D6)',
    'linear-gradient(135deg, #FF9500, #FF2D55)',
    'linear-gradient(135deg, #34C759, #5AC8FA)',
    'linear-gradient(135deg, #AF52DE, #FF2D55)',
    'linear-gradient(135deg, #5AC8FA, #007AFF)',
    'linear-gradient(135deg, #FF2D55, #FF9500)',
    'linear-gradient(135deg, #5856D6, #AF52DE)',
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return gradients[Math.abs(hash) % gradients.length];
}

function statusType(s: string) {
  switch (s) { case 'INDEXED': return 'success' as const; case 'PROCESSING': return 'warning' as const; case 'FAILED': return 'error' as const; default: return 'default' as const; }
}

type KnowledgeFormState = {
  name: string;
  description: string;
  chunkSize: number;
  chunkOverlap: number;
};

function CreateKnowledgeBaseDialog({
  open,
  loading,
  onClose,
  onSubmit,
}: {
  open: boolean;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: KnowledgeFormState) => void;
}) {
  const [form, setForm] = useState<KnowledgeFormState>({ name: '', description: '', chunkSize: 800, chunkOverlap: 200 });

  useEffect(() => {
    if (!open) {
      return;
    }
    setForm({ name: '', description: '', chunkSize: 800, chunkOverlap: 200 });
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>新建知识库</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
        <TextField label="名称" required value={form.name} onChange={(e) => setForm((current) => ({ ...current, name: e.target.value }))} />
        <TextField label="描述" multiline rows={2} value={form.description} onChange={(e) => setForm((current) => ({ ...current, description: e.target.value }))} />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField type="number" label="分块大小" value={form.chunkSize} onChange={(e) => setForm((current) => ({ ...current, chunkSize: Number(e.target.value) }))} />
          <TextField type="number" label="分块重叠" value={form.chunkOverlap} onChange={(e) => setForm((current) => ({ ...current, chunkOverlap: Number(e.target.value) }))} />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 8 }}>创建</Button>
      </DialogActions>
    </Dialog>
  );
}

function GitImportDialog({
  open,
  loading,
  onClose,
  onSubmit,
}: {
  open: boolean;
  loading: boolean;
  onClose: () => void;
  onSubmit: (gitUrl: string) => void;
}) {
  const [gitUrl, setGitUrl] = useState('');

  useEffect(() => {
    if (!open) {
      return;
    }
    setGitUrl('');
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Git 仓库导入</DialogTitle>
      <DialogContent sx={{ pt: '16px !important' }}>
        <TextField fullWidth label="Git 仓库 URL" value={gitUrl} onChange={(e) => setGitUrl(e.target.value)} placeholder="https://github.com/..." />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(gitUrl)} disabled={loading} sx={{ borderRadius: 8 }}>导入</Button>
      </DialogActions>
    </Dialog>
  );
}

export default function KnowledgePage() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
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
    mutationFn: (data: Record<string, unknown>) => knowledgeApi.create(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] }); setCreateOpen(false); enqueueSnackbar('创建成功', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '创建失败', { variant: 'error' }),
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
      const results = await knowledgeApi.search(selectedKb.id, searchQuery);
      setSearchResults(results);
    } catch (e: any) {
      enqueueSnackbar(e?.response?.data?.message || '搜索失败', { variant: 'error' });
    }
  };

  const handleCreate = (payload: KnowledgeFormState) => {
    if (!payload.name.trim()) { enqueueSnackbar('请输入名称', { variant: 'warning' }); return; }
    createMutation.mutate({
      name: payload.name.trim(),
      description: payload.description.trim(),
      chunkSize: payload.chunkSize,
      chunkOverlap: payload.chunkOverlap,
    });
  };

  const openCreateDialog = () => setCreateOpen(true);

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5">知识库</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog} sx={{ borderRadius: 8 }}>
          新建
        </Button>
      </Box>

      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {/* KB Cards */}
      {kbs.length === 0 && !isLoading ? (
        <IOSEmptyState icon={<FolderOpen />} title="暂无知识库" subtitle="创建知识库来管理文档和向量检索" action={{ label: '新建知识库', onClick: openCreateDialog }} />
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
                      borderRadius: 3,
                      overflow: 'hidden',
                      border: isActive ? `2px solid #007AFF` : `2px solid transparent`,
                      backgroundColor: isDark ? '#1C1C1E' : '#FFFFFF',
                      boxShadow: isActive
                        ? (isDark ? '0 0 20px rgba(0,122,255,0.3)' : '0 0 20px rgba(0,122,255,0.15)')
                        : (isDark ? '0 2px 12px rgba(0,0,0,0.3)' : '0 1px 8px rgba(0,0,0,0.06)'),
                      transition: 'all 300ms cubic-bezier(0.25,0.46,0.45,0.94)',
                      '&:hover': { transform: 'translateY(-2px)' },
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
                        <IOSStatusBadge label={`${kb.documentCount} 文档`} status="info" />
                        <IOSStatusBadge label={kb.active ? '活跃' : '停用'} status={kb.active ? 'success' : 'default'} />
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
              backgroundColor: isDark ? '#1C1C1E' : '#FFFFFF',
              borderRadius: 3, p: 2.5,
              boxShadow: isDark ? '0 4px 24px rgba(0,0,0,0.4)' : '0 2px 16px rgba(0,0,0,0.06)',
            }}>
              {/* Detail header */}
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
                <Box>
                  <Typography variant="h6">{selectedKb.name}</Typography>
                  <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{selectedKb.description}</Typography>
                </Box>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button size="small" variant="outlined" startIcon={<GitHub />} onClick={() => setGitOpen(true)} sx={{ borderRadius: 8 }}>Git 导入</Button>
                  <Button size="small" variant="outlined" color="secondary" startIcon={<Autorenew />} onClick={() => setRebuildOpen(true)} disabled={rebuildMutation.isPending} sx={{ borderRadius: 8 }}>重建向量</Button>
                  <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(selectedKb.id)}><Delete /></IconButton>
                </Box>
              </Box>

              {/* Upload zone */}
              <Box {...getRootProps()} sx={{
                border: `2px dashed ${isDragActive ? '#007AFF' : (isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.1)')}`,
                borderRadius: 2, p: 4, textAlign: 'center', mb: 2.5, cursor: 'pointer',
                backgroundColor: isDragActive ? (isDark ? 'rgba(0,122,255,0.1)' : 'rgba(0,122,255,0.04)') : 'transparent',
                transition: 'all 200ms',
              }}>
                <input {...getInputProps()} />
                <Upload sx={{ fontSize: 36, color: isDragActive ? '#007AFF' : 'text.disabled', mb: 0.5 }} />
                <Typography sx={{ fontSize: 15, color: isDragActive ? '#007AFF' : 'text.secondary' }}>
                  拖拽文件到此处或点击上传
                </Typography>
                <Typography sx={{ fontSize: 12, color: 'text.secondary', mt: 0.5 }}>支持 PDF, TXT, MD, DOCX</Typography>
              </Box>
              {uploadMutation.isPending && <LinearProgress sx={{ mb: 2 }} />}
              {rebuildMutation.isPending && <LinearProgress color="secondary" sx={{ mb: 2 }} />}

              {/* Search */}
              <Box sx={{ mb: 2.5 }}>
                <IOSSearchBar value={searchQuery} onChange={setSearchQuery} onSearch={handleSearch} placeholder="搜索知识库内容..." />
              </Box>
              {searchResults.length > 0 && (
                <Box sx={{
                  backgroundColor: isDark ? 'rgba(118,118,128,0.12)' : 'rgba(118,118,128,0.06)',
                  borderRadius: 2, p: 2, mb: 2.5, maxHeight: 240, overflow: 'auto',
                }}>
                  {searchResults.map((r, i) => (
                    <Box key={i} sx={{
                      py: 1.5,
                      borderBottom: i < searchResults.length - 1 ? `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)'}` : 'none',
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
                backgroundColor: isDark ? 'rgba(118,118,128,0.12)' : 'rgba(118,118,128,0.06)',
                borderRadius: 2, overflow: 'hidden',
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
                      borderBottom: i < documents.length - 1 ? `0.5px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'}` : 'none',
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
                      <IOSStatusBadge label={d.status} status={statusType(d.status)} dot />
                      <IconButton size="small" onClick={() => deleteDocMutation.mutate(d.id)} sx={{ color: 'text.secondary', '&:hover': { color: '#FF3B30' } }}>
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
      <IOSActionSheet
        open={!!actionSheetKb}
        onClose={() => setActionSheetKb(null)}
        title={actionSheetKb?.name}
        actions={[
          { label: '重建向量库', onClick: () => actionSheetKb && rebuildMutation.mutate(actionSheetKb.id), color: 'primary' },
          { label: '删除', onClick: () => actionSheetKb && deleteMutation.mutate(actionSheetKb.id), color: 'error' },
        ]}
      />

      <CreateKnowledgeBaseDialog open={createOpen} loading={createMutation.isPending} onClose={() => setCreateOpen(false)} onSubmit={handleCreate} />

      <GitImportDialog
        open={gitOpen}
        loading={gitImportMutation.isPending}
        onClose={() => setGitOpen(false)}
        onSubmit={(gitRepoUrl) => gitImportMutation.mutate(gitRepoUrl)}
      />

      {/* Rebuild confirm */}
      <Dialog open={rebuildOpen} onClose={() => !rebuildMutation.isPending && setRebuildOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>重建向量库</DialogTitle>
        <DialogContent sx={{ pt: '16px !important' }}>
          <Typography sx={{ fontSize: 15, color: 'text.secondary' }}>
            将删除当前知识库已有向量，并基于已保存的文本分片重新向量化。不会删除文档记录。
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRebuildOpen(false)} disabled={rebuildMutation.isPending}>取消</Button>
          <Button variant="contained" color="secondary" onClick={() => selectedKb && rebuildMutation.mutate(selectedKb.id)} disabled={rebuildMutation.isPending} sx={{ borderRadius: 8 }}>
            开始重建
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
