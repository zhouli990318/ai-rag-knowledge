import { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Paper, IconButton,
  Card, Chip, Button, TextField,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Select, MenuItem, FormControl, InputLabel, Stack,
  CircularProgress, Alert, List, ListItem, ListItemText, Collapse,
} from '@mui/material';
import {
  ExpandMore, ExpandLess, Add, Delete,
} from '@mui/icons-material';
import { intentTreeApi, IntentNode } from '../api/orchestrationApi';

export default function IntentTreePage() {
  const [nodes, setNodes] = useState<IntentNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
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

  const renderTree = (node: IntentNode) => (
    <ListItem key={node.id} sx={{ flexDirection: 'column', alignItems: 'stretch', pl: node.level * 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}>
        <Typography variant="body2" sx={{ fontWeight: 500 }}>{node.name}</Typography>
        <Chip label={node.routingAdvice} size="small" variant="outlined"
          color={node.routingAdvice === 'TOOL' ? 'warning' : node.routingAdvice === 'HYBRID' ? 'info' : 'default'} />
        <Chip label={node.status} size="small"
          color={node.status === 'PUBLISHED' ? 'success' : 'default'} />
        {node.description && (
          <Typography variant="caption" color="text.secondary">{node.description}</Typography>
        )}
        <IconButton size="small" onClick={() => handleDelete(node.id)}>
          <Delete fontSize="small" />
        </IconButton>
      </Box>
      {node.children && node.children.length > 0 && (
        <List disablePadding>
          {node.children.map(renderTree)}
        </List>
      )}
    </ListItem>
  );

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>;

  return (
    <Box sx={{ p: 3, maxWidth: 900, mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>意图树管理</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setDialogOpen(true)}>
          新建节点
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <Paper sx={{ p: 2 }}>
        {nodes.length === 0 ? (
          <Typography color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
            暂无意图节点，点击"新建节点"开始构建意图树
          </Typography>
        ) : (
          <List>
            {nodes.map(renderTree)}
          </List>
        )}
      </Paper>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>新建意图节点</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="节点名称" fullWidth value={editNode.name}
              onChange={e => setEditNode(prev => ({ ...prev, name: e.target.value }))} />
            <TextField label="描述" fullWidth multiline rows={2} value={editNode.description}
              onChange={e => setEditNode(prev => ({ ...prev, description: e.target.value }))} />
            <TextField label="关键词（逗号分隔）" fullWidth value={editNode.keywords}
              onChange={e => setEditNode(prev => ({ ...prev, keywords: e.target.value }))} />
            <FormControl fullWidth>
              <InputLabel>路由建议</InputLabel>
              <Select value={editNode.routingAdvice}
                onChange={e => setEditNode(prev => ({ ...prev, routingAdvice: e.target.value as any }))}>
                <MenuItem value="RETRIEVAL">RETRIEVAL（知识检索）</MenuItem>
                <MenuItem value="TOOL">TOOL（工具调用）</MenuItem>
                <MenuItem value="DIRECT">DIRECT（直接回答）</MenuItem>
                <MenuItem value="HYBRID">HYBRID（混合）</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>层级</InputLabel>
              <Select value={editNode.level}
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
