import { memo, useCallback } from 'react';
import { Box, IconButton, InputBase, Typography } from '@mui/material';
import {
  ArrowUpward as SendIcon,
  StopOutlined as Stop,
  AttachFileOutlined as AttachFile,
  InsertDriveFileOutlined as InsertDriveFile,
  LinkOutlined as LinkIcon,
} from '@mui/icons-material';
import { ink, radius, useInk } from '../../theme/ThemeProvider';
import { useThemeStore } from '../../stores/themeStore';

interface Props {
  value: string;
  onChange: (val: string) => void;
  onSend: () => void;
  streaming: boolean;
  disabled?: boolean;
  onStop?: () => void;
}

export default memo(function ChatInput({ value, onChange, onSend, streaming, disabled, onStop }: Props) {
  const canSend = value.trim().length > 0 && !disabled;
  const di = useInk();
  const mode = useThemeStore((s) => s.mode);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (canSend && !streaming) onSend();
    }
  }, [canSend, streaming, onSend]);

  return (
    <Box sx={{
      px: { xs: 2, md: 3 },
      pb: 'max(16px, env(safe-area-inset-bottom, 16px))',
      position: 'relative',
    }}>
      {/* 输入框容器 - 悬浮圆角卡片风格 */}
      <Box sx={{
        display: 'flex', flexDirection: 'column',
        backgroundColor: mode === 'dark' ? '#2A2826' : '#f7f2e6',
        border: `1px solid ${mode === 'dark' ? 'rgba(255,255,255,0.08)' : 'rgba(210,200,180,0.6)'}`,
        borderRadius: '16px',
        overflow: 'hidden',
        transition: 'all 220ms ease-in-out',
        boxShadow: mode === 'dark'
          ? '0 4px 24px rgba(0,0,0,0.25), 0 1px 4px rgba(0,0,0,0.15)'
          : '0 4px 24px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04)',
        '&:focus-within': {
          borderColor: mode === 'dark' ? 'rgba(255,255,255,0.15)' : '#B8B0A0',
          boxShadow: mode === 'dark'
            ? '0 6px 32px rgba(0,0,0,0.3), 0 0 0 3px rgba(255,255,255,0.04)'
            : '0 6px 32px rgba(0,0,0,0.1), 0 0 0 3px rgba(180,170,150,0.12)',
          backgroundColor: mode === 'dark' ? '#2E2C2A' : '#faf6ec',
        },
      }}>
        {/* 上方：文本输入 + 发送按钮 */}
        <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 0.75, px: 1.5, py: 0.6 }}>
          <InputBase
            multiline
            maxRows={5}
            placeholder="输入你的问题，Enter 发送，Shift + Enter 换行"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={streaming}
            sx={{
              flex: 1,
              fontSize: 14.5,
              fontFamily: '"Noto Sans SC", sans-serif',
              py: 0.35,
              '& textarea': { lineHeight: 1.55 },
              '& input::placeholder, & textarea::placeholder': {
                color: di.placeholder, opacity: 1,
              },
            }}
          />

          {/* 发送/停止按钮 */}
          {streaming ? (
            <IconButton
              onClick={onStop}
              size="small"
              sx={{
                width: 34, height: 34,
                bgcolor: di.cinnabar, color: '#FFFFFF', borderRadius: '50%',
                flexShrink: 0,
                transition: 'all 180ms ease-in-out',
                '&:hover': { bgcolor: '#B5432C', transform: 'scale(1.05)' },
                '&:active': { transform: 'scale(0.92)' },
                mb: 0.25,
              }}
            >
              <Stop sx={{ fontSize: 17 }} />
            </IconButton>
          ) : (
            <IconButton
              onClick={onSend}
              disabled={!canSend}
              size="small"
              sx={{
                width: 34, height: 34,
                bgcolor: canSend ? di.sendBtnBg : (mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(74,74,74,0.08)'),
                color: canSend ? '#FFFFFF' : di.muted,
                borderRadius: '50%',
                flexShrink: 0,
                transition: 'all 200ms ease-in-out',
                backgroundImage: canSend ? 'linear-gradient(135deg, #3D3D3D, #2D2D2D)' : 'none',
                boxShadow: canSend ? '0 2px 10px rgba(0,0,0,0.15)' : 'none',
                '&:hover': canSend
                  ? { bgcolor: di.cinnabar, backgroundImage: `linear-gradient(135deg, ${di.cinnabarLight}, ${di.cinnabar})`, boxShadow: '0 2px 12px rgba(196,92,92,0.25)', transform: 'scale(1.04)' }
                  : {},
                '&:active': { transform: 'scale(0.92)' },
                '&.Mui-disabled': {
                  bgcolor: mode === 'dark' ? 'rgba(255,255,255,0.04)' : 'rgba(74,74,74,0.06)',
                  color: di.disabledText,
                  backgroundImage: 'none',
                  boxShadow: 'none',
                },
                mb: 0.25,
              }}
            >
              <SendIcon sx={{ fontSize: 18 }} />
            </IconButton>
          )}
        </Box>

        {/* 下方工具栏：左侧图标 + 右侧状态 */}
        <Box sx={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          borderTop: `0.5px solid ${mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(224,221,216,0.3)'}`,
          px: 1.5, py: 0.35, height: 36,
        }}>
          <Box sx={{ display: 'flex', gap: 0.15, alignItems: 'center' }}>
            <IconButton size="small" sx={{ p: 0.65, color: di.lightGray }} title="上传文件">
              <AttachFile sx={{ fontSize: 18 }} />
            </IconButton>
            <IconButton size="small" sx={{ p: 0.65, color: di.lightGray }} title="添加文档">
              <InsertDriveFile sx={{ fontSize: 18 }} />
            </IconButton>
            <IconButton size="small" sx={{ p: 0.65, color: di.lightGray }} title="粘贴链接">
              <LinkIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Typography sx={{ fontSize: 11, color: di.placeholder }}>
              内容由 AI 生成 · 请仔细甄别
            </Typography>
            <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: 0.4 }}>
              <Box component="span" style={{ fontSize: 13, color: di.placeholder }}>◉</Box>
              <Typography sx={{ fontSize: 11, color: di.placeholder }}>已连接</Typography>
            </Box>
          </Box>
        </Box>
      </Box>
    </Box>
  );
});
