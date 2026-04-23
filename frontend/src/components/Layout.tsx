import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Typography, useMediaQuery, useTheme,
  BottomNavigation, BottomNavigationAction,
  Drawer, Fab, Avatar,
  IconButton, Tooltip,
} from '@mui/material';
import {
  Chat as ChatIcon, Storage as StorageIcon, Hub as HubIcon,
  Settings as SettingsIcon, TuneRounded as TuneIcon,
  ModelTraining as ModelIcon,
  AccountTree as WorkflowIcon,
  Apps as AppIcon,
  Dataset as DatasetIcon,
  NotificationsNone as BellIcon,
  Description as DocCenterIcon,
  Timeline as TimelineIcon,
  CloudUpload as IngestIcon,
} from '@mui/icons-material';
import { InkLogo } from './ink';
import ChatConfig from '../pages/chat/ChatConfig';
import { ink, radius, serifFont, sansFont } from '../theme/ThemeProvider';

/* ── 导航菜单配置 ── */
const navItems = [
  { label: '对话', path: '/chat', icon: <ChatIcon /> },
  { label: '知识库', path: '/knowledge', icon: <StorageIcon /> },
  { label: 'MCP 服务', path: '/mcp', icon: <HubIcon /> },
  { label: '模型管理', path: '/settings', icon: <ModelIcon /> },
  { label: '意图树', path: '/intent-tree', icon: <WorkflowIcon /> },
  { label: '入库监控', path: '/ingest-monitor', icon: <IngestIcon /> },
  { label: '链路追踪', path: '/traces', icon: <TimelineIcon /> },
  { label: '系统设置', path: '/system-settings', icon: <SettingsIcon /> },
];

const SIDEBAR_WIDTH = 228;
const PANEL_WIDTH = 300;

/* ── 中间+右侧 水墨山峰 SVG 背景 ── */
function MountainBackground() {
  return (
    <Box
      component="svg"
      viewBox="0 0 1400 480"
      preserveAspectRatio="xMidYMin slice"
      sx={{
        position: 'absolute', top: 0, left: 0, right: 0,
        width: '100%', height: 420,
        pointerEvents: 'none', zIndex: 0,
      }}
    >
      <defs>
        {/* ── 山体渐变（水墨浓淡） ── */}
        <linearGradient id="mt-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#E8E4DC" stopOpacity={0.06} />
          <stop offset="100%" stopColor="#F5F2ED" stopOpacity={0} />
        </linearGradient>
        <linearGradient id="mt-far" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#A09A8E" stopOpacity={0.18} />
          <stop offset="60%" stopColor="#C5C0B6" stopOpacity={0.10} />
          <stop offset="100%" stopColor="#E0DCD4" stopOpacity={0.02} />
        </linearGradient>
        <linearGradient id="mt-mid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#8A8478" stopOpacity={0.28} />
          <stop offset="50%" stopColor="#B0AAA0" stopOpacity={0.16} />
          <stop offset="100%" stopColor="#D5D0C8" stopOpacity={0.03} />
        </linearGradient>
        <linearGradient id="mt-near" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#6B665C" stopOpacity={0.35} />
          <stop offset="40%" stopColor="#9A9488" stopOpacity={0.20} />
          <stop offset="100%" stopColor="#C8C2B8" stopOpacity={0.04} />
        </linearGradient>
        <linearGradient id="mt-front" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#5A554C" stopOpacity={0.30} />
          <stop offset="50%" stopColor="#8A8478" stopOpacity={0.15} />
          <stop offset="100%" stopColor="#BAB5AA" stopOpacity={0.02} />
        </linearGradient>
        {/* 云雾水平渐变 */}
        <linearGradient id="mt-mist" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#E8E4DC" stopOpacity={0} />
          <stop offset="30%" stopColor="#E8E4DC" stopOpacity={0.35} />
          <stop offset="70%" stopColor="#E8E4DC" stopOpacity={0.35} />
          <stop offset="100%" stopColor="#E8E4DC" stopOpacity={0} />
        </linearGradient>
        <linearGradient id="mt-mist2" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#F0ECE4" stopOpacity={0} />
          <stop offset="20%" stopColor="#F0ECE4" stopOpacity={0.25} />
          <stop offset="80%" stopColor="#F0ECE4" stopOpacity={0.25} />
          <stop offset="100%" stopColor="#F0ECE4" stopOpacity={0} />
        </linearGradient>
        {/* 底部白色渐隐 */}
        <linearGradient id="mt-fade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity={0} />
          <stop offset="60%" stopColor="#FFFFFF" stopOpacity={0.4} />
          <stop offset="100%" stopColor="#FFFFFF" stopOpacity={0.95} />
        </linearGradient>
        {/* 墨迹晕染滤镜 */}
        <filter id="mt-blur-xs"><feGaussianBlur in="SourceGraphic" stdDeviation={1.5} /></filter>
        <filter id="mt-blur-sm"><feGaussianBlur in="SourceGraphic" stdDeviation={3} /></filter>
        <filter id="mt-blur-md"><feGaussianBlur in="SourceGraphic" stdDeviation={5} /></filter>
        <filter id="mt-blur-lg"><feGaussianBlur in="SourceGraphic" stdDeviation={8} /></filter>
        {/* 水墨纹理 */}
        <filter id="mt-ink-texture">
          <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves={4} seed={42} result="noise" />
          <feDisplacementMap in="SourceGraphic" in2="noise" scale={6} xChannelSelector="R" yChannelSelector="G" />
        </filter>
      </defs>

      {/* ━━━ 天空淡墨染 ━━━ */}
      <rect x="0" y="0" width="1400" height="200" fill="url(#mt-sky)" />

      {/* ━━━ 第1层：极远山 — 淡墨虚影 ━━━ */}
      <path
        d="M-80 280 Q80 80 200 200 Q320 50 480 180 Q600 30 760 160 Q880 60 1020 150 Q1140 40 1300 130 Q1380 90 1500 180 L1500 480 L-80 480 Z"
        fill="url(#mt-far)" filter="url(#mt-blur-lg)"
      />

      {/* ━━━ 第2层：远山 — 水墨浓度中等 ━━━ */}
      <path
        d="M-50 320 Q100 120 260 240 Q380 80 530 210 Q650 60 800 190 Q920 100 1060 200 Q1180 90 1320 175 Q1420 130 1500 210 L1500 480 L-50 480 Z"
        fill="url(#mt-mid)" filter="url(#mt-blur-md)"
      />
      {/* 远山山脊墨线 */}
      <path
        d="M-50 320 Q100 120 260 240 Q380 80 530 210 Q650 60 800 190 Q920 100 1060 200 Q1180 90 1320 175"
        fill="none" stroke="#8A8478" strokeWidth={0.8} opacity={0.12} filter="url(#mt-blur-xs)"
      />

      {/* ━━━ 云雾层1 — 山间留白 ━━━ */}
      <rect x="0" y="220" width="1400" height="50" fill="url(#mt-mist)" filter="url(#mt-blur-md)" />

      {/* ━━━ 第3层：中山 — 墨色渐浓 ━━━ */}
      <path
        d="M-30 370 Q120 180 300 300 Q440 130 600 270 Q740 150 900 260 Q1020 170 1150 255 Q1280 180 1430 270 L1500 480 L-30 480 Z"
        fill="url(#mt-near)" filter="url(#mt-blur-sm)"
      />
      {/* 中山水墨纹理叠加 */}
      <path
        d="M-30 370 Q120 180 300 300 Q440 130 600 270 Q740 150 900 260 Q1020 170 1150 255 Q1280 180 1430 270 L1500 480 L-30 480 Z"
        fill="url(#mt-near)" filter="url(#mt-ink-texture)" opacity={0.3}
      />

      {/* ━━━ 云雾层2 — 更低处 ━━━ */}
      <rect x="-100" y="310" width="1600" height="40" fill="url(#mt-mist2)" filter="url(#mt-blur-lg)" />

      {/* ━━━ 第4层：近山 — 最浓 ━━━ */}
      <path
        d="M0 410 Q160 250 350 350 Q500 200 680 320 Q820 240 980 330 Q1100 260 1250 340 Q1350 290 1500 360 L1500 480 L0 480 Z"
        fill="url(#mt-front)" filter="url(#mt-blur-xs)"
      />

      {/* ━━━ 飞鸟 ━━━ */}
      <g opacity={0.22} strokeLinecap="round">
        <path d="M420 95 Q426 86 434 92" fill="none" stroke="#5A554C" strokeWidth={1.2} />
        <path d="M434 92 Q440 86 448 95" fill="none" stroke="#5A554C" strokeWidth={1.2} />
        <path d="M480 72 Q485 65 491 70" fill="none" stroke="#6B665C" strokeWidth={1} />
        <path d="M491 70 Q496 65 502 73" fill="none" stroke="#6B665C" strokeWidth={1} />
        <path d="M390 118 Q395 112 400 116" fill="none" stroke="#7A756C" strokeWidth={0.8} />
        <path d="M400 116 Q404 112 408 118" fill="none" stroke="#7A756C" strokeWidth={0.8} />
      </g>

      {/* ━━━ 松树剪影 ━━━ */}
      <g opacity={0.14} fill="#5A554C">
        {/* 左侧松树 */}
        <line x1="180" y1="345" x2="180" y2="390" stroke="#5A554C" strokeWidth={2} />
        <ellipse cx="180" cy="340" rx="18" ry="10" />
        <ellipse cx="180" cy="330" rx="14" ry="8" />
        <ellipse cx="180" cy="322" rx="9" ry="6" />
        {/* 右侧松树组 */}
        <line x1="1050" y1="320" x2="1050" y2="365" stroke="#5A554C" strokeWidth={1.8} />
        <ellipse cx="1050" cy="316" rx="15" ry="9" />
        <ellipse cx="1050" cy="307" rx="11" ry="7" />
        <ellipse cx="1050" cy="300" rx="7" ry="5" />
        <line x1="1080" y1="335" x2="1080" y2="370" stroke="#5A554C" strokeWidth={1.5} />
        <ellipse cx="1080" cy="331" rx="12" ry="7" />
        <ellipse cx="1080" cy="324" rx="8" ry="5" />
      </g>

      {/* ━━━ 底部渐隐到白色 ━━━ */}
      <rect x="0" y="300" width="1400" height="180" fill="url(#mt-fade)" />
    </Box>
  );
}

export default function Layout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  const currentIndex = navItems.findIndex((n) => location.pathname.startsWith(n.path));
  const isChat = location.pathname.startsWith('/chat');

  /* ── 移动端布局 ── */
  if (isMobile) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100dvh' }}>
        <Box sx={{
          height: 52, flexShrink: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 2,
          backgroundColor: 'rgba(250,248,245,0.75)',
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          borderBottom: `1px solid rgba(224,221,216,0.5)`,
        }}>
          <InkLogo size="sm" />
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <IconButton size="small" color="inherit" sx={{ color: ink.gray }}>
              <BellIcon fontSize="small" />
            </IconButton>
            <Avatar sx={{ width: 28, height: 28, fontSize: 13, bgcolor: ink.gray }}>墨</Avatar>
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
              bgcolor: ink.gray, color: '#FFF',
              '&:hover': { bgcolor: ink.cinnabar },
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
            sx: { width: 300, bgcolor: 'rgba(250,248,245,0.95)', backdropFilter: 'blur(20px)' },
          }}
        >
          <Box sx={{ p: 2, pt: 3 }}>
            <Typography variant="h6" sx={{ fontFamily: serifFont, mb: 2 }}>对话配置</Typography>
            <ChatConfig />
          </Box>
        </Drawer>

        <BottomNavigation
          value={currentIndex >= 0 ? currentIndex : 0}
          onChange={(_, v) => navigate(navItems[v].path)}
          showLabels
          sx={{
            borderTop: `1px solid rgba(224,221,216,0.6)`,
            height: 68, pb: 'env(safe-area-inset-bottom)',
            backgroundColor: 'rgba(250,248,245,0.85)',
            backdropFilter: 'blur(12px)',
            '& .Mui-selected': { color: `${ink.cinnabar} !important` },
            '& .MuiBottomNavigationAction-root': { color: ink.lightGray },
          }}
        >
          {navItems.slice(0, 4).map((n) => (
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
          borderRight: `1px solid rgba(224,221,216,0.5)`,
          backgroundColor: ink.sidebarBg,
          display: 'flex', flexDirection: 'column',
          position: 'relative',
        }}
      >
        {/* Logo 区域 */}
        <Box sx={{ pt: 3, pb: 2, px: 2.5, borderBottom: '1px solid rgba(224,221,216,0.35)' }}>
          <InkLogo size="lg" />
          <Typography
            sx={{
              fontSize: 11, color: ink.lightGray, letterSpacing: 2,
              mt: 0.5, pl: 0.5,
              fontFamily: serifFont,
            }}
          >
            让知识流淌如墨
          </Typography>
        </Box>

        {/* 导航菜单 */}
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 0.25, px: 1.5, pt: 1.5, overflow: 'auto', zIndex: 1 }}>
          {navItems.map((n, i) => {
            const active = currentIndex === i;
            return (
              <Box
                key={n.label}
                onClick={() => navigate(n.path)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1.5,
                  px: 1.5, py: 1.1,
                  borderRadius: radius.sm, cursor: 'pointer',
                  position: 'relative',
                  bgcolor: active ? 'rgba(200,75,49,0.06)' : 'transparent',
                  color: active ? ink.cinnabar : ink.lightGray,
                  transition: 'all 200ms ease-in-out',
                  '&:hover': {
                      bgcolor: active ? 'rgba(200,75,49,0.08)' : 'rgba(74,74,74,0.04)',
                      color: active ? ink.cinnabar : ink.gray,
                    },
                }}
              >
                {active && (
                  <Box sx={{
                    position: 'absolute', left: -14, top: '50%', transform: 'translateY(-50%)',
                    width: 3, height: 18, borderRadius: 1.5,
                    background: `linear-gradient(180deg, ${ink.cinnabar} 0%, #E07860 100%)`,
                  }} />
                )}
                <Box sx={{ fontSize: 19, display: 'flex', alignItems: 'center' }}>{n.icon}</Box>
                <span style={{
                  whiteSpace: 'nowrap', fontSize: 13.5,
                  fontWeight: active ? 600 : 400,
                  fontFamily: sansFont,
                }}>
                  {n.label}
                </span>
              </Box>
            );
          })}
        </Box>

        {/* 底部水墨山水画（含亭子）+ 署名，融为一体 */}
        <Box sx={{ position: 'relative', px: 0, pb: 1.5, pt: 0, flexShrink: 0 }}>
          <Box
            component="svg"
            viewBox="0 0 228 160"
            preserveAspectRatio="xMidYMax meet"
            sx={{
              width: '100%', display: 'block',
              opacity: 0.32,
            }}
          >
            <defs>
              <linearGradient id="sb-far" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#8A8478" stopOpacity={0.3} />
                <stop offset="100%" stopColor="#C0BAB0" stopOpacity={0.05} />
              </linearGradient>
              <linearGradient id="sb-mid" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#6B665C" stopOpacity={0.45} />
                <stop offset="100%" stopColor="#A09A8E" stopOpacity={0.08} />
              </linearGradient>
              <linearGradient id="sb-near" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#5A554C" stopOpacity={0.5} />
                <stop offset="100%" stopColor="#8A8478" stopOpacity={0.1} />
              </linearGradient>
              <linearGradient id="sb-water" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#A09A8E" stopOpacity={0.06} />
                <stop offset="100%" stopColor="#E0DCD4" stopOpacity={0.02} />
              </linearGradient>
              <filter id="sb-blur"><feGaussianBlur in="SourceGraphic" stdDeviation={1.5} /></filter>
              <filter id="sb-blur-lg"><feGaussianBlur in="SourceGraphic" stdDeviation={3} /></filter>
            </defs>

            {/* 远山 */}
            <path d="M-10 100 Q25 40 60 75 Q95 20 140 60 Q170 15 200 50 Q220 35 240 65 L240 160 L-10 160 Z"
              fill="url(#sb-far)" filter="url(#sb-blur-lg)" />

            {/* 中山 */}
            <path d="M0 115 Q40 55 80 90 Q115 35 155 75 Q185 30 210 65 Q230 50 240 78 L240 160 L0 160 Z"
              fill="url(#sb-mid)" filter="url(#sb-blur)" />

            {/* 山脊线 */}
            <path d="M0 115 Q40 55 80 90 Q115 35 155 75 Q185 30 210 65"
              fill="none" stroke="#6B665C" strokeWidth={0.6} opacity={0.25} />

            {/* 近山 */}
            <path d="M-5 130 Q50 80 100 110 Q145 65 185 100 Q215 78 240 95 L240 160 L-5 160 Z"
              fill="url(#sb-near)" />

            {/* 亭子 — 居中偏右的山腰 */}
            <g opacity={0.55} fill="#5A554C">
              {/* 亭顶 — 飞檐 */}
              <polygon points="148,72 128,84 168,84" />
              <line x1="128" y1="84" x2="125" y2="83" stroke="#5A554C" strokeWidth={0.8} />
              <line x1="168" y1="84" x2="171" y2="83" stroke="#5A554C" strokeWidth={0.8} />
              {/* 亭身 */}
              <rect x="133" y="84" width="30" height="16" rx="0.5" opacity={0.4} />
              {/* 柱子 */}
              <line x1="136" y1="84" x2="136" y2="100" stroke="#5A554C" strokeWidth={1.2} />
              <line x1="160" y1="84" x2="160" y2="100" stroke="#5A554C" strokeWidth={1.2} />
              {/* 台基 */}
              <rect x="130" y="100" width="36" height="3" rx="0.5" opacity={0.3} />
            </g>

            {/* 松树 — 亭旁 */}
            <g opacity={0.35} fill="#5A554C">
              <line x1="115" y1="90" x2="115" y2="108" stroke="#5A554C" strokeWidth={1.5} />
              <ellipse cx="115" cy="86" rx="10" ry="6" />
              <ellipse cx="115" cy="80" rx="7" ry="4" />
              <ellipse cx="113" cy="75" rx="5" ry="3" />
            </g>

            {/* 水面波纹 */}
            <g opacity={0.12} stroke="#6B665C" strokeWidth={0.5} fill="none">
              <path d="M15 140 Q35 137 55 140 Q75 137 95 140" />
              <path d="M60 145 Q80 142 100 145 Q120 142 140 145" />
              <path d="M130 148 Q150 145 170 148 Q190 145 210 148" />
              <path d="M5 150 Q25 147 45 150 Q65 147 85 150" />
            </g>

            {/* 水面倒影 — 极淡 */}
            <rect x="0" y="135" width="228" height="25" fill="url(#sb-water)" />

            {/* 飞鸟 */}
            <g opacity={0.2} fill="none" stroke="#6B665C" strokeWidth={0.7} strokeLinecap="round">
              <path d="M45 35 Q48 30 52 34" />
              <path d="M52 34 Q55 30 58 35" />
              <path d="M70 25 Q72 21 75 24" />
              <path d="M75 24 Q77 21 80 26" />
            </g>
          </Box>
          <Typography sx={{
            fontSize: 9.5, color: ink.lightGray, letterSpacing: 2.5,
            fontFamily: serifFont, textAlign: 'center',
            mt: -0.5,
            textShadow: '0 0 8px rgba(255,255,255,0.8)',
          }}>
            墨语 · 水墨智境
          </Typography>
        </Box>
      </Box>

      {/* ═══════ 中间 + 右侧容器（共享背景） ═══════ */}
      <Box sx={{ flex: 1, display: 'flex', position: 'relative', overflow: 'hidden' }}>
        {/* 水墨山峰背景层 */}
        <MountainBackground />

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
                color: ink.gray,
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
                color: ink.gray,
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
              bgcolor: ink.black,
              fontFamily: serifFont,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            墨
          </Avatar>
        </Box>

        {/* 主内容区 */}
        <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0, position: 'relative', zIndex: 1 }}>
          <Outlet />
        </Box>

        {/* 右侧面板（仅对话页，透明背景） */}
        {isChat && (
          <Box
            sx={{
              width: PANEL_WIDTH, flexShrink: 0,
              overflow: 'auto',
              display: 'flex', flexDirection: 'column',
              position: 'relative', zIndex: 1,
            }}
          >
            <Box sx={{ p: 2, pt: 7, flex: 1 }}>
              <ChatConfig />
            </Box>
          </Box>
        )}
      </Box>
    </Box>
  );
}
