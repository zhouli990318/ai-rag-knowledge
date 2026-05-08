import { Box, SxProps } from '@mui/material';
import { ink, radius, sansFont } from '../../theme/ThemeProvider';
import {
  CheckCircleOutlined as SuccessIcon,
  WarningAmberOutlined as WarningIcon,
  ErrorOutlineOutlined as ErrorIcon,
  InfoOutlined as InfoIcon,
} from '@mui/icons-material';

type BadgeStatus = 'default' | 'success' | 'warning' | 'error' | 'info' | 'cinnabar' | 'teal';

interface InkBadgeProps {
  label: string;
  status?: BadgeStatus;
  dot?: boolean;
  showIcon?: boolean;
  sx?: SxProps;
}

const statusColors: Record<BadgeStatus, { bg: string; color: string }> = {
  default:   { bg: 'rgba(74,74,74,0.08)', color: ink.gray },
  success:   { bg: 'rgba(44,110,73,0.1)', color: '#2C6E49' },
  warning:   { bg: 'rgba(200,155,60,0.1)', color: '#C89B3C' },
  error:     { bg: 'rgba(200,75,49,0.10)', color: '#C84B31' },
  info:      { bg: 'rgba(74,127,181,0.1)', color: '#4A7FB5' },
  cinnabar:  { bg: 'rgba(200,75,49,0.10)', color: ink.cinnabar },
  teal:      { bg: 'rgba(44,110,73,0.1)', color: '#2C6E49' },
};

const statusIcons: Partial<Record<BadgeStatus, React.ReactNode>> = {
  success: <SuccessIcon sx={{ fontSize: 13 }} />,
  warning: <WarningIcon sx={{ fontSize: 13 }} />,
  error: <ErrorIcon sx={{ fontSize: 13 }} />,
  info: <InfoIcon sx={{ fontSize: 13 }} />,
};

export default function InkBadge({ label, status = 'default', dot, showIcon, sx }: InkBadgeProps) {
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
      {showIcon && statusIcons[status]}
      {dot && !showIcon && (
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
