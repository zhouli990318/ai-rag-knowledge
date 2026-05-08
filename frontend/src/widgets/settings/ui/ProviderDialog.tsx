import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material';
import type { Provider, ProviderType } from '@/entities/provider';

export type ProviderFormState = {
  name: string;
  providerType: string;
  apiKey: string;
  baseUrl: string;
  defaultModel: string;
  embeddingModel: string;
  embeddingDimensions: number;
};

interface Props {
  open: boolean;
  providerTypes: ProviderType[];
  initialProvider: Provider | null;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: ProviderFormState) => void;
}

export default function ProviderDialog({ open, providerTypes, initialProvider, loading, onClose, onSubmit }: Props) {
  const [form, setForm] = useState<ProviderFormState>({
    name: '', providerType: 'OPENAI', apiKey: '', baseUrl: '',
    defaultModel: '', embeddingModel: '', embeddingDimensions: 1536,
  });

  useEffect(() => {
    if (!open) return;
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
      name: '', providerType: defaultType?.type || 'OPENAI', apiKey: '',
      baseUrl: defaultType?.defaultBaseUrl || '', defaultModel: '',
      embeddingModel: '', embeddingDimensions: 1536,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialProvider, open]);

  const selectedType = providerTypes.find((type) => type.type === form.providerType);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initialProvider ? '编辑供应商' : '添加供应商'}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
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
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 4 }}>
          {initialProvider ? '保存' : '创建'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
