import { useEffect, useState } from 'react';
import {
  Box, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, IconButton, Switch, Select, MenuItem,
  FormControl, InputLabel, LinearProgress, useTheme,
} from '@mui/material';
import { Add, Delete, CheckCircle, Edit, ChevronRight, Dns } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApi } from '../api/providerApi';
import { Provider, ProviderType } from '../api/types';
import { useSnackbar } from 'notistack';
import { IOSEmptyState, IOSStatusBadge } from '../components/ios';
import { motion } from 'framer-motion';

// Brand colors for provider types
const providerColors: Record<string, string> = {
  OPENAI: '#10A37F',
  ANTHROPIC: '#D97757',
  DASHSCOPE: '#6366f1',
  OLLAMA: '#7C3AED',
  ZHIPU: '#2563EB',
};

type ProviderFormState = {
  name: string;
  providerType: string;
  apiKey: string;
  baseUrl: string;
  defaultModel: string;
  embeddingModel: string;
  embeddingDimensions: number;
};

function ProviderDialog({
  open,
  providerTypes,
  initialProvider,
  loading,
  onClose,
  onSubmit,
}: {
  open: boolean;
  providerTypes: ProviderType[];
  initialProvider: Provider | null;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: ProviderFormState) => void;
}) {
  const [form, setForm] = useState<ProviderFormState>({
    name: '',
    providerType: 'OPENAI',
    apiKey: '',
    baseUrl: '',
    defaultModel: '',
    embeddingModel: '',
    embeddingDimensions: 1536,
  });

  useEffect(() => {
    if (!open) {
      return;
    }

    if (initialProvider) {
      setForm({
        name: initialProvider.name,
        providerType: initialProvider.providerType,
        apiKey: '',
        baseUrl: initialProvider.baseUrl || '',
        defaultModel: initialProvider.defaultModel || '',
        embeddingModel: initialProvider.embeddingModel || '',
        embeddingDimensions: initialProvider.embeddingDimensions || 1536,
      });
      return;
    }

    const defaultType = providerTypes[0];
    setForm({
      name: '',
      providerType: defaultType?.type || 'OPENAI',
      apiKey: '',
      baseUrl: defaultType?.defaultBaseUrl || '',
      defaultModel: '',
      embeddingModel: '',
      embeddingDimensions: 1536,
    });
  }, [initialProvider, open, providerTypes]);

  const selectedType = providerTypes.find((type) => type.type === form.providerType);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initialProvider ? '编辑供应商' : '添加供应商'}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
        <TextField label="名称" required value={form.name} onChange={(e) => setForm((current) => ({ ...current, name: e.target.value }))} />
        <FormControl disabled={!!initialProvider}>
          <InputLabel>供应商类型</InputLabel>
          <Select
            value={form.providerType}
            onChange={(e) => {
              const providerType = String(e.target.value);
              const typeConfig = providerTypes.find((item) => item.type === providerType);
              setForm((current) => ({ ...current, providerType, baseUrl: typeConfig?.defaultBaseUrl || '' }));
            }}
            label="供应商类型"
          >
            {providerTypes.map((type) => (
              <MenuItem key={type.type} value={type.type}>
                {type.displayName} {type.openAiCompatible && '(OpenAI 兼容)'}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <TextField label="API Key" type="password" value={form.apiKey} onChange={(e) => setForm((current) => ({ ...current, apiKey: e.target.value }))} placeholder={initialProvider ? '留空则不修改' : ''} />
        <TextField label="Base URL" value={form.baseUrl} onChange={(e) => setForm((current) => ({ ...current, baseUrl: e.target.value }))} helperText={selectedType ? `默认: ${selectedType.defaultBaseUrl}` : ''} />
        <TextField label="默认模型" value={form.defaultModel} onChange={(e) => setForm((current) => ({ ...current, defaultModel: e.target.value }))} helperText="如 gpt-4o, deepseek-chat, qwen-plus" />
        <TextField label="嵌入模型" value={form.embeddingModel} onChange={(e) => setForm((current) => ({ ...current, embeddingModel: e.target.value }))} helperText="如 text-embedding-3-small（可选）" />
        <TextField type="number" label="嵌入维度" value={form.embeddingDimensions} onChange={(e) => setForm((current) => ({ ...current, embeddingDimensions: Number(e.target.value) }))} />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 8 }}>
          {initialProvider ? '保存' : '创建'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function SettingsPage() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);

  const { data: providers = [], isLoading } = useQuery({ queryKey: ['providers'], queryFn: providerApi.list });
  const { data: providerTypes = [] } = useQuery({ queryKey: ['providerTypes'], queryFn: providerApi.types });

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => providerApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      setCreateOpen(false);
      setEditingProvider(null);
      enqueueSnackbar('创建成功', { variant: 'success' });
    },
    onError: () => enqueueSnackbar('创建失败', { variant: 'error' }),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => providerApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      setCreateOpen(false); setEditingProvider(null);
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

  const openCreateDialog = () => {
    setEditingProvider(null);
    setCreateOpen(true);
  };

  const openEditDialog = (p: Provider) => {
    setEditingProvider(p);
    setCreateOpen(true);
  };

  const handleSubmit = (form: ProviderFormState) => {
    const normalizedPayload = {
      ...form, apiKey: form.apiKey.trim(), name: form.name.trim(),
      baseUrl: form.baseUrl.trim(), defaultModel: form.defaultModel.trim(),
      embeddingModel: form.embeddingModel.trim(),
    };
    if (editingProvider) { updateMutation.mutate({ id: editingProvider.id, data: normalizedPayload }); return; }
    createMutation.mutate(normalizedPayload);
  };

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5">AI 供应商</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog} sx={{ borderRadius: 8 }}>添加</Button>
      </Box>

      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {/* Providers list — iOS Settings style */}
      {providers.length === 0 && !isLoading ? (
        <IOSEmptyState icon={<Dns />} title="暂无供应商" subtitle="添加 AI 供应商来开始对话" action={{ label: '添加供应商', onClick: openCreateDialog }} />
      ) : (
        <Box sx={{
          backgroundColor: isDark ? '#1C1C1E' : '#FFFFFF',
          borderRadius: 3, overflow: 'hidden',
          boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.3)' : '0 1px 8px rgba(0,0,0,0.05)',
        }}>
          {providers.map((p: Provider, i: number) => {
            const brandColor = providerColors[p.providerType] || '#007AFF';
            return (
              <motion.div key={p.id} whileTap={{ scale: 0.98 }}>
                <Box sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: 2, py: 1.5,
                  borderBottom: i < providers.length - 1 ? `0.5px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'}` : 'none',
                  cursor: 'pointer',
                  '&:active': { backgroundColor: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)' },
                }}
                onClick={() => openEditDialog(p)}
                >
                  {/* Brand icon */}
                  <Box sx={{
                    width: 36, height: 36, borderRadius: 2, flexShrink: 0,
                    background: `linear-gradient(135deg, ${brandColor}, ${brandColor}BB)`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Typography sx={{ fontSize: 14, fontWeight: 700, color: '#fff' }}>
                      {p.providerType.slice(0, 2)}
                    </Typography>
                  </Box>

                  {/* Info */}
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography sx={{ fontSize: 17, fontWeight: 500 }}>{p.name}</Typography>
                    <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>
                      {p.defaultModel || p.providerType}
                      {p.baseUrl && ` · ${new URL(p.baseUrl).host}`}
                    </Typography>
                  </Box>

                  {/* Actions */}
                  <IconButton size="small" onClick={(e) => { e.stopPropagation(); testMutation.mutate(p.id); }}
                    sx={{ color: testMutation.isPending ? 'text.secondary' : '#34C759' }}>
                    <CheckCircle sx={{ fontSize: 20 }} />
                  </IconButton>

                  <Switch
                    checked={p.enabled}
                    onChange={(e) => { e.stopPropagation(); toggleMutation.mutate(p.id); }}
                    onClick={(e) => e.stopPropagation()}
                  />

                  <IconButton size="small" onClick={(e) => { e.stopPropagation(); deleteMutation.mutate(p.id); }}
                    sx={{ color: 'text.secondary', '&:hover': { color: '#FF3B30' } }}>
                    <Delete sx={{ fontSize: 18 }} />
                  </IconButton>
                </Box>
              </motion.div>
            );
          })}
        </Box>
      )}

      <ProviderDialog
        open={createOpen}
        providerTypes={providerTypes}
        initialProvider={editingProvider}
        loading={createMutation.isPending || updateMutation.isPending}
        onClose={() => {
          setCreateOpen(false);
          setEditingProvider(null);
        }}
        onSubmit={handleSubmit}
      />
    </Box>
  );
}
