/**
 * 业务语义色 — 集中管理各页面中的状态/阶段/路由等颜色映射
 * 使用 ink/inkDark tokens 作为基础色值
 */
import { ink, inkDark } from './ThemeProvider';

/* ── HTTP Method 色 (McpPage) ── */
export const methodColors: Record<string, string> = {
  GET: '#5B7065',
  POST: '#4A4A4A',
  PUT: '#C89B3C',
  DELETE: '#C84B31',
  PATCH: '#8B8B8B',
};

/* ── ETL Stage 色 (IngestMonitorPage) ── */
export const stageStyles: Record<string, { bg: string; color: string }> = {
  PENDING:    { bg: ink.statusInfoBg.replace('0.1', '0.08'), color: '#8B8B8B' },
  FETCH:      { bg: ink.statusInfoBg, color: ink.statusInfo },
  PARSE:      { bg: ink.statusInfoBg, color: ink.statusInfo },
  ENHANCE:    { bg: ink.statusInfoBg, color: ink.statusInfo },
  CHUNK:      { bg: ink.statusWarningBg, color: ink.statusWarning },
  VECTORIZE:  { bg: ink.statusWarningBg, color: ink.statusWarning },
  WRITE:      { bg: ink.statusSuccessBg, color: ink.statusSuccess },
  COMPLETED:  { bg: ink.statusSuccessBg, color: ink.statusSuccess },
  FAILED:     { bg: ink.statusErrorBg, color: ink.statusError },
};

/* ── Trace Stage 色 (TracePage) ── */
export function traceStageColor(stage: string, success: boolean): string {
  if (!success) return ink.statusError;
  const map: Record<string, string> = {
    REWRITE: ink.statusInfo,
    INTENT: ink.statusInfo,
    RETRIEVAL: ink.statusSuccess,
    RERANK: ink.statusInfo,
    TOOL: ink.statusWarning,
    GENERATION: ink.statusSuccess,
    PERSIST: '#8B8B8B',
  };
  return map[stage] || ink.gray;
}

/* ── 路由建议色 (IntentTreePage) ── */
export const routingColors: Record<string, { bg: string; color: string; border: string }> = {
  RETRIEVAL: { bg: 'rgba(91,112,101,0.1)', color: '#5B7065', border: '#5B7065' },
  TOOL:      { bg: 'rgba(139,105,20,0.1)', color: '#8B6914', border: '#8B6914' },
  DIRECT:    { bg: 'rgba(139,139,139,0.1)', color: '#8B8B8B', border: '#8B8B8B' },
  HYBRID:    { bg: 'rgba(74,111,165,0.1)', color: '#4A6FA5', border: '#4A6FA5' },
};

/* ── KB 卡片渐变色 (KnowledgePage) ── */
export const kbGradients = [
  `linear-gradient(135deg, ${ink.gray}, ${ink.black})`,
  `linear-gradient(135deg, ${ink.statusError}, #A03B26)`,
  `linear-gradient(135deg, #5B7065, #4A5D53)`,
  `linear-gradient(135deg, #8B8B8B, #6B6B6B)`,
  `linear-gradient(135deg, #5B7065, #3D4D43)`,
  `linear-gradient(135deg, ${ink.statusError}, #8B3522)`,
  `linear-gradient(135deg, ${ink.gray}, #6B6B6B)`,
];

/* ── Progress Bar 色 (IngestMonitorPage) ── */
export const progressBarColor = (stage: string): string => {
  if (stage === 'FAILED') return ink.statusError;
  if (stage === 'COMPLETED') return '#5B7065';
  return '#8B6914';
};

/* ── Provider 品牌色 (SettingsPage) — 第三方品牌，保留硬编码 ── */
export const providerColors: Record<string, string> = {
  OPENAI: '#10A37F',
  ANTHROPIC: '#D97757',
  DASHSCOPE: '#6366f1',
  OLLAMA: '#7C3AED',
  ZHIPU: '#2563EB',
};

/* ── 通用 UI 色值引用（避免在页面中直接写 hex） ── */
export const ui = {
  textPrimary: ink.black,          // #2D2D2D → #2C2C2C 近似
  textSecondary: ink.gray,         // #4A4A4A
  textMuted: '#8B8B8B',           // 弱文字 / 时间戳
  border: ink.border,              // #E0DCD5
  hoverBg: 'rgba(245,243,238,0.5)',
  cardBg: '#f7f2e6',
  tableHeaderColor: ink.gray,      // #4A4A4A
  tableBorder: 'rgba(224,221,216,0.6)',
  accentRed: ink.statusError,      // #C84B31
  accentRedHover: '#A83D27',
} as const;

export const uiDark = {
  textPrimary: inkDark.black,
  textSecondary: inkDark.gray,
  textMuted: '#706D68',
  border: inkDark.border,
  hoverBg: 'rgba(58,56,54,0.5)',
  cardBg: 'rgba(42,40,38,0.85)',
  tableHeaderColor: inkDark.gray,
  tableBorder: 'rgba(58,56,54,0.6)',
  accentRed: inkDark.statusError,
  accentRedHover: '#E08080',
} as const;

/** Hook-friendly: 根据 mode 获取 ui 色 */
export function getUi(mode: 'light' | 'dark') {
  return mode === 'dark' ? uiDark : ui;
}
