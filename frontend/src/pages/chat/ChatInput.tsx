import { memo, useCallback } from 'react';
import { Box, IconButton, InputBase, useTheme } from '@mui/material';
import { ArrowUpward, Stop } from '@mui/icons-material';

interface Props {
  value: string;
  onChange: (val: string) => void;
  onSend: () => void;
  streaming: boolean;
  disabled?: boolean;
  onStop?: () => void;
}

export default memo(function ChatInput({ value, onChange, onSend, streaming, disabled, onStop }: Props) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const canSend = value.trim().length > 0 && !disabled;

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (canSend && !streaming) onSend();
    }
  }, [canSend, streaming, onSend]);

  return (
    <Box sx={{
      px: 1.5, pt: 1.5,
      pb: 'max(12px, env(safe-area-inset-bottom, 12px))',
      borderTop: `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)'}`,
      backgroundColor: isDark ? 'rgba(0,0,0,0.4)' : 'rgba(249,249,249,0.94)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
    }}>
      <Box sx={{
        display: 'flex', alignItems: 'flex-end', gap: 1,
        backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : '#FFFFFF',
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'}`,
        borderRadius: '18px',
        px: 1.5, py: 0.5,
        transition: 'border-color 200ms',
        '&:focus-within': {
          borderColor: '#007AFF',
        },
      }}>
        <InputBase
          multiline
          maxRows={5}
          placeholder="输入消息..."
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={streaming}
          sx={{
            flex: 1,
            fontSize: 16,
            py: 0.5,
            '& textarea': { lineHeight: 1.4 },
            '& input::placeholder, & textarea::placeholder': {
              color: isDark ? 'rgba(235,235,245,0.4)' : 'rgba(60,60,67,0.4)',
              opacity: 1,
            },
          }}
        />
        {streaming ? (
          <IconButton
            onClick={onStop}
            size="small"
            sx={{
              width: 30, height: 30,
              backgroundColor: '#FF3B30',
              color: '#FFFFFF',
              '&:hover': { backgroundColor: '#E0332B' },
              '&:active': { transform: 'scale(0.9)' },
              mb: 0.25,
            }}
          >
            <Stop sx={{ fontSize: 16 }} />
          </IconButton>
        ) : (
          <IconButton
            onClick={onSend}
            disabled={!canSend}
            size="small"
            sx={{
              width: 30, height: 30,
              backgroundColor: canSend ? '#007AFF' : (isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)'),
              color: canSend ? '#FFFFFF' : (isDark ? 'rgba(235,235,245,0.3)' : 'rgba(60,60,67,0.3)'),
              transition: 'all 200ms',
              '&:hover': { backgroundColor: canSend ? '#0071E3' : undefined },
              '&:active': { transform: 'scale(0.9)' },
              '&.Mui-disabled': {
                backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)',
                color: isDark ? 'rgba(235,235,245,0.3)' : 'rgba(60,60,67,0.3)',
              },
              mb: 0.25,
            }}
          >
            <ArrowUpward sx={{ fontSize: 18 }} />
          </IconButton>
        )}
      </Box>
    </Box>
  );
});
