import { createTheme, ThemeProvider as MuiThemeProvider, CssBaseline } from '@mui/material';
import { ReactNode, useMemo } from 'react';

/* ── 水墨色彩体系 2.0 (统一 Token) ── */
export const ink = {
  black: '#2C2C2C',       // 水墨黑
  gray: '#4A4A4A',        // 墨灰 (主色)
  lightGray: '#8B8B8B',   // 淡墨
  cream: '#F5F3EE',       // 米白 (背景)
  border: '#E0DDD8',      // 淡墨边框
  cinnabar: '#C84B31',    // 朱砂红 (强调)
  teal: '#5B7065',        // 青墨 (辅助)
  success: '#5B7065',
  warning: '#C89B3C',
  error: '#C84B31',

  /* 毛玻璃背景色 */
  glassBg: 'rgba(255,255,255,0.72)',
  glassBorder: 'rgba(224,221,216,0.6)',

  /* 扩展色值 — 收拢页面中的"野生颜色" */
  muted: '#B0ADA6',            // 极淡墨 — 时间戳、省略号
  placeholder: '#C0BCB4',      // 水渍灰 — placeholder 文字
  disabledText: '#C8C4BE',     // 禁用文字
  disabledNav: '#C8C8C8',      // 禁用导航
  kbIconBg: '#F0EBE5',         // 知识库图标背景
  cinnabarLight: '#D46A4F',    // 浅朱砂 — 小印章边框
  sidebarBg: 'rgba(238,234,226,0.85)',  // 左侧宣纸底色（比中间浓 ~10%）
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

export const serifFont = '"Noto Serif SC", "Source Han Serif SC", "SimSun", serif';
export const sansFont = '"Noto Sans SC", "Source Han Sans SC", "Microsoft YaHei", sans-serif';

export default function ThemeProvider({ children }: { children: ReactNode }) {
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: 'light',
          primary: { main: ink.gray, dark: ink.black, light: ink.lightGray },
          secondary: { main: ink.cinnabar },
          success: { main: ink.success },
          warning: { main: ink.warning },
          error: { main: ink.error },
          info: { main: ink.teal },
          background: {
            default: 'transparent',
            paper: ink.glassBg,
          },
          divider: 'rgba(74,74,74,0.12)',
          text: { primary: ink.black, secondary: ink.lightGray },
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
              '::-webkit-scrollbar-thumb': { background: 'rgba(74,74,74,0.15)', borderRadius: 3 },
              '::-webkit-scrollbar-thumb:hover': { background: 'rgba(74,74,74,0.28)' },
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
                backgroundColor: ink.gray, color: '#FFFFFF', fontWeight: 500,
                backgroundImage: 'linear-gradient(135deg, #4A4A4A, #3A3A3A)',
                '&:hover': { backgroundImage: 'linear-gradient(135deg, #C84B31, #A83D27)' },
              },
              outlined: {
                borderColor: ink.glassBorder, color: ink.gray,
                backgroundColor: ink.glassBg,
                backdropFilter: 'blur(8px)',
                '&:hover': { borderColor: ink.cinnabar, color: ink.cinnabar, backgroundColor: 'rgba(255,255,255,0.9)' },
              },
              text: { color: ink.gray, '&:hover': { color: ink.cinnabar, backgroundColor: 'rgba(200,75,49,0.04)' } },
            },
          },
          MuiIconButton: {
            styleOverrides: {
              root: {
                transition: 'all 180ms ease-in-out', color: ink.gray,
                '&:hover': { color: ink.cinnabar, backgroundColor: 'rgba(200,75,49,0.06)' },
                '&:active': { transform: 'scale(0.92)' },
              },
            },
          },
          MuiCard: {
            styleOverrides: {
              root: {
                backgroundImage: 'none',
                backgroundColor: ink.glassBg,
                backdropFilter: 'blur(12px) saturate(180%)',
                WebkitBackdropFilter: 'blur(12px) saturate(180%)',
                border: `1px solid ${ink.glassBorder}`,
                borderRadius: 4,
                transition: 'all 300ms cubic-bezier(0.25,0.46,0.45,0.94)',
                boxShadow: 'none',
                '&:hover': { borderColor: 'rgba(224,221,216,0.95)', boxShadow: '0 4px 20px rgba(0,0,0,0.06)' },
              },
            },
          },
          MuiPaper: {
            styleOverrides: {
              root: { backgroundImage: 'none', borderRadius: 4 },
              elevation1: {
                boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
              },
            },
          },
          MuiDialog: {
            styleOverrides: {
              paper: {
                borderRadius: 16, backgroundImage: 'none',
                backgroundColor: 'rgba(252,250,247,0.95)',
                backdropFilter: 'blur(24px) saturate(180%)',
                WebkitBackdropFilter: 'blur(24px) saturate(180%)',
                border: `1px solid rgba(224,221,216,0.5)`,
                boxShadow: '0 8px 32px rgba(0,0,0,0.08), 0 0 0 100vmax rgba(245,243,238,0.55)',
              },
            },
          },
          MuiDialogTitle: {
            styleOverrides: {
              root: {
                fontFamily: serifFont, fontSize: 18, fontWeight: 600,
                textAlign: 'center', color: ink.black,
                padding: '20px 24px 14px',
                borderBottom: '1px solid rgba(224,221,216,0.35)',
                letterSpacing: 0.5,
              },
            },
          },
          MuiDialogContent: {
            styleOverrides: {
              root: {
                padding: '20px 24px 16px',
                '& .MuiTextField-root': {
                  '& .MuiOutlinedInput-root': {
                    '&.Mui-focused': {
                      borderColor: ink.cinnabar,
                      boxShadow: `0 0 0 2px rgba(200,75,49,0.08)`,
                    },
                  },
                  '& .MuiInputLabel-root.Mui-focused': { color: ink.cinnabar },
                },
              },
            },
          },
          MuiDialogActions: {
            styleOverrides: {
              root: {
                padding: '12px 24px 20px', gap: 8,
                borderTop: '1px solid rgba(224,221,216,0.35)',
              },
            },
          },
          MuiDrawer: {
            styleOverrides: {
              paper: {
                borderRight: 'none',
                backgroundColor: 'rgba(250,248,245,0.92)',
                backdropFilter: 'blur(16px)',
                WebkitBackdropFilter: 'blur(16px)',
                borderLeft: `1px solid ${ink.glassBorder}`,
              },
            },
          },
          MuiAppBar: {
            styleOverrides: {
              root: {
                backgroundColor: 'rgba(250,248,245,0.85)',
                backdropFilter: 'blur(12px) saturate(160%)',
                WebkitBackdropFilter: 'blur(12px) saturate(160%)',
                borderBottom: `1px solid ${ink.glassBorder}`,
                boxShadow: 'none', color: ink.black,
              },
            },
          },
          MuiTextField: {
            styleOverrides: {
              root: {
                '& .MuiOutlinedInput-root': {
                  borderRadius: 4,
                  backgroundColor: ink.glassBg,
                  border: `1px solid ${ink.glassBorder}`,
                  '& fieldset': { border: 'none' },
                  '&:hover': { borderColor: ink.border, backgroundColor: 'rgba(255,255,255,0.85)' },
                  '&.Mui-focused': {
                    borderColor: ink.gray,
                    backgroundColor: '#FFFFFF',
                    boxShadow: `0 0 0 2px rgba(74,74,74,0.08)`,
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
                backgroundColor: ink.glassBg,
                border: `1px solid ${ink.glassBorder}`,
                '& fieldset': { border: 'none' },
                '&:hover': { borderColor: ink.border },
                '&.Mui-focused': { borderColor: ink.gray, boxShadow: `0 0 0 2px rgba(74,74,74,0.08)` },
              },
            },
          },
          MuiChip: {
            styleOverrides: {
              root: { borderRadius: 4, fontWeight: 500, fontSize: 12 },
              filled: { backgroundColor: ink.teal, color: '#FFFFFF' },
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
                    '& + .MuiSwitch-track': { backgroundColor: ink.cinnabar, opacity: 1 },
                  },
                },
                '& .MuiSwitch-thumb': { width: 20, height: 20, boxShadow: '0 1px 3px rgba(0,0,0,0.15)' },
                '& .MuiSwitch-track': {
                  borderRadius: 8, backgroundColor: 'rgba(74,74,74,0.15)', opacity: 1,
                },
              },
            },
          },
          MuiListItemButton: {
            styleOverrides: {
              root: {
                borderRadius: 4, transition: 'all 200ms ease-in-out',
                '&.Mui-selected': { backgroundColor: 'rgba(200,75,49,0.05)', color: ink.cinnabar },
                '&:hover': { backgroundColor: 'rgba(74,74,74,0.03)' },
                '&:active': { backgroundColor: 'rgba(74,74,74,0.06)' },
              },
            },
          },
          MuiTable: { styleOverrides: { root: { borderCollapse: 'separate', borderSpacing: 0 } } },
          MuiTableCell: {
            styleOverrides: {
              root: {
                borderBottom: `1px solid ${ink.glassBorder}`, padding: '12px 16px', fontSize: 14,
              },
              head: {
                fontWeight: 600, fontSize: 12, fontFamily: sansFont,
                color: ink.lightGray, textTransform: 'uppercase', letterSpacing: 0.8,
                backgroundColor: 'rgba(245,243,238,0.5)',
              },
            },
          },
          MuiLinearProgress: {
            styleOverrides: {
              root: { borderRadius: 4, height: 3, backgroundColor: 'rgba(74,74,74,0.06)' },
            },
          },
          MuiTabs: {
            styleOverrides: {
              root: { minHeight: 36, borderBottom: `1px solid ${ink.glassBorder}` },
              indicator: { backgroundColor: ink.cinnabar, height: 2, borderRadius: 1 },
            },
          },
          MuiTab: {
            styleOverrides: {
              root: {
                minHeight: 36, textTransform: 'none', fontSize: 14, fontWeight: 500,
                fontFamily: sansFont, padding: '8px 16px',
                transition: 'all 200ms ease-in-out', color: ink.lightGray,
                '&.Mui-selected': { color: ink.cinnabar },
                '&:hover': { color: ink.gray },
              },
            },
          },
          MuiSkeleton: {
            styleOverrides: {
              root: { borderRadius: 4, backgroundColor: 'rgba(74,74,74,0.04)' },
            },
          },
          MuiFab: {
            styleOverrides: {
              root: {
                backgroundColor: ink.gray, color: '#FFFFFF',
                backgroundImage: 'linear-gradient(135deg, #4A4A4A, #3A3A3A)',
                boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
                transition: 'all 200ms ease-in-out',
                '&:hover': { backgroundImage: 'linear-gradient(135deg, #C84B31, #A83D27)', boxShadow: '0 4px 20px rgba(200,75,49,0.3)' },
              },
            },
          },
          /* 毛玻璃 Tooltip */
          MuiTooltip: {
            styleOverrides: {
              tooltip: {
                backgroundColor: 'rgba(44,44,44,0.88)',
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
                backgroundColor: ink.glassBg,
                backdropFilter: 'blur(16px) saturate(180%)',
                WebkitBackdropFilter: 'blur(16px) saturate(180%)',
                border: `1px solid ${ink.glassBorder}`,
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
    [],
  );

  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </MuiThemeProvider>
  );
}
