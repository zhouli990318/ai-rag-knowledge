import { createTheme, ThemeProvider as MuiThemeProvider, CssBaseline } from '@mui/material';
import { ReactNode, useMemo } from 'react';
import { useThemeStore } from '../stores/themeStore';

export default function ThemeProvider({ children }: { children: ReactNode }) {
  const mode = useThemeStore((s) => s.mode);

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: { main: '#6366f1' },
          secondary: { main: '#ec4899' },
          background: mode === 'dark'
            ? { default: '#0f172a', paper: '#1e293b' }
            : { default: '#f8fafc', paper: '#ffffff' },
        },
        typography: {
          fontFamily: '"Inter", "Noto Sans SC", sans-serif',
        },
        shape: { borderRadius: 12 },
        components: {
          MuiButton: { styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } } },
          MuiCard: { styleOverrides: { root: { backgroundImage: 'none' } } },
        },
      }),
    [mode],
  );

  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </MuiThemeProvider>
  );
}
