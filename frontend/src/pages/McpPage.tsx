import { useState, lazy, Suspense } from 'react';
import {
  Box, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, IconButton, Switch, Select, MenuItem,
  FormControl, InputLabel, LinearProgress, useTheme, useMediaQuery,
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
import { IOSSegmentedControl, IOSStatusBadge, IOSEmptyState } from '../components/ios';
import { motion } from 'framer-motion';

const LazyEditor = lazy(() => import('@monaco-editor/react'));

function MonacoEditor({ value, onChange, height = 180, isDark = false }: { value: string; onChange: (v: string) => void; height?: number; isDark?: boolean }) {
  return (
    <Box sx={{ height, borderRadius: 3, overflow: 'hidden', border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'}` }}>
      <Suspense fallback={<Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}><CircularProgress size={24} /></Box>}>
        <LazyEditor height="100%" defaultLanguage="json" value={value} onChange={(v) => onChange(v || '')} theme={isDark ? 'vs-dark' : 'light'} options={{ minimap: { enabled: false }, fontSize: 13, scrollBeyondLastLine: false }} />
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

const METHOD_COLORS: Record<string, string> = { GET: '#34C759', POST: '#007AFF', PUT: '#FF9500', DELETE: '#FF3B30', PATCH: '#AF52DE' };

export default function McpPage() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [selectedSource, setSelectedSource] = useState<McpApiSource | null>(null);
  const [testToolId, setTestToolId] = useState<number | null>(null);
  const [testArgs, setTestArgs] = useState('{}');
  const [testResult, setTestResult] = useState('');
  const [parseMode, setParseMode] = useState<string | number>('paste');
  const [specContent, setSpecContent] = useState('');
  const [specUrl, setSpecUrl] = useState('');
  const [editingSourceId, setEditingSourceId] = useState<number | null>(null);
  const [editingTool, setEditingTool] = useState<McpToolMapping | null>(null);
  const [parameterEditMode, setParameterEditMode] = useState<string | number>('table');
  const [form, setForm] = useState({ name: '', description: '', baseUrl: '', authType: 'NONE', authConfig: '' });

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
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['mcp-sources'] }); setCreateOpen(false); enqueueSnackbar('创建成功', { variant: 'success' }); },
  });
  const updateSourceMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateSource(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['mcp-sources'] }); setCreateOpen(false); setEditingSourceId(null); enqueueSnackbar('更新成功', { variant: 'success' }); },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
  const deleteMutation = useMutation({
    mutationFn: mcpGatewayApi.deleteSource,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['mcp-sources'] }); setSelectedSource(null); },
  });
  const parseMutation = useMutation({
    mutationFn: (data: { openApiSpec?: string; openApiUrl?: string }) => mcpGatewayApi.parseSpec(selectedSource!.id, data),
    onSuccess: () => { refetchTools(); enqueueSnackbar('解析完成', { variant: 'success' }); },
    onError: () => enqueueSnackbar('解析失败', { variant: 'error' }),
  });
  const toggleToolMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) => mcpGatewayApi.updateTool(id, { enabled }),
    onSuccess: () => refetchTools(),
  });
  const updateToolMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => mcpGatewayApi.updateTool(editingTool!.id, data),
    onSuccess: () => { refetchTools(); setEditingTool(null); enqueueSnackbar('已更新', { variant: 'success' }); },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
  const testToolMutation = useMutation({
    mutationFn: ({ id, args }: { id: number; args: string }) => mcpGatewayApi.testTool(id, args),
    onSuccess: (data) => setTestResult(typeof data === 'string' ? data : JSON.stringify(data, null, 2)),
    onError: (e: any) => setTestResult('Error: ' + (e.message || 'Unknown')),
  });

  const resetSourceForm = () => { setForm({ name: '', description: '', baseUrl: '', authType: 'NONE', authConfig: '' }); setEditingSourceId(null); };
  const openCreateDialog = () => { resetSourceForm(); setCreateOpen(true); };
  const openEditSourceDialog = (s: McpApiSource) => {
    setForm({ name: s.name, description: s.description || '', baseUrl: s.baseUrl || '', authType: s.authType || 'NONE', authConfig: '' });
    setEditingSourceId(s.id); setCreateOpen(true);
  };
  const handleSourceSubmit = () => {
    const payload = { ...form, name: form.name.trim(), description: form.description.trim(), baseUrl: form.baseUrl.trim(), authConfig: form.authConfig.trim() };
    if (editingSourceId !== null) { updateSourceMutation.mutate({ id: editingSourceId, data: payload }); return; }
    createMutation.mutate(payload);
  };

  const updateParameterRows = (updater: (rows: ParameterRow[]) => ParameterRow[]) => {
    setEditingTool((cur) => {
      if (!cur) return cur;
      const next = updater(parseParameterRows(cur.parameterSchema));
      return { ...cur, parameterSchema: buildParameterSchema(next) };
    });
  };

  const copyText = async (value: string, label: string) => {
    try { await navigator.clipboard.writeText(value); enqueueSnackbar(`${label} 已复制`, { variant: 'success' }); }
    catch { enqueueSnackbar('复制失败', { variant: 'error' }); }
  };

  const CodeEditor = isMobile ? MobileTextarea : MonacoEditor;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5">MCP 网关</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog} sx={{ borderRadius: 10 }}>添加 API 源</Button>
      </Box>
      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {/* Source grid — macOS System Preferences style */}
      {sources.length === 0 && !isLoading ? (
        <IOSEmptyState icon={<Api />} title="暂无 API 源" subtitle="添加 API 源来配置 MCP 工具映射" action={{ label: '添加 API 源', onClick: openCreateDialog }} />
      ) : (
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
          {sources.map((s: McpApiSource) => {
            const isActive = selectedSource?.id === s.id;
            return (
              <motion.div key={s.id} whileTap={{ scale: 0.97 }}>
                <Box
                  onClick={() => setSelectedSource(s)}
                  sx={{
                    width: 140, textAlign: 'center', cursor: 'pointer',
                    p: 2, borderRadius: 4,
                    backgroundColor: isActive ? (isDark ? 'rgba(0,122,255,0.15)' : 'rgba(0,122,255,0.08)') : 'transparent',
                    border: isActive ? '2px solid #007AFF' : '2px solid transparent',
                    transition: 'all 200ms',
                    '&:hover': { backgroundColor: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)' },
                  }}
                >
                  <Box sx={{
                    width: 52, height: 52, borderRadius: 3, mx: 'auto', mb: 1,
                    background: s.active ? 'linear-gradient(135deg, #007AFF, #5856D6)' : (isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)'),
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Api sx={{ fontSize: 24, color: s.active ? '#fff' : 'text.secondary' }} />
                  </Box>
                  <Typography sx={{ fontSize: 13, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {s.name}
                  </Typography>
                  <IOSStatusBadge label={s.active ? '活跃' : '停用'} status={s.active ? 'success' : 'default'} />
                </Box>
              </motion.div>
            );
          })}
          {/* Add card */}
          <Box
            onClick={openCreateDialog}
            sx={{
              width: 140, textAlign: 'center', cursor: 'pointer',
              p: 2, borderRadius: 4,
              border: `2px dashed ${isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.1)'}`,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              '&:hover': { borderColor: '#007AFF' },
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
          backgroundColor: isDark ? '#1C1C1E' : '#FFFFFF',
          borderRadius: 4, p: 3,
          boxShadow: isDark ? '0 4px 24px rgba(0,0,0,0.4)' : '0 2px 16px rgba(0,0,0,0.06)',
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
              backgroundColor: isDark ? 'rgba(118,118,128,0.12)' : 'rgba(118,118,128,0.06)',
              borderRadius: 3, p: 2, mb: 2.5,
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
              <IOSSegmentedControl value={parseMode} onChange={setParseMode} options={[{ value: 'paste', label: '粘贴 Spec' }, { value: 'url', label: 'URL 导入' }]} />
            </Box>
            {parseMode === 'paste' ? (
              <Box>
                <CodeEditor value={specContent} onChange={setSpecContent} height={180} />
                <Button variant="outlined" startIcon={<Refresh />} onClick={() => parseMutation.mutate({ openApiSpec: specContent })} disabled={parseMutation.isPending} sx={{ mt: 1, borderRadius: 10 }}>
                  解析
                </Button>
              </Box>
            ) : (
              <Box sx={{ display: 'flex', gap: 1 }}>
                <TextField size="small" fullWidth value={specUrl} onChange={(e) => setSpecUrl(e.target.value)} placeholder="https://petstore.swagger.io/v2/swagger.json" />
                <Button variant="outlined" onClick={() => parseMutation.mutate({ openApiUrl: specUrl })} disabled={parseMutation.isPending} sx={{ borderRadius: 10 }}>导入</Button>
              </Box>
            )}
          </Box>

          {/* Tools list — iOS grouped list */}
          <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: 0.5, mb: 1 }}>
            工具 ({tools.length})
          </Typography>
          <Box sx={{
            backgroundColor: isDark ? 'rgba(118,118,128,0.12)' : 'rgba(118,118,128,0.06)',
            borderRadius: 3, overflow: 'hidden',
          }}>
            {tools.length === 0 ? (
              <Box sx={{ py: 4, textAlign: 'center' }}>
                <Typography sx={{ fontSize: 15, color: 'text.secondary' }}>暂无工具，请先解析 OpenAPI 规范</Typography>
              </Box>
            ) : (
              tools.map((t: McpToolMapping, i: number) => (
                <Box key={t.id} sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: 2, py: 1.25,
                  borderBottom: i < tools.length - 1 ? `0.5px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'}` : 'none',
                }}>
                  {/* Method badge */}
                  <Box sx={{
                    px: 0.75, py: 0.25, borderRadius: 1,
                    backgroundColor: `${METHOD_COLORS[t.httpMethod] || '#007AFF'}18`,
                    flexShrink: 0,
                  }}>
                    <Typography sx={{ fontSize: 10, fontWeight: 700, fontFamily: 'monospace', color: METHOD_COLORS[t.httpMethod] || '#007AFF' }}>
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
                  <Switch checked={t.enabled} onChange={(_, v) => toggleToolMutation.mutate({ id: t.id, enabled: v })} />
                  {/* Actions */}
                  <IconButton size="small" onClick={() => { setEditingTool(t); setParameterEditMode('table'); }}><Edit sx={{ fontSize: 18 }} /></IconButton>
                  <IconButton size="small" onClick={() => { setTestToolId(t.id); setTestResult(''); }}><PlayArrow sx={{ fontSize: 18 }} /></IconButton>
                </Box>
              ))
            )}
          </Box>
        </Box>
      )}

      {/* Create/Edit Source Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingSourceId !== null ? '编辑 API 源' : '添加 API 源'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="名称" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <TextField label="描述" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <TextField label="Base URL" value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })} />
          <FormControl>
            <InputLabel>认证方式</InputLabel>
            <Select value={form.authType} onChange={(e) => setForm({ ...form, authType: e.target.value })} label="认证方式">
              <MenuItem value="NONE">无</MenuItem>
              <MenuItem value="API_KEY">API Key</MenuItem>
              <MenuItem value="BEARER_TOKEN">Bearer Token</MenuItem>
              <MenuItem value="BASIC_AUTH">Basic Auth</MenuItem>
            </Select>
          </FormControl>
          {form.authType !== 'NONE' && (
            <TextField label="认证配置" value={form.authConfig} onChange={(e) => setForm({ ...form, authConfig: e.target.value })} helperText="API Key 或 Token" />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setCreateOpen(false); resetSourceForm(); }}>取消</Button>
          <Button variant="contained" onClick={handleSourceSubmit} disabled={createMutation.isPending || updateSourceMutation.isPending} sx={{ borderRadius: 10 }}>
            {editingSourceId !== null ? '保存' : '创建'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Tool Dialog */}
      <Dialog open={editingTool !== null} onClose={() => setEditingTool(null)} maxWidth="md" fullWidth>
        <DialogTitle>编辑工具</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="工具名" value={editingTool?.toolName || ''} onChange={(e) => setEditingTool((c) => c ? { ...c, toolName: e.target.value } : c)} />
          <TextField label="描述" value={editingTool?.toolDescription || ''} onChange={(e) => setEditingTool((c) => c ? { ...c, toolDescription: e.target.value } : c)} />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="HTTP 方法" value={editingTool?.httpMethod || ''} onChange={(e) => setEditingTool((c) => c ? { ...c, httpMethod: e.target.value } : c)} />
            <TextField label="路径" fullWidth value={editingTool?.path || ''} onChange={(e) => setEditingTool((c) => c ? { ...c, path: e.target.value } : c)} />
          </Box>

          {/* Parameters */}
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography sx={{ fontSize: 15, fontWeight: 600 }}>参数定义</Typography>
              <IOSSegmentedControl value={parameterEditMode} onChange={setParameterEditMode} options={[{ value: 'table', label: '表格' }, { value: 'json', label: 'JSON' }]} />
            </Box>
            {parameterEditMode === 'table' ? (
              <Box sx={{
                backgroundColor: isDark ? 'rgba(118,118,128,0.12)' : 'rgba(118,118,128,0.06)',
                borderRadius: 3, overflow: 'hidden',
              }}>
                {parseParameterRows(editingTool?.parameterSchema).map((row) => (
                  <Box key={row.key} sx={{
                    display: 'flex', gap: 1, alignItems: 'center', px: 2, py: 1,
                    borderBottom: `0.5px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'}`,
                  }}>
                    <TextField size="small" placeholder="参数名" value={row.name} onChange={(e) => updateParameterRows((rows) => rows.map((r) => r.key === row.key ? { ...r, name: e.target.value } : r))} sx={{ flex: 1 }} />
                    <Select size="small" value={row.type} onChange={(e) => updateParameterRows((rows) => rows.map((r) => r.key === row.key ? { ...r, type: e.target.value } : r))} sx={{ width: 100 }}>
                      {['string', 'number', 'integer', 'boolean', 'array', 'object'].map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                    </Select>
                    <TextField size="small" placeholder="描述" value={row.description} onChange={(e) => updateParameterRows((rows) => rows.map((r) => r.key === row.key ? { ...r, description: e.target.value } : r))} sx={{ flex: 2 }} />
                    <Switch size="small" checked={row.required} onChange={(_, v) => updateParameterRows((rows) => rows.map((r) => r.key === row.key ? { ...r, required: v } : r))} />
                    <IconButton size="small" color="error" onClick={() => updateParameterRows((rows) => rows.filter((r) => r.key !== row.key))}>
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
              <CodeEditor value={editingTool?.parameterSchema || DEFAULT_PARAMETER_SCHEMA} onChange={(v) => setEditingTool((c) => c ? { ...c, parameterSchema: v } : c)} height={200} />
            )}
          </Box>

          {/* Response schema */}
          <Box>
            <Typography sx={{ fontSize: 15, fontWeight: 600, mb: 1 }}>响应 Schema</Typography>
            <CodeEditor value={editingTool?.responseSchema || '{}'} onChange={(v) => setEditingTool((c) => c ? { ...c, responseSchema: v } : c)} height={160} />
          </Box>

          {/* Example */}
          <Box>
            <Typography sx={{ fontSize: 15, fontWeight: 600, mb: 1 }}>调用示例</Typography>
            <CodeEditor value={editingTool?.examplePayload || '{}'} onChange={(v) => setEditingTool((c) => c ? { ...c, examplePayload: v } : c)} height={140} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditingTool(null)}>取消</Button>
          <Button variant="contained" onClick={() => editingTool && updateToolMutation.mutate({
            toolName: editingTool.toolName, toolDescription: editingTool.toolDescription,
            httpMethod: editingTool.httpMethod, path: editingTool.path,
            parameterSchema: editingTool.parameterSchema, responseSchema: editingTool.responseSchema,
            examplePayload: editingTool.examplePayload, enabled: editingTool.enabled,
          })} disabled={updateToolMutation.isPending} sx={{ borderRadius: 10 }}>保存</Button>
        </DialogActions>
      </Dialog>

      {/* Test Tool — side drawer */}
      <Drawer
        anchor="right"
        open={testToolId !== null}
        onClose={() => setTestToolId(null)}
        PaperProps={{
          sx: { width: { xs: '100%', md: 420 }, p: 3, borderRadius: '16px 0 0 16px' },
        }}
      >
        <Typography sx={{ fontSize: 17, fontWeight: 600, mb: 2 }}>测试工具调用</Typography>
        <CodeEditor value={testArgs} onChange={setTestArgs} height={160} />
        <Button variant="contained" startIcon={<PlayArrow />} fullWidth
          onClick={() => testToolId && testToolMutation.mutate({ id: testToolId, args: testArgs })}
          sx={{ mt: 2, borderRadius: 10 }}>
          执行
        </Button>
        {testResult && (
          <Box sx={{
            mt: 2, p: 2, borderRadius: 3,
            backgroundColor: isDark ? 'rgba(118,118,128,0.12)' : 'rgba(118,118,128,0.06)',
            maxHeight: 300, overflow: 'auto',
          }}>
            <Typography sx={{ fontSize: 13, fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>{testResult}</Typography>
          </Box>
        )}
      </Drawer>
    </Box>
  );
}
