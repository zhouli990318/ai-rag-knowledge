import { Box, Typography } from '@mui/material';
import { ink, serifFont } from '../../theme/ThemeProvider';

interface InkLogoProps {
  size?: 'sm' | 'md' | 'lg';
}

export default function InkLogo({ size = 'md' }: InkLogoProps) {
  const sizes = {
    sm: { fontSize: 18, subSize: 9, gap: 0.3 },
    md: { fontSize: 22, subSize: 10, gap: 0.4 },
    lg: { fontSize: 26, subSize: 11, gap: 0.5 },
  };

  const s = sizes[size];

  return (
    <Box sx={{ display: 'flex', alignItems: 'baseline', gap: s.gap, flexWrap: 'wrap' }}>
      <Typography
        sx={{
          fontFamily: serifFont,
          fontWeight: 900,
          fontSize: s.fontSize,
          color: ink.black,
          letterSpacing: 3,
          lineHeight: 1.2,
        }}
      >
        墨语 RAG
      </Typography>
      {/* 红色印章风格标签 */}
      <Box
        component="span"
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontFamily: serifFont,
          fontWeight: 700,
          fontSize: size === 'lg' ? 10 : size === 'md' ? 9 : 8,
          color: ink.cinnabar,
          border: `1.5px solid ${size === 'lg' ? ink.cinnabar : ink.cinnabarLight}`,
          borderRadius: 2,
          px: 0.6, py: 0.15,
          letterSpacing: 1.5,
          lineHeight: 1,
          verticalAlign: 'middle',
        }}
      >
        水墨
      </Box>
    </Box>
  );
}
