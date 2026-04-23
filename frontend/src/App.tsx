import { lazy, Suspense, Component, ReactNode } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress, Typography, Button } from '@mui/material';
import Layout from './components/Layout';
import ChatPage from './pages/ChatPage';

const KnowledgePage = lazy(() => import('./pages/KnowledgePage'));
const McpPage = lazy(() => import('./pages/McpPage'));
const SettingsPage = lazy(() => import('./pages/SettingsPage'));
const IntentTreePage = lazy(() => import('./pages/IntentTreePage'));
const IngestMonitorPage = lazy(() => import('./pages/IngestMonitorPage'));
const TracePage = lazy(() => import('./pages/TracePage'));
const SystemSettingsPage = lazy(() => import('./pages/SystemSettingsPage'));

function PageFallback() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', minHeight: 200 }}>
      <CircularProgress size={28} />
    </Box>
  );
}

class InkErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false };
  static getDerivedStateFromError() { return { hasError: true }; }
  render() {
    if (this.state.hasError) {
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 8, px: 4, textAlign: 'center' }}>
          <Box sx={{ width: 72, height: 72, borderRadius: '50%', backgroundColor: 'rgba(74,74,74,0.04)', display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2.5, color: '#B0ADA6', fontSize: 32 }}>
            ⚠
          </Box>
          <Typography variant="h6" sx={{ fontFamily: '"Noto Serif SC", serif', fontWeight: 600, color: '#4A4A4A', mb: 0.5 }}>
            页面加载出错
          </Typography>
          <Typography sx={{ fontSize: 14, color: '#8B8B8B', mb: 2 }}>请刷新页面重试</Typography>
          <Button variant="contained" onClick={() => this.setState({ hasError: false })}>重试</Button>
        </Box>
      );
    }
    return this.props.children;
  }
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/chat" replace />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/knowledge" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><KnowledgePage /></Suspense></InkErrorBoundary>} />
        <Route path="/mcp" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><McpPage /></Suspense></InkErrorBoundary>} />
        <Route path="/settings" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><SettingsPage /></Suspense></InkErrorBoundary>} />
        <Route path="/intent-tree" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><IntentTreePage /></Suspense></InkErrorBoundary>} />
        <Route path="/ingest-monitor" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><IngestMonitorPage /></Suspense></InkErrorBoundary>} />
        <Route path="/traces" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><TracePage /></Suspense></InkErrorBoundary>} />
        <Route path="/system-settings" element={<InkErrorBoundary><Suspense fallback={<PageFallback />}><SystemSettingsPage /></Suspense></InkErrorBoundary>} />
      </Route>
    </Routes>
  );
}
