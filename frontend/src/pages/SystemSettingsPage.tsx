import { Box, Typography, Paper, Stack, TextField, FormControlLabel, Divider, Alert } from '@mui/material';
import { InkSwitch } from '../components/ink';
import { useState } from 'react';

export default function SystemSettingsPage() {
  const [config, setConfig] = useState({
    intentEnabled: true,
    intentConfidenceThreshold: 0.6,
    rewriteContextRounds: 5,
    memorySummaryThreshold: 20,
    memoryMaxChars: 12000,
    modelMaxRetries: 2,
    toolAutoExecuteThreshold: 0.8,
    traceSampleRate: 1.0,
  });

  return (
    <Box sx={{ p: 3, maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>系统设置</Typography>
      <Alert severity="info" sx={{ mb: 3 }}>
        编排配置通过后端 application.yml 的 app.orchestrator 节点管理，此页面为只读预览。
        修改配置需重启后端服务。
      </Alert>

      <Stack spacing={3}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>意图识别</Typography>
          <Stack spacing={2}>
            <FormControlLabel control={<InkSwitch checked={config.intentEnabled} />} label="启用意图识别" />
            <TextField label="意图置信度阈值" type="number" value={config.intentConfidenceThreshold}
              inputProps={{ step: 0.1, min: 0, max: 1 }} disabled size="small" />
          </Stack>
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>查询重写</Typography>
          <TextField label="上下文轮数" type="number" value={config.rewriteContextRounds}
            disabled size="small" fullWidth />
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>会话记忆</Typography>
          <Stack spacing={2}>
            <TextField label="摘要压缩阈值（消息数）" type="number" value={config.memorySummaryThreshold}
              disabled size="small" />
            <TextField label="最大上下文字符数" type="number" value={config.memoryMaxChars}
              disabled size="small" />
          </Stack>
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>模型路由</Typography>
          <TextField label="最大重试次数" type="number" value={config.modelMaxRetries}
            disabled size="small" fullWidth />
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>工具调用</Typography>
          <TextField label="自动执行置信度阈值" type="number" value={config.toolAutoExecuteThreshold}
            inputProps={{ step: 0.1, min: 0, max: 1 }} disabled size="small" fullWidth />
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>链路追踪</Typography>
          <TextField label="采样率" type="number" value={config.traceSampleRate}
            inputProps={{ step: 0.1, min: 0, max: 1 }} disabled size="small" fullWidth />
        </Paper>
      </Stack>
    </Box>
  );
}
