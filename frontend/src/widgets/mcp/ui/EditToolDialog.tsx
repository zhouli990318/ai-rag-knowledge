import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Box, Typography, IconButton, Select, MenuItem,
} from '@mui/material';
import { AddOutlined as Add, DeleteOutlined as Delete } from '@mui/icons-material';
import type { McpToolMapping } from '@/entities/mcp';
import { InkSegmentedControl, InkSwitch } from '@/shared/ui/ink';
import { ui } from '@/shared/theme/semanticColors';
import type { CodeEditorComponent } from './CodeEditor';
import { parseParameterRows, buildParameterSchema, DEFAULT_PARAMETER_SCHEMA } from './helpers';
import type { ParameterRow } from './helpers';

export type ToolUpdatePayload = {
  toolName: string;
  toolDescription: string;
  httpMethod: string;
  path: string;
  parameterSchema: string;
  responseSchema: string;
  examplePayload: string;
  enabled: boolean;
};

interface Props {
  open: boolean;
  tool: McpToolMapping | null;
  EditorComponent: CodeEditorComponent;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: ToolUpdatePayload) => void;
}

export default function EditToolDialog({ open, tool, EditorComponent, loading, onClose, onSubmit }: Props) {
  const [localTool, setLocalTool] = useState<McpToolMapping | null>(tool);
  const [parameterEditMode, setParameterEditMode] = useState<string | number>('table');

  useEffect(() => {
    if (!open) return;
    setLocalTool(tool);
    setParameterEditMode('table');
  }, [open, tool]);

  const updateParameterRows = (updater: (rows: ParameterRow[]) => ParameterRow[]) => {
    setLocalTool((current) => {
      if (!current) return current;
      const next = updater(parseParameterRows(current.parameterSchema));
      return { ...current, parameterSchema: buildParameterSchema(next) };
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>编辑工具</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
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
              backgroundColor: ui.hoverBg,
              borderRadius: 2, overflow: 'hidden',
              border: `1px solid ${ui.border}`,
            }}>
              {parseParameterRows(localTool?.parameterSchema).map((row) => (
                <Box key={row.key} sx={{
                  display: 'flex', gap: 1, alignItems: 'center', px: 2, py: 1.25,
                  borderBottom: `0.5px solid ${ui.border}`,
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
