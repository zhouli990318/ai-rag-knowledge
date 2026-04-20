import { memo, useState } from 'react';
import {
  Box, Typography, Chip, IconButton, Select, MenuItem,
  FormControl, InputLabel, Collapse, useTheme,
} from '@mui/material';
import { ExpandMore, ExpandLess, Storage, Hub } from '@mui/icons-material';
import { Provider, KnowledgeBase, McpApiSource } from '../../api/types';

interface Props {
  selectedProvider: number;
  setSelectedProvider: (id: number) => void;
  selectedKb: number;
  setSelectedKb: (id: number) => void;
  selectedMcpServers: number[];
  setSelectedMcpServers: (ids: number[]) => void;
  providers: Provider[];
  kbs: KnowledgeBase[];
  mcpSources: McpApiSource[];
}

export default memo(function ChatConfig({
  selectedProvider, setSelectedProvider,
  selectedKb, setSelectedKb,
  selectedMcpServers, setSelectedMcpServers,
  providers, kbs, mcpSources,
}: Props) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [expanded, setExpanded] = useState(false);

  const currentProvider = providers.find((p) => p.id === selectedProvider);
  const currentKb = kbs.find((kb) => kb.id === selectedKb);
  const hasRAG = selectedKb > 0;
  const hasMCP = selectedMcpServers.length > 0;

  return (
    <Box sx={{
      backgroundColor: isDark ? 'rgba(28,28,30,0.6)' : 'rgba(255,255,255,0.8)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
      borderBottom: `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)'}`,
      borderRadius: expanded ? '16px 16px 0 0' : 0,
    }}>
      {/* Collapsed bar */}
      <Box
        onClick={() => setExpanded(!expanded)}
        sx={{
          display: 'flex', alignItems: 'center', gap: 1,
          px: 2, py: 1, cursor: 'pointer',
          '&:active': { opacity: 0.7 },
        }}
      >
        <Box sx={{
          width: 8, height: 8, borderRadius: '50%',
          backgroundColor: currentProvider ? '#34C759' : '#FF9500',
          flexShrink: 0,
        }} />
        <Typography sx={{ fontSize: 14, fontWeight: 500, flex: 1 }}>
          {currentProvider?.name || '未选择供应商'}
          {currentProvider?.defaultModel && (
            <Typography component="span" sx={{ fontSize: 13, color: 'text.secondary', ml: 0.75 }}>
              {currentProvider.defaultModel}
            </Typography>
          )}
        </Typography>
        {hasRAG && <Chip icon={<Storage sx={{ fontSize: '14px !important' }} />} label="RAG" size="small" sx={{ height: 22, fontSize: 11 }} />}
        {hasMCP && <Chip icon={<Hub sx={{ fontSize: '14px !important' }} />} label={`MCP·${selectedMcpServers.length}`} size="small" sx={{ height: 22, fontSize: 11 }} />}
        <IconButton size="small" sx={{ p: 0.25 }}>
          {expanded ? <ExpandLess sx={{ fontSize: 18 }} /> : <ExpandMore sx={{ fontSize: 18 }} />}
        </IconButton>
      </Box>

      {/* Expanded panel */}
      <Collapse in={expanded}>
        <Box sx={{ px: 2, pb: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <FormControl size="small" fullWidth>
            <InputLabel>AI 供应商</InputLabel>
            <Select value={selectedProvider} onChange={(e) => setSelectedProvider(Number(e.target.value))} label="AI 供应商">
              {providers.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.name} ({p.providerType})</MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" fullWidth>
            <InputLabel>知识库 (RAG)</InputLabel>
            <Select value={selectedKb} onChange={(e) => setSelectedKb(Number(e.target.value))} label="知识库 (RAG)">
              <MenuItem value={0}>不使用</MenuItem>
              {kbs.map((kb) => (
                <MenuItem key={kb.id} value={kb.id}>{kb.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" fullWidth>
            <InputLabel>MCP 服务器</InputLabel>
            <Select
              multiple
              value={selectedMcpServers}
              onChange={(e) => setSelectedMcpServers(e.target.value as number[])}
              label="MCP 服务器"
              renderValue={(selected) => {
                const ids = selected as number[];
                if (ids.length === 0) return '不使用';
                return mcpSources.filter((s) => ids.includes(s.id)).map((s) => s.name).join(', ');
              }}
            >
              {mcpSources.filter((s) => s.active).map((source) => (
                <MenuItem key={source.id} value={source.id}>{source.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      </Collapse>
    </Box>
  );
});
