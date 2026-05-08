import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material';
import type { McpApiSource } from '@/entities/mcp';

export type SourceFormState = {
  name: string;
  description: string;
  baseUrl: string;
  authType: string;
  authConfig: string;
};

interface Props {
  open: boolean;
  initialSource: McpApiSource | null;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: SourceFormState) => void;
}

export default function CreateEditSourceDialog({ open, initialSource, loading, onClose, onSubmit }: Props) {
  const [form, setForm] = useState<SourceFormState>({
    name: '', description: '', baseUrl: '', authType: 'NONE', authConfig: '',
  });

  useEffect(() => {
    if (!open) return;
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
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
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
          <TextField type="password" autoComplete="off" label="认证配置" value={form.authConfig} onChange={(e) => setForm((current) => ({ ...current, authConfig: e.target.value }))} helperText="API Key 或 Token" />
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
