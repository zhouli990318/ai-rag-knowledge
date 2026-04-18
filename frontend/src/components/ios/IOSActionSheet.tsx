import { ReactNode } from 'react';
import { Box, Typography, Button, useTheme, Slide } from '@mui/material';
import { motion, AnimatePresence } from 'framer-motion';

interface ActionItem {
  label: string;
  onClick: () => void;
  color?: 'primary' | 'error' | 'default';
  bold?: boolean;
}

interface IOSActionSheetProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  message?: string;
  actions: ActionItem[];
  children?: ReactNode;
}

export default function IOSActionSheet({ open, onClose, title, message, actions, children }: IOSActionSheetProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const bgColor = isDark ? 'rgba(44,44,46,0.92)' : 'rgba(255,255,255,0.92)';
  const separatorColor = isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)';

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
              position: 'fixed', inset: 0, zIndex: 1300,
              backgroundColor: 'rgba(0,0,0,0.4)',
              backdropFilter: 'blur(4px)',
              WebkitBackdropFilter: 'blur(4px)',
            }}
          />
          {/* Sheet */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            style={{
              position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 1301,
              padding: '0 8px env(safe-area-inset-bottom, 8px)',
              display: 'flex', flexDirection: 'column', gap: 8,
            }}
          >
            {/* Actions group */}
            <Box sx={{
              backgroundColor: bgColor,
              backdropFilter: 'blur(40px)',
              WebkitBackdropFilter: 'blur(40px)',
              borderRadius: '14px',
              overflow: 'hidden',
            }}>
              {(title || message) && (
                <Box sx={{
                  py: 1.5, px: 2, textAlign: 'center',
                  borderBottom: `0.5px solid ${separatorColor}`,
                }}>
                  {title && <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.secondary' }}>{title}</Typography>}
                  {message && <Typography sx={{ fontSize: 13, color: 'text.secondary', mt: 0.5 }}>{message}</Typography>}
                </Box>
              )}
              {children}
              {actions.map((action, i) => (
                <Box key={i}>
                  {i > 0 && <Box sx={{ borderTop: `0.5px solid ${separatorColor}` }} />}
                  <Button
                    fullWidth
                    onClick={() => { action.onClick(); onClose(); }}
                    sx={{
                      py: 1.5, borderRadius: 0,
                      fontSize: 20,
                      fontWeight: action.bold ? 600 : 400,
                      color: action.color === 'error' ? '#FF3B30'
                        : action.color === 'primary' ? '#007AFF'
                        : (isDark ? '#FFFFFF' : '#007AFF'),
                    }}
                  >
                    {action.label}
                  </Button>
                </Box>
              ))}
            </Box>

            {/* Cancel button */}
            <Box sx={{
              backgroundColor: bgColor,
              backdropFilter: 'blur(40px)',
              WebkitBackdropFilter: 'blur(40px)',
              borderRadius: '14px',
              mb: 1,
            }}>
              <Button
                fullWidth
                onClick={onClose}
                sx={{ py: 1.5, fontSize: 20, fontWeight: 600, color: '#007AFF', borderRadius: '14px' }}
              >
                取消
              </Button>
            </Box>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
