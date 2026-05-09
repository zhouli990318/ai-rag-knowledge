import { useEffect, useState, lazy, Suspense } from 'react';
import {
  Box, Typography, Button, TextField, IconButton,
  LinearProgress, useMediaQuery,
  Tooltip, CircularProgress,
} from '@mui/material';
import {
  AddOutlined as Add, DeleteOutlined as Delete, PlayArrowOutlined as PlayArrow,
  EditOutlined as Edit, ContentCopyOutlined as ContentCopy, RefreshOutlined as Refresh,
  ApiOutlined as Api, CheckCircle,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mcpGatewayApi, toolIndexApi } from '@/entities/mcp/api/mcpApi';
import type { McpApiSource, McpToolMapping } from '@/entities/mcp/model/types';
import { useSnackbar } from 'notistack';
import { InkSegmentedControl, InkBadge, InkEmptyState, InkSwitch } from '@/shared/ui/ink';
import { motion } from 'framer-motion';
import { methodColors, ui } from '@/shared/theme/semanticColors';
import {
  MonacoEditor, MobileTextarea,
  CreateEditSourceDialog, EditToolDialog, InvokeToolDrawer,
  parseParameterRows, buildParameterSchema, DEFAULT_PARAMETER_SCHEMA,
} from '@/widgets/mcp';
import type { SourceFormState, ToolUpdatePayload } from '@/widgets/mcp';

const METHOD_COLORS = methodColors;

export default function McpPage() {
  const isMobile = useMediaQuery('(max-width:899px)');
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [selectedSource, setSelectedSource] = useState<McpApiSource | null>(null);
  const [toolToInvoke, setToolToInvoke] = useState<McpToolMapping | null>(null);
  const [parseMode, setParseMode] = useState<string | number>('paste');
  const [specContent, setSpecContent] = useState('');
  const [specUrl, setSpecUrl] = useState('');
  const [editingSource, setEditingSource] = useState<McpApiSource | null>(null);
  const [editingTool, setEditingTool] = useState<McpToolMapping | null>(null);

  const { data: sources = [], isLoading } = useQuery({ queryKey: ['mcp-sources'], queryFn: mcpGatewayApi.listSources });
  const { data: connectionInfo } = useQuery({
    queryKey: ['mcp-source-connection-info', selectedSource?.id],
    queryFn: () => mcpGatewayApi.getSourceConnectionInfo(selectedSource!.id),
    enabled: !!selectedSource,
  });
  const { data: tools = [], refetch: refetchTools } = useQuery({
    queryKey: ['mcp-tools', selectedSource?.id],
    queryFn: () => mcpGatewayApi.getTools(selectedSource!.id),
    enabled: !!selectedSource,
  });

  // Mutations
  const triggerReindex = () => { toolIndexApi.reindex().catch(() => {}); };
  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => mcpGatewayApi.createSource(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      setCreateOpen(false);
      setEditingSource(null);
      enqueueSnackbar('创建成功', { variant: 'success' });
      triggerReindex();
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '创建失败', { variant: 'error' }),
  });
  const updateSourceMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateSource(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      setCreateOpen(false);
      setEditingSource(null);
      enqueueSnackbar('更新成功', { variant: 'success' });
      triggerReindex();
    },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
  const deleteMutation = useMutation({
    mutationFn: mcpGatewayApi.deleteSource,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['mcp-sources'] }); setSelectedSource(null); enqueueSnackbar('删除成功', { variant: 'success' }); triggerReindex(); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
  const parseMutation = useMutation({
    mutationFn: (data: { openApiSpec?: string; openApiUrl?: string }) => mcpGatewayApi.parseSpec(selectedSource!.id, data),
    onSuccess: () => { refetchTools(); enqueueSnackbar('解析完成', { variant: 'success' }); triggerReindex(); },
    onError: () => enqueueSnackbar('解析失败', { variant: 'error' }),
  });
  const toggleToolMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) => mcpGatewayApi.updateTool(id, { enabled }),
    onSuccess: () => { refetchTools(); enqueueSnackbar('工具状态已更新', { variant: 'success' }); triggerReindex(); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
  const updateToolMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateTool(id, data),
    onSuccess: () => { refetchTools(); setEditingTool(null); enqueueSnackbar('已更新', { variant: 'success' }); triggerReindex(); },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
  const toggleSourceMutation = useMutation({
    mutationFn: mcpGatewayApi.toggleSourceActive,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      queryClient.invalidateQueries({ queryKey: ['mcp-health'] });
      enqueueSnackbar('源状态已切换', { variant: 'success' });
      triggerReindex();
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
  const healthCheckMutation = useMutation({
    mutationFn: mcpGatewayApi.triggerHealthCheck,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-health'] });
      enqueueSnackbar('健康检查已完成', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '检查失败', { variant: 'error' }),
  });
  const { data: sourceHealth = [] } = useQuery({
    queryKey: ['mcp-health'],
    queryFn: mcpGatewayApi.listSourcesHealth,
    refetchInterval: 30000,
    refetchIntervalInBackground: false,
  });
  const healthMap = new Map(sourceHealth.map((h) => [h.id, h]));

  const openCreateDialog = () => {
    setEditingSource(null);
    setCreateOpen(true);
  };
  const openEditSourceDialog = (source: McpApiSource) => {
    setEditingSource(source);
    setCreateOpen(true);
  };
  const handleSourceSubmit = (payload: SourceFormState) => {
    const normalizedPayload = {
      ...payload,
      name: payload.name.trim(),
      description: payload.description.trim(),
      baseUrl: payload.baseUrl.trim(),
      authConfig: payload.authConfig.trim(),
    };
    if (editingSource) {
      updateSourceMutation.mutate({ id: editingSource.id, data: normalizedPayload });
      return;
    }
    createMutation.mutate(normalizedPayload);
  };

  const copyText = async (value: string, label: string) => {
    try { await navigator.clipboard.writeText(value); enqueueSnackbar(`${label} 已复制`, { variant: 'success' }); }
    catch { enqueueSnackbar('复制失败', { variant: 'error' }); }
  };

  const CodeEditor = isMobile ? MobileTextarea : MonacoEditor;

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 }, pt: { xs: 7, md: 7 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5">MCP 网关</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog} sx={{ borderRadius: 4 }}>添加 API 源</Button>
      </Box>
      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {/* Source grid — macOS System Preferences style */}
      {sources.length === 0 && !isLoading ? (
        <InkEmptyState icon={<Api />} title="暂无 API 源" subtitle="添加 API 源来配置 MCP 工具映射" action={{ label: '添加 API 源', onClick: openCreateDialog }} />
      ) : (
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2.5 }}>
          {sources.map((s: McpApiSource) => {
            const isActive = selectedSource?.id === s.id;
            const health = healthMap.get(s.id);
            const status = health?.healthStatus ?? 'UNKNOWN';
            const healthLabel =
              !s.active ? '停用' :
              status === 'HEALTHY' ? '健康' :
              status === 'DEGRADED' ? '缓慢' :
              status === 'UNREACHABLE' ? '不可达' : '未知';
            const healthBadgeStatus: 'success' | 'warning' | 'error' | 'default' =
              !s.active ? 'default' :
              status === 'HEALTHY' ? 'success' :
              status === 'DEGRADED' ? 'warning' :
              status === 'UNREACHABLE' ? 'error' : 'default';
            return (
              <motion.div key={s.id} whileTap={{ scale: 0.97 }}>
                <Box
                  onClick={() => setSelectedSource(s)}
                  sx={{
                    width: 140, textAlign: 'center', cursor: 'pointer',
                    p: 2, borderRadius: 3,
                    backgroundColor: isActive ? 'rgba(200,75,49,0.08)' : 'transparent',
                    border: isActive ? `2px solid ${ui.accentRed}` : '2px solid transparent',
                    transition: 'all 200ms',
                    position: 'relative',
                    '&:hover': { backgroundColor: 'rgba(0,0,0,0.02)' },
                  }}
                >
                  {/* 源级启停 Switch */}
                  <InkSwitch
                    size="small"
                    checked={s.active}
                    onClick={(e) => e.stopPropagation()}
                    onChange={() => toggleSourceMutation.mutate(s.id)}
                    sx={{ position: 'absolute', top: 4, right: 4 }}
                  />
                  <Box sx={{
                    width: 52, height: 52, borderRadius: 2.5, mx: 'auto', mb: 1,
                    background: s.active ? 'linear-gradient(135deg, #4A4A4A, #2C2C2C)' : 'rgba(118,118,128,0.12)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Api sx={{ fontSize: 24, color: s.active ? '#fff' : 'text.secondary' }} />
                  </Box>
                  <Typography sx={{ fontSize: 13, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {s.name}
                  </Typography>
                  <InkBadge label={healthLabel} status={healthBadgeStatus} dot />
                </Box>
              </motion.div>
            );
          })}
          {/* Add card */}
          <Box
            onClick={openCreateDialog}
            sx={{
              width: 140, textAlign: 'center', cursor: 'pointer',
              p: 2, borderRadius: 3,
              border: `2px dashed ${ui.border}`,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              '&:hover': { borderColor: ui.accentRed },
              transition: 'border-color 200ms',
            }}
          >
            <Add sx={{ fontSize: 28, color: 'text.secondary', mb: 0.5 }} />
            <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>添加</Typography>
          </Box>
        </Box>
      )}

      {/* Selected source detail */}
      {selectedSource && (
        <Box sx={{
          backgroundColor: ui.cardBg,
          borderRadius: 3, p: 2.5,
          border: `1px solid ${ui.border}`,
          boxShadow: '0 2px 16px rgba(0,0,0,0.06)',
        }}>
          {/* Source header */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box>
              <Typography variant="h6">{selectedSource.name}</Typography>
              <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{selectedSource.description}</Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <IconButton size="small" onClick={() => openEditSourceDialog(selectedSource)}><Edit fontSize="small" /></IconButton>
              <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(selectedSource.id)}><Delete fontSize="small" /></IconButton>
            </Box>
          </Box>

          {/* Connection info */}
          {connectionInfo && (
            <Box sx={{
              backgroundColor: ui.hoverBg,
              borderRadius: 2, p: 2, mb: 2.5,
              border: `1px solid ${ui.border}`,
            }}>
              <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.secondary', mb: 1 }}>
                {connectionInfo.serverName} {connectionInfo.version}
              </Typography>
              {[
                { label: 'SSE', value: connectionInfo.sseUrl },
                { label: 'HTTP', value: connectionInfo.streamableHttpUrl },
              ].map((item) => (
                <Box key={item.label} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.75 }}>
                  <Typography sx={{ fontSize: 11, fontWeight: 600, color: 'text.secondary', width: 36 }}>{item.label}</Typography>
                  <Typography sx={{ fontSize: 13, fontFamily: 'monospace', color: 'text.secondary', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.value}
                  </Typography>
                  <Tooltip title="复制">
                    <IconButton size="small" onClick={() => copyText(item.value, item.label)} sx={{ '&:active .copy-check': { display: 'block' } }}>
                      <ContentCopy sx={{ fontSize: 14 }} />
                    </IconButton>
                  </Tooltip>
                </Box>
              ))}
            </Box>
          )}

          {/* Parse OpenAPI */}
          <Box sx={{ mb: 2.5 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography sx={{ fontSize: 15, fontWeight: 600 }}>解析 OpenAPI</Typography>
              <InkSegmentedControl value={parseMode} onChange={setParseMode} options={[{ value: 'paste', label: '粘贴 Spec' }, { value: 'url', label: 'URL 导入' }]} />
            </Box>
            {parseMode === 'paste' ? (
              <Box>
                <CodeEditor value={specContent} onChange={setSpecContent} height={180} />
                <Button variant="outlined" startIcon={<Refresh />} onClick={() => parseMutation.mutate({ openApiSpec: specContent })} disabled={parseMutation.isPending} sx={{ mt: 1, borderRadius: 4 }}>
                  解析
                </Button>
              </Box>
            ) : (
              <Box sx={{ display: 'flex', gap: 1 }}>
                <TextField size="small" fullWidth value={specUrl} onChange={(e) => setSpecUrl(e.target.value)} placeholder="https://api.example.com/v3/openapi.json" />
                <Button variant="outlined" onClick={() => parseMutation.mutate({ openApiUrl: specUrl })} disabled={parseMutation.isPending} sx={{ borderRadius: 4 }}>导入</Button>
              </Box>
            )}
          </Box>

          {/* Tools list — iOS grouped list */}
          <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: 0.5, mb: 1 }}>
            工具 ({tools.length})
          </Typography>
          <Box sx={{
            backgroundColor: ui.hoverBg,
            borderRadius: 2, overflow: 'hidden',
            border: `1px solid ${ui.border}`,
          }}>
            {tools.length === 0 ? (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography sx={{ fontSize: 15, color: 'text.secondary' }}>暂无工具，请先解析 OpenAPI 规范</Typography>
              </Box>
            ) : (
              tools.map((t: McpToolMapping, i: number) => (
                <Box key={t.id} sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: 2, py: 1.5,
                  borderBottom: i < tools.length - 1 ? `0.5px solid ${ui.border}` : 'none',
                }}>
                  {/* Method badge */}
                  <Box sx={{
                    px: 0.75, py: 0.25, borderRadius: 1,
                    backgroundColor: `${METHOD_COLORS[t.httpMethod] || '#4A4A4A'}18`,
                    flexShrink: 0,
                  }}>
                    <Typography sx={{ fontSize: 10, fontWeight: 700, fontFamily: 'monospace', color: METHOD_COLORS[t.httpMethod] || '#4A4A4A' }}>
                      {t.httpMethod}
                    </Typography>
                  </Box>
                  {/* Tool info */}
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography sx={{ fontSize: 14, fontWeight: 500, fontFamily: 'monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {t.toolName}
                    </Typography>
                    <Typography sx={{ fontSize: 12, color: 'text.secondary', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {t.path}
                    </Typography>
                  </Box>
                  {/* Toggle */}
                  <InkSwitch checked={t.enabled} onChange={(_, v) => toggleToolMutation.mutate({ id: t.id, enabled: v })} />
                  {/* Actions */}
                  <IconButton size="small" onClick={() => setEditingTool(t)}><Edit sx={{ fontSize: 18 }} /></IconButton>
                  <IconButton size="small" onClick={() => setToolToInvoke(t)}><PlayArrow sx={{ fontSize: 18 }} /></IconButton>
                </Box>
              ))
            )}
          </Box>
        </Box>
      )}

      <CreateEditSourceDialog
        open={createOpen}
        initialSource={editingSource}
        loading={createMutation.isPending || updateSourceMutation.isPending}
        onClose={() => {
          setCreateOpen(false);
          setEditingSource(null);
        }}
        onSubmit={handleSourceSubmit}
      />

      <EditToolDialog
        open={editingTool !== null}
        tool={editingTool}
        EditorComponent={CodeEditor}
        loading={updateToolMutation.isPending}
        onClose={() => setEditingTool(null)}
        onSubmit={(payload) => editingTool && updateToolMutation.mutate({ id: editingTool.id, data: payload })}
      />

      <InvokeToolDrawer
        open={toolToInvoke !== null}
        toolId={toolToInvoke?.id || null}
        EditorComponent={CodeEditor}
        onClose={() => setToolToInvoke(null)}
      />
    </Box>
  );
}
