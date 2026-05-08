/**
 * Shared style tokens used across page components.
 * Import these instead of re-declaring locally.
 */
export const glassCard = {
  borderRadius: '12px',
  backgroundColor: '#f7f2e6',
  backdropFilter: 'blur(12px)',
  WebkitBackdropFilter: 'blur(12px)',
  border: '1px solid rgba(224,221,216,0.5)',
  boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
} as const;

export const glassCardDark = {
  borderRadius: '12px',
  backgroundColor: 'rgba(42,40,38,0.85)',
  backdropFilter: 'blur(12px)',
  WebkitBackdropFilter: 'blur(12px)',
  border: '1px solid rgba(58,56,54,0.6)',
  boxShadow: '0 2px 12px rgba(0,0,0,0.15)',
} as const;

/** Get mode-aware glassCard */
export function getGlassCard(mode: 'light' | 'dark') {
  return mode === 'dark' ? glassCardDark : glassCard;
}

/* ── 字号规范 (Typography Scale) ── */
export const fontSize = {
  xs: 11,     // 最小标签、overline
  sm: 12,     // 时间戳、元数据、badge
  md: 13,     // 表头、说明文字
  base: 14,   // 正文、列表项
  lg: 15,     // 段落标题、强调文字
  xl: 17,     // 卡片标题
  '2xl': 20,  // 小标题
  '3xl': 24,  // 页面标题
} as const;
