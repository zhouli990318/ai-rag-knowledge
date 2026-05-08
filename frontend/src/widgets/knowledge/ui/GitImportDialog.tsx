import { useEffect, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button,
} from '@mui/material';

interface Props {
  open: boolean;
  loading: boolean;
  onClose: () => void;
  onSubmit: (gitUrl: string) => void;
}

export default function GitImportDialog({ open, loading, onClose, onSubmit }: Props) {
  const [gitUrl, setGitUrl] = useState('');

  useEffect(() => {
    if (!open) return;
    setGitUrl('');
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Git 仓库导入</DialogTitle>
      <DialogContent>
        <TextField fullWidth label="Git 仓库 URL" value={gitUrl} onChange={(e) => setGitUrl(e.target.value)} placeholder="https://github.com/..." />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSubmit(gitUrl)} disabled={loading} sx={{ borderRadius: 4 }}>导入</Button>
      </DialogActions>
    </Dialog>
  );
}
