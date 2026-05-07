import { memo } from 'react';
import { Box, Typography } from '@mui/material';
import MarkdownRenderer from '../../components/MarkdownRenderer';
import fixIncompleteMarkdown from '../../utils/fixIncompleteMarkdown';
import { ink, serifFont, useInk } from '../../theme/ThemeProvider';
import { useThemeStore } from '../../stores/themeStore';

interface Props {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  isStreaming?: boolean;
  showTimestamp?: string;
  isThinking?: boolean;
}

/* ── 水墨研磨等待动画 ── */
function InkThinkingIndicator({ mode }: { mode: 'light' | 'dark' }) {
  const dotColors = mode === 'dark'
    ? ['#A09A94', '#8B8B8B', '#6A6A6A']
    : [ink.gray, ink.lightGray, ink.muted];
  const lineColor = mode === 'dark' ? '#6A6A6A' : ink.muted;
  const textColor = mode === 'dark' ? '#8B8B8B' : ink.lightGray;
  const rippleBorder = mode === 'dark' ? '#5A5A5A' : ink.muted;
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
                backgroundColor: dotColors[i],
                animation: `inkDrop 1.6s ease-in-out ${i * 0.25}s infinite`,
              },
              '&::after': {
                content: '""',
                position: 'absolute',
                top: '50%', left: '50%',
                width: 18, height: 18,
                transform: 'translate(-50%, -50%) scale(0)',
                borderRadius: '50%',
                border: `1px solid ${rippleBorder}`,
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
        color: textColor,
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
        background: `linear-gradient(90deg, ${lineColor}, transparent)`,
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

/* ── 水墨文人头像 SVG ── */
function ScholarAvatar({ pulse, mode }: { pulse?: boolean; mode: 'light' | 'dark' }) {
  const strokeColor = mode === 'dark' ? '#A09A94' : '#4A4540';
  const bgColor = mode === 'dark' ? '#2A2826' : ink.cardBg;
  const borderColor = mode === 'dark' ? '#3A3836' : ink.border;
  return (
    <Box sx={{
      width: '2.25rem', height: '2.25rem', borderRadius: '50%',
      flexShrink: 0, mt: 0.25,
      bgcolor: bgColor,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      overflow: 'hidden',
      border: `1px solid ${borderColor}`,
      ...(pulse && {
        animation: 'avatarPulse 2s ease-in-out infinite',
        '@keyframes avatarPulse': {
          '0%, 100%': { boxShadow: `0 0 0 0 rgba(45,45,45,0.2)` },
          '50%': { boxShadow: `0 0 0 6px rgba(45,45,45,0)` },
        },
      }),
    }}>
      <Box component="svg" viewBox="0 0 36 36" sx={{ width: '80%', height: '80%' }}>
        {/* 斗笠 */}
        <path d="M10 14 Q18 6 26 14" fill="none" stroke={strokeColor} strokeWidth={1.2} strokeLinecap="round" />
        <path d="M12 14 Q18 8 24 14" fill="none" stroke={strokeColor} strokeWidth={0.6} opacity={0.4} />
        {/* 头部 */}
        <circle cx="18" cy="16.5" r="2.8" fill="none" stroke={strokeColor} strokeWidth={1} />
        {/* 身体 — 宽袖长袍 */}
        <path d="M14 19 Q13 24 10 30 L18 28 L26 30 Q23 24 22 19" fill="none" stroke={strokeColor} strokeWidth={1.1} strokeLinejoin="round" />
        {/* 袖子 */}
        <path d="M14 22 Q9 21 7 24" fill="none" stroke={strokeColor} strokeWidth={0.9} strokeLinecap="round" />
        <path d="M22 22 Q27 21 29 24" fill="none" stroke={strokeColor} strokeWidth={0.9} strokeLinecap="round" />
        {/* 下摆 */}
        <path d="M10 30 Q14 32 18 31 Q22 32 26 30" fill="none" stroke={strokeColor} strokeWidth={0.8} />
      </Box>
    </Box>
  );
}

export default memo(function MessageBubble({ role, content, isStreaming, showTimestamp, isThinking }: Props) {
  const isUser = role === 'USER';
  const di = useInk();
  const mode = useThemeStore((s) => s.mode);

  const displayContent = isStreaming ? fixIncompleteMarkdown(content) : content;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: isUser ? 'flex-end' : 'flex-start', width: '100%' }}>
      {showTimestamp && (
        <Typography sx={{
          fontSize: 11.5, color: di.muted, textAlign: isUser ? 'right' : 'left',
          width: '100%', mb: 1,
          px: 1,
        }}>
          {showTimestamp}
        </Typography>
      )}

      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.2, width: '100%', ...(isUser && { justifyContent: 'flex-end' }) }}>
        {/* AI消息 - 左侧水墨文人头像 */}
        {!isUser && (
          <ScholarAvatar pulse={isThinking} mode={mode} />
        )}

        {/* 消息内容 */}
        <Box
          sx={{
            minWidth: 40,
            maxWidth: isUser ? '80%' : 'calc(100% - 52px)',
            width: 'fit-content',
            px: 2.2, py: 1.4,
            borderRadius: isUser ? '8px 4px 8px 8px' : '4px 8px 8px 8px',
            backgroundColor: isUser
              ? (mode === 'dark' ? 'rgba(200,200,200,0.12)' : 'rgba(74,74,74,0.9)')
              : di.cardBg,
            color: isUser ? (mode === 'dark' ? '#E8E4DF' : '#FFFFFF') : di.black,
            border: isUser
              ? (mode === 'dark' ? '1px solid rgba(255,255,255,0.08)' : 'none')
              : `1px solid ${di.border}`,
            wordBreak: 'break-word',
            transition: 'all 200ms ease-in-out',
            boxShadow: isUser
              ? (mode === 'dark' ? '0 2px 8px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.1)')
              : (mode === 'dark' ? '0 1px 4px rgba(0,0,0,0.15)' : '0 1px 4px rgba(0,0,0,0.03)'),
            ...(isThinking && {
              animation: 'bubbleBreathe 2.5s ease-in-out infinite',
              '@keyframes bubbleBreathe': {
                '0%, 100%': { borderColor: di.glassBorder, boxShadow: '0 1px 4px rgba(0,0,0,0.03)' },
                '50%': { borderColor: di.border, boxShadow: `0 2px 12px rgba(45,45,45,${mode === 'dark' ? '0.15' : '0.06'})` },
              },
            }),
            ...(isUser && {
              '& a': { color: mode === 'dark' ? '#E8E4DF' : '#FFFFFF', textDecoration: 'underline' },
              '& code': { backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 3, px: 0.4 },
            }),
          }}
        >
          {isThinking ? (
            <InkThinkingIndicator mode={mode} />
          ) : isUser ? (
            <Typography sx={{ fontSize: '0.875rem', lineHeight: 1.65, whiteSpace: 'pre-wrap' }}>{content}</Typography>
          ) : (
            <MarkdownRenderer content={displayContent} isStreaming={isStreaming} />
          )}
        </Box>

        {/* 用户消息右侧头像 */}
        {isUser && (
          <Box sx={{
            width: '2.25rem', height: '2.25rem', borderRadius: '50%',
            flexShrink: 0, mt: 0.25,
            bgcolor: di.gray,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 12, color: mode === 'dark' ? '#1A1A1A' : '#FFF',
            fontWeight: 600,
          }}>
            我
          </Box>
        )}
      </Box>
    </Box>
  );
});
