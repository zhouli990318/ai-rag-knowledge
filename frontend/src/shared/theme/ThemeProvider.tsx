import { createTheme, ThemeProvider as MuiThemeProvider, CssBaseline } from '@mui/material';
import { ReactNode, useEffect, useMemo } from 'react';
import { useThemeStore } from '../stores/themeStore';

/* ── 水墨色彩体系 3.0 (中国水墨画 × 现代 AI) ── */
export const ink = {
  black: '#2D2D2D',       // 主文字色
  gray: '#4A4A4A',        // 墨灰 (主色)
  lightGray: '#6B6560',   // 次要文字 (提升对比度)
  cream: '#F7F4EF',       // 页面底色 — 暖米白
  border: '#E0DCD5',      // 边框/分割线
  cinnabar: '#C45C5C',    // 印章红
  teal: '#5A9E6B',        // 青墨 → 状态绿
  success: '#5A9E6B',
  warning: '#C89B3C',
  error: '#C45C5C',

  /* 毛玻璃背景色 */
  glassBg: '#f7f2e6',
  glassBorder: 'rgba(224,220,213,0.6)',

  /* 扩展色值 */
  muted: '#B0ADA6',            // 极淡墨 — 时间戳、省略号
  placeholder: '#C0BCB4',      // 水渍灰 — placeholder 文字
  disabledText: '#C8C4BE',     // 禁用文字
  disabledNav: '#C8C8C8',      // 禁用导航
  kbIconBg: '#F0EBE5',         // 知识库图标背景
  cinnabarLight: '#D47070',    // 浅印章红
  sidebarBg: '#E8E2D8',        // 导航栏水墨画底色

  /* 新增 Token */
  cardBg: '#f7f2e6',           // 卡片/气泡背景
  inkAreaBg: '#E8E2D8',        // 水墨画区域底色
  hoverBg: '#EAE5DD',          // 列表项悬停背景
  statusGray: '#9E9A95',       // 状态灰标签
  sendBtnBg: '#3D3D3D',        // 发送按钮背景
  navIcon: '#404040',          // 导航图标色 (提升对比度)
  navText: '#3D3D3D',          // 导航文字色 (提升对比度)
  tagline: '#A09A94',          // 标语色

  /* 标准状态色体系 */
  statusSuccess: '#2C6E49',
  statusSuccessBg: 'rgba(44,110,73,0.1)',
  statusWarning: '#C89B3C',
  statusWarningBg: 'rgba(200,155,60,0.1)',
  statusError: '#C84B31',
  statusErrorBg: 'rgba(200,75,49,0.1)',
  statusInfo: '#4A7FB5',
  statusInfoBg: 'rgba(74,127,181,0.1)',
};

/* ── 圆角 Token ── */
export const radius = {
  xs: 2,    // 印章、极小元素
  sm: 4,    // 按钮、输入框、Badge
  md: 6,    // 卡片、对话框
  lg: 8,    // 输入框容器、大面板
  pill: 9999, // 标签、推荐问题气泡
};

/* ── 模糊 Token ── */
export const blur = {
  sm: 'blur(8px)',    // 轻度（内嵌小元素）
  md: 'blur(12px)',   // 标准（卡片、面板）
  lg: 'blur(20px)',   // 强度（对话框、浮层）
};

/* ── 暗色模式色彩 ── */
export const inkDark = {
  black: '#E8E4DF',
  gray: '#C0BCB4',
  lightGray: '#8A8580',
  cream: '#1A1A1A',
  border: '#3A3836',
  cinnabar: '#C45C5C',
  teal: '#5A9E6B',
  success: '#5A9E6B',
  warning: '#C89B3C',
  error: '#C45C5C',
  glassBg: 'rgba(42,40,38,0.85)',
  glassBorder: 'rgba(58,56,54,0.6)',
  muted: '#706D68',
  placeholder: '#5A5856',
  disabledText: '#4A4846',
  disabledNav: '#4A4A4A',
  kbIconBg: '#2A2826',
  cinnabarLight: '#D47070',
  sidebarBg: '#242220',
  cardBg: '#2A2826',
  inkAreaBg: '#242220',
  hoverBg: '#333130',
  statusGray: '#706D68',
  sendBtnBg: '#555250',
  navIcon: '#8A8580',
  navText: '#8A8580',
  tagline: '#5A5856',

  /* 标准状态色体系 (暗色) */
  statusSuccess: '#5A9E6B',
  statusSuccessBg: 'rgba(90,158,107,0.15)',
  statusWarning: '#D4A94C',
  statusWarningBg: 'rgba(212,169,76,0.15)',
  statusError: '#D47070',
  statusErrorBg: 'rgba(212,112,112,0.15)',
  statusInfo: '#6A9FD5',
  statusInfoBg: 'rgba(106,159,213,0.15)',
};

export const serifFont = '"Noto Serif SC", "Source Han Serif SC", "SimSun", serif';
export const sansFont = '"Noto Sans SC", "Source Han Sans SC", "Microsoft YaHei", sans-serif';

/* ── 获取当前模式对应的 ink Token ── */
export function getInk(mode: 'light' | 'dark') {
  return mode === 'dark' ? { ...ink, ...inkDark } : ink;
}

/** 响应式 ink hook — 组件内使用以获取随 mode 切换的色彩 */
export function useInk() {
  const mode = useThemeStore((s) => s.mode);
  return getInk(mode);
}

export default function ThemeProvider({ children }: { children: ReactNode }) {
  const mode = useThemeStore((s) => s.mode);
  const i = getInk(mode);

  /* 将 data-theme 同步到 <html> — 让 CSS 变量跟随切换 */
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', mode);
  }, [mode]);

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: { main: i.gray, dark: i.black, light: i.lightGray },
          secondary: { main: i.cinnabar },
          success: { main: i.success },
          warning: { main: i.warning },
          error: { main: i.error },
          info: { main: i.teal },
          background: {
            default: 'transparent',
            paper: i.glassBg,
          },
          divider: mode === 'dark' ? 'rgba(255,255,255,0.08)' : 'rgba(74,74,74,0.12)',
          text: { primary: i.black, secondary: i.lightGray },
        },
        typography: {
          fontFamily: sansFont,
          h1: { fontFamily: serifFont, fontWeight: 900, fontSize: 40, letterSpacing: 3 },
          h2: { fontFamily: serifFont, fontWeight: 700, fontSize: 34, letterSpacing: 2 },
          h3: { fontFamily: serifFont, fontWeight: 700, fontSize: 28, letterSpacing: 1.5 },
          h4: { fontFamily: serifFont, fontWeight: 700, fontSize: 24, letterSpacing: 1 },
          h5: { fontFamily: serifFont, fontWeight: 600, fontSize: 20, letterSpacing: 0.5 },
          h6: { fontFamily: serifFont, fontWeight: 600, fontSize: 18, letterSpacing: 0.3 },
          subtitle1: { fontFamily: sansFont, fontSize: 16, fontWeight: 500, letterSpacing: 0.1 },
          subtitle2: { fontFamily: sansFont, fontSize: 14, fontWeight: 500 },
          body1: { fontFamily: sansFont, fontSize: 15, fontWeight: 400, lineHeight: 1.7 },
          body2: { fontFamily: sansFont, fontSize: 14, fontWeight: 400, lineHeight: 1.6 },
          caption: { fontFamily: sansFont, fontSize: 12, fontWeight: 400, color: ink.lightGray },
          overline: { fontFamily: sansFont, fontSize: 11, fontWeight: 500, letterSpacing: 1, textTransform: 'uppercase' },
          button: { fontFamily: sansFont, textTransform: 'none', fontWeight: 500, fontSize: 15 },
        },
        shape: { borderRadius: radius.md },
        transitions: {
          easing: {
            easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
            easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
            easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
            sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
          },
          duration: { shortest: 150, shorter: 200, short: 250, standard: 300, complex: 375, enteringScreen: 300, leavingScreen: 200 },
        },
        components: {
          MuiCssBaseline: {
            styleOverrides: {
              body: {
                WebkitFontSmoothing: 'antialiased',
                MozOsxFontSmoothing: 'grayscale',
                backgroundColor: 'transparent',
              },
              '*': { WebkitTapHighlightColor: 'transparent' },
              '::-webkit-scrollbar': { width: 5, height: 5 },
              '::-webkit-scrollbar-track': { background: 'transparent' },
              '::-webkit-scrollbar-thumb': {
                background: mode === 'dark' ? 'rgba(255,255,255,0.12)' : 'rgba(74,74,74,0.15)',
                borderRadius: 3,
              },
              '::-webkit-scrollbar-thumb:hover': {
                background: mode === 'dark' ? 'rgba(255,255,255,0.22)' : 'rgba(74,74,74,0.28)',
              },
            },
          },
          MuiButton: {
            styleOverrides: {
              root: {
                borderRadius: 4, padding: '8px 20px', boxShadow: 'none',
                transition: 'all 200ms cubic-bezier(0.25,0.46,0.45,0.94)',
                '&:hover': { boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
                '&:active': { transform: 'scale(0.97)' },
              },
              contained: {
                backgroundColor: i.gray, color: '#FFFFFF', fontWeight: 500,
                backgroundImage: mode === 'dark'
                  ? 'linear-gradient(135deg, #555250, #444240)'
                  : 'linear-gradient(135deg, #4A4A4A, #3A3A3A)',
                '&:hover': { backgroundImage: `linear-gradient(135deg, ${i.cinnabar}, ${i.cinnabarLight})` },
              },
              outlined: {
                borderColor: i.glassBorder, color: i.gray,
                backgroundColor: i.glassBg,
                backdropFilter: 'blur(8px)',
                '&:hover': {
                  borderColor: i.cinnabar, color: i.cinnabar,
                  backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(255,255,255,0.9)',
                },
              },
              text: {
                color: i.gray,
                '&:hover': {
                  color: i.cinnabar,
                  backgroundColor: mode === 'dark' ? 'rgba(196,92,92,0.1)' : 'rgba(196,92,92,0.04)',
                },
              },
            },
          },
          MuiIconButton: {
            styleOverrides: {
              root: {
                transition: 'all 180ms ease-in-out', color: i.gray,
                '&:hover': {
                  color: i.cinnabar,
                  backgroundColor: mode === 'dark' ? 'rgba(196,92,92,0.12)' : 'rgba(196,92,92,0.06)',
                },
                '&:active': { transform: 'scale(0.92)' },
              },
            },
          },
          MuiCard: {
            styleOverrides: {
              root: {
                backgroundImage: 'none',
                backgroundColor: i.glassBg,
                backdropFilter: 'blur(12px) saturate(180%)',
                WebkitBackdropFilter: 'blur(12px) saturate(180%)',
                border: `1px solid ${i.glassBorder}`,
                borderRadius: 4,
                transition: 'all 300ms cubic-bezier(0.25,0.46,0.45,0.94)',
                boxShadow: 'none',
                '&:hover': {
                  borderColor: mode === 'dark' ? 'rgba(255,255,255,0.12)' : 'rgba(224,221,216,0.95)',
                  boxShadow: mode === 'dark' ? '0 4px 20px rgba(0,0,0,0.2)' : '0 4px 20px rgba(0,0,0,0.06)',
                },
              },
            },
          },
          MuiPaper: {
            styleOverrides: {
              root: { backgroundImage: 'none', borderRadius: 4 },
              elevation1: {
                boxShadow: mode === 'dark' ? '0 1px 4px rgba(0,0,0,0.2)' : '0 1px 4px rgba(0,0,0,0.04)',
              },
            },
          },
          MuiDialog: {
            styleOverrides: {
              paper: {
                borderRadius: 16, backgroundImage: 'none',
                backgroundColor: mode === 'dark' ? 'rgba(42,40,38,0.96)' : '#f7f2e6',
                backdropFilter: 'blur(24px) saturate(180%)',
                WebkitBackdropFilter: 'blur(24px) saturate(180%)',
                border: `1px solid ${mode === 'dark' ? 'rgba(58,56,54,0.5)' : 'rgba(224,221,216,0.5)'}`,
                boxShadow: mode === 'dark'
                  ? '0 8px 32px rgba(0,0,0,0.4), 0 0 0 100vmax rgba(0,0,0,0.55)'
                  : '0 8px 32px rgba(0,0,0,0.08), 0 0 0 100vmax rgba(245,243,238,0.55)',
              },
            },
          },
          MuiDialogTitle: {
            styleOverrides: {
              root: {
                fontFamily: serifFont, fontSize: 18, fontWeight: 600,
                textAlign: 'center', color: i.black,
                padding: '20px 24px 14px',
                borderBottom: `1px solid ${mode === 'dark' ? 'rgba(58,56,54,0.35)' : 'rgba(224,221,216,0.35)'}`,
                letterSpacing: 0.5,
              },
            },
          },
          MuiDialogContent: {
            styleOverrides: {
              root: {
                padding: '16px 24px 16px',
                '& .MuiTextField-root': {
                  '& .MuiOutlinedInput-root': {
                    '&.Mui-focused': {
                      borderColor: i.cinnabar,
                      boxShadow: `0 0 0 2px ${mode === 'dark' ? 'rgba(196,92,92,0.15)' : 'rgba(200,75,49,0.08)'}`,
                    },
                  },
                  '& .MuiInputLabel-root.Mui-focused': { color: i.cinnabar },
                },
              },
            },
          },
          MuiDialogActions: {
            styleOverrides: {
              root: {
                padding: '12px 24px 20px', gap: 8,
                borderTop: `1px solid ${mode === 'dark' ? 'rgba(58,56,54,0.35)' : 'rgba(224,221,216,0.35)'}`,
              },
            },
          },
          MuiDrawer: {
            styleOverrides: {
              paper: {
                borderRight: 'none',
                backgroundColor: mode === 'dark' ? 'rgba(36,34,32,0.95)' : 'rgba(250,248,245,0.92)',
                backdropFilter: 'blur(16px)',
                WebkitBackdropFilter: 'blur(16px)',
                borderLeft: `1px solid ${i.glassBorder}`,
              },
            },
          },
          MuiAppBar: {
            styleOverrides: {
              root: {
                backgroundColor: mode === 'dark' ? 'rgba(26,26,26,0.85)' : 'rgba(250,248,245,0.85)',
                backdropFilter: 'blur(12px) saturate(160%)',
                WebkitBackdropFilter: 'blur(12px) saturate(160%)',
                borderBottom: `1px solid ${i.glassBorder}`,
                boxShadow: 'none', color: i.black,
              },
            },
          },
          MuiTextField: {
            styleOverrides: {
              root: {
                '& .MuiOutlinedInput-root': {
                  borderRadius: 4,
                  backgroundColor: mode === 'dark' ? i.glassBg : '#f7f2e6',
                  border: `1px solid ${i.glassBorder}`,
                  '& fieldset': { border: 'none' },
                  '&:hover': {
                    borderColor: i.border,
                    backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.05)' : '#f7f2e6',
                  },
                  '&.Mui-focused': {
                    borderColor: i.gray,
                    backgroundColor: mode === 'dark' ? 'rgba(42,40,38,1)' : '#FAF6EC',
                    boxShadow: `0 0 0 2px ${mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(74,74,74,0.08)'}`,
                  },
                },
              },
            },
          },
          MuiSelect: {
            styleOverrides: {
              root: { borderRadius: 4 },
            },
          },
          MuiOutlinedInput: {
            styleOverrides: {
              root: {
                borderRadius: 4,
                backgroundColor: mode === 'dark' ? i.glassBg : '#f7f2e6',
                border: `1px solid ${i.glassBorder}`,
                '& fieldset': { border: 'none' },
                '&:hover': {
                  borderColor: i.border,
                  backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.05)' : '#f7f2e6',
                },
                '&.Mui-focused': {
                  borderColor: i.gray,
                  backgroundColor: mode === 'dark' ? 'rgba(42,40,38,1)' : '#FAF6EC',
                  boxShadow: `0 0 0 2px ${mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(74,74,74,0.08)'}`,
                },
              },
            },
          },
          MuiChip: {
            styleOverrides: {
              root: { borderRadius: 4, fontWeight: 500, fontSize: 12 },
              filled: { backgroundColor: i.teal, color: '#FFFFFF' },
              sizeSmall: { height: 22, fontSize: 11 },
            },
          },
          MuiSwitch: {
            styleOverrides: {
              root: {
                width: 44, height: 24, padding: 0,
                '& .MuiSwitch-switchBase': {
                  padding: 2,
                  '&.Mui-checked': {
                    transform: 'translateX(20px)',
                    '& + .MuiSwitch-track': { backgroundColor: i.cinnabar, opacity: 1 },
                  },
                },
                '& .MuiSwitch-thumb': { width: 20, height: 20, boxShadow: '0 1px 3px rgba(0,0,0,0.15)' },
                '& .MuiSwitch-track': {
                  borderRadius: 8,
                  backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.12)' : 'rgba(74,74,74,0.15)',
                  opacity: 1,
                },
              },
            },
          },
          MuiListItemButton: {
            styleOverrides: {
              root: {
                borderRadius: 4, transition: 'all 200ms ease-in-out',
                '&.Mui-selected': {
                  backgroundColor: mode === 'dark' ? 'rgba(196,92,92,0.12)' : 'rgba(196,92,92,0.05)',
                  color: i.cinnabar,
                },
                '&:hover': {
                  backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.04)' : 'rgba(74,74,74,0.03)',
                },
                '&:active': {
                  backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.08)' : 'rgba(74,74,74,0.06)',
                },
              },
            },
          },
          MuiTable: { styleOverrides: { root: { borderCollapse: 'separate', borderSpacing: 0 } } },
          MuiTableCell: {
            styleOverrides: {
              root: {
                borderBottom: `1px solid ${i.glassBorder}`, padding: '12px 16px', fontSize: 14,
              },
              head: {
                fontWeight: 600, fontSize: 12, fontFamily: sansFont,
                color: i.lightGray, textTransform: 'uppercase', letterSpacing: 0.8,
                backgroundColor: mode === 'dark' ? 'rgba(42,40,38,0.5)' : 'rgba(245,243,238,0.5)',
              },
            },
          },
          MuiLinearProgress: {
            styleOverrides: {
              root: {
                borderRadius: 4, height: 3,
                backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(74,74,74,0.06)',
              },
            },
          },
          MuiTabs: {
            styleOverrides: {
              root: { minHeight: 36, borderBottom: `1px solid ${i.glassBorder}` },
              indicator: { backgroundColor: i.cinnabar, height: 2, borderRadius: 1 },
            },
          },
          MuiTab: {
            styleOverrides: {
              root: {
                minHeight: 36, textTransform: 'none', fontSize: 14, fontWeight: 500,
                fontFamily: sansFont, padding: '8px 16px',
                transition: 'all 200ms ease-in-out', color: i.lightGray,
                '&.Mui-selected': { color: i.cinnabar },
                '&:hover': { color: i.gray },
              },
            },
          },
          MuiSkeleton: {
            styleOverrides: {
              root: {
                borderRadius: 4,
                backgroundColor: mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(74,74,74,0.04)',
              },
            },
          },
          MuiFab: {
            styleOverrides: {
              root: {
                backgroundColor: i.gray, color: '#FFFFFF',
                backgroundImage: mode === 'dark'
                  ? 'linear-gradient(135deg, #555250, #444240)'
                  : 'linear-gradient(135deg, #4A4A4A, #3A3A3A)',
                boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
                transition: 'all 200ms ease-in-out',
                '&:hover': { backgroundImage: `linear-gradient(135deg, ${i.cinnabar}, ${i.cinnabarLight})`, boxShadow: '0 4px 20px rgba(196,92,92,0.3)' },
              },
            },
          },
          /* 毛玻璃 Tooltip */
          MuiTooltip: {
            styleOverrides: {
              tooltip: {
                backgroundColor: mode === 'dark' ? 'rgba(60,58,56,0.95)' : 'rgba(44,44,44,0.88)',
                backdropFilter: 'blur(8px)',
                borderRadius: 4,
                fontSize: 13,
              },
            },
          },
          /* 毛玻璃 Menu/Popover */
          MuiMenu: {
            styleOverrides: {
              paper: {
                backgroundColor: i.glassBg,
                backdropFilter: 'blur(16px) saturate(180%)',
                WebkitBackdropFilter: 'blur(16px) saturate(180%)',
                border: `1px solid ${i.glassBorder}`,
                borderRadius: 4,
                marginTop: 1,
              },
            },
          },
          MuiPopper: {
            styleOverrides: {
              root: {},
            },
          },
        },
      }),
    [mode, i],
  );

  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </MuiThemeProvider>
  );
}
