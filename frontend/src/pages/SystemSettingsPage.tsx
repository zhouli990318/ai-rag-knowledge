import {
  Box, Typography, Stack, TextField, FormControlLabel,
  Button, Alert, Snackbar, MenuItem, Select, FormControl,
  CircularProgress, Grid, Tabs, Tab,
} from '@mui/material';
import { InkSwitch } from '@/shared/ui/ink';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi } from '@/entities/settings/api/settingsApi';
import { providerApi } from '@/entities/provider/api/providerApi';
import type { SystemSettings } from '@/entities/settings/model/types';
import { useInk, serifFont, radius } from '@/shared/theme/ThemeProvider';
import { useState, useEffect } from 'react';

const TAB_KEY = 'system-settings-tab';

export default function SystemSettingsPage() {
  const queryClient = useQueryClient();
  const di = useInk();
  const normalizeSettings = (settings: SystemSettings): SystemSettings => ({
    ...settings,
    rerankerApiKeyMasked: settings.rerankerApiKeyMasked ?? '',
    rerankerApiKey: settings.rerankerApiKeyMasked ?? '',
    clearRerankerApiKey: false,
  });
  const toSubmitSettings = (settings: SystemSettings): SystemSettings => ({
    ...settings,
    rerankerApiKey: settings.clearRerankerApiKey
      ? ''
      : settings.rerankerApiKey === settings.rerankerApiKeyMasked
        ? ''
        : settings.rerankerApiKey ?? '',
  });
  const cardSx = {
    p: 2.5,
    borderRadius: `${radius.md + 2}px`,
    backgroundColor: di.glassBg,
    backdropFilter: 'blur(12px) saturate(180%)',
    WebkitBackdropFilter: 'blur(12px) saturate(180%)',
    border: `1px solid ${di.glassBorder}`,
    boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
  };
  const [activeTab, setActiveTab] = useState(() => {
    const saved = localStorage.getItem(TAB_KEY);
    return saved ? Number(saved) : 0;
  });

  const handleTabChange = (_: React.SyntheticEvent, v: number) => {
    setActiveTab(v);
    localStorage.setItem(TAB_KEY, String(v));
  };

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
    if (settings && !form) setForm(normalizeSettings(settings));
  }, [settings, form]);

  const mutation = useMutation({
    mutationFn: settingsApi.update,
    onSuccess: (saved) => {
      queryClient.setQueryData(['systemSettings'], saved);
      setForm(normalizeSettings(saved));
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
    <Box>
      <Typography sx={{ fontSize: 13, color: di.lightGray, mb: 0.5 }}>{label}</Typography>
      <TextField
        type="number" size="small" fullWidth
        value={form[key]}
        onChange={(e) => set(key, Number(e.target.value) as never)}
        {...props}
      />
    </Box>
  );

  const switchField = (label: string, key: keyof SystemSettings) => (
    <FormControlLabel
      control={<InkSwitch checked={form[key] as boolean} onChange={(_, v) => set(key, v as never)} />}
      label={<Typography sx={{ fontSize: 14, color: di.black }}>{label}</Typography>}
      sx={{ ml: 0, mr: 0, justifyContent: 'space-between', width: '100%' }}
      labelPlacement="start"
    />
  );

  const textField = (label: string, key: keyof SystemSettings, props?: Record<string, unknown>) => (
    <Box>
      <Typography sx={{ fontSize: 13, color: di.lightGray, mb: 0.5 }}>{label}</Typography>
      <TextField
        size="small" fullWidth
        value={form[key] as string}
        onChange={(e) => set(key, e.target.value as never)}
        {...props}
      />
    </Box>
  );

  const sectionTitle = (text: string) => (
    <Typography sx={{ fontFamily: serifFont, fontWeight: 600, fontSize: 15, letterSpacing: 1, mb: 1.5, color: di.black }}>
      {text}
    </Typography>
  );

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 }, maxWidth: 1100, mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ fontFamily: serifFont, fontWeight: 700, letterSpacing: 2, color: di.black }}>
          系统设置
        </Typography>
        <Button
          variant="contained" disabled={mutation.isPending}
          onClick={() => mutation.mutate(toSubmitSettings(form))}
          sx={{ fontFamily: serifFont, letterSpacing: 1 }}
        >
          {mutation.isPending ? '保存中...' : '保存'}
        </Button>
      </Box>

      {/* Tab 分组 */}
      <Tabs
        value={activeTab}
        onChange={handleTabChange}
        sx={{
          mb: 2.5,
          minHeight: 36,
          '& .MuiTab-root': {
            fontFamily: serifFont,
            fontSize: 14,
            fontWeight: 500,
            minHeight: 36,
            textTransform: 'none',
            letterSpacing: 1,
          },
        }}
      >
        <Tab label="基础配置" />
        <Tab label="模型算法" />
        <Tab label="高级调优" />
      </Tabs>

      {/* Tab 0: 基础配置 */}
      {activeTab === 0 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Stack spacing={2}>
              <Box sx={cardSx}>
                {sectionTitle('会话记忆')}
                <Stack spacing={1.5}>
                  {numField('完整轮数', 'memoryFullRounds', { inputProps: { min: 1 } })}
                  {numField('最大上下文字符数', 'memoryMaxChars', { inputProps: { min: 1000 } })}
                  {numField('摘要压缩阈值', 'memorySummaryThreshold', { inputProps: { min: 1 } })}
                </Stack>
              </Box>
            </Stack>
          </Grid>
          <Grid item xs={12} md={6}>
            <Stack spacing={2}>
              <Box sx={cardSx}>
                {sectionTitle('查询重写')}
                <Stack spacing={1.5}>
                  {switchField('启用查询重写', 'rewriteEnabled')}
                  {switchField('启用 HyDE 假设文档', 'hydeEnabled')}
                  {numField('上下文轮数', 'rewriteContextRounds', { inputProps: { min: 1 } })}
                </Stack>
              </Box>
            </Stack>
          </Grid>
        </Grid>
      )}

      {/* Tab 1: 模型算法 */}
      {activeTab === 1 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Stack spacing={2}>
              <Box sx={cardSx}>
                {sectionTitle('模型路由')}
                <Stack spacing={1.5}>
                  {switchField('启用模型降级', 'modelFallbackEnabled')}
                  {numField('最大重试次数', 'modelMaxRetries', { inputProps: { min: 0 } })}
                  {numField('健康检查间隔（分钟）', 'healthCheckIntervalMinutes', { inputProps: { min: 1 } })}
                </Stack>
              </Box>
            </Stack>
          </Grid>
          <Grid item xs={12} md={6}>
            <Stack spacing={2}>
              <Box sx={cardSx}>
                {sectionTitle('意图识别')}
                <Stack spacing={1.5}>
                  {switchField('启用意图识别', 'intentEnabled')}
                  {numField('置信度阈值', 'intentConfidenceThreshold', { inputProps: { step: 0.1, min: 0, max: 1 } })}
                </Stack>
              </Box>
            </Stack>
          </Grid>
        </Grid>
      )}

      {/* Tab 2: 高级调优 */}
      {activeTab === 2 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Stack spacing={2}>
              <Box sx={cardSx}>
                {sectionTitle('Reranker 服务')}
                <Stack spacing={1.5}>
                  {switchField('启用 Cross-Encoder Reranker', 'rerankerServiceEnabled')}
                  {textField('服务地址', 'rerankerBaseUrl', {
                    placeholder: 'http://localhost:8080',
                    helperText: '运行时调用的 reranker HTTP 服务地址',
                  })}
                  {textField('模型名称', 'rerankerModel', {
                    placeholder: 'bge-reranker-v2-m3',
                    helperText: '发送给 reranker 服务的模型标识',
                  })}
                  {textField('API Key', 'rerankerApiKey', {
                    type: form.rerankerApiKeyMasked && form.rerankerApiKey === form.rerankerApiKeyMasked ? 'text' : 'password',
                    autoComplete: 'off',
                    disabled: !!form.clearRerankerApiKey,
                    onFocus: () => {
                      if (form.rerankerApiKeyMasked && form.rerankerApiKey === form.rerankerApiKeyMasked) {
                        set('rerankerApiKey', '');
                      }
                    },
                  })}
                  {form.rerankerApiKeyConfigured && switchField('保存时清除当前 API Key', 'clearRerankerApiKey')}
                </Stack>
              </Box>
              <Box sx={cardSx}>
                {sectionTitle('检索')}
                <Stack spacing={1.5}>
                  {switchField('启用多路召回', 'multiPathRetrievalEnabled')}
                  {numField('Rerank TopK', 'rerankTopK', { inputProps: { min: 1 } })}
                  {numField('检索超时（秒）', 'retrievalTimeoutSeconds', { inputProps: { min: 1 } })}
                  {numField('去重前缀长度', 'deduplicatePrefixLength', { inputProps: { min: 0 } })}
                </Stack>
              </Box>
              <Box sx={cardSx}>
                {sectionTitle('链路追踪')}
                <Stack spacing={1.5}>
                  {switchField('启用追踪', 'traceEnabled')}
                  {numField('采样率', 'traceSampleRate', { inputProps: { step: 0.1, min: 0, max: 1 } })}
                </Stack>
              </Box>
            </Stack>
          </Grid>
          <Grid item xs={12} md={6}>
            <Stack spacing={2}>
              <Box sx={cardSx}>
                {sectionTitle('工具调用')}
                <Stack spacing={1.5}>
                  {numField('自动执行置信度阈值', 'toolAutoExecuteThreshold', { inputProps: { step: 0.1, min: 0, max: 1 } })}
                  {switchField('启用语义工具检索', 'toolSemanticRetrievalEnabled')}
                  {numField('召回工具数量 (topK)', 'toolRetrievalTopK', { inputProps: { step: 1, min: 1, max: 50 } })}
                  {numField('最低相似度阈值', 'toolRetrievalThreshold', { inputProps: { step: 0.05, min: 0, max: 1 } })}
                  {switchField('启用工具降级', 'toolFallbackEnabled')}
                </Stack>
              </Box>
              <Box sx={cardSx}>
                {sectionTitle('辅助任务')}
                <Stack spacing={1.5}>
                  <Box>
                    <Typography sx={{ fontSize: 13, color: di.lightGray, mb: 0.5 }}>辅助 Provider</Typography>
                    <FormControl size="small" fullWidth>
                      <Select
                        value={form.auxiliaryProviderId ?? ''}
                        onChange={(e) => set('auxiliaryProviderId', e.target.value === '' ? null : Number(e.target.value))}
                      >
                        <MenuItem value="">跟随对话 Provider</MenuItem>
                        {providers?.filter((p) => p.enabled).map((p) => (
                          <MenuItem key={p.id} value={p.id}>{p.name} ({p.defaultModel})</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Box>
                  <Box>
                    <Typography sx={{ fontSize: 13, color: di.lightGray, mb: 0.5 }}>推荐问题提示词</Typography>
                    <TextField
                      multiline rows={5} size="small" fullWidth
                      value={form.suggestionPrompt}
                      onChange={(e) => set('suggestionPrompt', e.target.value)}
                      helperText="使用 {conversation} 作为对话上下文占位符"
                    />
                  </Box>
                </Stack>
              </Box>
            </Stack>
          </Grid>
        </Grid>
      )}

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
