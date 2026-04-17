import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Drawer, List, ListItemButton, ListItemIcon, ListItemText,
  AppBar, Toolbar, Typography, IconButton, useTheme, useMediaQuery,
} from '@mui/material';
import {
  Chat as ChatIcon, Storage as StorageIcon, Hub as HubIcon,
  Settings as SettingsIcon, Menu as MenuIcon,
  DarkMode, LightMode,
} from '@mui/icons-material';
import { useThemeStore } from '../stores/themeStore';

const DRAWER_WIDTH = 240;

const navItems = [
  { path: '/chat', label: 'AI 对话', icon: <ChatIcon /> },
  { path: '/knowledge', label: '知识库', icon: <StorageIcon /> },
  { path: '/mcp', label: 'MCP 网关', icon: <HubIcon /> },
  { path: '/settings', label: '供应商管理', icon: <SettingsIcon /> },
];

export default function Layout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [open, setOpen] = useState(!isMobile);
  const navigate = useNavigate();
  const location = useLocation();
  const toggleTheme = useThemeStore((s) => s.toggle);
  const mode = useThemeStore((s) => s.mode);

  const drawer = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar sx={{ justifyContent: 'center' }}>
        <Typography variant="h6" fontWeight={700} color="primary">
          AI RAG Platform
        </Typography>
      </Toolbar>
      <List sx={{ flex: 1, px: 1 }}>
        {navItems.map((item) => (
          <ListItemButton
            key={item.path}
            selected={location.pathname === item.path}
            onClick={() => { navigate(item.path); isMobile && setOpen(false); }}
            sx={{ borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {isMobile ? (
        <Drawer open={open} onClose={() => setOpen(false)} sx={{ '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}>
          {drawer}
        </Drawer>
      ) : (
        <Drawer variant="permanent" sx={{
          width: DRAWER_WIDTH, flexShrink: 0,
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, borderRight: '1px solid', borderColor: 'divider' },
        }}>
          {drawer}
        </Drawer>
      )}

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <AppBar position="sticky" color="default" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Toolbar>
            {isMobile && (
              <IconButton edge="start" onClick={() => setOpen(true)} sx={{ mr: 1 }}>
                <MenuIcon />
              </IconButton>
            )}
            <Typography variant="h6" sx={{ flex: 1 }}>
              {navItems.find((n) => n.path === location.pathname)?.label}
            </Typography>
            <IconButton onClick={toggleTheme}>
              {mode === 'dark' ? <LightMode /> : <DarkMode />}
            </IconButton>
          </Toolbar>
        </AppBar>
        <Box sx={{ flex: 1, overflow: 'auto', p: { xs: 1, md: 3 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
