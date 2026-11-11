import { Box, Typography } from '@mui/material';
import { useEffect, useRef, useState } from 'react';
import {
    Bar,
    BarChart,
    CartesianGrid,
    Line,
    LineChart as RechartsLineChart,
    ReferenceLine,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from 'recharts';
import { VIZ } from './vizTokens';

const TICK = { fontSize: 11, fill: VIZ.muted };

const compact = (v) => (v >= 10000 ? `${Math.round(v / 1000)}k` : v >= 1000 ? `${(v / 1000).toFixed(1)}k` : `${v}`);

const tipBoxSx = {
    bgcolor: '#ffffff',
    border: '1px solid rgba(11,11,11,0.10)',
    borderRadius: 1.5,
    boxShadow: '0 4px 14px rgba(16,24,40,0.12)',
    px: 1.25,
    py: 0.75,
    whiteSpace: 'nowrap',
};

/** Floating tooltip, positioned in the chart's own box (used by non-Recharts widgets). */
export const Tip = ({ x, y, children, width }) => {
    if (x == null) return null;
    const left = Math.max(4, Math.min(x, (width ?? 9999) - 4));
    return (
        <Box sx={{
            position: 'absolute', left, top: y, transform: 'translate(-50%, calc(-100% - 10px))',
            ...tipBoxSx, pointerEvents: 'none', zIndex: 2,
        }}>
            {children}
        </Box>
    );
};

export const TipLine = ({ label, value, color }) => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, fontSize: 12, lineHeight: 1.6 }}>
        {color && <Box sx={{ width: 8, height: 8, borderRadius: '2px', bgcolor: color }} />}
        <Box component="span" sx={{ color: VIZ.inkSecondary }}>{label}</Box>
        <Box component="span" sx={{ color: VIZ.ink, fontWeight: 600, ml: 'auto', pl: 1.5, fontVariantNumeric: 'tabular-nums' }}>
            {value}
        </Box>
    </Box>
);

const TipTitle = ({ children }) => (
    <Typography sx={{ fontSize: 12, fontWeight: 700, color: VIZ.ink, mb: 0.25 }}>{children}</Typography>
);

const ChartTooltip = ({ active, payload, valueLabel, unit, color, emptyLabel }) => {
    if (!active || !payload?.[0]) return null;
    const d = payload[0].payload;
    const raw = d.value;
    const display = raw != null
        ? `${typeof raw === 'number' ? raw.toLocaleString() : raw}${unit}`
        : (emptyLabel ?? 'No data');
    return (
        <Box sx={tipBoxSx}>
            <TipTitle>{d.tooltipTitle ?? d.label}</TipTitle>
            <TipLine label={valueLabel} value={display} color={color} />
            {d.extra?.map((e) => <TipLine key={e.label} label={e.label} value={e.value} />)}
        </Box>
    );
};

/**
 * Vertical bar chart, one series.
 * data: [{ key, label, tooltipTitle, value }]
 */
export const ColumnChart = ({ data, color = VIZ.blue, height = 220, unit = '', valueLabel = 'Value', integer = false }) => (
    <Box sx={{ width: '100%', height }} role="img" aria-label={`${valueLabel} bar chart with ${data.length} bars`}>
        <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 12, right: 8, bottom: 4, left: 4 }}>
                <CartesianGrid vertical={false} stroke={VIZ.grid} strokeDasharray="0" />
                <XAxis
                    dataKey="label"
                    tick={TICK}
                    tickLine={false}
                    axisLine={{ stroke: VIZ.axis }}
                    interval="preserveStartEnd"
                    minTickGap={28}
                />
                <YAxis
                    tick={TICK}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={compact}
                    allowDecimals={!integer}
                    width={40}
                />
                <Tooltip
                    cursor={{ fill: 'rgba(11,11,11,0.04)' }}
                    content={<ChartTooltip valueLabel={valueLabel} unit={unit} color={color} />}
                />
                <Bar dataKey="value" fill={color} radius={[4, 4, 0, 0]} maxBarSize={24} isAnimationActive={false} />
            </BarChart>
        </ResponsiveContainer>
    </Box>
);

/**
 * Line chart with markers; null values leave a gap.
 * data: [{ key, label, tooltipTitle, value|null }], bands: [{ value, label }] reference lines
 */
export const LineChart = ({ data, color = VIZ.blue, height = 220, unit = '', valueLabel = 'Value', bands = [] }) => (
    <Box sx={{ width: '100%', height }} role="img" aria-label={`${valueLabel} line chart`}>
        <ResponsiveContainer width="100%" height="100%">
            <RechartsLineChart data={data} margin={{ top: 12, right: 64, bottom: 4, left: 4 }}>
                <CartesianGrid vertical={false} stroke={VIZ.grid} />
                <XAxis
                    dataKey="label"
                    tick={TICK}
                    tickLine={false}
                    axisLine={{ stroke: VIZ.axis }}
                    interval="preserveStartEnd"
                    minTickGap={28}
                />
                <YAxis tick={TICK} tickLine={false} axisLine={false} width={40} />
                {bands.map((b) => (
                    <ReferenceLine
                        key={b.label}
                        y={b.value}
                        stroke={VIZ.axis}
                        strokeDasharray="3 4"
                        label={{ value: b.label, position: 'right', fill: VIZ.muted, fontSize: 10 }}
                    />
                ))}
                <Tooltip
                    content={<ChartTooltip valueLabel={valueLabel} unit={unit} color={color} emptyLabel="No workouts" />}
                />
                <Line
                    type="linear"
                    dataKey="value"
                    stroke={color}
                    strokeWidth={2}
                    connectNulls={false}
                    isAnimationActive={false}
                    dot={{ r: 4, fill: color, stroke: '#ffffff', strokeWidth: 2 }}
                    activeDot={{ r: 5, fill: color, stroke: '#ffffff', strokeWidth: 2 }}
                />
            </RechartsLineChart>
        </ResponsiveContainer>
    </Box>
);

/**
 * Horizontal bars, sorted by the caller. Labels on the left, value at the bar end.
 * data: [{ key, label, value, valueText, extra }]
 */
export const BarList = ({ data, color = VIZ.blue, valueLabel = 'Value' }) => {
    const [hover, setHover] = useState(null);
    const max = Math.max(...data.map((d) => d.value), 1);
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {data.map((d) => (
                <Box key={d.key} sx={{ display: 'grid', gridTemplateColumns: 'minmax(96px, 34%) 1fr', alignItems: 'center', gap: 1.5, position: 'relative' }}
                     onMouseEnter={() => setHover(d.key)} onMouseLeave={() => setHover(null)}>
                    <Typography noWrap sx={{ fontSize: 13, color: VIZ.inkSecondary }}>{d.label}</Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 0 }}>
                        <Box sx={{
                            height: 14, width: `${(d.value / max) * 82}%`, minWidth: d.value > 0 ? 3 : 0,
                            bgcolor: color, borderRadius: '0 4px 4px 0',
                            opacity: hover && hover !== d.key ? 0.55 : 1, transition: 'opacity 120ms',
                        }} />
                        <Typography sx={{ fontSize: 12, color: VIZ.ink, fontWeight: 600, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                            {d.valueText ?? d.value}
                        </Typography>
                    </Box>
                    {hover === d.key && d.extra && (
                        <Tip x={160} y={0}>
                            <TipTitle>{d.label}</TipTitle>
                            <TipLine label={valueLabel} value={d.valueText ?? d.value} color={color} />
                            {d.extra.map((e) => <TipLine key={e.label} label={e.label} value={e.value} />)}
                        </Tip>
                    )}
                </Box>
            ))}
        </Box>
    );
};

/**
 * One whole split into ordered parts (e.g. workouts by intensity level).
 * parts: [{ key, label, value, color, textColor }]
 */
export const SegmentedBar = ({ parts, unit = 'workouts' }) => {
    const [hover, setHover] = useState(null);
    const total = parts.reduce((s, p) => s + p.value, 0);
    if (!total) return <Typography variant="body2" color="text.secondary">No workouts in this period.</Typography>;
    const visible = parts.filter((p) => p.value > 0);
    return (
        <Box>
            <Box sx={{ display: 'flex', gap: '2px', height: 28, position: 'relative' }} onMouseLeave={() => setHover(null)}>
                {visible.map((p) => {
                    const share = p.value / total;
                    return (
                        <Box key={p.key} onMouseEnter={() => setHover(p.key)}
                             aria-label={`${p.label}: ${p.value} ${unit} (${Math.round(share * 100)}%)`}
                             sx={{
                                 flex: `${p.value} 0 0`, minWidth: 6, bgcolor: p.color, borderRadius: '4px',
                                 display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative',
                                 color: p.textColor, fontSize: 11, fontWeight: 700, overflow: 'hidden',
                             }}>
                            {share >= 0.12 && `${Math.round(share * 100)}%`}
                            {hover === p.key && (
                                <Tip x={0} y={0}>
                                    <TipTitle>{p.label}</TipTitle>
                                    <TipLine label="Workouts" value={p.value} color={p.color} />
                                    <TipLine label="Share" value={`${Math.round(share * 100)}%`} />
                                </Tip>
                            )}
                        </Box>
                    );
                })}
            </Box>
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 1.25 }}>
                {parts.map((p) => (
                    <Box key={p.key} sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                        <Box sx={{ width: 10, height: 10, borderRadius: '2px', bgcolor: p.color }} />
                        <Typography sx={{ fontSize: 12, color: VIZ.inkSecondary }}>{p.label}</Typography>
                    </Box>
                ))}
            </Box>
        </Box>
    );
};

/** Activity calendar: weeks as columns, Mon..Sun as rows. days: [{ date, workouts, minutes }] oldest first. */
export const ActivityHeatmap = ({ days }) => {
    const [hover, setHover] = useState(null);
    const scrollRef = useRef(null);
    useEffect(() => {
        // On narrow screens start scrolled to the most recent weeks.
        if (scrollRef.current) scrollRef.current.scrollLeft = scrollRef.current.scrollWidth;
    }, [days]);

    const level = (minutes) => (minutes <= 0 ? 0 : minutes <= 20 ? 1 : minutes <= 45 ? 2 : minutes <= 75 ? 3 : 4);
    const weeks = [];
    days.forEach((d, i) => {
        if (i % 7 === 0) weeks.push([]);
        weeks[weeks.length - 1].push(d);
    });
    const cell = 16;
    const gap = 3;
    const fmt = (iso) => new Date(`${iso}T00:00`).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });

    return (
        <Box>
            <Box ref={scrollRef} sx={{ overflowX: 'auto', pb: 0.5 }}>
                <Box sx={{ display: 'flex', gap: `${gap}px`, position: 'relative', width: 'max-content', pt: 0.5 }}>
                    <Box sx={{ display: 'grid', gridTemplateRows: `repeat(7, ${cell}px)`, gap: `${gap}px`, mr: 0.5 }}>
                        {['Mon', '', 'Wed', '', 'Fri', '', 'Sun'].map((l, i) => (
                            <Typography key={i} sx={{ fontSize: 10, color: VIZ.muted, lineHeight: `${cell}px` }}>{l}</Typography>
                        ))}
                    </Box>
                    {weeks.map((week) => (
                        <Box key={week[0].date} sx={{ display: 'grid', gridTemplateRows: `repeat(7, ${cell}px)`, gap: `${gap}px` }}>
                            {week.map((d) => (
                                <Box key={d.date}
                                     onMouseEnter={() => setHover(d.date)} onMouseLeave={() => setHover(null)}
                                     aria-label={`${fmt(d.date)}: ${d.workouts} workouts, ${d.minutes} minutes`}
                                     sx={{
                                         width: cell, height: cell, borderRadius: '3px', position: 'relative',
                                         bgcolor: VIZ.heat[level(d.minutes)],
                                         outline: hover === d.date ? `2px solid ${VIZ.ink}` : 'none', outlineOffset: 1,
                                     }}>
                                    {hover === d.date && (
                                        <Tip x={cell / 2} y={0}>
                                            <TipTitle>{fmt(d.date)}</TipTitle>
                                            <TipLine label="Workouts" value={d.workouts} />
                                            <TipLine label="Minutes" value={d.minutes} />
                                        </Tip>
                                    )}
                                </Box>
                            ))}
                        </Box>
                    ))}
                </Box>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 1, justifyContent: 'flex-end' }}>
                <Typography sx={{ fontSize: 11, color: VIZ.muted, mr: 0.5 }}>Less</Typography>
                {VIZ.heat.map((c) => <Box key={c} sx={{ width: 10, height: 10, borderRadius: '2px', bgcolor: c }} />)}
                <Typography sx={{ fontSize: 11, color: VIZ.muted, ml: 0.5 }}>More minutes</Typography>
            </Box>
        </Box>
    );
};
