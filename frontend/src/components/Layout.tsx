import { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Box, Typography, useMediaQuery, useTheme,
  BottomNavigation, BottomNavigationAction,
  Drawer, Fab, Avatar,
  IconButton, Tooltip, Collapse,
} from '@mui/material';
import {
  ChatOutlined as ChatIcon,
  StorageOutlined as StorageIcon,
  HubOutlined as HubIcon,
  SettingsOutlined as SettingsIcon,
  TuneRounded as TuneIcon,
  ModelTrainingOutlined as ModelIcon,
  AccountTreeOutlined as WorkflowIcon,
  AppsOutlined as AppIcon,
  DatasetOutlined as DatasetIcon,
  NotificationsNoneOutlined as BellIcon,
  DescriptionOutlined as DocCenterIcon,
  TimelineOutlined as TimelineIcon,
  CloudUploadOutlined as IngestIcon,
  LightModeOutlined as LightModeIcon,
  DarkModeOutlined as DarkModeIcon,
  ExpandMoreOutlined as ExpandMoreIcon,
  VisibilityOffOutlined as ImmersiveIcon,
  VisibilityOutlined as ImmersiveOffIcon,
  CloseOutlined as CloseIcon,
} from '@mui/icons-material';
import { InkLogo } from '@/shared/ui/ink';
import { InkBreadcrumb } from '@/shared/ui/ink';
import { ChatConfig } from '@/widgets/chat';
import { ink, radius, serifFont, sansFont, getInk, useInk } from '@/shared/theme/ThemeProvider';
import { useThemeStore } from '@/shared/stores/themeStore';
import sidebarInkPainting from '@/shared/assets/images/sidebar-ink-painting.webp';
import mainInkLandscape from '@/shared/assets/images/main-ink-landscape.webp';

/* ── 导航菜单配置 ── */
const primaryNav = [
  { label: '对话', path: '/chat', icon: <ChatIcon /> },
  { label: '知识库', path: '/knowledge', icon: <StorageIcon /> },
  { label: 'MCP 服务', path: '/mcp', icon: <HubIcon /> },
  { label: '模型管理', path: '/settings', icon: <ModelIcon /> },
  { label: '系统设置', path: '/system-settings', icon: <SettingsIcon /> },
];
const secondaryNav = [
  { label: '意图决策', path: '/intent-tree', icon: <WorkflowIcon /> },
  { label: '文墨入库', path: '/ingest-monitor', icon: <IngestIcon /> },
  { label: '墨迹溯源', path: '/traces', icon: <TimelineIcon /> },
];
const allNav = [...primaryNav, ...secondaryNav];

/* ── 面包屑路由映射 ── */
const breadcrumbMap: Record<string, { label: string; parent?: { label: string; path: string } }> = {
  '/chat': { label: '对话' },
  '/knowledge': { label: '知识库' },
  '/mcp': { label: 'MCP 服务' },
  '/settings': { label: '模型管理' },
  '/system-settings': { label: '系统设置' },
  '/intent-tree': { label: '意图决策', parent: { label: '更多工具', path: '/chat' } },
  '/ingest-monitor': { label: '文墨入库', parent: { label: '更多工具', path: '/chat' } },
  '/traces': { label: '墨迹溯源', parent: { label: '更多工具', path: '/chat' } },
};

const SIDEBAR_WIDTH = 228;
const PANEL_WIDTH = 300;

/* ── 中间+右侧 水墨山峰背景（真实图片） ── */
function MountainBackground({ mode, hidden }: { mode: 'light' | 'dark'; hidden?: boolean }) {
  if (hidden) return null;
  return (
    <Box
      sx={{
        position: 'absolute', top: 0, left: 0, right: 0,
        width: '100%', height: '100%',
        pointerEvents: 'none', zIndex: 0,
        overflow: 'hidden',
      }}
    >
      <Box
        component="img"
        src={mainInkLandscape}
        alt=""
        sx={{
          position: 'absolute',
          bottom: 0, left: 0,
          width: '100%', maxHeight: '40vh',
          objectFit: 'cover', objectPosition: 'center bottom',
          display: 'block',
          opacity: mode === 'dark' ? 0.08 : 0.88,
          filter: mode === 'dark' ? 'brightness(0.5) contrast(1.3) invert(0.85)' : 'sepia(0.25) saturate(0.8) brightness(1.05)',
          transition: 'opacity 0.4s ease, filter 0.4s ease',
          mask: 'linear-gradient(to top, rgba(0,0,0,1) 0%, rgba(0,0,0,0.6) 50%, rgba(0,0,0,0) 100%)',
          WebkitMask: 'linear-gradient(to top, rgba(0,0,0,1) 0%, rgba(0,0,0,0.6) 50%, rgba(0,0,0,0) 100%)',
        }}
      />
    </Box>
  );
}

export default function Layout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const [configOpen, setConfigOpen] = useState(false);
  const themeMode = useThemeStore((s) => s.mode);
  const toggleMode = useThemeStore((s) => s.toggleMode);
  const immersive = useThemeStore((s) => s.immersive);
  const toggleImmersive = useThemeStore((s) => s.toggleImmersive);
  const di = useInk(); // dark-aware ink tokens

  /* 同步 data-immersive 到 <html> */
  useEffect(() => {
    document.documentElement.setAttribute('data-immersive', String(immersive));
  }, [immersive]);

  const currentIndex = allNav.findIndex((n) => location.pathname.startsWith(n.path));
  const isChat = location.pathname.startsWith('/chat');

  /* ── 移动端布局 ── */
  if (isMobile) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100dvh' }}>
        <Box sx={{
          height: 52, flexShrink: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 2,
          backgroundColor: themeMode === 'dark' ? 'rgba(26,26,26,0.75)' : 'rgba(250,248,245,0.75)',
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          borderBottom: `1px solid ${themeMode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(224,221,216,0.5)'}`,
        }}>
          <InkLogo size="sm" />
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <IconButton size="small" color="inherit" sx={{ color: di.gray }}>
              <BellIcon fontSize="small" />
            </IconButton>
            <Avatar sx={{ width: 28, height: 28, fontSize: 13, bgcolor: di.gray }}>墨</Avatar>
          </Box>
        </Box>

        <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0 }}>
          <Outlet />
        </Box>

        {isChat && (
          <Fab
            size="small"
            onClick={() => setDrawerOpen(true)}
            sx={{
              position: 'fixed', right: 16, bottom: 100, zIndex: 1200,
              bgcolor: di.gray, color: '#FFF',
              '&:hover': { bgcolor: di.cinnabar },
            }}
          >
            <TuneIcon fontSize="small" />
          </Fab>
        )}

        <Drawer
          anchor="right"
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          PaperProps={{
            sx: { width: 300, bgcolor: themeMode === 'dark' ? 'rgba(30,30,30,0.95)' : 'rgba(250,248,245,0.95)', backdropFilter: 'blur(20px)' },
          }}
        >
          <Box sx={{ p: 2, pt: 3 }}>
            <Typography variant="h6" sx={{ fontFamily: serifFont, mb: 2 }}>对话配置</Typography>
            <ChatConfig />
          </Box>
        </Drawer>

        <BottomNavigation
          value={(() => {
            const mobileNav = primaryNav.slice(0, 4);
            return mobileNav.findIndex((n) => location.pathname.startsWith(n.path));
          })()}
          onChange={(_, v) => navigate(primaryNav[v].path)}
          showLabels
          sx={{
            borderTop: `1px solid ${themeMode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(224,221,216,0.6)'}`,
            height: 68, pb: 'env(safe-area-inset-bottom)',
            backgroundColor: themeMode === 'dark' ? 'rgba(26,26,26,0.85)' : 'rgba(250,248,245,0.85)',
            backdropFilter: 'blur(12px)',
            '& .Mui-selected': { color: `${di.cinnabar} !important` },
            '& .MuiBottomNavigationAction-root': { color: di.lightGray },
          }}
        >
          {primaryNav.slice(0, 4).map((n) => (
            <BottomNavigationAction key={n.path} label={n.label} icon={n.icon} />
          ))}
        </BottomNavigation>
      </Box>
    );
  }

  /* ── 桌面端三栏布局 ── */
  return (
    <Box sx={{ display: 'flex', height: '100dvh', position: 'relative', overflow: 'hidden' }}>

      {/* ═══════ 左侧导航栏 ═══════ */}
      <Box
        sx={{
          width: SIDEBAR_WIDTH, flexShrink: 0, overflow: 'hidden',
          borderRight: `1px solid ${di.border}`,
          backgroundColor: di.sidebarBg,
          display: 'flex', flexDirection: 'column',
          position: 'relative',
        }}
      >
        {/* 竖版水墨山水画背景 — 真实图片 */}
        {!immersive && (
        <Box sx={{
          position: 'absolute', inset: 0, zIndex: 0,
          overflow: 'hidden',
        }}>
          <Box
            component="img"
            src={sidebarInkPainting}
            alt=""
            sx={{
              width: '210%', height: '100%',
              objectFit: 'cover', objectPosition: 'center top',
              display: 'block',
              opacity: themeMode === 'dark' ? 1.0 : 0.56,
              filter: themeMode === 'dark' ? 'brightness(0.6) contrast(1.2)' : 'none',
              transition: 'opacity 0.4s ease, filter 0.4s ease',
            }}
          />
        </Box>
        )}

        {/* 半透明遮罩保证文字可读性 */}
        {!immersive && (
        <Box sx={{
          position: 'absolute', inset: 0, zIndex: 1,
          background: themeMode === 'dark'
            ? 'linear-gradient(180deg, rgba(36,34,32,0.94) 0%, rgba(36,34,32,0.88) 50%, rgba(36,34,32,0.82) 100%)'
            : 'linear-gradient(180deg, rgba(232,226,216,0.93) 0%, rgba(232,226,216,0.85) 50%, rgba(232,226,216,0.78) 100%)',
          pointerEvents: 'none',
        }} />
        )}

        {/* Logo 区域 */}
        <Box sx={{ pt: 3, pb: 2, px: 2.5, borderBottom: `1px solid ${di.border}`, position: 'relative', zIndex: 2 }}>
          <InkLogo size="lg" showTagline />
        </Box>

        {/* 导航菜单 */}
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', px: 1.5, pt: 1.5, overflow: 'auto', zIndex: 2 }}>
          {/* 主导航 — 5 项 */}
          {primaryNav.map((n) => {
            const active = location.pathname.startsWith(n.path);
            return (
              <Box
                key={n.label}
                onClick={() => navigate(n.path)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: 1.5, py: 1.1,
                  mb: '4%',
                  borderRadius: radius.sm, cursor: 'pointer',
                  position: 'relative',
                  bgcolor: 'transparent',
                  color: active ? di.black : di.navText,
                  transition: 'all 200ms ease-in-out',
                  '&:hover': {
                      bgcolor: 'rgba(0,0,0,0.03)',
                      color: active ? di.black : di.gray,
                    },
                }}
              >
                {active && (
                  <Box sx={{
                    position: 'absolute', left: -14, top: '20%', height: '60%',
                    width: 3, borderRadius: 1.5,
                    backgroundColor: di.black,
                  }} />
                )}
                <Box sx={{ fontSize: '1.25rem', display: 'flex', alignItems: 'center', color: active ? di.black : di.navIcon, '& svg': { strokeWidth: 1.5 } }}>{n.icon}</Box>
                <span style={{
                  whiteSpace: 'nowrap', fontSize: '0.7rem',
                  fontWeight: active ? 600 : 400,
                  fontFamily: sansFont,
                }}>
                  {n.label}
                </span>
              </Box>
            );
          })}

          {/* 分隔线 + "更多工具" 折叠区 */}
          <Box sx={{ mx: 1, my: 0.5, borderTop: `1px solid ${di.border}`, opacity: 0.5 }} />
          <Box
            onClick={() => setMoreOpen(!moreOpen)}
            sx={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              px: 1.5, py: 0.7,
              borderRadius: radius.sm, cursor: 'pointer',
              color: di.lightGray,
              transition: 'all 200ms ease-in-out',
              '&:hover': { bgcolor: 'rgba(0,0,0,0.03)', color: di.gray },
            }}
          >
            <span style={{ fontSize: '0.625rem', fontFamily: sansFont, fontWeight: 500, letterSpacing: 1 }}>更多工具</span>
            <ExpandMoreIcon sx={{
              fontSize: 16,
              transition: 'transform 200ms ease-in-out',
              transform: moreOpen ? 'rotate(180deg)' : 'rotate(0deg)',
            }} />
          </Box>
          <Collapse in={moreOpen} timeout={250}>
            <Box sx={{ pb: 0.5 }}>
              {secondaryNav.map((n) => {
                const active = location.pathname.startsWith(n.path);
                return (
                  <Box
                    key={n.label}
                    onClick={() => navigate(n.path)}
                    sx={{
                      display: 'flex', alignItems: 'center', gap: 1.5,
                      px: 1.5, py: 0.85,
                      mb: '2%',
                      borderRadius: radius.sm, cursor: 'pointer',
                      position: 'relative',
                      bgcolor: 'transparent',
                      color: active ? di.black : di.navText,
                      transition: 'all 200ms ease-in-out',
                      opacity: active ? 1 : 0.8,
                      '&:hover': {
                        bgcolor: 'rgba(0,0,0,0.03)',
                        color: active ? di.black : di.gray,
                        opacity: 1,
                      },
                    }}
                  >
                    {active && (
                      <Box sx={{
                        position: 'absolute', left: -14, top: '20%', height: '60%',
                        width: 3, borderRadius: 1.5,
                        backgroundColor: di.black,
                      }} />
                    )}
                    <Box sx={{ fontSize: '1.1rem', display: 'flex', alignItems: 'center', color: active ? di.black : di.navIcon }}>{n.icon}</Box>
                    <span style={{
                      whiteSpace: 'nowrap', fontSize: '0.65rem',
                      fontWeight: active ? 600 : 400,
                      fontFamily: sansFont,
                    }}>
                      {n.label}
                    </span>
                  </Box>
                );
              })}
            </Box>
          </Collapse>
        </Box>

        {/* 底部：暗色切换 + 署名 */}
        <Box sx={{ position: 'relative', zIndex: 2, pb: 1.5, pt: 0.5, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Tooltip title={themeMode === 'dark' ? '切换亮色' : '切换暗色'} placement="right">
              <IconButton
                onClick={toggleMode}
                size="small"
                sx={{
                  color: di.navIcon,
                  '&:hover': { color: di.black, bgcolor: 'rgba(0,0,0,0.04)' },
                }}
              >
                {themeMode === 'dark' ? <LightModeIcon sx={{ fontSize: 18 }} /> : <DarkModeIcon sx={{ fontSize: 18 }} />}
              </IconButton>
            </Tooltip>
            <Tooltip title={immersive ? '退出沉浸模式' : '沉浸模式'} placement="right">
              <IconButton
                onClick={toggleImmersive}
                size="small"
                sx={{
                  color: immersive ? di.cinnabar : di.navIcon,
                  '&:hover': { color: di.black, bgcolor: 'rgba(0,0,0,0.04)' },
                }}
              >
                {immersive ? <ImmersiveOffIcon sx={{ fontSize: 18 }} /> : <ImmersiveIcon sx={{ fontSize: 18 }} />}
              </IconButton>
            </Tooltip>
          </Box>
          <Typography sx={{
            fontSize: 9.5, color: di.lightGray, letterSpacing: 2.5,
            fontFamily: serifFont, textAlign: 'center',
            textShadow: '0 0 8px rgba(232,226,216,0.9)',
          }}>
            墨语 · 水墨智境
          </Typography>
        </Box>
      </Box>

      {/* ═══════ 中间 + 右侧容器（共享背景） ═══════ */}
      <Box sx={{
        flex: 1, display: 'flex', position: 'relative', overflow: 'hidden',
        backgroundColor: immersive ? (themeMode === 'dark' ? '#1A1A1A' : '#f7f2e6') : (themeMode === 'dark' ? 'transparent' : '#f7f2e6'),
        transition: 'background-color 0.3s ease',
      }}>
        {/* 水墨山峰背景层 */}
        <MountainBackground mode={themeMode} hidden={immersive} />

        {/* 顶部信息栏 — 浮在最右上角 */}
        <Box sx={{
          position: 'absolute', top: 0, right: 0, zIndex: 20,
          display: 'flex', alignItems: 'center', gap: 1,
          px: 2, py: 1.2,
        }}>
          <Tooltip title="文档中心">
            <Box
              onClick={() => navigate('/knowledge')}
              sx={{
                display: 'flex', alignItems: 'center', gap: 0.5,
                px: 1.2, py: 0.4,
                borderRadius: radius.xs + 2,
                cursor: 'pointer',
                color: di.gray,
                fontSize: 13,
                transition: 'all 150ms ease-in-out',
                '&:hover': { backgroundColor: 'rgba(255,255,255,0.5)', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' },
              }}
            >
              <DocCenterIcon sx={{ fontSize: 17 }} />
              <span style={{ fontWeight: 500 }}>文档中心</span>
            </Box>
          </Tooltip>

          <Tooltip title="通知">
            <IconButton size="small"
              sx={{
                color: di.gray,
                p: 0.6,
                transition: 'all 150ms ease-in-out',
                '&:hover': { backgroundColor: 'rgba(255,255,255,0.5)' },
              }}
            >
              <BellIcon sx={{ fontSize: 19 }} />
            </IconButton>
          </Tooltip>

          <Avatar
            sx={{
              width: 30, height: 30,
              fontSize: 13,
              bgcolor: di.black,
              fontFamily: serifFont,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            墨
          </Avatar>

          {/* 对话配置按钮（仅 Chat 页面） */}
          {isChat && (
            <Tooltip title={configOpen ? '收起配置' : '对话配置'}>
              <IconButton
                size="small"
                onClick={() => setConfigOpen(!configOpen)}
                sx={{
                  color: configOpen ? di.cinnabar : di.gray,
                  p: 0.6,
                  transition: 'all 150ms ease-in-out',
                  '&:hover': { backgroundColor: 'rgba(255,255,255,0.5)' },
                }}
              >
                <TuneIcon sx={{ fontSize: 19 }} />
              </IconButton>
            </Tooltip>
          )}
        </Box>

        {/* 主内容区 */}
        <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0, position: 'relative', zIndex: 1 }}>
          {/* 面包屑导航 */}
          {(() => {
            const route = Object.keys(breadcrumbMap).find(k => location.pathname.startsWith(k));
            if (!route || route === '/chat') return null;
            const entry = breadcrumbMap[route];
            const items: Array<{ label: string; path?: string }> = [{ label: '首页', path: '/chat' }];
            if (entry.parent) items.push(entry.parent);
            items.push({ label: entry.label });
            return (
              <Box sx={{ px: { xs: 2, md: 2.5 }, pt: { xs: 7, md: 7 }, pb: 0 }}>
                <InkBreadcrumb items={items} />
              </Box>
            );
          })()}
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.15 }}
            style={{ display: 'contents' }}
          >
            <Outlet />
          </motion.div>
        </Box>

        {/* 右侧配置面板（仅对话页） */}
        <AnimatePresence initial={false}>
          {isChat && configOpen && (
            <motion.div
              key="config-panel"
              initial={{ width: 0, opacity: 0 }}
              animate={{ width: PANEL_WIDTH, opacity: 1 }}
              exit={{ width: 0, opacity: 0 }}
              transition={{ duration: 0.25, ease: [0.25, 0.46, 0.45, 0.94] }}
              style={{ overflow: 'hidden', flexShrink: 0, height: '100%', position: 'relative', zIndex: 1 }}
            >
              <Box sx={{
                width: PANEL_WIDTH,
                p: 1.5, pt: 7,
                height: '100%', overflow: 'auto',
              }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5, px: 0.5 }}>
                  <Typography variant="h6" sx={{ fontFamily: serifFont, fontSize: 16 }}>对话配置</Typography>
                  <IconButton size="small" onClick={() => setConfigOpen(false)} sx={{ color: di.lightGray }}>
                    <CloseIcon sx={{ fontSize: 18 }} />
                  </IconButton>
                </Box>
                <ChatConfig />
              </Box>
            </motion.div>
          )}
        </AnimatePresence>
      </Box>
    </Box>
  );
}
