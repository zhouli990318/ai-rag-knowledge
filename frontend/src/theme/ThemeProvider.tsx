import { createTheme, ThemeProvider as MuiThemeProvider, CssBaseline } from '@mui/material';
import { ReactNode, useMemo } from 'react';
import { useThemeStore } from '../stores/themeStore';

const ios = {
  blue: '#007AFF',
  green: '#34C759',
  indigo: '#5856D6',
  orange: '#FF9500',
  red: '#FF3B30',
  teal: '#5AC8FA',
};

const iosEasing = 'cubic-bezier(0.25, 0.46, 0.45, 0.94)';

export default function ThemeProvider({ children }: { children: ReactNode }) {
  const mode = useThemeStore((s) => s.mode);
  const isDark = mode === 'dark';

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: { main: ios.blue },
          secondary: { main: ios.indigo },
          success: { main: ios.green },
          warning: { main: ios.orange },
          error: { main: ios.red },
          info: { main: ios.teal },
          background: isDark
            ? { default: '#000000', paper: '#1C1C1E' }
            : { default: '#F2F2F7', paper: '#FFFFFF' },
          divider: isDark ? 'rgba(255,255,255,0.15)' : 'rgba(60,60,67,0.12)',
          text: isDark
            ? { primary: '#FFFFFF', secondary: 'rgba(235,235,245,0.6)' }
            : { primary: '#000000', secondary: 'rgba(60,60,67,0.6)' },
        },
        typography: {
          fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", "SF Pro Display", "Helvetica Neue", "Segoe UI", Roboto, sans-serif',
          h4: { fontSize: 34, fontWeight: 700, letterSpacing: 0.37, lineHeight: 1.2 },
          h5: { fontSize: 28, fontWeight: 700, letterSpacing: 0.36, lineHeight: 1.25 },
          h6: { fontSize: 22, fontWeight: 700, letterSpacing: 0.35, lineHeight: 1.3 },
          subtitle1: { fontSize: 17, fontWeight: 600, letterSpacing: -0.41 },
          subtitle2: { fontSize: 15, fontWeight: 600, letterSpacing: -0.24 },
          body1: { fontSize: 17, fontWeight: 400, letterSpacing: -0.41 },
          body2: { fontSize: 15, fontWeight: 400, letterSpacing: -0.24 },
          caption: { fontSize: 13, fontWeight: 400, letterSpacing: -0.08 },
          overline: { fontSize: 11, fontWeight: 400, letterSpacing: 0.07, textTransform: 'uppercase' },
          button: { textTransform: 'none', fontWeight: 600, fontSize: 17, letterSpacing: -0.41 },
        },
        shape: { borderRadius: 8 },
        transitions: {
          easing: { easeInOut: iosEasing, easeOut: iosEasing, easeIn: iosEasing, sharp: iosEasing },
          duration: { shortest: 150, shorter: 200, short: 250, standard: 300, complex: 375, enteringScreen: 300, leavingScreen: 250 },
        },
        components: {
          MuiCssBaseline: {
            styleOverrides: {
              body: { WebkitFontSmoothing: 'antialiased', MozOsxFontSmoothing: 'grayscale' },
              '*': { WebkitTapHighlightColor: 'transparent' },
              '::-webkit-scrollbar': { width: 6, height: 6 },
              '::-webkit-scrollbar-track': { background: 'transparent' },
              '::-webkit-scrollbar-thumb': { background: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)', borderRadius: 3 },
            },
          },
          MuiButton: {
            styleOverrides: {
              root: {
                borderRadius: 8, padding: '10px 20px', boxShadow: 'none',
                transition: `all 200ms ${iosEasing}`,
                '&:hover': { boxShadow: 'none' },
                '&:active': { transform: 'scale(0.97)', opacity: 0.85 },
              },
              contained: { fontWeight: 600 },
              text: { color: ios.blue },
            },
          },
          MuiIconButton: {
            styleOverrides: {
              root: { transition: `all 150ms ${iosEasing}`, '&:active': { transform: 'scale(0.9)' } },
            },
          },
          MuiCard: {
            styleOverrides: {
              root: {
                backgroundImage: 'none',
                boxShadow: isDark ? '0 2px 16px rgba(0,0,0,0.4)' : '0 1px 10px rgba(0,0,0,0.06)',
                borderRadius: 12, transition: `all 300ms ${iosEasing}`,
              },
            },
          },
          MuiPaper: { styleOverrides: { root: { backgroundImage: 'none', borderRadius: 12 } } },
          MuiDialog: {
            styleOverrides: {
              paper: {
                borderRadius: 14, backgroundImage: 'none',
                backgroundColor: isDark ? '#2C2C2E' : '#FFFFFF',
                boxShadow: isDark ? '0 20px 60px rgba(0,0,0,0.6)' : '0 20px 60px rgba(0,0,0,0.15)',
              },
            },
          },
          MuiDialogTitle: { styleOverrides: { root: { fontSize: 17, fontWeight: 600, textAlign: 'center', padding: '16px 24px 8px' } } },
          MuiDialogContent: { styleOverrides: { root: { padding: '16px 24px' } } },
          MuiDialogActions: { styleOverrides: { root: { padding: '8px 24px 16px', gap: 8 } } },
          MuiDrawer: {
            styleOverrides: {
              paper: {
                borderRight: 'none',
                backgroundColor: isDark ? 'rgba(28,28,30,0.85)' : 'rgba(242,242,247,0.85)',
                backdropFilter: 'blur(40px)', WebkitBackdropFilter: 'blur(40px)',
              },
            },
          },
          MuiAppBar: {
            styleOverrides: {
              root: {
                backgroundColor: isDark ? 'rgba(0,0,0,0.72)' : 'rgba(249,249,249,0.94)',
                backdropFilter: 'saturate(180%) blur(20px)', WebkitBackdropFilter: 'saturate(180%) blur(20px)',
                borderBottom: `0.5px solid ${isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.12)'}`,
                boxShadow: 'none', color: isDark ? '#FFFFFF' : '#000000',
              },
            },
          },
          MuiTextField: {
            styleOverrides: {
              root: {
                '& .MuiOutlinedInput-root': {
                  borderRadius: 8,
                  backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)',
                  '& fieldset': { borderColor: 'transparent' },
                  '&:hover fieldset': { borderColor: 'transparent' },
                  '&.Mui-focused fieldset': { borderColor: ios.blue, borderWidth: 2 },
                },
              },
            },
          },
          MuiSelect: { styleOverrides: { root: { borderRadius: 8 } } },
          MuiOutlinedInput: {
            styleOverrides: {
              root: {
                borderRadius: 8,
                backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)',
                '& fieldset': { borderColor: 'transparent' },
                '&:hover fieldset': { borderColor: 'transparent' },
                '&.Mui-focused fieldset': { borderColor: ios.blue, borderWidth: 2 },
              },
            },
          },
          MuiChip: {
            styleOverrides: {
              root: { borderRadius: 12, fontWeight: 500, fontSize: 13 },
              sizeSmall: { height: 24, fontSize: 12 },
            },
          },
          MuiSwitch: {
            styleOverrides: {
              root: {
                width: 51, height: 31, padding: 0,
                '& .MuiSwitch-switchBase': {
                  padding: 2,
                  '&.Mui-checked': {
                    transform: 'translateX(20px)',
                    '& + .MuiSwitch-track': { backgroundColor: ios.green, opacity: 1 },
                  },
                },
                '& .MuiSwitch-thumb': { width: 27, height: 27, boxShadow: '0 2px 4px rgba(0,0,0,0.2)' },
                '& .MuiSwitch-track': {
                  borderRadius: 31 / 2,
                  backgroundColor: isDark ? 'rgba(120,120,128,0.32)' : 'rgba(120,120,128,0.16)',
                  opacity: 1,
                },
              },
            },
          },
          MuiListItemButton: {
            styleOverrides: {
              root: {
                borderRadius: 6, transition: `all 200ms ${iosEasing}`,
                '&.Mui-selected': { backgroundColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,122,255,0.08)' },
                '&:active': { backgroundColor: isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.06)' },
              },
            },
          },
          MuiTable: { styleOverrides: { root: { borderCollapse: 'separate', borderSpacing: 0 } } },
          MuiTableCell: {
            styleOverrides: {
              root: {
                borderBottom: `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'}`,
                padding: '12px 16px', fontSize: 15,
              },
              head: {
                fontWeight: 600, fontSize: 13,
                color: isDark ? 'rgba(235,235,245,0.6)' : 'rgba(60,60,67,0.6)',
                textTransform: 'uppercase', letterSpacing: 0.5,
              },
            },
          },
          MuiLinearProgress: {
            styleOverrides: { root: { borderRadius: 4, height: 3, backgroundColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' } },
          },
          MuiTabs: {
            styleOverrides: {
              root: {
                minHeight: 32,
                backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)',
                borderRadius: 6, padding: 2,
              },
              indicator: { display: 'none' },
            },
          },
          MuiTab: {
            styleOverrides: {
              root: {
                minHeight: 28, borderRadius: 5, textTransform: 'none',
                fontSize: 13, fontWeight: 600, padding: '4px 12px',
                transition: `all 200ms ${iosEasing}`,
                '&.Mui-selected': {
                  backgroundColor: isDark ? 'rgba(255,255,255,0.18)' : '#FFFFFF',
                  boxShadow: isDark ? 'none' : '0 1px 3px rgba(0,0,0,0.08)',
                  color: isDark ? '#FFFFFF' : '#000000',
                },
              },
            },
          },
          MuiSkeleton: {
            styleOverrides: { root: { borderRadius: 8, backgroundColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' } },
          },
        },
      }),
    [mode, isDark],
  );

  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </MuiThemeProvider>
  );
}
