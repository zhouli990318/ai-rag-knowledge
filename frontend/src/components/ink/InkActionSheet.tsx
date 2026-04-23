import { ReactNode } from 'react';
import { Box, Typography, ButtonBase, SxProps } from '@mui/material';
import { motion, AnimatePresence } from 'framer-motion';
import { ink, radius, serifFont } from '../../theme/ThemeProvider';

interface Action {
  label: string;
  onClick: () => void;
  color?: 'primary' | 'error' | 'default';
}

interface InkActionSheetProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  actions: Action[];
  sx?: SxProps;
}

export default function InkActionSheet({ open, onClose, title, actions, sx }: InkActionSheetProps) {
  const colorMap = {
    primary: ink.gray,
    error: ink.cinnabar,
    default: ink.gray,
  };

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            style={{
              position: 'fixed', inset: 0,
              backgroundColor: 'rgba(0,0,0,0.25)',
              backdropFilter: 'blur(4px)',
              WebkitBackdropFilter: 'blur(4px)',
              zIndex: 1300,
            }}
          />
          {/* Sheet */}
          <Box
            component={motion.div}
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 28, stiffness: 350 }}
            sx={{
              position: 'fixed',
              bottom: 0, left: 0, right: 0,
              zIndex: 1400,
              borderRadius: '8px 8px 0 0',
              backgroundColor: 'rgba(250,248,245,0.95)',
              backdropFilter: 'blur(20px)',
              WebkitBackdropFilter: 'blur(20px)',
              borderTop: '1px solid rgba(224,221,216,0.5)',
              maxHeight: '70vh',
              overflow: 'auto',
              ...sx,
            }}
          >
            {/* Drag handle */}
            <Box sx={{ display: 'flex', justifyContent: 'center', pt: 1.5, pb: 1 }}>
              <Box sx={{ width: 36, height: 3.5, borderRadius: 2, backgroundColor: 'rgba(74,74,74,0.12)' }} />
            </Box>

            {title && (
              <Typography
                sx={{
                  fontSize: 15, fontWeight: 600,
                  textAlign: 'center',
                  py: 1.5, pb: 2,
                  fontFamily: serifFont,
                }}
              >
                {title}
              </Typography>
            )}

            <Box sx={{ px: 2, pb: 'max(20px, env(safe-area-inset-bottom))' }}>
              {actions.map((action, i) => (
                <ButtonBase
                  key={i}
                  onClick={() => { action.onClick(); onClose(); }}
                  sx={{
                    width: '100%',
                    textAlign: 'left',
                    py: 1.75, px: 2,
                    borderRadius: radius.md,
                    mb: 0.25,
                    fontSize: 15,
                    fontWeight: 500,
                    color: action.color === 'error' ? ink.cinnabar : action.color === 'primary' ? ink.cinnabar : ink.black,
                    backgroundColor: 'transparent',
                    transition: 'all 150ms ease-in-out',
                    '&:hover': {
                      backgroundColor: action.color === 'error'
                        ? 'rgba(200,75,49,0.06)'
                        : 'rgba(74,74,74,0.04)',
                    },
                  }}
                >
                  {action.label}
                </ButtonBase>
              ))}

              {/* Cancel */}
              <ButtonBase
                onClick={onClose}
                sx={{
                  width: '100%',
                  textAlign: 'center',
                  py: 1.75, px: 2,
                  borderRadius: radius.md,
                  mt: 1,
                  fontSize: 15,
                  fontWeight: 500,
                  color: ink.lightGray,
                  backgroundColor: 'rgba(74,74,74,0.04)',
                  transition: 'all 150ms ease-in-out',
                  '&:hover': { backgroundColor: 'rgba(74,74,74,0.08)' },
                }}
              >
                取消
              </ButtonBase>
            </Box>
          </Box>
        </>
      )}
    </AnimatePresence>
  );
}
