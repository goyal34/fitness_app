import { Box, Typography } from '@mui/material';
import { useEffect, useRef, useState } from 'react';
import { useWidth, VIZ } from './vizTokens';

const TICK_FONT = { fontSize: 11, fill: VIZ.muted, fontVariantNumeric: 'tabular-nums' };

/** A round upper bound and evenly spaced ticks for a value axis. */
const niceScale = (max, count = 4, integer = false) => {
    if (!max || max <= 0) return { max: 1, ticks: [0, 1] };
    const rough = max / count;
    const power = 10 ** Math.floor(Math.log10(rough));
    // Counts get whole-number steps only (no 2.5 workouts).
    const multipliers = integer ? [1, 2, 5, 10] : [1, 2, 2.5, 5, 10];
    const step = Math.max(integer ? 1 : 0, multipliers.map((m) => m * power).find((s) => s >= rough));
    const top = Math.ceil(max / step) * step;
    const ticks = [];
    for (let v = 0; v <= top + step / 2; v += step) ticks.push(Math.round(v * 100) / 100);
    return { max: top, ticks };
};

const compact = (v) => (v >= 10000 ? `${Math.round(v / 1000)}k` : v >= 1000 ? `${(v / 1000).toFixed(1)}k` : `${v}`);

/** Column with 4px rounded data-end, square at the baseline. */
const columnPath = (x, y, w, h) => {
    const r = Math.min(4, w / 2, h);
    return `M${x},${y + h} V${y + r} Q${x},${y} ${x + r},${y} H${x + w - r} Q${x + w},${y} ${x + w},${y + r} V${y + h} Z`;
};

/** Floating tooltip, positioned in the chart's own box. */
export const Tip = ({ x, y, children, width }) => {
    if (x == null) return null;
    const left = Math.max(4, Math.min(x, (width ?? 9999) - 4));
    return (
        <Box sx={{
            position: 'absolute', left, top: y, transform: 'translate(-50%, calc(-100% - 10px))',
            bgcolor: '#ffffff', border: '1px solid rgba(11,11,11,0.10)', borderRadius: 1.5,
            boxShadow: '0 4px 14px rgba(16,24,40,0.12)', px: 1.25, py: 0.75,
            pointerEvents: 'none', whiteSpace: 'nowrap', zIndex: 2,
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

/**
 * Vertical bar chart, one series.
 * data: [{ key, label, tooltipTitle, value }]
 */
export const ColumnChart = ({ data, color = VIZ.blue, height = 220, unit = '', valueLabel = 'Value', integer = false }) => {
    const [ref, width] = useWidth();
    const [hover, setHover] = useState(null);
    const m = { top: 12, right: 8, bottom: 26, left: 40 };
    const innerW = Math.max(0, width - m.left - m.right);
    const innerH = height - m.top - m.bottom;
    const { max, ticks } = niceScale(Math.max(...data.map((d) => d.value), 0), 4, integer);
    const band = data.length ? innerW / data.length : 0;
    const barW = Math.min(24, band * 0.62);
    const labelEvery = Math.max(1, Math.ceil(44 / Math.max(band, 1)));
    const y = (v) => innerH - (v / max) * innerH;

    const h = hover != null ? data[hover] : null;
    return (
        <Box ref={ref} sx={{ position: 'relative', width: '100%' }} onMouseLeave={() => setHover(null)}>
            {width > 0 && (
                <svg width={width} height={height} role="img"
                     aria-label={`${valueLabel} bar chart with ${data.length} bars`}>
                    <g transform={`translate(${m.left},${m.top})`}>
                        {ticks.map((t) => (
                            <g key={t}>
                                <line x1={0} x2={innerW} y1={y(t)} y2={y(t)}
                                      stroke={t === 0 ? VIZ.axis : VIZ.grid} strokeWidth={1} />
                                <text x={-8} y={y(t)} dy="0.32em" textAnchor="end" style={TICK_FONT}>{compact(t)}</text>
                            </g>
                        ))}
                        {data.map((d, i) => {
                            const x = i * band + (band - barW) / 2;
                            const barH = innerH - y(d.value);
                            return (
                                <g key={d.key}>
                                    {hover === i && <rect x={i * band} y={0} width={band} height={innerH} fill="rgba(11,11,11,0.04)" />}
                                    {d.value > 0 && <path d={columnPath(x, y(d.value), barW, barH)} fill={color} />}
                                    {i % labelEvery === (data.length - 1) % labelEvery && (
                                        <text x={i * band + band / 2} y={innerH + 17} textAnchor="middle" style={TICK_FONT}>
                                            {d.label}
                                        </text>
                                    )}
                                    {/* Hit target: the whole band, taller than the bar. */}
                                    <rect x={i * band} y={0} width={band} height={innerH} fill="transparent"
                                          onMouseEnter={() => setHover(i)} />
                                </g>
                            );
                        })}
                    </g>
                </svg>
            )}
            {h && (
                <Tip x={m.left + hover * band + band / 2} y={m.top + y(h.value)} width={width}>
                    <TipTitle>{h.tooltipTitle ?? h.label}</TipTitle>
                    <TipLine label={valueLabel} value={`${h.value.toLocaleString()}${unit}`} color={color} />
                    {h.extra?.map((e) => <TipLine key={e.label} label={e.label} value={e.value} />)}
                </Tip>
            )}
        </Box>
    );
};

/**
 * Line chart with markers; null values leave a gap.
 * data: [{ key, label, tooltipTitle, value|null }], bands: [{ value, label }] reference lines
 */
export const LineChart = ({ data, color = VIZ.blue, height = 220, unit = '', valueLabel = 'Value', bands = [] }) => {
    const [ref, width] = useWidth();
    const [hover, setHover] = useState(null);
    const m = { top: 12, right: 64, bottom: 26, left: 40 };
    const innerW = Math.max(0, width - m.left - m.right);
    const innerH = height - m.top - m.bottom;
    const values = data.map((d) => d.value).filter((v) => v != null);
    const { max, ticks } = niceScale(Math.max(...values, ...bands.map((b) => b.value), 0));
    const step = data.length > 1 ? innerW / (data.length - 1) : 0;
    const x = (i) => (data.length > 1 ? i * step : innerW / 2);
    const y = (v) => innerH - (v / max) * innerH;
    const labelEvery = Math.max(1, Math.ceil(44 / Math.max(step, 1)));

    // Split into runs of consecutive non-null points.
    const runs = [];
    let run = [];
    data.forEach((d, i) => {
        if (d.value == null) { if (run.length) runs.push(run); run = []; } else run.push(i);
    });
    if (run.length) runs.push(run);

    const h = hover != null ? data[hover] : null;
    return (
        <Box ref={ref} sx={{ position: 'relative', width: '100%' }} onMouseLeave={() => setHover(null)}>
            {width > 0 && (
                <svg width={width} height={height} role="img" aria-label={`${valueLabel} line chart`}>
                    <g transform={`translate(${m.left},${m.top})`}>
                        {ticks.map((t) => (
                            <g key={t}>
                                <line x1={0} x2={innerW} y1={y(t)} y2={y(t)} stroke={t === 0 ? VIZ.axis : VIZ.grid} />
                                <text x={-8} y={y(t)} dy="0.32em" textAnchor="end" style={TICK_FONT}>{t}</text>
                            </g>
                        ))}
                        {bands.filter((b) => b.value <= max).map((b) => (
                            <g key={b.label}>
                                <line x1={0} x2={innerW} y1={y(b.value)} y2={y(b.value)}
                                      stroke={VIZ.axis} strokeDasharray="3 4" />
                                <text x={innerW + 6} y={y(b.value)} dy="0.32em" style={{ ...TICK_FONT, fontSize: 10 }}>{b.label}</text>
                            </g>
                        ))}
                        {hover != null && <line x1={x(hover)} x2={x(hover)} y1={0} y2={innerH} stroke={VIZ.axis} />}
                        {runs.map((r) => (
                            <polyline key={r[0]} fill="none" stroke={color} strokeWidth={2}
                                      strokeLinejoin="round" strokeLinecap="round"
                                      points={r.map((i) => `${x(i)},${y(data[i].value)}`).join(' ')} />
                        ))}
                        {data.map((d, i) => d.value != null && (
                            <circle key={d.key} cx={x(i)} cy={y(d.value)} r={hover === i ? 5 : 4}
                                    fill={color} stroke="#ffffff" strokeWidth={2} />
                        ))}
                        {data.map((d, i) => i % labelEvery === (data.length - 1) % labelEvery && (
                            <text key={`l${d.key}`} x={x(i)} y={innerH + 17} textAnchor="middle" style={TICK_FONT}>{d.label}</text>
                        ))}
                        {data.map((d, i) => (
                            <rect key={`h${d.key}`} x={x(i) - step / 2} y={0} width={Math.max(step, 24)} height={innerH}
                                  fill="transparent" onMouseEnter={() => setHover(i)} />
                        ))}
                    </g>
                </svg>
            )}
            {h && (
                <Tip x={m.left + x(hover)} y={m.top + (h.value != null ? y(h.value) : innerH / 2)} width={width}>
                    <TipTitle>{h.tooltipTitle ?? h.label}</TipTitle>
                    <TipLine label={valueLabel} value={h.value != null ? `${h.value}${unit}` : 'No workouts'} color={color} />
                    {h.extra?.map((e) => <TipLine key={e.label} label={e.label} value={e.value} />)}
                </Tip>
            )}
        </Box>
    );
};

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
