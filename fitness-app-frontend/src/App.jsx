import {
  Alert, AppBar, Box, Button, Container, CssBaseline, Snackbar,
  ThemeProvider, Toolbar, Typography
} from "@mui/material";
import { useCallback, useContext, useEffect, useState } from "react";
import { AuthContext } from "react-oauth2-code-pkce";
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Navigate, Route, Routes, useLocation, useNavigate } from "react-router";
import { logout, setCredentials } from "./store/authSlice";
import ActivityForm from "./components/ActivityForm";
import ActivityList from "./components/ActivityList";
import ActivityDetail from "./components/ActivityDetail";
import AnalyticsDashboard from "./components/analytics/AnalyticsDashboard";
import {
  fitnessApi,
  useDeleteActivityMutation,
  useGetActivitiesQuery,
} from "./services/fitnessApi";
import theme from "./theme";
import { SignUpContext } from "./signUp";

const ActivitiesPage = ({ notify }) => {
  // Cached by RTK Query: revisiting the page reuses the list, and mutations
  // invalidate it so it refetches in the background.
  const { data: activities = [], isLoading, isError } = useGetActivitiesQuery();
  const [deleteActivity] = useDeleteActivityMutation();

  useEffect(() => {
    if (isError) notify('Could not load your activities.', 'error');
  }, [isError, notify]);

  const handleAdded = () => {
    notify('Activity added. Your AI report is being generated.', 'success');
  };

  const handleDelete = async (activity) => {
    try {
      // The cache is updated optimistically and rolled back on failure.
      await deleteActivity(activity.id).unwrap();
      notify('Activity deleted.', 'success');
    } catch (error) {
      console.error(error);
      notify('Could not delete the activity.', 'error');
    }
  };

  return (
    <Box>
      <ActivityForm onActivityAdded={handleAdded} onError={(m) => notify(m, 'error')} />
      <ActivityList activities={activities} loading={isLoading} onDelete={handleDelete} />
    </Box>
  );
};

const NAV = [
  { path: '/activities', label: 'Activities' },
  { path: '/analytics', label: 'Analytics' },
];

const NavLinks = () => {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  return (
    <Box sx={{ display: 'flex', gap: 0.5, mr: 2 }}>
      {NAV.map((item) => {
        const active = pathname.startsWith(item.path);
        return (
          <Button key={item.path} onClick={() => navigate(item.path)}
                  color={active ? 'primary' : 'inherit'}
                  aria-current={active ? 'page' : undefined}
                  sx={{ bgcolor: active ? 'rgba(91,33,182,0.08)' : 'transparent' }}>
            {item.label}
          </Button>
        );
      })}
    </Box>
  );
};

function App() {
  const { token, tokenData, logIn, logOut } = useContext(AuthContext);
  const startSignUp = useContext(SignUpContext);
  const dispatch = useDispatch();
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  const notify = useCallback((message, severity = 'success') => {
    setToast({ open: true, message, severity });
  }, []);

  // Clear the saved login as well as the Keycloak session, and drop any
  // cached API data so the next user never sees the previous user's records.
  const handleLogout = () => {
    dispatch(logout());
    dispatch(fitnessApi.util.resetApiState());
    logOut();
  };

  useEffect(() => {
    if (token) {
      dispatch(setCredentials({ token, user: tokenData }));
    }
  }, [token, tokenData, dispatch]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        {!token ? (
          <Box sx={{
            height: "100vh", display: "flex", flexDirection: "column",
            alignItems: "center", justifyContent: "center", textAlign: "center", px: 2,
          }}>
            <Typography component="div" sx={{ fontSize: 56, mb: 1 }}>🏋️</Typography>
            <Typography variant="h4" gutterBottom>Fitness Tracker</Typography>
            <Typography variant="subtitle1" color="text.secondary" sx={{ mb: 3 }}>
              Track your workouts and get AI-powered recommendations.
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', justifyContent: 'center' }}>
              <Button variant="contained" size="large" onClick={() => logIn()}>
                Login
              </Button>
              <Button variant="outlined" size="large" onClick={startSignUp}>
                Create account
              </Button>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              New here? Create a free account with your email.
            </Typography>
          </Box>
        ) : (
          <>
            <AppBar position="sticky" color="default" elevation={0}
                    sx={{ borderBottom: '1px solid rgba(0,0,0,0.08)', bgcolor: 'background.paper' }}>
              <Toolbar>
                <Typography component="span" sx={{ fontSize: 24, mr: 1 }}>🏋️</Typography>
                <Typography variant="h6" sx={{ mr: 3, display: { xs: 'none', sm: 'block' } }}>Fitness Tracker</Typography>
                <NavLinks />
                <Box sx={{ flexGrow: 1 }} />
                {tokenData?.given_name && (
                  <Typography variant="body2" color="text.secondary" sx={{ mr: 2, display: { xs: 'none', md: 'block' } }}>
                    {tokenData.given_name}
                  </Typography>
                )}
                <Button variant="outlined" color="secondary" onClick={handleLogout}>Logout</Button>
              </Toolbar>
            </AppBar>

            <Container maxWidth="lg" sx={{ py: 4 }}>
              <Routes>
                <Route path="/activities" element={<ActivitiesPage notify={notify} />} />
                <Route path="/activities/:id" element={<ActivityDetail />} />
                <Route path="/analytics" element={<AnalyticsDashboard />} />
                <Route path="/" element={<Navigate to="/activities" replace />} />
              </Routes>
            </Container>
          </>
        )}

        <Snackbar
          open={toast.open} autoHideDuration={4000}
          onClose={() => setToast((t) => ({ ...t, open: false }))}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity={toast.severity} variant="filled"
                 onClose={() => setToast((t) => ({ ...t, open: false }))}>
            {toast.message}
          </Alert>
        </Snackbar>
      </Router>
    </ThemeProvider>
  )
}

export default App
