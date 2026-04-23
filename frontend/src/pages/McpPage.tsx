import { useEffect, useState, lazy, Suspense } from 'react';
import {
  Box, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, IconButton, Select, MenuItem,
  FormControl, InputLabel, LinearProgress, useMediaQuery,
  Tooltip, Drawer, CircularProgress,
} from '@mui/material';
import {
  Add, Delete, PlayArrow, Edit, ContentCopy, Refresh,
  Api, CheckCircle,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mcpGatewayApi } from '../api/mcpApi';
import { McpApiSource, McpToolMapping } from '../api/types';
import { useSnackbar } from 'notistack';
import { InkSegmentedControl, InkBadge, InkEmptyState, InkSwitch } from '../components/ink';
import { motion } from 'framer-motion';

const LazyEditor = lazy(() => import('@monaco-editor/react'));

function MonacoEditor({ value, onChange, height = 180 }: { value: string; onChange: (v: string) => void; height?: number; isDark?: boolean }) {
  return (
    <Box sx={{ height, borderRadius: 3, overflow: 'hidden', border: '1px solid #E0DDD8' }}>
      <Suspense fallback={<Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}><CircularProgress size={24} /></Box>}>
        <LazyEditor height="100%" defaultLanguage="json" value={value} onChange={(v) => onChange(v || '')} theme="light" options={{ minimap: { enabled: false }, fontSize: 13, scrollBeyondLastLine: false }} />
      </Suspense>
    </Box>
  );
}

function MobileTextarea({ value, onChange }: { value: string; onChange: (v: string) => void; height?: number; isDark?: boolean }) {
  return <TextField multiline fullWidth minRows={4} maxRows={10} value={value} onChange={(e) => onChange(e.target.value)} sx={{ fontFamily: 'monospace' }} />;
}

type ParameterRow = {
  key: string; name: string; type: string; description: string; required: boolean;
};

const DEFAULT_PARAMETER_SCHEMA = '{\n  "type": "object",\n  "properties": {},\n  "required": []\n}';

function parseParameterRows(parameterSchema?: string): ParameterRow[] {
  try {
    const parsed = JSON.parse(parameterSchema || DEFAULT_PARAMETER_SCHEMA) as {
      properties?: Record<string, { type?: string; description?: string }>;
      required?: string[];
    };
    const props = parsed.properties || {};
    const req = new Set(parsed.required || []);
    return Object.entries(props).map(([name, v], i) => ({
      key: `${name}-${i}`, name, type: v?.type || 'string', description: v?.description || '', required: req.has(name),
    }));
  } catch { return []; }
}

function buildParameterSchema(rows: ParameterRow[]): string {
  const properties = rows.reduce<Record<string, { type: string; description: string }>>((acc, r) => {
    const n = r.name.trim();
    if (!n) return acc;
    acc[n] = { type: r.type || 'string', description: r.description || '' };
    return acc;
  }, {});
  const required = rows.filter((r) => r.required && r.name.trim()).map((r) => r.name.trim());
  return JSON.stringify({ type: 'object', properties, required }, null, 2);
}

const METHOD_COLORS: Record<string, string> = { GET: '#5B7065', POST: '#4A4A4A', PUT: '#C89B3C', DELETE: '#C84B31', PATCH: '#8B8B8B' };

type SourceFormState = {
  name: string;
  description: string;
  baseUrl: string;
  authType: string;
  authConfig: string;
};

type ToolUpdatePayload = {
  toolName: string;
  toolDescription: string;
  httpMethod: string;
  path: string;
  parameterSchema: string;
  responseSchema: string;
  examplePayload: string;
  enabled: boolean;
};

type CodeEditorComponent = typeof MonacoEditor;

function CreateEditSourceDialog({
  open,
  initialSource,
  loading,
  onClose,
  onSubmit,
}: {
  open: boolean;
  initialSource: McpApiSource | null;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: SourceFormState) => void;
}) {
  const [form, setForm] = useState<SourceFormState>({
    name: '',
    description: '',
    baseUrl: '',
    authType: 'NONE',
    authConfig: '',
  });

  useEffect(() => {
    if (!open) {
      return;
    }

    if (initialSource) {
      setForm({
        name: initialSource.name,
        description: initialSource.description || '',
        baseUrl: initialSource.baseUrl || '',
        authType: initialSource.authType || 'NONE',
        authConfig: '',
      });
      return;
    }

    setForm({ name: '', description: '', baseUrl: '', authType: 'NONE', authConfig: '' });
  }, [initialSource, open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initialSource ? '编辑 API 源' : '添加 API 源'}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
        <TextField label="名称" required value={form.name} onChange={(e) => setForm((current) => ({ ...current, name: e.target.value }))} />
        <TextField label="描述" value={form.description} onChange={(e) => setForm((current) => ({ ...current, description: e.target.value }))} />
        <TextField label="Base URL" value={form.baseUrl} onChange={(e) => setForm((current) => ({ ...current, baseUrl: e.target.value }))} />
        <FormControl>
          <InputLabel>认证方式</InputLabel>
          <Select value={form.authType} onChange={(e) => setForm((current) => ({ ...current, authType: String(e.target.value) }))} label="认证方式">
            <MenuItem value="NONE">无</MenuItem>
            <MenuItem value="API_KEY">API Key</MenuItem>
            <MenuItem value="BEARER_TOKEN">Bearer Token</MenuItem>
            <MenuItem value="BASIC_AUTH">Basic Auth</MenuItem>
          </Select>
        </FormControl>
        {form.authType !== 'NONE' && (
          <TextField label="认证配置" value={form.authConfig} onChange={(e) => setForm((current) => ({ ...current, authConfig: e.target.value }))} helperText="API Key 或 Token" />
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 4 }}>
          {initialSource ? '保存' : '创建'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function EditToolDialog({
  open,
  tool,
  EditorComponent,
  loading,
  onClose,
  onSubmit,
}: {
  open: boolean;
  tool: McpToolMapping | null;
  EditorComponent: CodeEditorComponent;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: ToolUpdatePayload) => void;
}) {
  const [localTool, setLocalTool] = useState<McpToolMapping | null>(tool);
  const [parameterEditMode, setParameterEditMode] = useState<string | number>('table');

  useEffect(() => {
    if (!open) {
      return;
    }
    setLocalTool(tool);
    setParameterEditMode('table');
  }, [open, tool]);

  const updateParameterRows = (updater: (rows: ParameterRow[]) => ParameterRow[]) => {
    setLocalTool((current) => {
      if (!current) {
        return current;
      }
      const next = updater(parseParameterRows(current.parameterSchema));
      return { ...current, parameterSchema: buildParameterSchema(next) };
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>编辑工具</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
        <TextField label="工具名" value={localTool?.toolName || ''} onChange={(e) => setLocalTool((current) => current ? { ...current, toolName: e.target.value } : current)} />
        <TextField label="描述" value={localTool?.toolDescription || ''} onChange={(e) => setLocalTool((current) => current ? { ...current, toolDescription: e.target.value } : current)} />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField label="HTTP 方法" value={localTool?.httpMethod || ''} onChange={(e) => setLocalTool((current) => current ? { ...current, httpMethod: e.target.value } : current)} />
          <TextField label="路径" fullWidth value={localTool?.path || ''} onChange={(e) => setLocalTool((current) => current ? { ...current, path: e.target.value } : current)} />
        </Box>

        <Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography sx={{ fontSize: 15, fontWeight: 600 }}>参数定义</Typography>
            <InkSegmentedControl value={parameterEditMode} onChange={setParameterEditMode} options={[{ value: 'table', label: '表格' }, { value: 'json', label: 'JSON' }]} />
          </Box>
          {parameterEditMode === 'table' ? (
            <Box sx={{
              backgroundColor: 'rgba(245,243,238,0.6)',
              borderRadius: 2, overflow: 'hidden',
              border: '1px solid #E0DDD8',
            }}>
              {parseParameterRows(localTool?.parameterSchema).map((row) => (
                <Box key={row.key} sx={{
                  display: 'flex', gap: 1, alignItems: 'center', px: 2, py: 1.25,
                  borderBottom: `0.5px solid #E0DDD8`,
                }}>
                  <TextField size="small" placeholder="参数名" value={row.name} onChange={(e) => updateParameterRows((rows) => rows.map((item) => item.key === row.key ? { ...item, name: e.target.value } : item))} sx={{ flex: 1 }} />
                  <Select size="small" value={row.type} onChange={(e) => updateParameterRows((rows) => rows.map((item) => item.key === row.key ? { ...item, type: String(e.target.value) } : item))} sx={{ width: 100 }}>
                    {['string', 'number', 'integer', 'boolean', 'array', 'object'].map((type) => <MenuItem key={type} value={type}>{type}</MenuItem>)}
                  </Select>
                  <TextField size="small" placeholder="描述" value={row.description} onChange={(e) => updateParameterRows((rows) => rows.map((item) => item.key === row.key ? { ...item, description: e.target.value } : item))} sx={{ flex: 2 }} />
                  <InkSwitch size="small" checked={row.required} onChange={(_, value) => updateParameterRows((rows) => rows.map((item) => item.key === row.key ? { ...item, required: value } : item))} />
                  <IconButton size="small" color="error" onClick={() => updateParameterRows((rows) => rows.filter((item) => item.key !== row.key))}>
                    <Delete sx={{ fontSize: 16 }} />
                  </IconButton>
                </Box>
              ))}
              <Box sx={{ p: 1.5 }}>
                <Button size="small" startIcon={<Add />} onClick={() => updateParameterRows((rows) => [...rows, { key: `new-${Date.now()}`, name: '', type: 'string', description: '', required: false }])}>
                  添加参数
                </Button>
              </Box>
            </Box>
          ) : (
            <EditorComponent value={localTool?.parameterSchema || DEFAULT_PARAMETER_SCHEMA} onChange={(value: string) => setLocalTool((current) => current ? { ...current, parameterSchema: value } : current)} height={200} />
          )}
        </Box>

        <Box>
          <Typography sx={{ fontSize: 15, fontWeight: 600, mb: 1 }}>响应 Schema</Typography>
          <EditorComponent value={localTool?.responseSchema || '{}'} onChange={(value: string) => setLocalTool((current) => current ? { ...current, responseSchema: value } : current)} height={160} />
        </Box>

        <Box>
          <Typography sx={{ fontSize: 15, fontWeight: 600, mb: 1 }}>调用示例</Typography>
          <EditorComponent value={localTool?.examplePayload || '{}'} onChange={(value: string) => setLocalTool((current) => current ? { ...current, examplePayload: value } : current)} height={140} />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          onClick={() => localTool && onSubmit({
            toolName: localTool.toolName,
            toolDescription: localTool.toolDescription,
            httpMethod: localTool.httpMethod,
            path: localTool.path,
            parameterSchema: localTool.parameterSchema || DEFAULT_PARAMETER_SCHEMA,
            responseSchema: localTool.responseSchema || '{}',
            examplePayload: localTool.examplePayload || '{}',
            enabled: localTool.enabled,
          })}
          disabled={loading || !localTool}
          sx={{ borderRadius: 4 }}
        >
          保存
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function TestToolDrawer({
  open,
  toolId,
  EditorComponent,
  onClose,
}: {
  open: boolean;
  toolId: number | null;
  EditorComponent: CodeEditorComponent;
  onClose: () => void;
}) {
  const [args, setArgs] = useState('{}');
  const [result, setResult] = useState('');

  useEffect(() => {
    if (!open) {
      return;
    }
    setArgs('{}');
    setResult('');
  }, [open, toolId]);

  const testToolMutation = useMutation({
    mutationFn: (payload: { id: number; args: string }) => mcpGatewayApi.testTool(payload.id, payload.args),
    onSuccess: (data) => setResult(typeof data === 'string' ? data : JSON.stringify(data, null, 2)),
    onError: (error: any) => setResult('Error: ' + (error.message || 'Unknown')),
  });

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: { width: { xs: '100%', md: 420 }, p: 2.5, borderRadius: '4px 0 0 4px' },
      }}
    >
      <Typography sx={{ fontSize: 17, fontWeight: 600, mb: 2 }}>测试工具调用</Typography>
      <EditorComponent value={args} onChange={setArgs} height={160} />
      <Button
        variant="contained"
        startIcon={<PlayArrow />}
        fullWidth
        onClick={() => toolId && testToolMutation.mutate({ id: toolId, args })}
        sx={{ mt: 2, borderRadius: 4 }}
        disabled={testToolMutation.isPending || !toolId}
      >
        执行
      </Button>
      {result && (
        <Box sx={{
          mt: 2, p: 2.5, borderRadius: 2,
          backgroundColor: 'rgba(245,243,238,0.6)',
          border: '1px solid #E0DDD8',
          maxHeight: 300, overflow: 'auto',
        }}>
          <Typography sx={{ fontSize: 13, fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>{result}</Typography>
        </Box>
      )}
    </Drawer>
  );
}

export default function McpPage() {
  const isMobile = useMediaQuery('(max-width:899px)');
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [selectedSource, setSelectedSource] = useState<McpApiSource | null>(null);
  const [testTool, setTestTool] = useState<McpToolMapping | null>(null);
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
  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => mcpGatewayApi.createSource(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      setCreateOpen(false);
      setEditingSource(null);
      enqueueSnackbar('创建成功', { variant: 'success' });
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
    },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
  const deleteMutation = useMutation({
    mutationFn: mcpGatewayApi.deleteSource,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['mcp-sources'] }); setSelectedSource(null); enqueueSnackbar('删除成功', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
  const parseMutation = useMutation({
    mutationFn: (data: { openApiSpec?: string; openApiUrl?: string }) => mcpGatewayApi.parseSpec(selectedSource!.id, data),
    onSuccess: () => { refetchTools(); enqueueSnackbar('解析完成', { variant: 'success' }); },
    onError: () => enqueueSnackbar('解析失败', { variant: 'error' }),
  });
  const toggleToolMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) => mcpGatewayApi.updateTool(id, { enabled }),
    onSuccess: () => { refetchTools(); enqueueSnackbar('工具状态已更新', { variant: 'success' }); },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
  const updateToolMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateTool(id, data),
    onSuccess: () => { refetchTools(); setEditingTool(null); enqueueSnackbar('已更新', { variant: 'success' }); },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
  const toggleSourceMutation = useMutation({
    mutationFn: mcpGatewayApi.toggleSourceActive,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      queryClient.invalidateQueries({ queryKey: ['mcp-health'] });
      enqueueSnackbar('源状态已切换', { variant: 'success' });
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
                    border: isActive ? '2px solid #C84B31' : '2px solid transparent',
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
              border: '2px dashed #E0DDD8',
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              '&:hover': { borderColor: '#C84B31' },
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
          backgroundColor: '#FFFFFF',
          borderRadius: 3, p: 2.5,
          border: '1px solid #E0DDD8',
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
              backgroundColor: 'rgba(245,243,238,0.6)',
              borderRadius: 2, p: 2, mb: 2.5,
              border: '1px solid #E0DDD8',
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
                <TextField size="small" fullWidth value={specUrl} onChange={(e) => setSpecUrl(e.target.value)} placeholder="https://petstore.swagger.io/v2/swagger.json" />
                <Button variant="outlined" onClick={() => parseMutation.mutate({ openApiUrl: specUrl })} disabled={parseMutation.isPending} sx={{ borderRadius: 4 }}>导入</Button>
              </Box>
            )}
          </Box>

          {/* Tools list — iOS grouped list */}
          <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: 0.5, mb: 1 }}>
            工具 ({tools.length})
          </Typography>
          <Box sx={{
            backgroundColor: 'rgba(245,243,238,0.6)',
            borderRadius: 2, overflow: 'hidden',
            border: '1px solid #E0DDD8',
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
                  borderBottom: i < tools.length - 1 ? '0.5px solid #E0DDD8' : 'none',
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
                  <IconButton size="small" onClick={() => setTestTool(t)}><PlayArrow sx={{ fontSize: 18 }} /></IconButton>
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

      <TestToolDrawer
        open={testTool !== null}
        toolId={testTool?.id || null}
        EditorComponent={CodeEditor}
        onClose={() => setTestTool(null)}
      />
    </Box>
  );
}
