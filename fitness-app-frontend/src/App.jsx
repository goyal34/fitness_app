import {
  Alert, AppBar, Box, Button, Container, CssBaseline, Snackbar,
  ThemeProvider, Toolbar, Typography
} from "@mui/material";
import { useCallback, useContext, useEffect, useState } from "react";
import { AuthContext } from "react-oauth2-code-pkce";
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router";
import { setCredentials } from "./store/authSlice";
import ActivityForm from "./components/ActivityForm";
import ActivityList from "./components/ActivityList";
import ActivityDetail from "./components/ActivityDetail";
import { deleteActivity, getActivities } from "./services/api";
import theme from "./theme";

const ActivitiesPage = ({ notify }) => {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchActivities = useCallback(async () => {
    try {
      const response = await getActivities();
      setActivities(response.data ?? []);
    } catch (error) {
      console.error(error);
      notify('Could not load your activities.', 'error');
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => { fetchActivities(); }, [fetchActivities]);

  // Prepend the newly created activity instead of reloading the page.
  const handleAdded = (activity) => {
    setActivities((prev) => [activity, ...prev]);
    notify('Activity added. Your AI report is being generated.', 'success');
  };

  const handleDelete = async (activity) => {
    const previous = activities;
    setActivities((prev) => prev.filter((a) => a.id !== activity.id));
    try {
      await deleteActivity(activity.id);
      notify('Activity deleted.', 'success');
    } catch (error) {
      console.error(error);
      setActivities(previous);   // roll back the optimistic removal
      notify('Could not delete the activity.', 'error');
    }
  };

  return (
    <Box>
      <ActivityForm onActivityAdded={handleAdded} onError={(m) => notify(m, 'error')} />
      <ActivityList activities={activities} loading={loading} onDelete={handleDelete} />
    </Box>
  );
};

function App() {
  const { token, tokenData, logIn, logOut } = useContext(AuthContext);
  const dispatch = useDispatch();
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  const notify = useCallback((message, severity = 'success') => {
    setToast({ open: true, message, severity });
  }, []);

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
            <Button variant="contained" size="large" onClick={() => logIn()}>
              Login
            </Button>
          </Box>
        ) : (
          <>
            <AppBar position="sticky" color="default" elevation={0}
                    sx={{ borderBottom: '1px solid rgba(0,0,0,0.08)', bgcolor: 'background.paper' }}>
              <Toolbar>
                <Typography component="span" sx={{ fontSize: 24, mr: 1 }}>🏋️</Typography>
                <Typography variant="h6" sx={{ flexGrow: 1 }}>Fitness Tracker</Typography>
                {tokenData?.given_name && (
                  <Typography variant="body2" color="text.secondary" sx={{ mr: 2 }}>
                    {tokenData.given_name}
                  </Typography>
                )}
                <Button variant="outlined" color="secondary" onClick={logOut}>Logout</Button>
              </Toolbar>
            </AppBar>

            <Container maxWidth="lg" sx={{ py: 4 }}>
              <Routes>
                <Route path="/activities" element={<ActivitiesPage notify={notify} />} />
                <Route path="/activities/:id" element={<ActivityDetail />} />
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
