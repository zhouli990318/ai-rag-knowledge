import { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, LinearProgress, Chip,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  CircularProgress, Alert, FormControl, InputLabel, Select, MenuItem, Stack,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { etlTaskApi } from '@/entities/orchestration/api/orchestrationApi';
import type { EtlTask } from '@/entities/orchestration/model/types';
import { knowledgeApi } from '@/entities/knowledge/api/knowledgeApi';
import { serifFont } from '@/shared/theme/ThemeProvider';
import { getGlassCard } from '@/shared/theme/tokens';
import { stageStyles, progressBarColor, ui } from '@/shared/theme/semanticColors';
import { useThemeStore } from '@/shared/stores/themeStore';

const stageStyle = stageStyles;

export default function IngestMonitorPage() {
  const mode = useThemeStore((s) => s.mode);
  const glassCard = getGlassCard(mode);
  const [tasks, setTasks] = useState<EtlTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [kbId, setKbId] = useState<number | ''>('');

  const { data: kbs = [] } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });

  const loadTasks = useCallback(async () => {
    if (!kbId) return;
    try {
      setLoading(true);
      const data = await etlTaskApi.getByKnowledgeBase(kbId as number);
      setTasks(data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [kbId]);

  useEffect(() => { if (kbId) loadTasks(); }, [kbId, loadTasks]);

  useEffect(() => {
    const hasActive = tasks.some(t => !['COMPLETED', 'FAILED'].includes(t.currentStage));
    if (!hasActive) return;
    const interval = setInterval(loadTasks, 3000);
    return () => clearInterval(interval);
  }, [tasks, loadTasks]);

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 } }}>
      <Typography variant="h5" sx={{ fontFamily: serifFont, fontWeight: 700, letterSpacing: 2, color: ui.textPrimary, mb: 2.5 }}>
        文墨入库
      </Typography>

      <Box sx={{ mb: 2.5 }}>
        <FormControl sx={{ minWidth: 280 }} size="small">
          <InputLabel>选择知识库</InputLabel>
          <Select
            label="选择知识库"
            value={kbId}
            onChange={e => setKbId(e.target.value as number)}
          >
            {kbs.length === 0 && (
              <MenuItem disabled value=""><em>暂无知识库</em></MenuItem>
            )}
            {kbs.map(kb => (
              <MenuItem key={kb.id} value={kb.id}>{kb.name}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
      ) : tasks.length === 0 ? (
        <Box sx={{ ...glassCard, p: 4, textAlign: 'center' }}>
          <Typography sx={{ color: ui.textMuted, fontFamily: serifFont }}>
            {kbId ? '该知识库暂无入库任务' : '选择知识库查看入库任务'}
          </Typography>
        </Box>
      ) : (
        <Box sx={{ ...glassCard, overflow: 'hidden' }}>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ '& th': { fontWeight: 600, color: ui.textSecondary, fontSize: 13, borderBottom: `1px solid ${ui.tableBorder}` } }}>
                  <TableCell>ID</TableCell>
                  <TableCell>类型</TableCell>
                  <TableCell>当前阶段</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>进度</TableCell>
                  <TableCell>错误信息</TableCell>
                  <TableCell>创建时间</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tasks.map(task => {
                  const s = stageStyle[task.currentStage] || stageStyle.PENDING;
                  return (
                    <TableRow key={task.id} sx={{ '&:hover': { backgroundColor: ui.hoverBg }, '& td': { borderBottom: '1px solid rgba(224,221,216,0.3)' } }}>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: 13 }}>{task.id}</TableCell>
                      <TableCell>
                        <Chip label={task.taskType} size="small" variant="outlined"
                          sx={{ borderColor: ui.border, color: ui.textSecondary, fontSize: 12 }} />
                      </TableCell>
                      <TableCell>
                        <Box sx={{
                          display: 'inline-flex', px: 1, py: 0.25, borderRadius: '4px',
                          backgroundColor: s.bg, color: s.color, fontSize: 12, fontWeight: 500,
                        }}>
                          {task.currentStage}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <LinearProgress variant="determinate" value={task.progress}
                            sx={{
                              flex: 1, height: 6, borderRadius: 3,
                              backgroundColor: 'rgba(224,221,216,0.4)',
                              '& .MuiLinearProgress-bar': {
                              backgroundColor: progressBarColor(task.currentStage),
                                borderRadius: 3,
                              },
                            }} />
                          <Typography sx={{ fontSize: 12, color: ui.textSecondary, fontWeight: 500, minWidth: 36 }}>
                            {task.progress}%
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        {task.errorMessage && (
                          <Typography sx={{ fontSize: 12, color: ui.accentRed, maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {task.errorMessage}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell sx={{ fontSize: 12, color: ui.textMuted }}>
                        {new Date(task.createdAt).toLocaleString()}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}
    </Box>
  );
}
