import { useEffect, useState } from 'react';
import {
  Box, Typography, Button, IconButton, LinearProgress,
} from '@mui/material';
import { AddOutlined as Add, DeleteOutlined as Delete, CheckCircle, EditOutlined as Edit, ChevronRightOutlined as ChevronRight, DnsOutlined as Dns } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApi } from '@/entities/provider/api/providerApi';
import type { Provider, ProviderType } from '@/entities/provider/model/types';
import { useSnackbar } from 'notistack';
import { InkEmptyState, InkSwitch } from '@/shared/ui/ink';
import { motion } from 'framer-motion';
import { ink, serifFont, radius, useInk } from '@/shared/theme/ThemeProvider';
import { providerColors } from '@/shared/theme/semanticColors';
import { ProviderDialog } from '@/widgets/settings';
import type { ProviderFormState } from '@/widgets/settings';

export default function SettingsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const di = useInk();
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
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['providers'] }); enqueueSnackbar('删除成功', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
  const toggleMutation = useMutation({
    mutationFn: providerApi.toggle,
    onSuccess: (data) => { queryClient.invalidateQueries({ queryKey: ['providers'] }); enqueueSnackbar(data.enabled ? '已启用' : '已停用', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
  const validateMutation = useMutation({
    mutationFn: providerApi.validate,
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
    <Box sx={{ p: { xs: 2, md: 2.5 }, pt: { xs: 7, md: 7 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5" sx={{ fontFamily: serifFont, color: di.black }}>AI 供应商</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog} sx={{ borderRadius: radius.sm }}>添加</Button>
      </Box>

      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {/* Providers list */}
      {providers.length === 0 && !isLoading ? (
        <InkEmptyState icon={<Dns />} title="暂无供应商" subtitle="添加 AI 供应商来开始对话" action={{ label: '添加供应商', onClick: openCreateDialog }} />
      ) : (
        <Box sx={{
          backgroundColor: di.glassBg,
          backdropFilter: 'blur(12px) saturate(180%)',
          WebkitBackdropFilter: 'blur(12px) saturate(180%)',
          borderRadius: `${radius.md + 2}px`,
          overflow: 'hidden',
          border: `1px solid ${di.glassBorder}`,
          boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
        }}>
          {providers.map((p: Provider, i: number) => {
            const brandColor = providerColors[p.providerType] || di.gray;
            return (
              <motion.div key={p.id} whileTap={{ scale: 0.98 }}>
                <Box sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: 2, py: 1.5,
                  borderBottom: i < providers.length - 1 ? `0.5px solid ${di.glassBorder}` : 'none',
                  cursor: 'pointer',
                  transition: 'background-color 150ms ease-in-out',
                  '&:hover': { backgroundColor: 'rgba(74,74,74,0.02)' },
                  '&:active': { backgroundColor: 'rgba(74,74,74,0.04)' },
                }}
                onClick={() => openEditDialog(p)}
                >
                  {/* Brand icon */}
                  <Box sx={{
                    width: 36, height: 36, borderRadius: radius.sm, flexShrink: 0,
                    background: `linear-gradient(135deg, ${brandColor}, ${brandColor}BB)`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Typography sx={{ fontSize: 14, fontWeight: 700, color: '#fff' }}>
                      {p.providerType.slice(0, 2)}
                    </Typography>
                  </Box>

                  {/* Info */}
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography sx={{ fontSize: 15.5, fontWeight: 500, color: di.black }}>{p.name}</Typography>
                    <Typography sx={{ fontSize: 13, color: di.lightGray }}>
                      {p.defaultModel || p.providerType}
                      {p.baseUrl && (() => { try { return ` · ${new URL(p.baseUrl).host}`; } catch { return ''; } })()}
                    </Typography>
                  </Box>

                  {/* Actions — wrapper stops pointer events from bubbling to motion.div / row onClick */}
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
                    onClick={(e) => e.stopPropagation()}
                    onPointerDown={(e) => e.stopPropagation()}
                  >
                    <IconButton size="small" onClick={() => validateMutation.mutate(p.id)}
                      sx={{ color: validateMutation.isPending ? di.lightGray : di.teal }}>
                      <CheckCircle sx={{ fontSize: 20 }} />
                    </IconButton>

                    <InkSwitch
                      checked={p.enabled}
                      onChange={() => toggleMutation.mutate(p.id)}
                    />

                    <IconButton size="small" onClick={() => deleteMutation.mutate(p.id)}
                      sx={{ color: di.lightGray, '&:hover': { color: di.cinnabar } }}>
                      <Delete sx={{ fontSize: 18 }} />
                    </IconButton>
                  </Box>
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
