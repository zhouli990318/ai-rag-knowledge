import { Box, SxProps } from '@mui/material';
import { ink, radius, sansFont } from '../../theme/ThemeProvider';

type BadgeStatus = 'default' | 'success' | 'warning' | 'error' | 'info' | 'cinnabar' | 'teal';

interface InkBadgeProps {
  label: string;
  status?: BadgeStatus;
  dot?: boolean;
  sx?: SxProps;
}

const statusColors: Record<BadgeStatus, { bg: string; color: string }> = {
  default:   { bg: 'rgba(74,74,74,0.08)', color: ink.gray },
  success:   { bg: 'rgba(91,112,101,0.12)', color: ink.teal },
  warning:   { bg: 'rgba(200,155,60,0.12)', color: '#B07D28' },
  error:     { bg: 'rgba(200,75,49,0.10)', color: ink.cinnabar },
  info:      { bg: 'rgba(91,112,101,0.08)', color: ink.teal },
  cinnabar:  { bg: 'rgba(200,75,49,0.10)', color: ink.cinnabar },
  teal:      { bg: 'rgba(91,112,101,0.12)', color: ink.teal },
};

export default function InkBadge({ label, status = 'default', dot, sx }: InkBadgeProps) {
  const c = statusColors[status];

  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.4,
        px: 1,
        py: 0.2,
        borderRadius: 4,
        backgroundColor: c.bg,
        color: c.color,
        fontSize: 11.5,
        fontWeight: 500,
        fontFamily: sansFont,
        lineHeight: 1.5,
        ...sx,
      }}
    >
      {dot && (
        <Box
          sx={{
            width: 5, height: 5, borderRadius: '50%',
            backgroundColor: c.color,
            flexShrink: 0,
          }}
        />
      )}
      {label}
    </Box>
  );
}
