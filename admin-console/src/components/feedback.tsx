import { useState, useCallback } from 'react';
import { Snackbar, Alert } from '@mui/material';

type Severity = 'success' | 'error' | 'info' | 'warning';

export function useFeedback() {
  const [msg, setMsg] = useState<{ text: string; severity: Severity } | null>(null);

  const show = useCallback((text: string, severity: Severity = 'info') => {
    setMsg({ text, severity });
  }, []);

  const Feedback = msg ? (
    <Snackbar
      open
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      autoHideDuration={3200}
      onClose={() => setMsg(null)}
    >
      <Alert severity={msg.severity} variant="filled" onClose={() => setMsg(null)}>
        {msg.text}
      </Alert>
    </Snackbar>
  ) : null;

  return { show, Feedback };
}
