import axios from "axios";

const API_URL = 'http://localhost:8080/api';

const api = axios.create({
    baseURL:API_URL
});

// The gateway works out who the user is from the token itself.
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');

    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
}
);


export const getActivities = () => api.get('/activities');
export const addActivity = (activity) => api.post('/activities', activity);
export const getActivity = (id) => api.get(`/activities/${id}`);
export const deleteActivity = (id) => api.delete(`/activities/${id}`);
export const getActivityDetail = (id) => api.get(`/recommendations/activity/${id}`);
// Analytics: every figure is aggregated by the Activity Service (MongoDB pipelines).
export const getAnalyticsDashboard = () => api.get('/activities/analytics/dashboard');
