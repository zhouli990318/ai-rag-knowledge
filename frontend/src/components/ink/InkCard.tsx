import { ReactNode, CSSProperties } from 'react';
import { Box, SxProps } from '@mui/material';
import { ink, radius } from '../../theme/ThemeProvider';

type AccentColor = 'default' | 'cinnabar' | 'teal' | 'gray';

interface InkCardProps {
  children?: ReactNode;
  accent?: AccentColor;
  sx?: SxProps;
  onClick?: () => void;
  style?: Record<string, any>;
}

const accentColors: Record<AccentColor, string> = {
  default: ink.gray,
  cinnabar: ink.cinnabar,
  teal: ink.teal,
  gray: ink.lightGray,
};

export default function InkCard({ children, accent = 'default', sx, onClick, style }: InkCardProps) {
  const color = accentColors[accent];

  return (
    <Box
      onClick={onClick}
      sx={{
        backgroundColor: 'rgba(255,255,255,0.72)',
        backdropFilter: 'blur(12px) saturate(180%)',
        WebkitBackdropFilter: 'blur(12px) saturate(180%)',
        border: '1px solid rgba(224,221,216,0.6)',
        borderRadius: radius.md,
        transition: 'all 300ms cubic-bezier(0.25,0.46,0.45,0.94)',
        overflow: 'hidden',
        position: 'relative',
        ...(onClick && {
          cursor: 'pointer',
          '&:hover': {
            borderColor: 'rgba(224,221,216,0.95)',
            boxShadow: `0 4px 20px rgba(0,0,0,0.06), 0 0 0 1px ${color}15`,
            transform: 'translateY(-1px)',
          },
        }),
        ...sx,
      }}
      style={style}
    >
      {/* 顶部彩色装饰线 (仅在有accent时显示) */}
      {accent !== 'default' && (
        <Box sx={{
          height: 3,
          background: `linear-gradient(90deg, ${color}, ${color}80)`,
          opacity: 0.7,
        }} />
      )}
      {children}
    </Box>
  );
}
