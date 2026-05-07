import { Box, Typography } from '@mui/material';
import { ink, serifFont, useInk } from '../../theme/ThemeProvider';

interface InkLogoProps {
  size?: 'sm' | 'md' | 'lg';
  showTagline?: boolean;
}

export default function InkLogo({ size = 'md', showTagline = false }: InkLogoProps) {
  const di = useInk();
  const sizes = {
    sm: { title: '1rem', rag: '0.6rem', seal: '0.75rem', tagline: '0.5rem' },
    md: { title: '1.1rem', rag: '0.65rem', seal: '0.875rem', tagline: '0.5625rem' },
    lg: { title: '1.25rem', rag: '0.7rem', seal: '1rem', tagline: '0.625rem' },
  };

  const s = sizes[size];

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.6 }}>
        {/* 墨语 */}
        <Typography
          sx={{
            fontFamily: serifFont,
            fontWeight: 700,
            fontSize: s.title,
            color: di.black,
            letterSpacing: 2,
            lineHeight: 1.2,
          }}
        >
          墨语
        </Typography>
        {/* RAG */}
        <Typography
          component="span"
          sx={{
            fontSize: s.rag,
            color: di.lightGray,
            fontWeight: 500,
            lineHeight: 1,
            letterSpacing: 0.5,
          }}
        >
          RAG
        </Typography>
        {/* 红色印章 */}
        <Box
          sx={{
            width: s.seal,
            height: s.seal,
            backgroundColor: di.cinnabar,
            borderRadius: '2px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <Typography
            sx={{
              fontFamily: serifFont,
              fontWeight: 900,
              fontSize: `calc(${s.seal} * 0.55)`,
              color: '#FFFFFF',
              lineHeight: 1,
              letterSpacing: 0,
            }}
          >
            墨
          </Typography>
        </Box>
      </Box>
      {/* 标语 */}
      {showTagline && (
        <Typography
          sx={{
            fontSize: s.tagline,
            color: di.tagline,
            letterSpacing: 1.5,
            mt: 0.3,
            fontFamily: serifFont,
            lineHeight: 1,
          }}
        >
          让知识流动起来
        </Typography>
      )}
    </Box>
  );
}
