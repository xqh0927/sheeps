import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#176a4a', dark: '#105237', light: '#a7d3b7' },
    secondary: { main: '#8a6230' },
    background: { default: '#f4f6f3', paper: '#ffffff' },
    text: { primary: '#192c24', secondary: '#586a60' },
    divider: '#dce4dd',
  },
  typography: {
    fontFamily: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
    h5: { fontSize: '1.65rem', fontWeight: 700, letterSpacing: '-.035em' },
    h6: { fontWeight: 650 },
    button: { fontWeight: 650, textTransform: 'none' },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiCssBaseline: { styleOverrides: {
      body: { margin: 0 },
      ':focus-visible': { outline: '2px solid #187653', outlineOffset: 2 },
    } },
    MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: {
      root: { borderRadius: 8, minHeight: 36 },
      outlined: { borderColor: '#c5d3c8' },
    } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiCard: { defaultProps: { variant: 'outlined' } },
    MuiDialog: { styleOverrides: { paper: { borderRadius: 16 } } },
    MuiDialogTitle: { styleOverrides: { root: { fontWeight: 700 } } },
    MuiTableCell: { styleOverrides: {
      head: { backgroundColor: '#edf2ed', color: '#40584b', fontSize: 13, fontWeight: 700, whiteSpace: 'nowrap' },
      body: { fontSize: 14, fontVariantNumeric: 'tabular-nums', borderColor: '#e5eae5' },
    } },
    MuiTableRow: { styleOverrides: { root: { '&.MuiTableRow-hover:hover': { backgroundColor: '#f4f8f4' } } } },
    MuiTextField: { defaultProps: { variant: 'outlined' } },
    MuiTooltip: { defaultProps: { arrow: true } },
  },
});
