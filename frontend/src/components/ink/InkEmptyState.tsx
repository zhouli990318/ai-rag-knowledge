import { ReactNode } from 'react';
import { SxProps } from '@mui/material';
import { Box, Typography, Button as MuiButton } from '@mui/material';
import { ink, radius, serifFont, useInk } from '../../theme/ThemeProvider';

interface InkEmptyStateProps {
  icon: ReactNode;
  title: string;
  subtitle?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  sx?: SxProps;
}

export default function InkEmptyState({ icon, title, subtitle, action, sx }: InkEmptyStateProps) {
  const di = useInk();
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        py: 8,
        px: 4,
        textAlign: 'center',
        ...sx,
      }}
    >
      {/* 水墨风格空状态图标 */}
      <Box
        sx={{
          width: 72, height: 72,
          borderRadius: '50%',
          backgroundColor: 'rgba(74,74,74,0.04)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mb: 2.5,
          color: di.muted,
          fontSize: 32,
        }}
      >
        {icon}
      </Box>
      <Typography
        variant="h6"
        sx={{ fontFamily: serifFont, fontWeight: 600, color: di.gray, mb: 0.5 }}
      >
        {title}
      </Typography>
      {subtitle && (
        <Typography sx={{ fontSize: 14, color: di.lightGray, maxWidth: 320, lineHeight: 1.6 }}>
          {subtitle}
        </Typography>
      )}
      {action && (
        <MuiButton
          variant="contained"
          onClick={action.onClick}
          sx={{
            mt: 2.5,
            borderRadius: radius.sm,
            px: 3,
            textTransform: 'none',
            backgroundImage: `linear-gradient(135deg, ${di.gray}, #3A3A3A)`,
            '&:hover': { backgroundImage: `linear-gradient(135deg, ${di.cinnabar}, #A83D27)` },
          }}
        >
          {action.label}
        </MuiButton>
      )}
    </Box>
  );
}
