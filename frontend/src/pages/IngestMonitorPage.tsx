import { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Paper, LinearProgress, Chip, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, CircularProgress,
  Alert, FormControl, InputLabel, Select, MenuItem, Stack,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { etlTaskApi, EtlTask } from '../api/orchestrationApi';
import { knowledgeApi } from '../api/knowledgeApi';

const stageColors: Record<string, 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info'> = {
  PENDING: 'default',
  FETCH: 'info',
  PARSE: 'info',
  ENHANCE: 'info',
  CHUNK: 'primary',
  VECTORIZE: 'primary',
  WRITE: 'primary',
  COMPLETED: 'success',
  FAILED: 'error',
};

export default function IngestMonitorPage() {
  const [tasks, setTasks] = useState<EtlTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [kbId, setKbId] = useState<number | ''>('');

  const { data: kbs = [] } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });

  const loadTasks = useCallback(async () => {
    if (!kbId) return;
    try {
      setLoading(true);
      const res = await etlTaskApi.getByKnowledgeBase(kbId as number);
      setTasks(res.data.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [kbId]);

  useEffect(() => { if (kbId) loadTasks(); }, [kbId, loadTasks]);

  // 自动刷新进行中的任务
  useEffect(() => {
    const hasActive = tasks.some(t => !['COMPLETED', 'FAILED'].includes(t.currentStage));
    if (!hasActive) return;
    const interval = setInterval(loadTasks, 3000);
    return () => clearInterval(interval);
  }, [tasks, loadTasks]);

  return (
    <Box sx={{ p: 3, maxWidth: 1000, mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>入库监控</Typography>

      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <FormControl sx={{ minWidth: 260 }}>
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
      </Stack>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
      ) : tasks.length === 0 ? (
        <Alert severity="info">选择知识库查看入库任务</Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>类型</TableCell>
                <TableCell>当前阶段</TableCell>
                <TableCell>进度</TableCell>
                <TableCell>错误信息</TableCell>
                <TableCell>创建时间</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tasks.map(task => (
                <TableRow key={task.id}>
                  <TableCell>{task.id}</TableCell>
                  <TableCell><Chip label={task.taskType} size="small" /></TableCell>
                  <TableCell>
                    <Chip label={task.currentStage} size="small"
                      color={stageColors[task.currentStage] || 'default'} />
                  </TableCell>
                  <TableCell sx={{ minWidth: 120 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <LinearProgress variant="determinate" value={task.progress}
                        sx={{ flex: 1, height: 6, borderRadius: 3 }} />
                      <Typography variant="caption">{task.progress}%</Typography>
                    </Box>
                  </TableCell>
                  <TableCell>
                    {task.errorMessage && (
                      <Typography variant="caption" color="error" sx={{ maxWidth: 200, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {task.errorMessage}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>{new Date(task.createdAt).toLocaleString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
