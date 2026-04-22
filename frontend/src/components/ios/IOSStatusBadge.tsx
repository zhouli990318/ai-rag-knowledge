import { Box, Typography, useTheme } from '@mui/material';

type StatusType = 'success' | 'warning' | 'error' | 'info' | 'default';

const statusConfig: Record<StatusType, { light: string; dark: string }> = {
  success: { light: '#34C759', dark: '#30D158' },
  warning: { light: '#FF9500', dark: '#FF9F0A' },
  error: { light: '#FF3B30', dark: '#FF453A' },
  info: { light: '#007AFF', dark: '#0A84FF' },
  default: { light: 'rgba(60,60,67,0.3)', dark: 'rgba(235,235,245,0.3)' },
};

interface IOSStatusBadgeProps {
  label: string;
  status?: StatusType;
  dot?: boolean;
}

export default function IOSStatusBadge({ label, status = 'default', dot = false }: IOSStatusBadgeProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const color = statusConfig[status][isDark ? 'dark' : 'light'];

  if (dot) {
    return (
      <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75 }}>
        <Box sx={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: color }} />
        <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{label}</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{
      display: 'inline-flex', alignItems: 'center',
      px: 1, py: 0.25, borderRadius: 1.5,
      backgroundColor: `${color}18`,
    }}>
      <Typography sx={{ fontSize: 12, fontWeight: 600, color }}>{label}</Typography>
    </Box>
  );
}
