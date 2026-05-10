import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Box, MenuItem,
} from '@mui/material';
import type { ChunkType, RetrievalMode } from '@/entities/knowledge/model/types';

export type KnowledgeFormState = {
  name: string;
  description: string;
  chunkType: ChunkType;
  chunkSize: number;
  chunkOverlap: number;
  retrievalTopK: number;
  similarityThreshold: number;
  retrievalMode: RetrievalMode;
  filterExpression: string;
  keywordWeight: number;
  vectorWeight: number;
};

export const defaultKnowledgeFormState: KnowledgeFormState = {
  name: '',
  description: '',
  chunkType: 'FIXED_SIZE',
  chunkSize: 800,
  chunkOverlap: 200,
  retrievalTopK: 5,
  similarityThreshold: 0.6,
  retrievalMode: 'HYBRID',
  filterExpression: '',
  keywordWeight: 0.3,
  vectorWeight: 0.7,
};

interface Props {
  open: boolean;
  loading: boolean;
  title?: string;
  submitLabel?: string;
  initialValue?: KnowledgeFormState | null;
  onClose: () => void;
  onSubmit: (payload: KnowledgeFormState) => void;
}

export default function CreateKnowledgeBaseDialog({
  open,
  loading,
  title = '新建知识库',
  submitLabel = '创建',
  initialValue,
  onClose,
  onSubmit,
}: Props) {
  const [form, setForm] = useState<KnowledgeFormState>(defaultKnowledgeFormState);

  useEffect(() => {
    if (!open) return;
    setForm(initialValue ?? defaultKnowledgeFormState);
  }, [open, initialValue]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField label="名称" required value={form.name} onChange={(e) => setForm((current) => ({ ...current, name: e.target.value }))} />
        <TextField label="描述" multiline rows={2} value={form.description} onChange={(e) => setForm((current) => ({ ...current, description: e.target.value }))} />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField
            select
            label="分块策略"
            value={form.chunkType}
            onChange={(e) => setForm((current) => ({ ...current, chunkType: e.target.value as ChunkType }))}
          >
            <MenuItem value="FIXED_SIZE">固定大小</MenuItem>
            <MenuItem value="SENTENCE">按句切分</MenuItem>
            <MenuItem value="PARAGRAPH">按段切分</MenuItem>
            <MenuItem value="RECURSIVE">递归切分</MenuItem>
          </TextField>
          <TextField type="number" label="检索 TopK" value={form.retrievalTopK} onChange={(e) => setForm((current) => ({ ...current, retrievalTopK: Number(e.target.value) }))} />
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField type="number" label="分块大小" value={form.chunkSize} onChange={(e) => setForm((current) => ({ ...current, chunkSize: Number(e.target.value) }))} />
          <TextField type="number" label="分块重叠" value={form.chunkOverlap} onChange={(e) => setForm((current) => ({ ...current, chunkOverlap: Number(e.target.value) }))} />
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField type="number" label="相似度阈值" inputProps={{ min: 0, max: 1, step: 0.05 }} value={form.similarityThreshold} onChange={(e) => setForm((current) => ({ ...current, similarityThreshold: Number(e.target.value) }))} />
          <TextField
            select
            label="检索模式"
            value={form.retrievalMode}
            onChange={(e) => setForm((current) => ({ ...current, retrievalMode: e.target.value as RetrievalMode }))}
          >
            <MenuItem value="VECTOR">向量检索</MenuItem>
            <MenuItem value="KEYWORD">关键词检索</MenuItem>
            <MenuItem value="HYBRID">混合检索</MenuItem>
          </TextField>
        </Box>
        {form.retrievalMode === 'HYBRID' && (
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              type="number"
              label="关键词权重"
              inputProps={{ min: 0, max: 1, step: 0.05 }}
              helperText="仅混合检索生效，建议与向量权重相加为 1"
              value={form.keywordWeight}
              onChange={(e) => setForm((current) => ({ ...current, keywordWeight: Number(e.target.value) }))}
            />
            <TextField
              type="number"
              label="向量权重"
              inputProps={{ min: 0, max: 1, step: 0.05 }}
              helperText="仅混合检索生效，建议与关键词权重相加为 1"
              value={form.vectorWeight}
              onChange={(e) => setForm((current) => ({ ...current, vectorWeight: Number(e.target.value) }))}
            />
          </Box>
        )}
        <TextField label="过滤表达式（可选）" value={form.filterExpression} onChange={(e) => setForm((current) => ({ ...current, filterExpression: e.target.value }))} />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 4 }}>{submitLabel}</Button>
      </DialogActions>
    </Dialog>
  );
}
