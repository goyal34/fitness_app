import React, { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { getActivity, getActivityDetail } from '../services/api';
import {
    Box, Button, Card, CardContent, Chip, CircularProgress, Divider,
    Grid2, Skeleton, Typography
} from '@mui/material';
import { activityStyle } from '../theme';

// The recommendation is produced asynchronously via RabbitMQ, so it may not
// exist for a few seconds after an activity is created. Poll briefly rather
// than making the user refresh.
const POLL_INTERVAL_MS = 4000;
const POLL_ATTEMPTS = 15;

const Metric = ({ label, value }) => (
    <Grid2 size={{ xs: 6, sm: 3 }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6">{value}</Typography>
    </Grid2>
);

const Bullets = ({ title, items }) => (
    <>
        <Typography variant="h6" gutterBottom>{title}</Typography>
        {items?.length ? items.map((item, index) => (
            <Typography key={index} paragraph>• {item}</Typography>
        )) : (
            <Typography color="text.secondary" paragraph>Nothing reported.</Typography>
        )}
    </>
);

const ActivityDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [activity, setActivity] = useState(null);
    const [recommendation, setRecommendation] = useState(null);
    const [loading, setLoading] = useState(true);
    const [waiting, setWaiting] = useState(true);
    const timerRef = useRef(null);

    useEffect(() => {
        let cancelled = false;
        let attempts = 0;

        const fetchActivity = async () => {
            try {
                const response = await getActivity(id);
                if (!cancelled) setActivity(response.data);
            } catch (error) {
                console.error(error);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        const pollRecommendation = async () => {
            attempts += 1;
            try {
                const response = await getActivityDetail(id);
                if (!cancelled) {
                    setRecommendation(response.data);
                    setWaiting(false);
                }
                return;
            } catch {
                // Not generated yet - keep polling until we run out of attempts.
            }
            if (!cancelled) {
                if (attempts >= POLL_ATTEMPTS) setWaiting(false);
                else timerRef.current = setTimeout(pollRecommendation, POLL_INTERVAL_MS);
            }
        };

        fetchActivity();
        pollRecommendation();

        return () => {
            cancelled = true;
            if (timerRef.current) clearTimeout(timerRef.current);
        };
    }, [id]);

    if (loading) {
        return (
            <Box sx={{ maxWidth: 860, mx: 'auto' }}>
                <Skeleton variant="rounded" height={160} sx={{ mb: 2 }} />
                <Skeleton variant="rounded" height={320} />
            </Box>
        );
    }

    if (!activity) {
        return (
            <Box sx={{ maxWidth: 860, mx: 'auto' }}>
                <Button onClick={() => navigate('/activities')} sx={{ mb: 2 }}>← Back</Button>
                <Card><CardContent>
                    <Typography variant="h6">Activity not found</Typography>
                    <Typography color="text.secondary">
                        It may have been deleted.
                    </Typography>
                </CardContent></Card>
            </Box>
        );
    }

    const style = activityStyle(activity.type);

    return (
        <Box sx={{ maxWidth: 860, mx: 'auto' }}>
            <Button onClick={() => navigate('/activities')} sx={{ mb: 2 }}>← Back to activities</Button>

            <Card sx={{ mb: 2, borderLeft: `4px solid ${style.color}` }}>
                <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                        <Typography component="span" sx={{ fontSize: 28 }}>{style.emoji}</Typography>
                        <Typography variant="h5">{style.label}</Typography>
                        <Chip
                            label={activity.type} size="small"
                            sx={{ bgcolor: `${style.color}14`, color: style.color, fontWeight: 600 }}
                        />
                    </Box>

                    <Grid2 container spacing={2}>
                        <Metric label="Duration"
                                value={activity.duration != null ? `${activity.duration} min` : '—'} />
                        <Metric label="Calories" value={activity.caloriesBurned ?? '—'} />
                        <Metric label="Logged"
                                value={activity.createdAt
                                    ? new Date(activity.createdAt).toLocaleDateString()
                                    : '—'} />
                        <Metric label="Time"
                                value={activity.createdAt
                                    ? new Date(activity.createdAt).toLocaleTimeString([], {
                                        hour: 'numeric', minute: '2-digit' })
                                    : '—'} />
                    </Grid2>
                </CardContent>
            </Card>

            <Card>
                <CardContent>
                    <Typography variant="h5" gutterBottom>AI Recommendation</Typography>

                    {recommendation ? (
                        <>
                            <Typography variant="h6">Analysis</Typography>
                            <Typography paragraph>{recommendation.recommendation}</Typography>
                            <Divider sx={{ my: 2 }} />
                            <Bullets title="Improvements" items={recommendation.improvements} />
                            <Divider sx={{ my: 2 }} />
                            <Bullets title="Suggestions" items={recommendation.suggestions} />
                            <Divider sx={{ my: 2 }} />
                            <Bullets title="Safety Guidelines" items={recommendation.safety} />
                        </>
                    ) : waiting ? (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 2 }}>
                            <CircularProgress size={20} />
                            <Typography color="text.secondary">
                                Generating your report… this usually takes a few seconds.
                            </Typography>
                        </Box>
                    ) : (
                        <Typography color="text.secondary">
                            No recommendation has been generated for this activity yet.
                        </Typography>
                    )}
                </CardContent>
            </Card>
        </Box>
    )
}

export default ActivityDetail
