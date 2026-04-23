import { memo } from 'react';
import { Box, Typography } from '@mui/material';
import MarkdownRenderer from '../../components/MarkdownRenderer';
import fixIncompleteMarkdown from '../../utils/fixIncompleteMarkdown';
import { ink, serifFont } from '../../theme/ThemeProvider';

interface Props {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  isStreaming?: boolean;
  showTimestamp?: string;
  isThinking?: boolean;
}

/* ── 水墨研磨等待动画 ── */
function InkThinkingIndicator() {
  return (
    <Box sx={{
      display: 'flex', alignItems: 'center', gap: 1.5,
      py: 0.5, px: 0.5,
    }}>
      {/* 墨滴涟漪 */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.2 }}>
        {[0, 1, 2].map((i) => (
          <Box
            key={i}
            sx={{
              position: 'relative',
              width: 8, height: 8,
              '&::before': {
                content: '""',
                position: 'absolute',
                inset: 0,
                borderRadius: '50%',
                backgroundColor: i === 0 ? ink.gray : i === 1 ? ink.lightGray : ink.muted,
                animation: `inkDrop 1.6s ease-in-out ${i * 0.25}s infinite`,
              },
              '&::after': {
                content: '""',
                position: 'absolute',
                top: '50%', left: '50%',
                width: 18, height: 18,
                transform: 'translate(-50%, -50%) scale(0)',
                borderRadius: '50%',
                border: `1px solid ${ink.muted}`,
                opacity: 0,
                animation: `inkRipple 1.6s ease-out ${i * 0.25}s infinite`,
              },
              '@keyframes inkDrop': {
                '0%, 100%': { transform: 'scale(0.6)', opacity: 0.35 },
                '35%': { transform: 'scale(1.1)', opacity: 1 },
                '70%': { transform: 'scale(0.85)', opacity: 0.55 },
              },
              '@keyframes inkRipple': {
                '0%': { transform: 'translate(-50%, -50%) scale(0.5)', opacity: 0.5 },
                '60%': { transform: 'translate(-50%, -50%) scale(1.4)', opacity: 0 },
                '100%': { transform: 'translate(-50%, -50%) scale(1.4)', opacity: 0 },
              },
            }}
          />
        ))}
      </Box>

      {/* "研墨中" 文字 */}
      <Typography sx={{
        fontSize: 12.5,
        color: ink.lightGray,
        fontFamily: serifFont,
        letterSpacing: 1.5,
        animation: 'inkTextFade 2s ease-in-out infinite',
        '@keyframes inkTextFade': {
          '0%, 100%': { opacity: 0.4 },
          '50%': { opacity: 0.85 },
        },
      }}>
        研墨中
      </Typography>

      {/* 水墨笔触装饰线 */}
      <Box sx={{
        width: 36, height: 2,
        borderRadius: '0 1px 1px 0',
        background: `linear-gradient(90deg, ${ink.muted}, transparent)`,
        animation: 'inkBrushLine 2.4s ease-in-out infinite',
        '@keyframes inkBrushLine': {
          '0%': { width: 0, opacity: 0 },
          '30%': { width: 36, opacity: 0.5 },
          '60%': { width: 36, opacity: 0.2 },
          '100%': { width: 0, opacity: 0 },
        },
      }} />
    </Box>
  );
}

export default memo(function MessageBubble({ role, content, isStreaming, showTimestamp, isThinking }: Props) {
  const isUser = role === 'USER';

  const displayContent = isStreaming ? fixIncompleteMarkdown(content) : content;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: isUser ? 'flex-end' : 'flex-start', width: '100%' }}>
      {showTimestamp && (
        <Typography sx={{
          fontSize: 11.5, color: ink.muted, textAlign: isUser ? 'right' : 'left',
          width: '100%', mb: 1,
          px: 1,
        }}>
          {showTimestamp}
        </Typography>
      )}

      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.2, width: '100%', ...(isUser && { justifyContent: 'flex-end' }) }}>
        {/* AI消息 - 左侧头像 */}
        {!isUser && (
          <Box sx={{
            width: 32, height: 32, borderRadius: '50%',
            flexShrink: 0, mt: 0.25,
            bgcolor: ink.black,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 13, color: '#FFF', fontFamily: serifFont,
            fontWeight: 600,
            ...(isThinking && {
              animation: 'avatarPulse 2s ease-in-out infinite',
              '@keyframes avatarPulse': {
                '0%, 100%': { boxShadow: '0 0 0 0 rgba(74,74,74,0.25)' },
                '50%': { boxShadow: '0 0 0 6px rgba(74,74,74,0)' },
              },
            }),
          }}>
            墨
          </Box>
        )}

        {/* 消息内容 */}
        <Box
          sx={{
            minWidth: 40,
            maxWidth: isUser ? '80%' : 'calc(100% - 44px)',
            width: 'fit-content',
            px: 2.2, py: 1.4,
            borderRadius: isUser ? '8px 4px 8px 8px' : '4px 8px 8px 8px',
            backgroundColor: isUser
              ? 'rgba(74,74,74,0.9)'
              : 'rgba(255,255,255,0.72)',
            backdropFilter: !isUser ? 'blur(10px)' : undefined,
            WebkitBackdropFilter: !isUser ? 'blur(10px)' : undefined,
            color: isUser ? '#FFFFFF' : ink.black,
            border: isUser
              ? 'none'
              : '1px solid rgba(224,221,216,0.55)',
            wordBreak: 'break-word',
            transition: 'all 200ms ease-in-out',
            boxShadow: isUser
              ? '0 2px 8px rgba(0,0,0,0.1)'
              : '0 1px 4px rgba(0,0,0,0.03)',
            ...(isThinking && {
              animation: 'bubbleBreathe 2.5s ease-in-out infinite',
              '@keyframes bubbleBreathe': {
                '0%, 100%': { borderColor: 'rgba(200,195,186,0.3)', boxShadow: '0 1px 4px rgba(0,0,0,0.03)' },
                '50%': { borderColor: 'rgba(200,195,186,0.65)', boxShadow: '0 2px 12px rgba(74,74,74,0.06)' },
              },
            }),
            ...(isUser && {
              '& a': { color: '#FFFFFF', textDecoration: 'underline' },
              '& code': { backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 3, px: 0.4 },
            }),
          }}
        >
          {isThinking ? (
            <InkThinkingIndicator />
          ) : isUser ? (
            <Typography sx={{ fontSize: 15, lineHeight: 1.65, whiteSpace: 'pre-wrap' }}>{content}</Typography>
          ) : (
            <MarkdownRenderer content={displayContent} isStreaming={isStreaming} />
          )}
        </Box>

        {/* 用户消息右侧头像 */}
        {isUser && (
          <Box sx={{
            width: 30, height: 30, borderRadius: '50%',
            flexShrink: 0, mt: 0.25,
            bgcolor: ink.gray,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 11.5, color: '#FFF',
            fontWeight: 600,
          }}>
            我
          </Box>
        )}
      </Box>
    </Box>
  );
});
