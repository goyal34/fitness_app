import { createTheme } from '@mui/material/styles';

// Visual identity for each activity type, used by cards, chips and icons.
export const ACTIVITY_STYLES = {
    RUNNING: { label: 'Running', color: '#ef6c00', emoji: '🏃' },
    WALKING: { label: 'Walking', color: '#2e7d32', emoji: '🚶' },
    CYCLING: { label: 'Cycling', color: '#1565c0', emoji: '🚴' },
};

export const activityStyle = (type) =>
    ACTIVITY_STYLES[type] ?? { label: type ?? 'Activity', color: '#616161', emoji: '🏋️' };

const theme = createTheme({
    palette: {
        mode: 'light',
        primary: { main: '#5b21b6' },
        secondary: { main: '#0891b2' },
        background: { default: '#f4f5f9', paper: '#ffffff' },
    },
    shape: { borderRadius: 12 },
    typography: {
        fontFamily: '"Inter", "Segoe UI", Roboto, system-ui, sans-serif',
        h5: { fontWeight: 600 },
        h6: { fontWeight: 600 },
    },
    components: {
        MuiCard: {
            styleOverrides: {
                root: {
                    border: '1px solid rgba(0,0,0,0.06)',
                    boxShadow: '0 1px 2px rgba(16,24,40,0.05)',
                },
            },
        },
        MuiButton: {
            defaultProps: { disableElevation: true },
            styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } },
        },
    },
});

export default theme;
