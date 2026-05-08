import { ReactNode, CSSProperties } from 'react';
import { Box, SxProps } from '@mui/material';
import { ink, radius, useInk } from '../../theme/ThemeProvider';

type AccentColor = 'default' | 'cinnabar' | 'teal' | 'gray';
type CardVariant = 'glass' | 'solid';

interface InkCardProps {
  children?: ReactNode;
  accent?: AccentColor;
  variant?: CardVariant;
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

export default function InkCard({ children, accent = 'default', variant = 'glass', sx, onClick, style }: InkCardProps) {
  const color = accentColors[accent];
  const di = useInk();

  const isSolid = variant === 'solid';

  return (
    <Box
      onClick={onClick}
      sx={{
        backgroundColor: isSolid
          ? di.cream === '#1A1A1A' ? '#2A2826' : '#FFFFFF'
          : '#f7f2e6',
        ...(isSolid ? {} : {
          backdropFilter: 'blur(12px) saturate(180%)',
          WebkitBackdropFilter: 'blur(12px) saturate(180%)',
        }),
        border: isSolid
          ? `1px solid ${di.cream === '#1A1A1A' ? '#3A3A3A' : '#E5E5E5'}`
          : '1px solid rgba(224,221,216,0.6)',
        borderRadius: radius.md,
        transition: 'all 300ms cubic-bezier(0.25,0.46,0.45,0.94)',
        overflow: 'hidden',
        position: 'relative',
        ...(onClick && {
          cursor: 'pointer',
          '&:hover': {
            borderColor: isSolid
              ? (di.cream === '#1A1A1A' ? '#505050' : '#D0D0D0')
              : 'rgba(224,221,216,0.95)',
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
