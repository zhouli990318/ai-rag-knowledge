import { Box, SxProps } from '@mui/material';
import { useInk } from '../../theme/ThemeProvider';
import { useThemeStore } from '../../stores/themeStore';
import mainInkLandscape from '../../assets/images/main-ink-landscape.webp';

interface InkBackgroundProps {
  sx?: SxProps;
}

export default function InkBackground({ sx }: InkBackgroundProps) {
  const mode = useThemeStore((s) => s.mode);

  return (
    <Box
      sx={{
        position: 'absolute',
        inset: 0,
        pointerEvents: 'none',
        overflow: 'hidden',
        ...sx,
      }}
    >
      <Box
        component="img"
        src={mainInkLandscape}
        alt=""
        sx={{
          position: 'absolute',
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          objectPosition: 'center bottom',
          bottom: 0,
          left: 0,
          opacity: mode === 'dark' ? 0.03 : 0.06,
          filter: mode === 'dark' ? 'brightness(0.5) contrast(1.3) invert(0.85)' : 'none',
          transition: 'opacity 0.4s ease, filter 0.4s ease',
        }}
      />
    </Box>
  );
}
