import {
    Alert, Box, Button, Card, CardActionArea, CardContent, Grid2, Skeleton, Table, TableBody,
    TableCell, TableHead, TableRow, Typography
} from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useGetAnalyticsDashboardQuery } from '../../services/fitnessApi';
import { activityStyle } from '../../theme';
import { ActivityHeatmap, BarList, ColumnChart, LineChart, SegmentedBar } from './charts';
import { VIZ } from './vizTokens';

// All numbers come pre-aggregated from GET /api/activities/analytics/dashboard;
// this page only formats them.

const LEVEL_COLORS = {
    LOW: { color: VIZ.ramp[0], text: VIZ.ink },
    MODERATE: { color: VIZ.ramp[1], text: '#ffffff' },
    HIGH: { color: VIZ.ramp[2], text: '#ffffff' },
    VERY_HIGH: { color: VIZ.ramp[3], text: '#ffffff' },
};

const shortDate = (iso) => new Date(`${iso}T00:00`).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
const longDate = (iso) => new Date(`${iso}T00:00`).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
const monthShort = (ym) => new Date(`${ym}-01T00:00`).toLocaleDateString(undefined, { month: 'short' });
const monthLong = (ym) => new Date(`${ym}-01T00:00`).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
const daysInMonth = (ym) => { const [y, m] = ym.split('-').map(Number); return new Date(y, m, 0).getDate(); };
const hours = (minutes) => (minutes >= 60 ? `${Math.floor(minutes / 60)}h ${minutes % 60}m` : `${minutes}m`);
const n = (v) => Number(v ?? 0).toLocaleString();

/** ▲/▼ with text, never colour alone. More is treated as better for every metric. */
const Delta = ({ change, changePercent, previous }) => {
    if (!change) return <Typography component="span" sx={{ fontSize: 12, color: VIZ.muted }}>No change</Typography>;
    const up = change > 0;
    const text = changePercent != null ? `${Math.abs(changePercent)}%` : previous === 0 ? 'new' : n(Math.abs(change));
    return (
        <Typography component="span" sx={{ fontSize: 12, fontWeight: 600, color: up ? VIZ.good : VIZ.critical }}>
            {up ? '▲' : '▼'} {text}
        </Typography>
    );
};

const StatTile = ({ label, value, sub }) => (
    <Card sx={{ height: '100%' }}>
        <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
            <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{label}</Typography>
            <Typography sx={{ fontSize: 28, fontWeight: 700, lineHeight: 1.25, my: 0.25 }}>{value}</Typography>
            <Box sx={{ fontSize: 12, color: 'text.secondary', minHeight: 18 }}>{sub}</Box>
        </CardContent>
    </Card>
);

/** Card with a title and a Chart/Table switch, so every chart has a table view. */
const ChartCard = ({ title, subtitle, table, children }) => {
    const [showTable, setShowTable] = useState(false);
    return (
        <Card sx={{ height: '100%' }}>
            <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 2 }}>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography variant="h6" sx={{ fontSize: 16 }}>{title}</Typography>
                        {subtitle && <Typography variant="body2" color="text.secondary">{subtitle}</Typography>}
                    </Box>
                    {table && (
                        <Button size="small" onClick={() => setShowTable((v) => !v)} sx={{ flexShrink: 0 }}>
                            {showTable ? 'Chart' : 'Table'}
                        </Button>
                    )}
                </Box>
                {showTable && table ? <DataTable {...table} /> : children}
            </CardContent>
        </Card>
    );
};

const DataTable = ({ columns, rows }) => (
    <Box sx={{ maxHeight: 260, overflow: 'auto' }}>
        <Table size="small" stickyHeader>
            <TableHead>
                <TableRow>
                    {columns.map((c, i) => (
                        <TableCell key={c} align={i ? 'right' : 'left'} sx={{ fontWeight: 600, bgcolor: 'background.paper' }}>{c}</TableCell>
                    ))}
                </TableRow>
            </TableHead>
            <TableBody>
                {rows.map((r) => (
                    <TableRow key={r[0]}>
                        {r.map((cell, i) => (
                            <TableCell key={i} align={i ? 'right' : 'left'} sx={{ fontVariantNumeric: 'tabular-nums' }}>{cell}</TableCell>
                        ))}
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    </Box>
);

const BestTile = ({ emoji, label, value, detail, onClick }) => {
    const body = (
        <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
            <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{emoji} {label}</Typography>
            <Typography sx={{ fontSize: 24, fontWeight: 700, lineHeight: 1.3, my: 0.25 }}>{value ?? '—'}</Typography>
            <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{detail ?? 'Not enough data yet'}</Typography>
        </CardContent>
    );
    return (
        <Card variant="outlined" sx={{ height: '100%', boxShadow: 'none' }}>
            {onClick ? <CardActionArea onClick={onClick} sx={{ height: '100%' }}>{body}</CardActionArea> : body}
        </Card>
    );
};

const LoadingState = () => (
    <Grid2 container spacing={2}>
        {[0, 1, 2, 3].map((i) => <Grid2 key={i} size={{ xs: 6, md: 3 }}><Skeleton variant="rounded" height={104} /></Grid2>)}
        {[0, 1, 2, 3].map((i) => <Grid2 key={`c${i}`} size={{ xs: 12, md: 6 }}><Skeleton variant="rounded" height={300} /></Grid2>)}
    </Grid2>
);

const AnalyticsDashboard = () => {
    const navigate = useNavigate();
    // Cached and invalidated by the add/delete activity mutations, so the
    // dashboard is already fresh when the user switches to this page.
    const { data, isLoading, isError, refetch } = useGetAnalyticsDashboardQuery();

    if (isError) {
        return (
            <Alert severity="error" action={<Button color="inherit" onClick={refetch}>Retry</Button>}>
                Could not load your analytics.
            </Alert>
        );
    }
    if (isLoading || !data) return <LoadingState />;

    const { totals, weeklyDuration, monthlyCalories, frequency, intensity, personalBests, streaks, monthComparison } = data;

    if (totals.workouts === 0) {
        return (
            <Card>
                <CardContent sx={{ textAlign: 'center', py: 6 }}>
                    <Typography variant="h6" gutterBottom>No analytics yet</Typography>
                    <Typography color="text.secondary" sx={{ mb: 2 }}>
                        Log a few workouts and your trends, streaks and personal bests will show up here.
                    </Typography>
                    <Button variant="contained" onClick={() => navigate('/activities')}>Log a workout</Button>
                </CardContent>
            </Card>
        );
    }

    const change = (metric) => monthComparison.changes.find((c) => c.metric === metric);
    const cur = monthComparison.current;
    const prev = monthComparison.previous;
    // The running month is compared month-to-date, e.g. "Nov 1–10" vs "Oct 1–10".
    const span = (ym) => (monthComparison.monthToDate
        ? `${monthShort(ym)} 1–${Math.min(monthComparison.throughDay, daysInMonth(ym))}`
        : monthShort(ym));

    const weekly = weeklyDuration.points.map((p) => ({
        key: p.weekStart, label: shortDate(p.weekStart), tooltipTitle: `Week of ${longDate(p.weekStart)}`,
        value: p.minutes, extra: [{ label: 'Workouts', value: p.workouts }, { label: 'Calories', value: n(p.calories) }],
    }));
    const monthly = monthlyCalories.points.map((p) => ({
        key: p.month, label: monthShort(p.month), tooltipTitle: monthLong(p.month),
        value: p.calories, extra: [{ label: 'Workouts', value: p.workouts }, { label: 'Active time', value: hours(p.minutes) }],
    }));
    const intensityLine = intensity.weekly.map((w) => ({
        key: w.weekStart, label: shortDate(w.weekStart), tooltipTitle: `Week of ${longDate(w.weekStart)}`,
        value: w.caloriesPerMinute,
        extra: w.workouts ? [{ label: 'Workouts', value: w.workouts }] : undefined,
    }));

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
                <Typography variant="h5">Your analytics</Typography>
                <Typography color="text.secondary">
                    {n(totals.workouts)} workouts · {hours(totals.minutes)} · {n(totals.calories)} kcal
                    {totals.firstWorkoutDate && ` since ${longDate(totals.firstWorkoutDate)}`}
                </Typography>
            </Box>

            {/* Headline tiles */}
            <Grid2 container spacing={2} sx={{ mb: 2 }}>
                <Grid2 size={{ xs: 6, md: 3 }}>
                    <StatTile label="Current streak"
                              value={`${streaks.currentStreakDays} ${streaks.currentStreakDays === 1 ? 'day' : 'days'}`}
                              sub={streaks.workedOutToday || !streaks.currentStreakDays
                                  ? `Longest: ${streaks.longestStreakDays} days`
                                  : 'Work out today to keep it going'} />
                </Grid2>
                <Grid2 size={{ xs: 6, md: 3 }}>
                    <StatTile label={`Workouts in ${monthShort(cur.month)}`} value={n(cur.workouts)}
                              sub={<><Delta {...change('workouts')} /> vs {span(prev.month)}</>} />
                </Grid2>
                <Grid2 size={{ xs: 6, md: 3 }}>
                    <StatTile label={`Calories in ${monthShort(cur.month)}`} value={n(cur.calories)}
                              sub={<><Delta {...change('calories')} /> vs {span(prev.month)}</>} />
                </Grid2>
                <Grid2 size={{ xs: 6, md: 3 }}>
                    <StatTile label="Average intensity"
                              value={intensity.averageCaloriesPerMinute != null ? `${intensity.averageCaloriesPerMinute} kcal/min` : '—'}
                              sub={intensity.levelLabel ? `${intensity.levelLabel} · last 12 weeks` : 'Last 12 weeks'} />
                </Grid2>
            </Grid2>

            <Grid2 container spacing={2}>
                {/* Weekly duration */}
                <Grid2 size={{ xs: 12, md: 7 }}>
                    <ChartCard title="Weekly workout duration"
                               subtitle={`Minutes per week · average ${hours(Math.round(weeklyDuration.averageMinutesPerWeek))}`}
                               table={{
                                   columns: ['Week of', 'Minutes', 'Workouts'],
                                   rows: weeklyDuration.points.map((p) => [longDate(p.weekStart), n(p.minutes), p.workouts]),
                               }}>
                        <ColumnChart data={weekly} color={VIZ.blue} valueLabel="Minutes" unit=" min" />
                    </ChartCard>
                </Grid2>

                {/* Streaks + calendar */}
                <Grid2 size={{ xs: 12, md: 5 }}>
                    <ChartCard title="Workout streaks" subtitle="Each square is a day, last 12 weeks">
                        <Box sx={{ display: 'flex', gap: 3, mb: 2, flexWrap: 'wrap' }}>
                            {[
                                ['Current', `${streaks.currentStreakDays} days`],
                                ['Longest', `${streaks.longestStreakDays} days`],
                                ['Weekly streak', `${streaks.currentWeeklyStreak} weeks`],
                                ['Active days', n(streaks.totalActiveDays)],
                            ].map(([label, value]) => (
                                <Box key={label}>
                                    <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{label}</Typography>
                                    <Typography sx={{ fontSize: 18, fontWeight: 700 }}>{value}</Typography>
                                </Box>
                            ))}
                        </Box>
                        <ActivityHeatmap days={streaks.recentDays} />
                        {streaks.longestStreakStart && (
                            <Typography sx={{ fontSize: 12, color: 'text.secondary', mt: 1 }}>
                                Longest streak: {longDate(streaks.longestStreakStart)} – {longDate(streaks.longestStreakEnd)}
                            </Typography>
                        )}
                    </ChartCard>
                </Grid2>

                {/* Monthly calories */}
                <Grid2 size={{ xs: 12, md: 7 }}>
                    <ChartCard title="Monthly calories burned"
                               subtitle={`Last 12 months · average ${n(Math.round(monthlyCalories.averageCaloriesPerMonth))} kcal/month`}
                               table={{
                                   columns: ['Month', 'Calories', 'Workouts'],
                                   rows: monthlyCalories.points.map((p) => [monthLong(p.month), n(p.calories), p.workouts]),
                               }}>
                        <ColumnChart data={monthly} color={VIZ.orange} valueLabel="Calories" unit=" kcal" />
                    </ChartCard>
                </Grid2>

                {/* Month comparison */}
                <Grid2 size={{ xs: 12, md: 5 }}>
                    <ChartCard title="This month vs last month"
                               subtitle={monthComparison.monthToDate
                                   ? `Month to date: ${span(cur.month)} vs ${span(prev.month)}`
                                   : `${monthLong(cur.month)} compared with ${monthLong(prev.month)}`}>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell sx={{ fontWeight: 600, pl: 0 }}>Metric</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600 }}>{span(prev.month)}</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600 }}>{span(cur.month)}</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600, pr: 0 }}>Change</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {monthComparison.changes.map((c) => (
                                    <TableRow key={c.metric} sx={{ '&:last-child td': { borderBottom: 0 } }}>
                                        <TableCell sx={{ pl: 0 }}>{c.label}</TableCell>
                                        <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>{n(c.previous)}</TableCell>
                                        <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>{n(c.current)}</TableCell>
                                        <TableCell align="right" sx={{ pr: 0 }}><Delta {...c} /></TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </ChartCard>
                </Grid2>

                {/* Frequency by type */}
                <Grid2 size={{ xs: 12, md: 6 }}>
                    <ChartCard title="Exercise frequency"
                               subtitle={`Last ${frequency.days} days · ${frequency.workoutsPerWeek} workouts per week`}
                               table={{
                                   columns: ['Type', 'Workouts', 'Share', 'Minutes'],
                                   rows: frequency.byType.map((t) => [activityStyle(t.type).label, t.workouts,
                                       `${Math.round(t.share * 100)}%`, n(t.minutes)]),
                               }}>
                        {frequency.byType.length ? (
                            <BarList valueLabel="Workouts" data={frequency.byType.map((t) => ({
                                key: t.type, label: `${activityStyle(t.type).emoji} ${activityStyle(t.type).label}`,
                                value: t.workouts, valueText: `${t.workouts} · ${Math.round(t.share * 100)}%`,
                                extra: [{ label: 'Minutes', value: n(t.minutes) }, { label: 'Calories', value: n(t.calories) }],
                            }))} />
                        ) : <Typography variant="body2" color="text.secondary">No workouts in this period.</Typography>}
                    </ChartCard>
                </Grid2>

                {/* Frequency by weekday */}
                <Grid2 size={{ xs: 12, md: 6 }}>
                    <ChartCard title="Workouts by day of week" subtitle={`Last ${frequency.days} days`}
                               table={{ columns: ['Day', 'Workouts'], rows: frequency.byWeekday.map((d) => [d.label, d.workouts]) }}>
                        <ColumnChart color={VIZ.blue} valueLabel="Workouts" height={200} integer
                                     data={frequency.byWeekday.map((d) => ({ key: d.label, label: d.label, value: d.workouts }))} />
                    </ChartCard>
                </Grid2>

                {/* Intensity */}
                <Grid2 size={{ xs: 12, md: 7 }}>
                    <ChartCard title="Average workout intensity"
                               subtitle="Calories burned per minute, by week"
                               table={{
                                   columns: ['Week of', 'kcal/min', 'Workouts'],
                                   rows: intensity.weekly.map((w) => [longDate(w.weekStart), w.caloriesPerMinute ?? '—', w.workouts]),
                               }}>
                        <LineChart data={intensityLine} valueLabel="kcal/min"
                                   bands={[{ value: 5, label: 'Moderate' }, { value: 8, label: 'High' }, { value: 11, label: 'Very high' }]} />
                    </ChartCard>
                </Grid2>
                <Grid2 size={{ xs: 12, md: 5 }}>
                    <ChartCard title="Workouts by intensity" subtitle="Last 12 weeks">
                        <SegmentedBar parts={intensity.distribution.map((l) => ({
                            key: l.level, label: l.label, value: l.workouts,
                            color: LEVEL_COLORS[l.level].color, textColor: LEVEL_COLORS[l.level].text,
                        }))} />
                        <Typography sx={{ fontSize: 12, color: 'text.secondary', mt: 2 }}>
                            Low &lt; 5 · Moderate 5–8 · High 8–11 · Very high 11+ kcal/min
                        </Typography>
                    </ChartCard>
                </Grid2>

                {/* Personal bests */}
                <Grid2 size={12}>
                    <ChartCard title="Personal bests" subtitle="All time">
                        <Grid2 container spacing={1.5} sx={{ mb: 2 }}>
                            {[
                                ['⏱️', 'Longest workout', personalBests.longestWorkout, (b) => `${b.value} min`],
                                ['🔥', 'Most calories', personalBests.mostCalories, (b) => `${n(b.value)} kcal`],
                                ['⚡', 'Highest intensity', personalBests.highestIntensity, (b) => `${b.value} kcal/min`],
                            ].map(([emoji, label, best, format]) => (
                                <Grid2 key={label} size={{ xs: 12, sm: 6, md: 3 }}>
                                    <BestTile emoji={emoji} label={label}
                                              value={best && format(best)}
                                              detail={best && `${activityStyle(best.type).label} · ${longDate(best.date)}`}
                                              onClick={best ? () => navigate(`/activities/${best.activityId}`) : undefined} />
                                </Grid2>
                            ))}
                            <Grid2 size={{ xs: 12, sm: 6, md: 3 }}>
                                <BestTile emoji="📅" label="Best week"
                                          value={personalBests.bestWeek && hours(personalBests.bestWeek.value)}
                                          detail={personalBests.bestWeek && `Week of ${longDate(personalBests.bestWeek.period)}`} />
                            </Grid2>
                        </Grid2>
                        <DataTable
                            columns={['Activity', 'Workouts', 'Longest', 'Most calories']}
                            rows={personalBests.byType.map((t) => [
                                `${activityStyle(t.type).emoji} ${activityStyle(t.type).label}`,
                                t.workouts, `${t.longestMinutes} min`, `${n(t.mostCalories)} kcal`,
                            ])} />
                        {personalBests.bestMonth && (
                            <Typography sx={{ fontSize: 12, color: 'text.secondary', mt: 1.5 }}>
                                Best month for calories: {monthLong(personalBests.bestMonth.period)} ({n(personalBests.bestMonth.value)} kcal)
                            </Typography>
                        )}
                    </ChartCard>
                </Grid2>
            </Grid2>
        </Box>
    );
};

export default AnalyticsDashboard;
