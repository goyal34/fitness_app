// TEMPORARY scratch entry: renders the real ActivityForm/ActivityList with the
// real theme, bypassing only the Keycloak auth gate. Delete after verifying.
import { Container, CssBaseline, ThemeProvider } from '@mui/material';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router';
import ActivityForm from './components/ActivityForm';
import ActivityList from './components/ActivityList';
import { store } from './store/store';
import theme, { ACTIVITY_STYLES } from './theme';

// One sample activity per category so every chip/colour renders.
const activities = Object.keys(ACTIVITY_STYLES).map((type, i) => ({
    id: String(i + 1),
    type,
    duration: 30 + i * 5,
    caloriesBurned: 200 + i * 25,
    startTime: new Date(2026, 8, 22 - i, 7, 30).toISOString(),
}));

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <Provider store={store}>
            <ThemeProvider theme={theme}>
                <CssBaseline />
                <BrowserRouter>
                    <Container maxWidth="lg" sx={{ py: 4 }}>
                        <ActivityForm />
                        <ActivityList activities={activities} loading={false} onDelete={() => { }} />
                    </Container>
                </BrowserRouter>
            </ThemeProvider>
        </Provider>
    </StrictMode>
);
