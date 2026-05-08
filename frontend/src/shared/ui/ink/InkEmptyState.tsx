import { ReactNode } from 'react';
import { SxProps } from '@mui/material';
import { Box, Typography, Button as MuiButton } from '@mui/material';
import { motion } from 'framer-motion';
import { ink, radius, serifFont, useInk } from '../../theme/ThemeProvider';

interface InkEmptyStateProps {
  icon: ReactNode;
  title: string;
  subtitle?: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  sx?: SxProps;
}

const MotionBox = motion.create(Box);

export default function InkEmptyState({ icon, title, subtitle, description, action, sx }: InkEmptyStateProps) {
  const di = useInk();
  return (
    <MotionBox
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
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
          width: 96, height: 96,
          borderRadius: '50%',
          backgroundColor: 'rgba(74,74,74,0.04)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mb: 3,
          color: di.muted,
          fontSize: 40,
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
        <Typography sx={{ fontSize: 14, color: di.lightGray, maxWidth: 400, lineHeight: 1.6 }}>
          {subtitle}
        </Typography>
      )}
      {description && (
        <Typography sx={{ fontSize: 13, color: di.muted, maxWidth: 400, lineHeight: 1.6, mt: 0.5 }}>
          {description}
        </Typography>
      )}
      {action && (
        <MuiButton
          variant="contained"
          onClick={action.onClick}
          sx={{
            mt: 3,
            borderRadius: radius.sm,
            px: 4,
            py: 1,
            fontSize: 15,
            textTransform: 'none',
            backgroundImage: `linear-gradient(135deg, ${di.cinnabar}, #A83D27)`,
            color: '#FFF',
            boxShadow: '0 2px 8px rgba(196,92,92,0.25)',
            '&:hover': {
              backgroundImage: `linear-gradient(135deg, #A83D27, ${di.cinnabar})`,
              boxShadow: '0 4px 16px rgba(196,92,92,0.35)',
            },
          }}
        >
          {action.label}
        </MuiButton>
      )}
    </MotionBox>
  );
}
