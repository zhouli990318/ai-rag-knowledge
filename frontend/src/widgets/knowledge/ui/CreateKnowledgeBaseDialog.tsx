import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, Box, MenuItem, FormControlLabel, Switch, Typography, Divider,
} from '@mui/material';
import type { ChunkType, RetrievalMode } from '@/entities/knowledge/model/types';

export type KnowledgeFormState = {
  name: string;
  description: string;
  chunkType: ChunkType;
  chunkSize: number;
  chunkOverlap: number;
  semanticThreshold: number;
  childChunkSize: number;
  windowSize: number;
  enableParentChild: boolean;
  retrievalTopK: number;
  similarityThreshold: number;
  retrievalMode: RetrievalMode;
  filterExpression: string;
  keywordWeight: number;
  vectorWeight: number;
  rerankerEnabled: boolean;
  rerankerTopK: number;
};

export const defaultKnowledgeFormState: KnowledgeFormState = {
  name: '',
  description: '',
  chunkType: 'FIXED_SIZE',
  chunkSize: 800,
  chunkOverlap: 200,
  semanticThreshold: 0.5,
  childChunkSize: 200,
  windowSize: 2,
  enableParentChild: false,
  retrievalTopK: 5,
  similarityThreshold: 0.6,
  retrievalMode: 'HYBRID',
  filterExpression: '',
  keywordWeight: 0.3,
  vectorWeight: 0.7,
  rerankerEnabled: false,
  rerankerTopK: 5,
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

        <Typography sx={{ fontSize: 13, fontWeight: 700, color: 'text.secondary', mt: 0.5 }}>
          分块策略
        </Typography>
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
            <MenuItem value="SEMANTIC">语义分块</MenuItem>
          </TextField>
          <TextField type="number" label="检索 TopK" value={form.retrievalTopK} onChange={(e) => setForm((current) => ({ ...current, retrievalTopK: Number(e.target.value) }))} />
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField type="number" label="分块大小" value={form.chunkSize} onChange={(e) => setForm((current) => ({ ...current, chunkSize: Number(e.target.value) }))} />
          <TextField type="number" label="分块重叠" value={form.chunkOverlap} onChange={(e) => setForm((current) => ({ ...current, chunkOverlap: Number(e.target.value) }))} />
        </Box>
        {form.chunkType === 'SEMANTIC' && (
          <TextField
            type="number"
            label="语义断裂阈值"
            inputProps={{ min: 0, max: 1, step: 0.05 }}
            helperText="相邻语句 embedding 相似度低于该值时切分"
            value={form.semanticThreshold}
            onChange={(e) => setForm((current) => ({ ...current, semanticThreshold: Number(e.target.value) }))}
          />
        )}
        <FormControlLabel
          control={(
            <Switch
              checked={form.enableParentChild}
              onChange={(e) => setForm((current) => ({ ...current, enableParentChild: e.target.checked }))}
            />
          )}
          label="启用父子块架构"
        />
        {form.enableParentChild && (
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              type="number"
              label="子块大小"
              helperText="子块用于检索，父块用于组织上下文"
              value={form.childChunkSize}
              onChange={(e) => setForm((current) => ({ ...current, childChunkSize: Number(e.target.value) }))}
            />
            <TextField
              type="number"
              label="窗口扩展"
              helperText="命中子块后向前后扩展的子块数量"
              value={form.windowSize}
              onChange={(e) => setForm((current) => ({ ...current, windowSize: Number(e.target.value) }))}
            />
          </Box>
        )}

        <Divider flexItem />
        <Typography sx={{ fontSize: 13, fontWeight: 700, color: 'text.secondary' }}>
          检索与重排
        </Typography>
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
        <FormControlLabel
          control={(
            <Switch
              checked={form.rerankerEnabled}
              onChange={(e) => setForm((current) => ({ ...current, rerankerEnabled: e.target.checked }))}
            />
          )}
          label="启用 Reranker 重排"
        />
        {form.rerankerEnabled && (
          <TextField
            type="number"
            label="Reranker TopK"
            helperText="重排后最终返回给大模型的片段数"
            value={form.rerankerTopK}
            onChange={(e) => setForm((current) => ({ ...current, rerankerTopK: Number(e.target.value) }))}
          />
        )}
        <TextField
          label="过滤表达式（可选）"
          helperText="支持 file_name = README.md、file_type IN (md,pdf)、document_id = 12；多个条件用 AND"
          value={form.filterExpression}
          onChange={(e) => setForm((current) => ({ ...current, filterExpression: e.target.value }))}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(form)} disabled={loading} sx={{ borderRadius: 4 }}>{submitLabel}</Button>
      </DialogActions>
    </Dialog>
  );
}
