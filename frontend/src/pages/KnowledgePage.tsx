import { useState } from 'react';
import {
  Box, Paper, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Table, TableHead, TableBody, TableRow,
  TableCell, IconButton, Chip, Card, CardContent, CardActions,
  Grid, LinearProgress,
} from '@mui/material';
import { Add, Autorenew, Delete, Upload, Search, GitHub } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { knowledgeApi } from '../api/knowledgeApi';
import { KnowledgeBase, KbDocument, SearchResult } from '../api/types';
import { useSnackbar } from 'notistack';
import { useDropzone } from 'react-dropzone';

export default function KnowledgePage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [gitUrl, setGitUrl] = useState('');
  const [gitOpen, setGitOpen] = useState(false);
  const [rebuildOpen, setRebuildOpen] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', chunkSize: 800, chunkOverlap: 200 });

  const { data: kbs = [], isLoading } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });
  const { data: documents = [], refetch: refetchDocs } = useQuery({
    queryKey: ['kb-docs', selectedKb?.id],
    queryFn: () => knowledgeApi.listDocuments(selectedKb!.id),
    enabled: !!selectedKb,
  });

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => knowledgeApi.create(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] }); setCreateOpen(false); enqueueSnackbar('创建成功', { variant: 'success' }); },
  });

  const deleteMutation = useMutation({
    mutationFn: knowledgeApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] }); setSelectedKb(null); },
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => knowledgeApi.uploadDocument(selectedKb!.id, file),
    onSuccess: () => { refetchDocs(); enqueueSnackbar('上传成功', { variant: 'success' }); },
    onError: (error: any) => {
      enqueueSnackbar(error?.response?.data?.message || error?.message || '上传失败', { variant: 'error' });
    },
  });

  const deleteDocMutation = useMutation({
    mutationFn: (docId: number) => knowledgeApi.deleteDocument(selectedKb!.id, docId),
    onSuccess: () => refetchDocs(),
  });

  const gitImportMutation = useMutation({
    mutationFn: (url: string) => knowledgeApi.importGit(selectedKb!.id, { repoUrl: url }),
    onSuccess: () => { setGitOpen(false); refetchDocs(); enqueueSnackbar('导入任务已提交', { variant: 'success' }); },
  });

  const rebuildMutation = useMutation({
    mutationFn: (kbId: number) => knowledgeApi.rebuildVectors(kbId),
    onSuccess: (message) => {
      setRebuildOpen(false);
      refetchDocs();
      queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] });
      enqueueSnackbar(message || '向量库重建完成', { variant: 'success' });
    },
    onError: (error: any) => {
      enqueueSnackbar(error?.response?.data?.message || error?.message || '向量库重建失败', { variant: 'error' });
    },
  });

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (files) => files.forEach((f) => uploadMutation.mutate(f)),
    accept: { 'application/pdf': ['.pdf'], 'text/*': ['.txt', '.md', '.csv'], 'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'] },
  });

  const handleSearch = async () => {
    if (!selectedKb || !searchQuery.trim()) return;
    const results = await knowledgeApi.search(selectedKb.id, searchQuery);
    setSearchResults(results);
  };

  const handleCreate = () => {
    if (!form.name.trim()) {
      enqueueSnackbar('请输入知识库名称', { variant: 'warning' });
      return;
    }

    createMutation.mutate({
      name: form.name.trim(),
      description: form.description.trim(),
      chunkSize: form.chunkSize,
      chunkOverlap: form.chunkOverlap,
    });
  };

  const statusColor = (s: string) => {
    switch (s) { case 'INDEXED': return 'success'; case 'PROCESSING': return 'warning'; case 'FAILED': return 'error'; default: return 'default'; }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>知识库管理</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>新建知识库</Button>
      </Box>

      {isLoading && <LinearProgress />}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {kbs.map((kb: KnowledgeBase) => (
          <Grid key={kb.id} item xs={12} sm={6} md={4}>
            <Card sx={{ cursor: 'pointer', border: selectedKb?.id === kb.id ? 2 : 0, borderColor: 'primary.main' }}
              onClick={() => setSelectedKb(kb)}>
              <CardContent>
                <Typography variant="h6" gutterBottom>{kb.name}</Typography>
                <Typography variant="body2" color="text.secondary" noWrap>{kb.description}</Typography>
                <Box sx={{ mt: 1, display: 'flex', gap: 1 }}>
                  <Chip label={`${kb.documentCount} 文档`} size="small" />
                  <Chip label={kb.active ? '活跃' : '停用'} color={kb.active ? 'success' : 'default'} size="small" />
                </Box>
              </CardContent>
              <CardActions>
                <IconButton color="error" onClick={(e) => { e.stopPropagation(); deleteMutation.mutate(kb.id); }}>
                  <Delete />
                </IconButton>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {selectedKb && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>{selectedKb.name} - 文档管理</Typography>

          {/* Upload */}
          <Box {...getRootProps()} sx={{
            border: '2px dashed', borderColor: isDragActive ? 'primary.main' : 'divider',
            borderRadius: 2, p: 3, textAlign: 'center', mb: 2, cursor: 'pointer',
            bgcolor: isDragActive ? 'action.hover' : 'transparent',
          }}>
            <input {...getInputProps()} />
            <Upload sx={{ fontSize: 40, color: 'text.disabled' }} />
            <Typography color="text.secondary">拖拽文件到此处或点击上传 (PDF, TXT, MD, DOCX)</Typography>
          </Box>
          {uploadMutation.isPending && <LinearProgress sx={{ mb: 2 }} />}

          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Button variant="outlined" startIcon={<GitHub />} onClick={() => setGitOpen(true)}>Git 导入</Button>
            <Button
              variant="outlined"
              color="secondary"
              startIcon={<Autorenew />}
              onClick={() => setRebuildOpen(true)}
              disabled={rebuildMutation.isPending}
            >
              重建向量库
            </Button>
          </Box>
          {rebuildMutation.isPending && <LinearProgress color="secondary" sx={{ mb: 2 }} />}

          {/* Search */}
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField size="small" fullWidth placeholder="搜索知识库..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()} />
            <IconButton onClick={handleSearch}><Search /></IconButton>
          </Box>
          {searchResults.length > 0 && (
            <Paper variant="outlined" sx={{ p: 2, mb: 2, maxHeight: 200, overflow: 'auto' }}>
              {searchResults.map((result, index) => (
                <Box key={index} sx={{ mb: 1.5 }}>
                  <Typography variant="body2" sx={{ mb: 0.5 }}>{result.content}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {JSON.stringify(result.metadata)}
                  </Typography>
                </Box>
              ))}
            </Paper>
          )}

          {/* Documents */}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>文件名</TableCell>
                <TableCell>类型</TableCell>
                <TableCell>大小</TableCell>
                <TableCell>状态</TableCell>
                <TableCell>分块数</TableCell>
                <TableCell>操作</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {documents.map((d: KbDocument) => (
                <TableRow key={d.id}>
                  <TableCell>{d.fileName}</TableCell>
                  <TableCell>{d.fileType}</TableCell>
                  <TableCell>{(d.fileSize / 1024).toFixed(1)} KB</TableCell>
                  <TableCell><Chip label={d.status} color={statusColor(d.status) as any} size="small" /></TableCell>
                  <TableCell>{d.chunkCount}</TableCell>
                  <TableCell>
                    <IconButton size="small" color="error" onClick={() => deleteDocMutation.mutate(d.id)}><Delete /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>新建知识库</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="名称" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <TextField label="描述" multiline rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField type="number" label="分块大小" value={form.chunkSize} onChange={(e) => setForm({ ...form, chunkSize: Number(e.target.value) })} />
            <TextField type="number" label="分块重叠" value={form.chunkOverlap} onChange={(e) => setForm({ ...form, chunkOverlap: Number(e.target.value) })} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleCreate}>创建</Button>
        </DialogActions>
      </Dialog>

      {/* Git import dialog */}
      <Dialog open={gitOpen} onClose={() => setGitOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Git 仓库导入</DialogTitle>
        <DialogContent sx={{ pt: '16px !important' }}>
          <TextField fullWidth label="Git 仓库 URL" value={gitUrl} onChange={(e) => setGitUrl(e.target.value)} placeholder="https://github.com/..." />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGitOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => gitImportMutation.mutate(gitUrl)}>导入</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={rebuildOpen} onClose={() => !rebuildMutation.isPending && setRebuildOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>重建向量库</DialogTitle>
        <DialogContent sx={{ pt: '16px !important' }}>
          <Typography variant="body2" color="text.secondary">
            将删除当前知识库已有向量，并基于已保存的文本分片重新向量化。这个操作不会删除文档记录。
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRebuildOpen(false)} disabled={rebuildMutation.isPending}>取消</Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={() => selectedKb && rebuildMutation.mutate(selectedKb.id)}
            disabled={rebuildMutation.isPending}
          >
            开始重建
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
