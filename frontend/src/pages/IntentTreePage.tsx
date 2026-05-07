import { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, IconButton, Chip, Button, TextField,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Select, MenuItem, FormControl, InputLabel, Stack,
  CircularProgress, Alert, Collapse,
} from '@mui/material';
import {
  ExpandMoreOutlined as ExpandMore, ExpandLessOutlined as ExpandLess,
  AddOutlined as Add, DeleteOutlined as Delete,
  AccountTreeOutlined as AccountTreeIcon,
} from '@mui/icons-material';
import { intentTreeApi, IntentNode } from '../api/orchestrationApi';
import { InkEmptyState } from '../components/ink';

const serifFont = '"Noto Serif SC", "Source Han Serif SC", serif';

const glassCard = {
  borderRadius: '12px',
  backgroundColor: 'rgba(255,255,255,0.72)',
  backdropFilter: 'blur(12px)',
  WebkitBackdropFilter: 'blur(12px)',
  border: '1px solid rgba(224,221,216,0.5)',
  boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
};

const routingColors: Record<string, { bg: string; color: string; border: string }> = {
  RETRIEVAL: { bg: 'rgba(91,112,101,0.1)', color: '#5B7065', border: '#5B7065' },
  TOOL:      { bg: 'rgba(139,105,20,0.1)', color: '#8B6914', border: '#8B6914' },
  DIRECT:    { bg: 'rgba(139,139,139,0.1)', color: '#8B8B8B', border: '#8B8B8B' },
  HYBRID:    { bg: 'rgba(74,111,165,0.1)', color: '#4A6FA5', border: '#4A6FA5' },
};

const levelLabels = ['领域', '类目', '话题'];

export default function IntentTreePage() {
  const [nodes, setNodes] = useState<IntentNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set());
  const [editNode, setEditNode] = useState<Partial<IntentNode>>({
    name: '', description: '', keywords: '', routingAdvice: 'RETRIEVAL', level: 0,
  });

  const loadTree = useCallback(async () => {
    try {
      setLoading(true);
      const res = await intentTreeApi.getTree();
      setNodes(res.data.data || []);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadTree(); }, [loadTree]);

  const handleCreate = async () => {
    try {
      await intentTreeApi.create(editNode);
      setDialogOpen(false);
      setEditNode({ name: '', description: '', keywords: '', routingAdvice: 'RETRIEVAL', level: 0 });
      loadTree();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await intentTreeApi.delete(id);
      loadTree();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const toggleCollapse = (id: number) => {
    setCollapsed(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const renderTree = (node: IntentNode, depth: number = 0) => {
    const rc = routingColors[node.routingAdvice] || routingColors.DIRECT;
    const hasChildren = node.children && node.children.length > 0;
    const isCollapsed = collapsed.has(node.id);

    return (
      <Box key={node.id}>
        <Box sx={{
          display: 'flex', alignItems: 'center', gap: 1, py: 1, px: 2,
          ml: depth * 2.5,
          borderLeft: depth > 0 ? '2px solid rgba(224,221,216,0.6)' : 'none',
          '&:hover': { backgroundColor: 'rgba(245,243,238,0.5)', borderRadius: '0 8px 8px 0' },
          transition: 'background-color 150ms',
        }}>
          {/* 展开/折叠按钮 */}
          {hasChildren ? (
            <IconButton size="small" onClick={() => toggleCollapse(node.id)} sx={{ color: '#8B8B8B', width: 28, height: 28 }}>
              {isCollapsed ? <ExpandMore fontSize="small" /> : <ExpandLess fontSize="small" />}
            </IconButton>
          ) : (
            <Box sx={{ width: 28 }} />
          )}

          {/* 层级标签 */}
          <Typography sx={{
            fontSize: 11, color: '#8B8B8B', fontWeight: 500,
            backgroundColor: 'rgba(224,221,216,0.3)', px: 0.8, py: 0.1, borderRadius: '3px',
            minWidth: 32, textAlign: 'center',
          }}>
            {levelLabels[node.level] || `L${node.level}`}
          </Typography>

          {/* 节点名称 */}
          <Typography sx={{ fontWeight: 600, fontSize: 14, color: '#2C2C2C', flex: 1 }}>
            {node.name}
          </Typography>

          {/* 路由建议 */}
          <Box sx={{
            display: 'inline-flex', px: 1, py: 0.2, borderRadius: '4px',
            backgroundColor: rc.bg, color: rc.color, fontSize: 11, fontWeight: 500,
            border: `1px solid ${rc.border}20`,
          }}>
            {node.routingAdvice}
          </Box>

          {/* 状态 */}
          <Box sx={{
            display: 'inline-flex', px: 0.8, py: 0.2, borderRadius: '4px',
            backgroundColor: node.status === 'PUBLISHED' ? 'rgba(44,110,73,0.1)' : 'rgba(139,139,139,0.1)',
            color: node.status === 'PUBLISHED' ? '#2C6E49' : '#8B8B8B',
            fontSize: 11, fontWeight: 500,
          }}>
            {node.status === 'PUBLISHED' ? '已发布' : '草稿'}
          </Box>

          {/* 描述 */}
          {node.description && (
            <Typography sx={{ fontSize: 12, color: '#8B8B8B', maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {node.description}
            </Typography>
          )}

          {/* 删除 */}
          <IconButton size="small" onClick={() => handleDelete(node.id)}
            sx={{ color: '#8B8B8B', '&:hover': { color: '#C84B31' }, width: 28, height: 28 }}>
            <Delete fontSize="small" />
          </IconButton>
        </Box>

        {/* 子节点 */}
        {hasChildren && (
          <Collapse in={!isCollapsed}>
            {node.children!.map(child => renderTree(child, depth + 1))}
          </Collapse>
        )}
      </Box>
    );
  };

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>;

  return (
    <Box sx={{ p: { xs: 2, md: 2.5 } }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
        <Typography variant="h5" sx={{ fontFamily: serifFont, fontWeight: 700, letterSpacing: 2, color: '#2C2C2C' }}>
          意图决策
        </Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setDialogOpen(true)}
          sx={{ fontFamily: serifFont, letterSpacing: 1 }}>
          新建节点
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <Box sx={{ ...glassCard, p: 1.5 }}>
        {nodes.length === 0 ? (
          <InkEmptyState
            icon={<AccountTreeIcon />}
            title="暂无意图节点"
            subtitle="构建意图决策树，让 AI 精准理解你的每一个意图"
            action={{ label: '新建节点', onClick: () => setDialogOpen(true) }}
          />
        ) : (
          nodes.map(node => renderTree(node))
        )}
      </Box>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontFamily: serifFont, fontWeight: 600 }}>新建意图节点</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="节点名称" fullWidth size="small" value={editNode.name}
              onChange={e => setEditNode(prev => ({ ...prev, name: e.target.value }))} />
            <TextField label="描述" fullWidth size="small" multiline rows={2} value={editNode.description}
              onChange={e => setEditNode(prev => ({ ...prev, description: e.target.value }))} />
            <TextField label="关键词（逗号分隔）" fullWidth size="small" value={editNode.keywords}
              onChange={e => setEditNode(prev => ({ ...prev, keywords: e.target.value }))} />
            <FormControl fullWidth size="small">
              <InputLabel>路由建议</InputLabel>
              <Select label="路由建议" value={editNode.routingAdvice}
                onChange={e => setEditNode(prev => ({ ...prev, routingAdvice: e.target.value as any }))}>
                <MenuItem value="RETRIEVAL">RETRIEVAL（知识检索）</MenuItem>
                <MenuItem value="TOOL">TOOL（工具调用）</MenuItem>
                <MenuItem value="DIRECT">DIRECT（直接回答）</MenuItem>
                <MenuItem value="HYBRID">HYBRID（混合）</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>层级</InputLabel>
              <Select label="层级" value={editNode.level}
                onChange={e => setEditNode(prev => ({ ...prev, level: Number(e.target.value) }))}>
                <MenuItem value={0}>领域（Domain）</MenuItem>
                <MenuItem value={1}>类目（Category）</MenuItem>
                <MenuItem value={2}>话题（Topic）</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!editNode.name}>创建</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
