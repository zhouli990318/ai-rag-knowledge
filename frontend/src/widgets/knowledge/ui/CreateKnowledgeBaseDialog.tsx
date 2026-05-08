import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Box,
} from '@mui/material';

export type KnowledgeFormState = {
  name: string;
  description: string;
  chunkSize: number;
  chunkOverlap: number;
};

interface Props {
  open: boolean;
  loading: boolean;
  onClose: () => void;
  onSubmit: (payload: KnowledgeFormState) => void;
}

export default function CreateKnowledgeBaseDialog({ open, loading, onClose, onSubmit }: Props) {
  const [form, setForm] = useState<KnowledgeFormState>({ name: '', description: '', chunkSize: 800, chunkOverlap: 200 });

  useEffect(() => {
    if (!open) return;
    setForm({ name: '', description: '', chunkSize: 800, chunkOverlap: 200 });
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>新建知识库</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField label="名称" required value={form.name} onChange={(e) => setForm((current) => ({ ...current, name: e.target.value }))} />
        <TextField label="描述" multiline rows={2} value={form.description} onChange={(e) => setForm((current) => ({ ...current, description: e.target.value }))} />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField type="number" label="分块大小" value={form.chunkSize} onChange={(e) => setForm((current) => ({ ...current, chunkSize: Number(e.target.value) }))} />
          <TextField type="number" label="分块重叠" value={form.chunkOverlap} onChange={(e) => setForm((current) => ({ ...current, chunkOverlap: Number(e.target.value) }))} />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 4 }}>创建</Button>
      </DialogActions>
    </Dialog>
  );
}
