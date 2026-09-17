import {
    Box, Button, Card, CardContent, CircularProgress, Grid2,
    MenuItem, TextField, Typography
} from '@mui/material'
import React, { useState } from 'react'
import { addActivity } from '../services/api'
import { ACTIVITY_STYLES } from '../theme'

const EMPTY = { type: 'RUNNING', duration: '', caloriesBurned: '' };

const ActivityForm = ({ onActivityAdded, onError }) => {
    const [activity, setActivity] = useState(EMPTY);
    const [errors, setErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);

    const validate = () => {
        const next = {};
        const duration = Number(activity.duration);
        const calories = Number(activity.caloriesBurned);

        if (activity.duration === '') next.duration = 'Duration is required';
        else if (!Number.isFinite(duration) || duration <= 0) next.duration = 'Must be greater than 0';
        else if (duration > 1440) next.duration = 'Must be 1440 minutes or less';

        if (activity.caloriesBurned === '') next.caloriesBurned = 'Calories burned is required';
        else if (!Number.isFinite(calories) || calories <= 0) next.caloriesBurned = 'Must be greater than 0';

        setErrors(next);
        return Object.keys(next).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validate()) return;

        setSubmitting(true);
        try {
            const response = await addActivity({
                type: activity.type,
                duration: Number(activity.duration),
                caloriesBurned: Number(activity.caloriesBurned),
                startTime: new Date().toISOString().slice(0, 19),
                additionalMetrics: {},
            });
            setActivity(EMPTY);
            setErrors({});
            onActivityAdded?.(response.data);
        } catch (error) {
            console.error(error);
            onError?.('Could not add the activity. Please try again.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Card sx={{ mb: 3 }}>
            <CardContent>
                <Typography variant="h6" gutterBottom>Log an activity</Typography>
                <Box component="form" onSubmit={handleSubmit} noValidate>
                    <Grid2 container spacing={2} alignItems="flex-start">
                        <Grid2 size={{ xs: 12, sm: 4 }}>
                            <TextField
                                select fullWidth label="Activity Type"
                                value={activity.type}
                                helperText=" "
                                onChange={(e) => setActivity({ ...activity, type: e.target.value })}
                            >
                                {Object.entries(ACTIVITY_STYLES).map(([value, meta]) => (
                                    <MenuItem key={value} value={value}>
                                        {meta.emoji}  {meta.label}
                                    </MenuItem>
                                ))}
                            </TextField>
                        </Grid2>

                        <Grid2 size={{ xs: 12, sm: 4 }}>
                            <TextField
                                fullWidth type="number" label="Duration (Minutes)"
                                value={activity.duration}
                                error={Boolean(errors.duration)}
                                helperText={errors.duration ?? ' '}
                                onChange={(e) => setActivity({ ...activity, duration: e.target.value })}
                            />
                        </Grid2>

                        <Grid2 size={{ xs: 12, sm: 4 }}>
                            <TextField
                                fullWidth type="number" label="Calories Burned"
                                value={activity.caloriesBurned}
                                error={Boolean(errors.caloriesBurned)}
                                helperText={errors.caloriesBurned ?? ' '}
                                onChange={(e) => setActivity({ ...activity, caloriesBurned: e.target.value })}
                            />
                        </Grid2>
                    </Grid2>

                    <Button
                        type="submit" variant="contained" disabled={submitting}
                        startIcon={submitting ? <CircularProgress size={16} color="inherit" /> : null}
                    >
                        {submitting ? 'Saving…' : 'Add Activity'}
                    </Button>
                </Box>
            </CardContent>
        </Card>
    )
}

export default ActivityForm
