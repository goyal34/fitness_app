import React, { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import {
    Box, Button, Card, CardContent, Chip, Grid2, Skeleton, Typography
} from '@mui/material';
import { activityStyle } from '../theme';
import AiReport from './AiReport';
import {
    useGetActivityQuery,
    useGetActivityRecommendationQuery,
} from '../services/fitnessApi';

// The recommendation is produced asynchronously via RabbitMQ, so it may not
// exist for a few seconds after an activity is created. RTK Query polls for it
// rather than making the user refresh.
const POLL_INTERVAL_MS = 4000;
const POLL_ATTEMPTS = 15;

const Metric = ({ label, value }) => (
    <Grid2 size={{ xs: 6, sm: 3 }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6">{value}</Typography>
    </Grid2>
);

const ActivityDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const { data: activity, isLoading } = useGetActivityQuery(id);

    const [polling, setPolling] = useState(true);
    const [exhausted, setExhausted] = useState(false);
    const attemptsRef = useRef(0);

    const { data: recommendation, isError } = useGetActivityRecommendationQuery(id, {
        pollingInterval: polling ? POLL_INTERVAL_MS : 0,
    });

    // A new activity gets a fresh poll budget.
    useEffect(() => {
        attemptsRef.current = 0;
        setExhausted(false);
        setPolling(true);
    }, [id]);

    // Stop polling once a report (including a FAILED one) arrives, or after
    // POLL_ATTEMPTS unsuccessful requests.
    useEffect(() => {
        if (recommendation) {
            setPolling(false);
            return;
        }
        if (!isError) return;
        attemptsRef.current += 1;
        if (attemptsRef.current >= POLL_ATTEMPTS) {
            setExhausted(true);
            setPolling(false);
        }
    }, [recommendation, isError]);

    if (isLoading) {
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
                        <Metric label="Date"
                                value={(activity.startTime ?? activity.createdAt)
                                    ? new Date(activity.startTime ?? activity.createdAt).toLocaleDateString()
                                    : '—'} />
                        <Metric label="Time"
                                value={(activity.startTime ?? activity.createdAt)
                                    ? new Date(activity.startTime ?? activity.createdAt).toLocaleTimeString([], {
                                        hour: 'numeric', minute: '2-digit' })
                                    : '—'} />
                    </Grid2>
                </CardContent>
            </Card>

            <AiReport recommendation={recommendation} waiting={!recommendation && !exhausted} />
        </Box>
    )
}

export default ActivityDetail
