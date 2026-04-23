import { useState, useCallback, useEffect } from 'react';
import {
  Box, Typography, Paper, Autocomplete, TextField, Chip, Stack,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  CircularProgress, Alert, Collapse, IconButton,
} from '@mui/material';
import { ExpandMore, ExpandLess } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { traceApi, ChatTrace, TraceSpan } from '../api/orchestrationApi';
import { chatApi } from '../api/chatApi';
import { Conversation } from '../api/types';

const stageLabels: Record<string, string> = {
  REWRITE: '查询重写',
  INTENT: '意图识别',
  RETRIEVAL: '知识检索',
  RERANK: '重排序',
  TOOL: '工具路由',
  GENERATION: '模型生成',
  PERSIST: '持久化',
};

function SpanRow({ span }: { span: TraceSpan }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <TableRow>
        <TableCell>
          <Chip label={stageLabels[span.stage] || span.stage} size="small"
            color={span.success ? 'primary' : 'error'} variant="outlined" />
        </TableCell>
        <TableCell>{span.durationMs}ms</TableCell>
        <TableCell>
          <Chip label={span.success ? '成功' : '失败'} size="small"
            color={span.success ? 'success' : 'error'} />
        </TableCell>
        <TableCell>
          {Object.keys(span.attributes || {}).length > 0 && (
            <IconButton size="small" onClick={() => setOpen(!open)}>
              {open ? <ExpandLess /> : <ExpandMore />}
            </IconButton>
          )}
        </TableCell>
      </TableRow>
      {open && (
        <TableRow>
          <TableCell colSpan={4} sx={{ py: 0 }}>
            <Collapse in={open}>
              <Box sx={{ p: 1, bgcolor: 'grey.50', borderRadius: 1 }}>
                {Object.entries(span.attributes || {}).map(([k, v]) => (
                  <Typography key={k} variant="caption" display="block">
                    <strong>{k}:</strong> {v}
                  </Typography>
                ))}
                {span.errorMessage && (
                  <Typography variant="caption" color="error">
                    <strong>Error:</strong> {span.errorMessage}
                  </Typography>
                )}
              </Box>
            </Collapse>
          </TableCell>
        </TableRow>
      )}
    </>
  );
}

export default function TracePage() {
  const [selectedConv, setSelectedConv] = useState<Conversation | null>(null);
  const [traces, setTraces] = useState<ChatTrace[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedTrace, setExpandedTrace] = useState<string | null>(null);

  const { data: conversations = [] } = useQuery({
    queryKey: ['conversations'],
    queryFn: chatApi.getConversations,
  });

  const loadTraces = useCallback(async (convId: number) => {
    try {
      setLoading(true);
      setError(null);
      const res = await traceApi.getByConversation(convId);
      setTraces(res.data.data || []);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedConv) {
      loadTraces(selectedConv.id);
    } else {
      setTraces([]);
    }
  }, [selectedConv, loadTraces]);

  return (
    <Box sx={{ p: 3, maxWidth: 1000, mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>链路追踪</Typography>

      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <Autocomplete
          sx={{ minWidth: 380 }}
          size="small"
          options={conversations}
          value={selectedConv}
          onChange={(_, v) => setSelectedConv(v)}
          getOptionLabel={(c) => `${c.title || '新对话'} · #${c.id}`}
          isOptionEqualToValue={(a, b) => a.id === b.id}
          renderInput={(params) => <TextField {...params} label="选择会话" placeholder="搜索或选择会话..." />}
          noOptionsText="暂无会话记录"
        />
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
      ) : !selectedConv ? (
        <Alert severity="info">选择一个会话查看其全链路追踪</Alert>
      ) : traces.length === 0 ? (
        <Alert severity="info">该会话暂无追踪记录</Alert>
      ) : (
        <Stack spacing={2}>
          {traces.map(trace => (
            <Paper key={trace.traceId} sx={{ p: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Box>
                  <Typography variant="subtitle2" sx={{ fontFamily: 'monospace' }}>
                    {trace.traceId}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    总耗时 {trace.totalDurationMs}ms · {new Date(trace.createdAt).toLocaleString()}
                  </Typography>
                </Box>
                <IconButton onClick={() => setExpandedTrace(
                  expandedTrace === trace.traceId ? null : trace.traceId)}>
                  {expandedTrace === trace.traceId ? <ExpandLess /> : <ExpandMore />}
                </IconButton>
              </Box>

              {/* 阶段简览条 */}
              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                {trace.spans?.map(span => (
                  <Chip key={span.spanId} label={`${stageLabels[span.stage] || span.stage} ${span.durationMs}ms`}
                    size="small" color={span.success ? 'default' : 'error'} variant="outlined" />
                ))}
              </Box>

              <Collapse in={expandedTrace === trace.traceId}>
                <TableContainer sx={{ mt: 2 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>阶段</TableCell>
                        <TableCell>耗时</TableCell>
                        <TableCell>状态</TableCell>
                        <TableCell>详情</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {trace.spans?.map(span => (
                        <SpanRow key={span.spanId} span={span} />
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Collapse>
            </Paper>
          ))}
        </Stack>
      )}
    </Box>
  );
}
