import { ReactNode } from 'react';
import { Box, Typography, Button } from '@mui/material';

interface IOSEmptyStateProps {
  icon: ReactNode;
  title: string;
  subtitle?: string;
  action?: { label: string; onClick: () => void };
}

export default function IOSEmptyState({ icon, title, subtitle, action }: IOSEmptyStateProps) {
  return (
    <Box sx={{
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      py: 8, px: 4, textAlign: 'center',
    }}>
      <Box sx={{ mb: 2, color: 'text.disabled', '& .MuiSvgIcon-root': { fontSize: 56 } }}>
        {icon}
      </Box>
      <Typography sx={{ fontSize: 20, fontWeight: 600, mb: 0.5 }}>{title}</Typography>
      {subtitle && (
        <Typography sx={{ fontSize: 15, color: 'text.secondary', maxWidth: 280, lineHeight: 1.4 }}>
          {subtitle}
        </Typography>
      )}
      {action && (
        <Button
          variant="contained"
          onClick={action.onClick}
          sx={{ mt: 3, borderRadius: 2, px: 3 }}
        >
          {action.label}
        </Button>
      )}
    </Box>
  );
}
