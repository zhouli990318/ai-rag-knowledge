import { useState } from 'react';
import {
  Box, Paper, Typography, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Table, TableHead, TableBody, TableRow,
  TableCell, IconButton, Chip, Switch, Card, CardContent, Select,
  MenuItem, FormControl, InputLabel, LinearProgress, Tab, Tabs,
} from '@mui/material';
import { Add, Delete, Refresh, PlayArrow, Edit, ContentCopy } from '@mui/icons-material';
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import { mcpGatewayApi } from '../api/mcpApi';
import { McpApiSource, McpConnectionInfo, McpToolMapping } from '../api/types';
import { useSnackbar } from 'notistack';
import Editor from '@monaco-editor/react';

type ParameterRow = {
  key: string;
  name: string;
  type: string;
  description: string;
  required: boolean;
};

const DEFAULT_PARAMETER_SCHEMA = '{\n  "type": "object",\n  "properties": {},\n  "required": []\n}';

function parseParameterRows(parameterSchema?: string): ParameterRow[] {
  try {
    const parsed = JSON.parse(parameterSchema || DEFAULT_PARAMETER_SCHEMA) as {
      properties?: Record<string, { type?: string; description?: string }>;
      required?: string[];
    };
    const properties = parsed.properties || {};
    const required = new Set(parsed.required || []);

    return Object.entries(properties).map(([name, value], index) => ({
      key: `${name}-${index}`,
      name,
      type: value?.type || 'string',
      description: value?.description || '',
      required: required.has(name),
    }));
  } catch {
    return [];
  }
}

function buildParameterSchema(rows: ParameterRow[]): string {
  const properties = rows.reduce<Record<string, { type: string; description: string }>>((acc, row) => {
    const name = row.name.trim();
    if (!name) {
      return acc;
    }
    acc[name] = {
      type: row.type || 'string',
      description: row.description || '',
    };
    return acc;
  }, {});

  const required = rows
    .filter((row) => row.required && row.name.trim())
    .map((row) => row.name.trim());

  return JSON.stringify(
    {
      type: 'object',
      properties,
      required,
    },
    null,
    2,
  );
}

export default function McpPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedSource, setSelectedSource] = useState<McpApiSource | null>(null);
  const [testToolId, setTestToolId] = useState<number | null>(null);
  const [testArgs, setTestArgs] = useState('{}');
  const [testResult, setTestResult] = useState('');
  const [parseTab, setParseTab] = useState(0);
  const [specContent, setSpecContent] = useState('');
  const [specUrl, setSpecUrl] = useState('');
  const [editingSourceId, setEditingSourceId] = useState<number | null>(null);
  const [editingTool, setEditingTool] = useState<McpToolMapping | null>(null);
  const [parameterEditMode, setParameterEditMode] = useState<'table' | 'json'>('table');
  const [form, setForm] = useState({ name: '', description: '', baseUrl: '', authType: 'NONE', authConfig: '', openApiSpec: '' });

  const { data: sources = [], isLoading } = useQuery({ queryKey: ['mcp-sources'], queryFn: mcpGatewayApi.listSources });
  const connectionInfoQueries = useQueries({
    queries: sources.map((source) => ({
      queryKey: ['mcp-source-connection-info', source.id],
      queryFn: () => mcpGatewayApi.getSourceConnectionInfo(source.id),
    })),
  });
  const { data: tools = [], refetch: refetchTools } = useQuery({
    queryKey: ['mcp-tools', selectedSource?.id],
    queryFn: () => mcpGatewayApi.getTools(selectedSource!.id),
    enabled: !!selectedSource,
  });

  const connectionInfoBySourceId = new Map<number, McpConnectionInfo>();
  connectionInfoQueries.forEach((query, index) => {
    const source = sources[index];
    if (source && query.data) {
      connectionInfoBySourceId.set(source.id, query.data);
    }
  });

  const connectionInfo = selectedSource ? connectionInfoBySourceId.get(selectedSource.id) : undefined;

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => mcpGatewayApi.createSource(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['mcp-sources'] }); setCreateOpen(false); enqueueSnackbar('创建成功', { variant: 'success' }); },
  });

  const updateSourceMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateSource(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      setCreateOpen(false);
      setEditingSourceId(null);
      enqueueSnackbar('更新成功', { variant: 'success' });
    },
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
    onSuccess: () => {
      refetchTools();
      setEditingTool(null);
      enqueueSnackbar('工具更新成功', { variant: 'success' });
    },
    onError: () => enqueueSnackbar('工具更新失败', { variant: 'error' }),
  });

  const testToolMutation = useMutation({
    mutationFn: ({ id, args }: { id: number; args: string }) => mcpGatewayApi.testTool(id, args),
    onSuccess: (data) => setTestResult(typeof data === 'string' ? data : JSON.stringify(data, null, 2)),
    onError: (e: any) => setTestResult('Error: ' + (e.message || 'Unknown')),
  });

  const resetSourceForm = () => {
    setForm({ name: '', description: '', baseUrl: '', authType: 'NONE', authConfig: '', openApiSpec: '' });
    setEditingSourceId(null);
  };

  const openCreateDialog = () => {
    resetSourceForm();
    setCreateOpen(true);
  };

  const openEditSourceDialog = (source: McpApiSource) => {
    setForm({
      name: source.name,
      description: source.description || '',
      baseUrl: source.baseUrl || '',
      authType: source.authType || 'NONE',
      authConfig: '',
      openApiSpec: '',
    });
    setEditingSourceId(source.id);
    setCreateOpen(true);
  };

  const handleSourceSubmit = () => {
    const payload = {
      ...form,
      name: form.name.trim(),
      description: form.description.trim(),
      baseUrl: form.baseUrl.trim(),
      authConfig: form.authConfig.trim(),
    };

    if (editingSourceId !== null) {
      updateSourceMutation.mutate({ id: editingSourceId, data: payload });
      return;
    }

    createMutation.mutate(payload);
  };

  const updateParameterRows = (updater: (rows: ParameterRow[]) => ParameterRow[]) => {
    setEditingTool((current) => {
      if (!current) {
        return current;
      }
      const nextRows = updater(parseParameterRows(current.parameterSchema));
      return {
        ...current,
        parameterSchema: buildParameterSchema(nextRows),
      };
    });
  };

  const addParameterRow = () => {
    updateParameterRows((rows) => ([
      ...rows,
      {
        key: `new-${Date.now()}`,
        name: '',
        type: 'string',
        description: '',
        required: false,
      },
    ]));
  };

  const updateParameterRow = (rowKey: string, patch: Partial<ParameterRow>) => {
    updateParameterRows((rows) => rows.map((row) => (row.key === rowKey ? { ...row, ...patch } : row)));
  };

  const removeParameterRow = (rowKey: string) => {
    updateParameterRows((rows) => rows.filter((row) => row.key !== rowKey));
  };

  const copyText = async (value: string, label: string) => {
    try {
      await navigator.clipboard.writeText(value);
      enqueueSnackbar(`${label} 已复制`, { variant: 'success' });
    } catch {
      enqueueSnackbar(`${label} 复制失败`, { variant: 'error' });
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>MCP 网关配置</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog}>添加 API 源</Button>
      </Box>

      {isLoading && <LinearProgress />}

      {/* Sources list */}
      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
        {sources.map((s: McpApiSource) => {
          const sourceConnectionInfo = connectionInfoBySourceId.get(s.id);

          return (
          <Card key={s.id} sx={{ width: 320, cursor: 'pointer', border: selectedSource?.id === s.id ? 2 : 0, borderColor: 'primary.main' }}
            onClick={() => setSelectedSource(s)}>
            <CardContent>
              <Typography variant="h6">{s.name}</Typography>
              <Typography variant="body2" color="text.secondary" noWrap>{s.description}</Typography>
              <Typography variant="caption" color="text.disabled">{s.baseUrl}</Typography>
              <Box sx={{ mt: 1 }}>
                <Chip label={s.authType} size="small" sx={{ mr: 1 }} />
                <Chip label={s.active ? '活跃' : '停用'} color={s.active ? 'success' : 'default'} size="small" />
              </Box>

              <Box sx={{ mt: 2, display: 'grid', gap: 1 }}>
                <Typography variant="caption" color="text.secondary">独立 MCP 连接</Typography>
                {sourceConnectionInfo ? (
                  <>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TextField
                        label="SSE"
                        size="small"
                        fullWidth
                        value={sourceConnectionInfo.sseUrl}
                        InputProps={{ readOnly: true }}
                        onClick={(event) => event.stopPropagation()}
                      />
                      <IconButton
                        size="small"
                        onClick={(event) => {
                          event.stopPropagation();
                          void copyText(sourceConnectionInfo.sseUrl, `${s.name} SSE`);
                        }}
                      >
                        <ContentCopy fontSize="small" />
                      </IconButton>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TextField
                        label="HTTP"
                        size="small"
                        fullWidth
                        value={sourceConnectionInfo.streamableHttpUrl}
                        InputProps={{ readOnly: true }}
                        onClick={(event) => event.stopPropagation()}
                      />
                      <IconButton
                        size="small"
                        onClick={(event) => {
                          event.stopPropagation();
                          void copyText(sourceConnectionInfo.streamableHttpUrl, `${s.name} HTTP`);
                        }}
                      >
                        <ContentCopy fontSize="small" />
                      </IconButton>
                    </Box>
                  </>
                ) : (
                  <Typography variant="caption" color="text.secondary">正在加载连接信息...</Typography>
                )}
              </Box>
            </CardContent>
          </Card>
        );})}
      </Box>

      {selectedSource && (
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6">{selectedSource.name} - 工具映射</Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <IconButton color="primary" onClick={() => openEditSourceDialog(selectedSource)}><Edit /></IconButton>
              <IconButton color="error" onClick={() => deleteMutation.mutate(selectedSource.id)}><Delete /></IconButton>
            </Box>
          </Box>

          {connectionInfo && (
            <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
              <Typography variant="subtitle2" fontWeight={700} gutterBottom>独立 MCP 接入连接</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {connectionInfo.serverName} {connectionInfo.version}
              </Typography>
              <Box sx={{ display: 'grid', gap: 1.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <TextField label="SSE" size="small" fullWidth value={connectionInfo.sseUrl} InputProps={{ readOnly: true }} />
                  <IconButton size="small" onClick={() => void copyText(connectionInfo.sseUrl, 'SSE')}>
                    <ContentCopy fontSize="small" />
                  </IconButton>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <TextField label="Streamable HTTP" size="small" fullWidth value={connectionInfo.streamableHttpUrl} InputProps={{ readOnly: true }} />
                  <IconButton size="small" onClick={() => void copyText(connectionInfo.streamableHttpUrl, 'Streamable HTTP')}>
                    <ContentCopy fontSize="small" />
                  </IconButton>
                </Box>
              </Box>
            </Paper>
          )}

          {/* Parse OpenAPI */}
          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>解析 OpenAPI 规范</Typography>
            <Tabs value={parseTab} onChange={(_, v) => setParseTab(v)} sx={{ mb: 2 }}>
              <Tab label="粘贴 Spec" />
              <Tab label="URL 导入" />
            </Tabs>
            {parseTab === 0 ? (
              <Box>
                <Box sx={{ height: 200, mb: 1, border: 1, borderColor: 'divider', borderRadius: 1 }}>
                  <Editor height="100%" defaultLanguage="yaml" value={specContent} onChange={(v) => setSpecContent(v || '')}
                    theme="vs-dark" options={{ minimap: { enabled: false }, fontSize: 13 }} />
                </Box>
                <Button variant="outlined" startIcon={<Refresh />} onClick={() => parseMutation.mutate({ openApiSpec: specContent })}
                  disabled={parseMutation.isPending}>解析</Button>
              </Box>
            ) : (
              <Box sx={{ display: 'flex', gap: 1 }}>
                <TextField size="small" fullWidth value={specUrl} onChange={(e) => setSpecUrl(e.target.value)} placeholder="https://petstore.swagger.io/v2/swagger.json" />
                <Button variant="outlined" onClick={() => parseMutation.mutate({ openApiUrl: specUrl })} disabled={parseMutation.isPending}>导入</Button>
              </Box>
            )}
          </Paper>

          {/* Tools table */}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>工具名</TableCell>
                <TableCell>描述</TableCell>
                <TableCell>方法</TableCell>
                <TableCell>路径</TableCell>
                <TableCell>启用</TableCell>
                <TableCell>测试</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tools.map((t: McpToolMapping) => (
                <TableRow key={t.id}>
                  <TableCell><Typography variant="body2" fontFamily="monospace">{t.toolName}</Typography></TableCell>
                  <TableCell><Typography variant="body2" noWrap sx={{ maxWidth: 200 }}>{t.toolDescription}</Typography></TableCell>
                  <TableCell><Chip label={t.httpMethod} size="small" color="primary" variant="outlined" /></TableCell>
                  <TableCell><Typography variant="body2" fontFamily="monospace">{t.path}</Typography></TableCell>
                  <TableCell>
                    <Switch checked={t.enabled} onChange={(_, v) => toggleToolMutation.mutate({ id: t.id, enabled: v })} size="small" />
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => { setEditingTool(t); setParameterEditMode('table'); }}><Edit /></IconButton>
                    <IconButton size="small" onClick={() => { setTestToolId(t.id); setTestResult(''); }}><PlayArrow /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Create dialog */}
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
          <Button variant="contained" onClick={handleSourceSubmit} disabled={createMutation.isPending || updateSourceMutation.isPending}>
            {editingSourceId !== null ? '保存' : '创建'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={editingTool !== null} onClose={() => setEditingTool(null)} maxWidth="md" fullWidth>
        <DialogTitle>编辑工具</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="工具名" value={editingTool?.toolName || ''} onChange={(e) => setEditingTool((current) => current ? { ...current, toolName: e.target.value } : current)} />
          <TextField label="描述" value={editingTool?.toolDescription || ''} onChange={(e) => setEditingTool((current) => current ? { ...current, toolDescription: e.target.value } : current)} />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="HTTP 方法" value={editingTool?.httpMethod || ''} onChange={(e) => setEditingTool((current) => current ? { ...current, httpMethod: e.target.value } : current)} />
            <TextField label="路径" fullWidth value={editingTool?.path || ''} onChange={(e) => setEditingTool((current) => current ? { ...current, path: e.target.value } : current)} />
          </Box>
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle2">参数定义</Typography>
              <Tabs value={parameterEditMode} onChange={(_, value) => setParameterEditMode(value)}>
                <Tab value="table" label="表格模式" />
                <Tab value="json" label="JSON 模式" />
              </Tabs>
            </Box>
            {parameterEditMode === 'table' ? (
              <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, overflow: 'hidden' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>参数名</TableCell>
                      <TableCell>类型</TableCell>
                      <TableCell>描述</TableCell>
                      <TableCell>必填</TableCell>
                      <TableCell align="right">操作</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {parseParameterRows(editingTool?.parameterSchema).map((row) => (
                      <TableRow key={row.key}>
                        <TableCell>
                          <TextField
                            size="small"
                            value={row.name}
                            onChange={(e) => updateParameterRow(row.key, { name: e.target.value })}
                            placeholder="参数名"
                          />
                        </TableCell>
                        <TableCell sx={{ minWidth: 120 }}>
                          <Select
                            size="small"
                            value={row.type}
                            onChange={(e) => updateParameterRow(row.key, { type: e.target.value })}
                          >
                            <MenuItem value="string">string</MenuItem>
                            <MenuItem value="number">number</MenuItem>
                            <MenuItem value="integer">integer</MenuItem>
                            <MenuItem value="boolean">boolean</MenuItem>
                            <MenuItem value="array">array</MenuItem>
                            <MenuItem value="object">object</MenuItem>
                          </Select>
                        </TableCell>
                        <TableCell>
                          <TextField
                            size="small"
                            fullWidth
                            value={row.description}
                            onChange={(e) => updateParameterRow(row.key, { description: e.target.value })}
                            placeholder="参数描述"
                          />
                        </TableCell>
                        <TableCell>
                          <Switch
                            size="small"
                            checked={row.required}
                            onChange={(_, checked) => updateParameterRow(row.key, { required: checked })}
                          />
                        </TableCell>
                        <TableCell align="right">
                          <IconButton size="small" color="error" onClick={() => removeParameterRow(row.key)}>
                            <Delete fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                    {parseParameterRows(editingTool?.parameterSchema).length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5}>
                          <Typography variant="body2" color="text.secondary">
                            当前没有参数，点击下方按钮新增。
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
                <Box sx={{ p: 1.5, borderTop: 1, borderColor: 'divider' }}>
                  <Button size="small" startIcon={<Add />} onClick={addParameterRow}>添加参数</Button>
                </Box>
              </Box>
            ) : (
              <Box sx={{ height: 220, border: 1, borderColor: 'divider', borderRadius: 1 }}>
                <Editor height="100%" defaultLanguage="json" value={editingTool?.parameterSchema || DEFAULT_PARAMETER_SCHEMA} onChange={(v) => setEditingTool((current) => current ? { ...current, parameterSchema: v || DEFAULT_PARAMETER_SCHEMA } : current)}
                  theme="vs-dark" options={{ minimap: { enabled: false }, fontSize: 13 }} />
              </Box>
            )}
          </Box>
          <Box sx={{ height: 180, border: 1, borderColor: 'divider', borderRadius: 1 }}>
            <Editor height="100%" defaultLanguage="json" value={editingTool?.responseSchema || '{}'} onChange={(v) => setEditingTool((current) => current ? { ...current, responseSchema: v || '{}' } : current)}
              theme="vs-dark" options={{ minimap: { enabled: false }, fontSize: 13 }} />
          </Box>
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>调用示例</Typography>
            <Box sx={{ height: 160, border: 1, borderColor: 'divider', borderRadius: 1 }}>
              <Editor height="100%" defaultLanguage="json" value={editingTool?.examplePayload || '{}'} onChange={(v) => setEditingTool((current) => current ? { ...current, examplePayload: v || '{}' } : current)}
                theme="vs-dark" options={{ minimap: { enabled: false }, fontSize: 13 }} />
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditingTool(null)}>取消</Button>
          <Button variant="contained" onClick={() => editingTool && updateToolMutation.mutate({
            toolName: editingTool.toolName,
            toolDescription: editingTool.toolDescription,
            httpMethod: editingTool.httpMethod,
            path: editingTool.path,
            parameterSchema: editingTool.parameterSchema,
            responseSchema: editingTool.responseSchema,
            examplePayload: editingTool.examplePayload,
            enabled: editingTool.enabled,
          })} disabled={updateToolMutation.isPending}>保存</Button>
        </DialogActions>
      </Dialog>

      {/* Test tool dialog */}
      <Dialog open={testToolId !== null} onClose={() => setTestToolId(null)} maxWidth="sm" fullWidth>
        <DialogTitle>测试工具调用</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <Box sx={{ height: 150, border: 1, borderColor: 'divider', borderRadius: 1 }}>
            <Editor height="100%" defaultLanguage="json" value={testArgs} onChange={(v) => setTestArgs(v || '{}')}
              theme="vs-dark" options={{ minimap: { enabled: false }, fontSize: 13 }} />
          </Box>
          {testResult && (
            <Paper variant="outlined" sx={{ p: 2, maxHeight: 200, overflow: 'auto' }}>
              <Typography variant="body2" fontFamily="monospace" whiteSpace="pre-wrap">{testResult}</Typography>
            </Paper>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestToolId(null)}>关闭</Button>
          <Button variant="contained" startIcon={<PlayArrow />}
            onClick={() => testToolId && testToolMutation.mutate({ id: testToolId, args: testArgs })}>执行</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
