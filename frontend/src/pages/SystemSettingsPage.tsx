import {
  Box, Typography, Stack, TextField, FormControlLabel,
  Button, Alert, Snackbar, MenuItem, Select, InputLabel, FormControl,
  CircularProgress, Grid,
} from '@mui/material';
import { InkSwitch } from '../components/ink';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi } from '../api/settingsApi';
import { providerApi } from '../api/providerApi';
import { SystemSettings } from '../api/types';
import { useState, useEffect } from 'react';

const serifFont = '"Noto Serif SC", "Source Han Serif SC", serif';

const glassCard = {
  p: 2.5,
  borderRadius: '12px',
  backgroundColor: 'rgba(255,255,255,0.72)',
  backdropFilter: 'blur(12px)',
  WebkitBackdropFilter: 'blur(12px)',
  border: '1px solid rgba(224,221,216,0.5)',
  boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
};

export default function SystemSettingsPage() {
  const queryClient = useQueryClient();
  const { data: settings, isLoading, error } = useQuery({
    queryKey: ['systemSettings'],
    queryFn: settingsApi.get,
  });
  const { data: providers } = useQuery({
    queryKey: ['providers'],
    queryFn: providerApi.list,
  });

  const [form, setForm] = useState<SystemSettings | null>(null);
  const [toast, setToast] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false, message: '', severity: 'success',
  });

  useEffect(() => {
    if (settings && !form) setForm({ ...settings });
  }, [settings, form]);

  const mutation = useMutation({
    mutationFn: settingsApi.update,
    onSuccess: (saved) => {
      queryClient.setQueryData(['systemSettings'], saved);
      setForm({ ...saved });
      setToast({ open: true, message: '保存成功', severity: 'success' });
    },
    onError: () => {
      setToast({ open: true, message: '保存失败', severity: 'error' });
    },
  });

  if (isLoading) return <Box sx={{ p: 3, textAlign: 'center' }}><CircularProgress /></Box>;
  if (error) return <Alert severity="error" sx={{ m: 3 }}>加载设置失败</Alert>;
  if (!form) return null;

  const set = <K extends keyof SystemSettings>(key: K, value: SystemSettings[K]) =>
    setForm((prev) => prev ? { ...prev, [key]: value } : prev);

  const numField = (label: string, key: keyof SystemSettings, props?: Record<string, unknown>) => (
    <TextField
      label={label} type="number" size="small" fullWidth
      InputLabelProps={{ shrink: true }}
      value={form[key]}
      onChange={(e) => set(key, Number(e.target.value) as never)}
      {...props}
    />
  );

  const switchField = (label: string, key: keyof SystemSettings) => (
    <FormControlLabel
      control={<InkSwitch checked={form[key] as boolean} onChange={(_, v) => set(key, v as never)} />}
      label={<Typography sx={{ fontSize: 14 }}>{label}</Typography>}
    />
  );

  const sectionTitle = (text: string) => (
    <Typography sx={{ fontFamily: serifFont, fontWeight: 600, fontSize: 15, letterSpacing: 1, mb: 1.5, color: '#2C2C2C' }}>
      {text}
    </Typography>
  );

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 }, maxWidth: 1100, mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5" sx={{ fontFamily: serifFont, fontWeight: 700, letterSpacing: 2, color: '#2C2C2C' }}>
          系统设置
        </Typography>
        <Button
          variant="contained" disabled={mutation.isPending}
          onClick={() => mutation.mutate(form)}
          sx={{ fontFamily: serifFont, letterSpacing: 1 }}
        >
          {mutation.isPending ? '保存中...' : '保存'}
        </Button>
      </Box>

      <Grid container spacing={2}>
        {/* 左列 */}
        <Grid item xs={12} md={6}>
          <Stack spacing={2}>
            {/* 意图识别 */}
            <Box sx={glassCard}>
              {sectionTitle('意图识别')}
              <Stack spacing={1.5}>
                {switchField('启用意图识别', 'intentEnabled')}
                {numField('置信度阈值', 'intentConfidenceThreshold', { inputProps: { step: 0.1, min: 0, max: 1 } })}
              </Stack>
            </Box>

            {/* 查询重写 */}
            <Box sx={glassCard}>
              {sectionTitle('查询重写')}
              <Stack spacing={1.5}>
                {switchField('启用查询重写', 'rewriteEnabled')}
                {numField('上下文轮数', 'rewriteContextRounds', { inputProps: { min: 1 } })}
              </Stack>
            </Box>

            {/* 检索 */}
            <Box sx={glassCard}>
              {sectionTitle('检索')}
              <Stack spacing={1.5}>
                {switchField('启用多路召回', 'multiPathRetrievalEnabled')}
                {numField('Rerank TopK', 'rerankTopK', { inputProps: { min: 1 } })}
                {numField('检索超时（秒）', 'retrievalTimeoutSeconds', { inputProps: { min: 1 } })}
                {numField('去重前缀长度', 'deduplicatePrefixLength', { inputProps: { min: 0 } })}
              </Stack>
            </Box>

            {/* 会话记忆 */}
            <Box sx={glassCard}>
              {sectionTitle('会话记忆')}
              <Stack spacing={1.5}>
                {numField('完整轮数', 'memoryFullRounds', { inputProps: { min: 1 } })}
                {numField('最大上下文字符数', 'memoryMaxChars', { inputProps: { min: 1000 } })}
                {numField('摘要压缩阈值', 'memorySummaryThreshold', { inputProps: { min: 1 } })}
              </Stack>
            </Box>
          </Stack>
        </Grid>

        {/* 右列 */}
        <Grid item xs={12} md={6}>
          <Stack spacing={2}>
            {/* 模型路由 */}
            <Box sx={glassCard}>
              {sectionTitle('模型路由')}
              <Stack spacing={1.5}>
                {switchField('启用模型降级', 'modelFallbackEnabled')}
                {numField('最大重试次数', 'modelMaxRetries', { inputProps: { min: 0 } })}
                {numField('健康检查间隔（分钟）', 'healthCheckIntervalMinutes', { inputProps: { min: 1 } })}
              </Stack>
            </Box>

            {/* 工具调用 */}
            <Box sx={glassCard}>
              {sectionTitle('工具调用')}
              <Stack spacing={1.5}>
                {numField('自动执行置信度阈值', 'toolAutoExecuteThreshold', { inputProps: { step: 0.1, min: 0, max: 1 } })}
                {switchField('启用语义工具检索', 'toolSemanticRetrievalEnabled')}
                {numField('召回工具数量 (topK)', 'toolRetrievalTopK', { inputProps: { step: 1, min: 1, max: 50 } })}
                {numField('最低相似度阈值', 'toolRetrievalThreshold', { inputProps: { step: 0.05, min: 0, max: 1 } })}
                {switchField('启用工具降级', 'toolFallbackEnabled')}
              </Stack>
            </Box>

            {/* 链路追踪 */}
            <Box sx={glassCard}>
              {sectionTitle('链路追踪')}
              <Stack spacing={1.5}>
                {switchField('启用追踪', 'traceEnabled')}
                {numField('采样率', 'traceSampleRate', { inputProps: { step: 0.1, min: 0, max: 1 } })}
              </Stack>
            </Box>

            {/* 辅助任务 */}
            <Box sx={glassCard}>
              {sectionTitle('辅助任务')}
              <Stack spacing={1.5}>
                <FormControl size="small" fullWidth>
                  <InputLabel>辅助 Provider</InputLabel>
                  <Select
                    label="辅助 Provider"
                    value={form.auxiliaryProviderId ?? ''}
                    onChange={(e) => set('auxiliaryProviderId', e.target.value === '' ? null : Number(e.target.value))}
                  >
                    <MenuItem value="">跟随对话 Provider</MenuItem>
                    {providers?.filter((p) => p.enabled).map((p) => (
                      <MenuItem key={p.id} value={p.id}>{p.name} ({p.defaultModel})</MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  label="推荐问题提示词"
                  multiline rows={5} size="small" fullWidth
                  value={form.suggestionPrompt}
                  onChange={(e) => set('suggestionPrompt', e.target.value)}
                  helperText="使用 {conversation} 作为对话上下文占位符"
                />
              </Stack>
            </Box>
          </Stack>
        </Grid>
      </Grid>

      <Snackbar
        open={toast.open} autoHideDuration={3000}
        onClose={() => setToast((t) => ({ ...t, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity={toast.severity} onClose={() => setToast((t) => ({ ...t, open: false }))}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
