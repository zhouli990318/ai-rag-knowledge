import { useState, useCallback, useEffect } from 'react';
import {
  Box, Typography, Autocomplete, TextField, Chip, Stack,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  CircularProgress, Alert, Collapse, IconButton,
} from '@mui/material';
import { ExpandMoreOutlined as ExpandMore, ExpandLessOutlined as ExpandLess, CheckCircleOutlined, ErrorOutlineOutlined } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { traceApi } from '@/entities/orchestration/api/orchestrationApi';
import type { ChatTrace, TraceSpan } from '@/entities/orchestration/model/types';
import { chatApi } from '@/entities/chat/api/chatApi';
import type { Conversation } from '@/entities/chat/model/types';
import { serifFont } from '@/shared/theme/ThemeProvider';
import { getGlassCard } from '@/shared/theme/tokens';
import { traceStageColor, ui } from '@/shared/theme/semanticColors';
import { useThemeStore } from '@/shared/stores/themeStore';

const stageLabels: Record<string, string> = {
  REWRITE: '查询重写',
  INTENT: '意图识别',
  RETRIEVAL: '知识检索',
  RERANK: '重排序',
  TOOL: '工具路由',
  GENERATION: '模型生成',
  PERSIST: '持久化',
};

const stageColor = traceStageColor;

function SpanRow({ span }: { span: TraceSpan }) {
  const [open, setOpen] = useState(false);
  const color = stageColor(span.stage, span.success);
  return (
    <>
      <TableRow sx={{ '&:hover': { backgroundColor: ui.hoverBg } }}>
        <TableCell>
          <Chip label={stageLabels[span.stage] || span.stage} size="small"
            sx={{ borderColor: color, color, fontWeight: 500 }} variant="outlined" />
        </TableCell>
        <TableCell>
          <Typography sx={{ fontFamily: 'monospace', fontSize: 13, fontWeight: 600, color: span.durationMs > 500 ? ui.accentRed : ui.textPrimary }}>
            {span.durationMs}ms
          </Typography>
        </TableCell>
        <TableCell>
          <Box sx={{
            display: 'inline-flex', alignItems: 'center', gap: 0.5, px: 1, py: 0.25,
            borderRadius: '4px',
            backgroundColor: span.success ? 'rgba(44,110,73,0.1)' : 'rgba(200,75,49,0.1)',
            color: span.success ? ui.textPrimary : ui.accentRed,
            fontSize: 12, fontWeight: 500,
          }}>
            {span.success
              ? <><CheckCircleOutlined sx={{ fontSize: 13 }} /> 成功</>
              : <><ErrorOutlineOutlined sx={{ fontSize: 13 }} /> 失败</>
            }
          </Box>
        </TableCell>
        <TableCell>
          {Object.keys(span.attributes || {}).length > 0 && (
            <IconButton size="small" onClick={() => setOpen(!open)} sx={{ color: ui.textMuted }}>
              {open ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
            </IconButton>
          )}
        </TableCell>
      </TableRow>
      {open && (
        <TableRow>
          <TableCell colSpan={4} sx={{ py: 0 }}>
            <Collapse in={open}>
              <Box sx={{ p: 1.5, backgroundColor: ui.hoverBg, borderRadius: '8px', my: 0.5 }}>
                {Object.entries(span.attributes || {}).map(([k, v]) => (
                  <Typography key={k} variant="caption" display="block" sx={{ color: ui.textSecondary, lineHeight: 1.8 }}>
                    <strong style={{ color: ui.textPrimary }}>{k}:</strong> {v}
                  </Typography>
                ))}
                {span.errorMessage && (
                  <Typography variant="caption" sx={{ color: ui.accentRed }}>
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
  const mode = useThemeStore((s) => s.mode);
  const glassCard = getGlassCard(mode);
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
      const data = await traceApi.getByConversation(convId);
      setTraces(data || []);
      // 默认展开最新 trace
      if (data && data.length > 0) setExpandedTrace(data[0].traceId);
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
      <Typography variant="h5" sx={{ fontFamily: serifFont, fontWeight: 700, letterSpacing: 2, color: ui.textPrimary, mb: 2.5 }}>
        墨迹溯源
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
          <Typography sx={{ color: ui.textMuted, fontFamily: serifFont }}>选择一个会话查看其全链路追踪</Typography>
        </Box>
      ) : traces.length === 0 ? (
        <Box sx={{ ...glassCard, p: 4, textAlign: 'center' }}>
          <Typography sx={{ color: ui.textMuted, fontFamily: serifFont }}>该会话暂无追踪记录</Typography>
        </Box>
      ) : (
        <Stack spacing={2}>
          {traces.map(trace => (
            <Box key={trace.traceId} sx={{ ...glassCard, p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Box>
                  <Typography sx={{ fontFamily: 'monospace', fontSize: 13, color: ui.textSecondary, letterSpacing: 0.5 }}>
                    {trace.traceId}
                  </Typography>
                  <Typography sx={{ fontSize: 12, color: ui.textMuted, mt: 0.25 }}>
                    总耗时 <strong style={{ color: ui.accentRed }}>{trace.totalDurationMs}ms</strong> · {new Date(trace.createdAt).toLocaleString()}
                  </Typography>
                </Box>
                <IconButton onClick={() => setExpandedTrace(expandedTrace === trace.traceId ? null : trace.traceId)}
                  sx={{ color: ui.textMuted }}>
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
                        <TableCell sx={{ fontWeight: 600, color: ui.textSecondary, fontSize: 13 }}>阶段</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: ui.textSecondary, fontSize: 13 }}>耗时</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: ui.textSecondary, fontSize: 13 }}>状态</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: ui.textSecondary, fontSize: 13 }}>详情</TableCell>
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
