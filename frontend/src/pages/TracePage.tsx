import { useState, useCallback, useEffect } from 'react';
import {
  Box, Typography, Autocomplete, TextField, Chip, Stack,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  CircularProgress, Alert, Collapse, IconButton,
} from '@mui/material';
import { ExpandMore, ExpandLess } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { traceApi, ChatTrace, TraceSpan } from '../api/orchestrationApi';
import { chatApi } from '../api/chatApi';
import { Conversation } from '../api/types';

const serifFont = '"Noto Serif SC", "Source Han Serif SC", serif';

const glassCard = {
  borderRadius: '12px',
  backgroundColor: 'rgba(255,255,255,0.72)',
  backdropFilter: 'blur(12px)',
  WebkitBackdropFilter: 'blur(12px)',
  border: '1px solid rgba(224,221,216,0.5)',
  boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
};

const stageLabels: Record<string, string> = {
  REWRITE: '查询重写',
  INTENT: '意图识别',
  RETRIEVAL: '知识检索',
  RERANK: '重排序',
  TOOL: '工具路由',
  GENERATION: '模型生成',
  PERSIST: '持久化',
};

const stageColor = (stage: string, success: boolean): string => {
  if (!success) return '#C84B31';
  const map: Record<string, string> = {
    REWRITE: '#5B7065', INTENT: '#5B7065', RETRIEVAL: '#2C6E49',
    RERANK: '#4A6FA5', TOOL: '#8B6914', GENERATION: '#C84B31', PERSIST: '#8B8B8B',
  };
  return map[stage] || '#4A4A4A';
};

function SpanRow({ span }: { span: TraceSpan }) {
  const [open, setOpen] = useState(false);
  const color = stageColor(span.stage, span.success);
  return (
    <>
      <TableRow sx={{ '&:hover': { backgroundColor: 'rgba(245,243,238,0.5)' } }}>
        <TableCell>
          <Chip label={stageLabels[span.stage] || span.stage} size="small"
            sx={{ borderColor: color, color, fontWeight: 500 }} variant="outlined" />
        </TableCell>
        <TableCell>
          <Typography sx={{ fontFamily: 'monospace', fontSize: 13, fontWeight: 600, color: span.durationMs > 500 ? '#C84B31' : '#2C2C2C' }}>
            {span.durationMs}ms
          </Typography>
        </TableCell>
        <TableCell>
          <Box sx={{
            display: 'inline-flex', alignItems: 'center', gap: 0.5, px: 1, py: 0.25,
            borderRadius: '4px',
            backgroundColor: span.success ? 'rgba(91,112,101,0.1)' : 'rgba(200,75,49,0.1)',
            color: span.success ? '#5B7065' : '#C84B31',
            fontSize: 12, fontWeight: 500,
          }}>
            {span.success ? '成功' : '失败'}
          </Box>
        </TableCell>
        <TableCell>
          {Object.keys(span.attributes || {}).length > 0 && (
            <IconButton size="small" onClick={() => setOpen(!open)} sx={{ color: '#8B8B8B' }}>
              {open ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
            </IconButton>
          )}
        </TableCell>
      </TableRow>
      {open && (
        <TableRow>
          <TableCell colSpan={4} sx={{ py: 0 }}>
            <Collapse in={open}>
              <Box sx={{ p: 1.5, backgroundColor: 'rgba(245,243,238,0.5)', borderRadius: '8px', my: 0.5 }}>
                {Object.entries(span.attributes || {}).map(([k, v]) => (
                  <Typography key={k} variant="caption" display="block" sx={{ color: '#4A4A4A', lineHeight: 1.8 }}>
                    <strong style={{ color: '#2C2C2C' }}>{k}:</strong> {v}
                  </Typography>
                ))}
                {span.errorMessage && (
                  <Typography variant="caption" sx={{ color: '#C84B31' }}>
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
      const data = res.data.data || [];
      setTraces(data);
      // 默认展开最新 trace
      if (data.length > 0) setExpandedTrace(data[0].traceId);
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
    <Box sx={{ p: { xs: 2, md: 2.5 } }}>
      <Typography variant="h5" sx={{ fontFamily: serifFont, fontWeight: 700, letterSpacing: 2, color: '#2C2C2C', mb: 2.5 }}>
        链路追踪
      </Typography>

      <Box sx={{ mb: 2.5 }}>
        <Autocomplete
          sx={{ maxWidth: 420 }}
          size="small"
          options={conversations}
          value={selectedConv}
          onChange={(_, v) => setSelectedConv(v)}
          getOptionLabel={(c) => `${c.title || '新对话'} · #${c.id}`}
          isOptionEqualToValue={(a, b) => a.id === b.id}
          renderInput={(params) => <TextField {...params} label="选择会话" placeholder="搜索或选择会话..." />}
          noOptionsText="暂无会话记录"
        />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
      ) : !selectedConv ? (
        <Box sx={{ ...glassCard, p: 4, textAlign: 'center' }}>
          <Typography sx={{ color: '#8B8B8B', fontFamily: serifFont }}>选择一个会话查看其全链路追踪</Typography>
        </Box>
      ) : traces.length === 0 ? (
        <Box sx={{ ...glassCard, p: 4, textAlign: 'center' }}>
          <Typography sx={{ color: '#8B8B8B', fontFamily: serifFont }}>该会话暂无追踪记录</Typography>
        </Box>
      ) : (
        <Stack spacing={2}>
          {traces.map(trace => (
            <Box key={trace.traceId} sx={{ ...glassCard, p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Box>
                  <Typography sx={{ fontFamily: 'monospace', fontSize: 13, color: '#4A4A4A', letterSpacing: 0.5 }}>
                    {trace.traceId}
                  </Typography>
                  <Typography sx={{ fontSize: 12, color: '#8B8B8B', mt: 0.25 }}>
                    总耗时 <strong style={{ color: '#C84B31' }}>{trace.totalDurationMs}ms</strong> · {new Date(trace.createdAt).toLocaleString()}
                  </Typography>
                </Box>
                <IconButton onClick={() => setExpandedTrace(expandedTrace === trace.traceId ? null : trace.traceId)}
                  sx={{ color: '#8B8B8B' }}>
                  {expandedTrace === trace.traceId ? <ExpandLess /> : <ExpandMore />}
                </IconButton>
              </Box>

              {/* 阶段简览条 */}
              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                {trace.spans?.map(span => (
                  <Chip key={span.spanId}
                    label={`${stageLabels[span.stage] || span.stage} ${span.durationMs}ms`}
                    size="small" variant="outlined"
                    sx={{
                      borderColor: stageColor(span.stage, span.success),
                      color: stageColor(span.stage, span.success),
                      fontSize: 12,
                    }}
                  />
                ))}
              </Box>

              <Collapse in={expandedTrace === trace.traceId}>
                <TableContainer sx={{ mt: 2 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 600, color: '#4A4A4A', fontSize: 13 }}>阶段</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#4A4A4A', fontSize: 13 }}>耗时</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#4A4A4A', fontSize: 13 }}>状态</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#4A4A4A', fontSize: 13 }}>详情</TableCell>
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
            </Box>
          ))}
        </Stack>
      )}
    </Box>
  );
}
