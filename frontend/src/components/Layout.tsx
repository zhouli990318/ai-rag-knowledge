import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Typography, IconButton, useTheme, useMediaQuery,
  BottomNavigation, BottomNavigationAction,
} from '@mui/material';
import {
  Chat as ChatIcon, Storage as StorageIcon, Hub as HubIcon,
  Settings as SettingsIcon, DarkMode, LightMode,
} from '@mui/icons-material';
import { useThemeStore } from '../stores/themeStore';
import { motion, AnimatePresence } from 'framer-motion';

const navItems = [
  { label: '对话', path: '/chat', icon: <ChatIcon /> },
  { label: '知识库', path: '/knowledge', icon: <StorageIcon /> },
  { label: 'MCP', path: '/mcp', icon: <HubIcon /> },
  { label: '设置', path: '/settings', icon: <SettingsIcon /> },
];

export default function Layout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();
  const { mode, toggle: toggleMode } = useThemeStore();
  const [hovered, setHovered] = useState(false);

  const currentIndex = navItems.findIndex((n) => location.pathname.startsWith(n.path));

  if (isMobile) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100dvh' }}>
        <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0 }}>
          <Outlet />
        </Box>
        <BottomNavigation
          value={currentIndex}
          onChange={(_, v) => navigate(navItems[v].path)}
          showLabels
          sx={{
            borderTop: `0.5px solid ${theme.palette.divider}`,
            bgcolor: theme.palette.mode === 'dark' ? 'rgba(0,0,0,0.72)' : 'rgba(249,249,249,0.94)',
            backdropFilter: 'saturate(180%) blur(20px)',
            WebkitBackdropFilter: 'saturate(180%) blur(20px)',
            height: 83, pb: 'env(safe-area-inset-bottom)',
          }}
        >
          {navItems.map((n) => (
            <BottomNavigationAction key={n.path} label={n.label} icon={n.icon} />
          ))}
        </BottomNavigation>
      </Box>
    );
  }

  // Desktop: macOS-style sidebar
  const sidebarWidth = hovered ? 220 : 72;

  return (
    <Box sx={{ display: 'flex', height: '100dvh' }}>
      <motion.div
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        animate={{ width: sidebarWidth }}
        transition={{ type: 'spring', stiffness: 400, damping: 30 }}
        style={{
          flexShrink: 0, overflow: 'hidden',
          borderRight: `0.5px solid ${theme.palette.divider}`,
          background: theme.palette.mode === 'dark' ? 'rgba(28,28,30,0.85)' : 'rgba(242,242,247,0.85)',
          backdropFilter: 'blur(40px)',
          display: 'flex', flexDirection: 'column',
        }}
      >
        <Box sx={{ pt: 3, pb: 2, px: hovered ? 2 : 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <AnimatePresence mode="wait">
            {hovered ? (
              <motion.div key="title" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <Typography variant="h6" noWrap sx={{ fontWeight: 700 }}>AI RAG</Typography>
              </motion.div>
            ) : (
              <motion.div key="dot" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: 'primary.main' }} />
              </motion.div>
            )}
          </AnimatePresence>
        </Box>

        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 0.5, px: 1, pt: 1 }}>
          {navItems.map((n, i) => {
            const active = currentIndex === i;
            return (
              <Box
                key={n.path}
                onClick={() => navigate(n.path)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: hovered ? 1.5 : 0, py: 1.2,
                  justifyContent: hovered ? 'flex-start' : 'center',
                  borderRadius: 2.5, cursor: 'pointer',
                  position: 'relative',
                  bgcolor: active
                    ? theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(0,122,255,0.08)'
                    : 'transparent',
                  color: active ? 'primary.main' : 'text.secondary',
                  transition: 'all 200ms cubic-bezier(0.25,0.46,0.45,0.94)',
                  '&:hover': {
                    bgcolor: active ? undefined : theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
                  },
                }}
              >
                {active && (
                  <Box sx={{
                    position: 'absolute', left: -4, top: '50%', transform: 'translateY(-50%)',
                    width: 3, height: 20, borderRadius: 1.5, bgcolor: 'primary.main',
                  }} />
                )}
                {n.icon}
                <AnimatePresence>
                  {hovered && (
                    <motion.span
                      initial={{ opacity: 0, width: 0 }}
                      animate={{ opacity: 1, width: 'auto' }}
                      exit={{ opacity: 0, width: 0 }}
                      style={{ whiteSpace: 'nowrap', fontSize: 15, fontWeight: active ? 600 : 400 }}
                    >
                      {n.label}
                    </motion.span>
                  )}
                </AnimatePresence>
              </Box>
            );
          })}
        </Box>

        <Box sx={{ p: 1, display: 'flex', justifyContent: 'center' }}>
          <IconButton onClick={toggleMode} size="small" sx={{ color: 'text.secondary' }}>
            {mode === 'dark' ? <LightMode fontSize="small" /> : <DarkMode fontSize="small" />}
          </IconButton>
        </Box>
      </motion.div>

      <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0 }}>
        <Outlet />
      </Box>
    </Box>
  );
}
