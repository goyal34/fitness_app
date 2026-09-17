import {
    Box, Card, CardActionArea, CardContent, Chip, Grid2, IconButton,
    MenuItem, Skeleton, TextField, Tooltip, Typography
} from '@mui/material'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router';
import { activityStyle, ACTIVITY_STYLES } from '../theme';

const formatDate = (value) => {
    if (!value) return '';
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? '' : d.toLocaleString(undefined, {
        month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',
    });
};

const StatTile = ({ label, value }) => (
    <Card sx={{ flex: 1, minWidth: 140 }}>
        <CardContent sx={{ py: 1.5 }}>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography variant="h5">{value}</Typography>
        </CardContent>
    </Card>
);

const ActivityList = ({ activities, loading, onDelete }) => {
    const navigate = useNavigate();
    const [filter, setFilter] = useState('ALL');
    const [sort, setSort] = useState('NEWEST');

    const stats = useMemo(() => {
        const totalMinutes = activities.reduce((sum, a) => sum + (a.duration ?? 0), 0);
        const totalCalories = activities.reduce((sum, a) => sum + (a.caloriesBurned ?? 0), 0);
        return { count: activities.length, totalMinutes, totalCalories };
    }, [activities]);

    const visible = useMemo(() => {
        const list = filter === 'ALL' ? activities : activities.filter((a) => a.type === filter);
        const sorted = [...list].sort((a, b) => {
            if (sort === 'CALORIES') return (b.caloriesBurned ?? 0) - (a.caloriesBurned ?? 0);
            if (sort === 'DURATION') return (b.duration ?? 0) - (a.duration ?? 0);
            const at = new Date(a.createdAt ?? 0).getTime();
            const bt = new Date(b.createdAt ?? 0).getTime();
            return sort === 'OLDEST' ? at - bt : bt - at;
        });
        return sorted;
    }, [activities, filter, sort]);

    if (loading) {
        return (
            <Grid2 container spacing={2}>
                {[0, 1, 2].map((i) => (
                    <Grid2 key={i} size={{ xs: 12, sm: 6, md: 4 }}>
                        <Skeleton variant="rounded" height={132} />
                    </Grid2>
                ))}
            </Grid2>
        );
    }

    if (activities.length === 0) {
        return (
            <Card>
                <CardContent sx={{ textAlign: 'center', py: 6 }}>
                    <Typography variant="h6" gutterBottom>No activities yet</Typography>
                    <Typography color="text.secondary">
                        Log your first workout above and an AI report will be generated for it.
                    </Typography>
                </CardContent>
            </Card>
        );
    }

    return (
        <Box>
            <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
                <StatTile label="Activities" value={stats.count} />
                <StatTile label="Total time" value={`${stats.totalMinutes} min`} />
                <StatTile label="Calories burned" value={stats.totalCalories} />
            </Box>

            <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
                <TextField
                    select size="small" label="Filter" value={filter}
                    onChange={(e) => setFilter(e.target.value)} sx={{ minWidth: 160 }}
                >
                    <MenuItem value="ALL">All types</MenuItem>
                    {Object.entries(ACTIVITY_STYLES).map(([value, meta]) => (
                        <MenuItem key={value} value={value}>{meta.emoji}  {meta.label}</MenuItem>
                    ))}
                </TextField>

                <TextField
                    select size="small" label="Sort by" value={sort}
                    onChange={(e) => setSort(e.target.value)} sx={{ minWidth: 160 }}
                >
                    <MenuItem value="NEWEST">Newest first</MenuItem>
                    <MenuItem value="OLDEST">Oldest first</MenuItem>
                    <MenuItem value="CALORIES">Most calories</MenuItem>
                    <MenuItem value="DURATION">Longest duration</MenuItem>
                </TextField>

                <Box sx={{ flex: 1 }} />
                <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
                    Showing {visible.length} of {activities.length}
                </Typography>
            </Box>

            {visible.length === 0 ? (
                <Card>
                    <CardContent sx={{ textAlign: 'center', py: 4 }}>
                        <Typography color="text.secondary">
                            No activities match this filter.
                        </Typography>
                    </CardContent>
                </Card>
            ) : (
                <Grid2 container spacing={2}>
                    {visible.map((activity) => {
                        const style = activityStyle(activity.type);
                        return (
                            <Grid2 key={activity.id} size={{ xs: 12, sm: 6, md: 4 }}>
                                <Card sx={{
                                    height: '100%', position: 'relative',
                                    borderLeft: `4px solid ${style.color}`,
                                    transition: 'transform 120ms ease, box-shadow 120ms ease',
                                    '&:hover': {
                                        transform: 'translateY(-2px)',
                                        boxShadow: '0 8px 20px rgba(16,24,40,0.10)',
                                    },
                                }}>
                                    <CardActionArea onClick={() => navigate(`/activities/${activity.id}`)}>
                                        <CardContent>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                                <Typography component="span" sx={{ fontSize: 22 }}>
                                                    {style.emoji}
                                                </Typography>
                                                <Chip
                                                    label={style.label} size="small"
                                                    sx={{
                                                        bgcolor: `${style.color}14`,
                                                        color: style.color, fontWeight: 600,
                                                    }}
                                                />
                                            </Box>

                                            <Typography variant="body2" color="text.secondary">
                                                Duration
                                            </Typography>
                                            <Typography sx={{ mb: 0.5 }}>
                                                {activity.duration != null ? `${activity.duration} min` : '—'}
                                            </Typography>

                                            <Typography variant="body2" color="text.secondary">
                                                Calories
                                            </Typography>
                                            <Typography sx={{ mb: 1 }}>
                                                {activity.caloriesBurned ?? '—'}
                                            </Typography>

                                            <Typography variant="caption" color="text.secondary">
                                                {formatDate(activity.createdAt)}
                                            </Typography>
                                        </CardContent>
                                    </CardActionArea>

                                    <Tooltip title="Delete activity">
                                        <IconButton
                                            size="small" aria-label="delete activity"
                                            onClick={(e) => { e.stopPropagation(); onDelete?.(activity); }}
                                            sx={{ position: 'absolute', top: 8, right: 8 }}
                                        >
                                            ✕
                                        </IconButton>
                                    </Tooltip>
                                </Card>
                            </Grid2>
                        );
                    })}
                </Grid2>
            )}
        </Box>
    )
}

export default ActivityList
