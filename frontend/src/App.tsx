import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import Layout from './components/Layout';
import ChatPage from './pages/ChatPage';

const KnowledgePage = lazy(() => import('./pages/KnowledgePage'));
const McpPage = lazy(() => import('./pages/McpPage'));
const SettingsPage = lazy(() => import('./pages/SettingsPage'));

function PageFallback() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', minHeight: 200 }}>
      <CircularProgress size={28} />
    </Box>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/chat" replace />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/knowledge" element={<Suspense fallback={<PageFallback />}><KnowledgePage /></Suspense>} />
        <Route path="/mcp" element={<Suspense fallback={<PageFallback />}><McpPage /></Suspense>} />
        <Route path="/settings" element={<Suspense fallback={<PageFallback />}><SettingsPage /></Suspense>} />
      </Route>
    </Routes>
  );
}
