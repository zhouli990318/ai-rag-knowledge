import { useEffect, useState } from 'react';
import { Box, Typography, Button, Drawer } from '@mui/material';
import { PlayArrowOutlined as PlayArrow } from '@mui/icons-material';
import { useMutation } from '@tanstack/react-query';
import { mcpGatewayApi } from '@/entities/mcp';
import { ui } from '@/shared/theme/semanticColors';
import type { CodeEditorComponent } from './CodeEditor';

interface Props {
  open: boolean;
  toolId: number | null;
  EditorComponent: CodeEditorComponent;
  onClose: () => void;
}

export default function InvokeToolDrawer({ open, toolId, EditorComponent, onClose }: Props) {
  const [args, setArgs] = useState('{}');
  const [result, setResult] = useState('');

  useEffect(() => {
    if (!open) return;
    setArgs('{}');
    setResult('');
  }, [open, toolId]);

  const invokeToolMutation = useMutation({
    mutationFn: (payload: { id: number; args: string }) => mcpGatewayApi.invokeTool(payload.id, payload.args),
    onSuccess: (data) => setResult(typeof data === 'string' ? data : JSON.stringify(data, null, 2)),
    onError: (error: any) => setResult('Error: ' + (error.message || 'Unknown')),
  });

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: { width: { xs: '100%', md: 420 }, p: 2.5, borderRadius: '4px 0 0 4px' },
      }}
    >
      <Typography sx={{ fontSize: 17, fontWeight: 600, mb: 2 }}>测试工具调用</Typography>
      <EditorComponent value={args} onChange={setArgs} height={160} />
      <Button
        variant="contained"
        startIcon={<PlayArrow />}
        fullWidth
        onClick={() => toolId && invokeToolMutation.mutate({ id: toolId, args })}
        sx={{ mt: 2, borderRadius: 4 }}
        disabled={invokeToolMutation.isPending || !toolId}
      >
        执行
      </Button>
      {result && (
        <Box sx={{
          mt: 2, p: 2.5, borderRadius: 2,
          backgroundColor: ui.hoverBg,
          border: `1px solid ${ui.border}`,
          maxHeight: 300, overflow: 'auto',
        }}>
          <Typography sx={{ fontSize: 13, fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>{result}</Typography>
        </Box>
      )}
    </Drawer>
  );
}
