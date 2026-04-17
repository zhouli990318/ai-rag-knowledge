import { useState } from 'react';
import {
  Box, Paper, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Table, TableHead, TableBody, TableRow,
  TableCell, IconButton, Chip, Switch, Select, MenuItem,
  FormControl, InputLabel, LinearProgress,
} from '@mui/material';
import { Add, Delete, CheckCircle, Edit } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApi } from '../api/providerApi';
import { Provider, ProviderType } from '../api/types';
import { useSnackbar } from 'notistack';

export default function SettingsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editingProviderId, setEditingProviderId] = useState<number | null>(null);
  const [form, setForm] = useState({
    name: '', providerType: 'OPENAI', apiKey: '', baseUrl: '', defaultModel: '',
    embeddingModel: '', embeddingDimensions: 1536,
  });

  const { data: providers = [], isLoading } = useQuery({ queryKey: ['providers'], queryFn: providerApi.list });
  const { data: providerTypes = [] } = useQuery({ queryKey: ['providerTypes'], queryFn: providerApi.types });

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => providerApi.create(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['providers'] }); setCreateOpen(false); enqueueSnackbar('创建成功', { variant: 'success' }); },
    onError: () => enqueueSnackbar('创建失败', { variant: 'error' }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => providerApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      setCreateOpen(false);
      setEditingProviderId(null);
      enqueueSnackbar('更新成功', { variant: 'success' });
    },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });

  const deleteMutation = useMutation({
    mutationFn: providerApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['providers'] }),
  });

  const toggleMutation = useMutation({
    mutationFn: providerApi.toggle,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['providers'] }),
  });

  const testMutation = useMutation({
    mutationFn: providerApi.test,
    onSuccess: (data) => enqueueSnackbar(`连接成功: ${data}`, { variant: 'success' }),
    onError: () => enqueueSnackbar('连接失败', { variant: 'error' }),
  });

  const selectedType = providerTypes.find((t: ProviderType) => t.type === form.providerType);

  const resetForm = () => {
    setForm({
      name: '', providerType: 'OPENAI', apiKey: '', baseUrl: '', defaultModel: '',
      embeddingModel: '', embeddingDimensions: 1536,
    });
    setEditingProviderId(null);
  };

  const openCreateDialog = () => {
    resetForm();
    setCreateOpen(true);
  };

  const openEditDialog = (provider: Provider) => {
    setForm({
      name: provider.name,
      providerType: provider.providerType,
      apiKey: '',
      baseUrl: provider.baseUrl || '',
      defaultModel: provider.defaultModel || '',
      embeddingModel: provider.embeddingModel || '',
      embeddingDimensions: provider.embeddingDimensions || 1536,
    });
    setEditingProviderId(provider.id);
    setCreateOpen(true);
  };

  const handleSubmit = () => {
    const payload = {
      ...form,
      apiKey: form.apiKey.trim(),
      name: form.name.trim(),
      baseUrl: form.baseUrl.trim(),
      defaultModel: form.defaultModel.trim(),
      embeddingModel: form.embeddingModel.trim(),
    };

    if (editingProviderId !== null) {
      updateMutation.mutate({ id: editingProviderId, data: payload });
      return;
    }

    createMutation.mutate(payload);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>AI 供应商管理</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog}>添加供应商</Button>
      </Box>

      {isLoading && <LinearProgress />}

      <Paper>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>名称</TableCell>
              <TableCell>类型</TableCell>
              <TableCell>Base URL</TableCell>
              <TableCell>默认模型</TableCell>
              <TableCell>嵌入模型</TableCell>
              <TableCell>状态</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {providers.map((p: Provider) => (
              <TableRow key={p.id}>
                <TableCell>{p.name}</TableCell>
                <TableCell><Chip label={p.providerType} size="small" variant="outlined" /></TableCell>
                <TableCell><Typography variant="body2" noWrap sx={{ maxWidth: 200 }}>{p.baseUrl}</Typography></TableCell>
                <TableCell>{p.defaultModel}</TableCell>
                <TableCell>{p.embeddingModel || '-'}</TableCell>
                <TableCell>
                  <Switch checked={p.enabled} onChange={() => toggleMutation.mutate(p.id)} size="small" />
                </TableCell>
                <TableCell>
                  <IconButton size="small" color="primary" onClick={() => openEditDialog(p)}>
                    <Edit />
                  </IconButton>
                  <IconButton size="small" color="primary" onClick={() => testMutation.mutate(p.id)}>
                    <CheckCircle />
                  </IconButton>
                  <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(p.id)}>
                    <Delete />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingProviderId !== null ? '编辑 AI 供应商' : '添加 AI 供应商'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="名称" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <FormControl disabled={editingProviderId !== null}>
            <InputLabel>供应商类型</InputLabel>
            <Select value={form.providerType} onChange={(e) => {
              const providerType = String(e.target.value);
              const typeConfig = providerTypes.find((pt: ProviderType) => pt.type === providerType);
              setForm({
                ...form,
                providerType,
                baseUrl: typeConfig?.defaultBaseUrl || '',
              });
            }} label="供应商类型">
              {providerTypes.map((t: ProviderType) => (
                <MenuItem key={t.type} value={t.type}>
                  {t.displayName} {t.openAiCompatible && '(OpenAI 兼容)'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField label="API Key" type="password" value={form.apiKey} onChange={(e) => setForm({ ...form, apiKey: e.target.value })}
            placeholder={editingProviderId !== null ? '留空则不修改' : ''} />
          <TextField label="Base URL" value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
            helperText={selectedType ? `默认: ${selectedType.defaultBaseUrl}` : ''} />
          <TextField label="默认模型" value={form.defaultModel} onChange={(e) => setForm({ ...form, defaultModel: e.target.value })}
            helperText="如 gpt-4o, deepseek-chat, qwen-plus" />
          <TextField label="嵌入模型" value={form.embeddingModel} onChange={(e) => setForm({ ...form, embeddingModel: e.target.value })}
            helperText="如 text-embedding-3-small（可选）" />
          <TextField type="number" label="嵌入维度" value={form.embeddingDimensions}
            onChange={(e) => setForm({ ...form, embeddingDimensions: Number(e.target.value) })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setCreateOpen(false); resetForm(); }}>取消</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={createMutation.isPending || updateMutation.isPending}>
            {editingProviderId !== null ? '保存' : '创建'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
