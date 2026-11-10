import {
    Alert, Box, Card, CardContent, Chip, CircularProgress, Divider, Grid2, Stack, Typography
} from '@mui/material';

// Colours for the enum values in Gemini's structured report.
const INTENSITY = {
    LOW: { label: 'Low intensity', color: '#2e7d32' },
    MODERATE: { label: 'Moderate intensity', color: '#0277bd' },
    HIGH: { label: 'High intensity', color: '#ef6c00' },
    VERY_HIGH: { label: 'Very high intensity', color: '#c62828' },
};
const PRIORITY = {
    HIGH: { label: 'High priority', color: '#c62828' },
    MEDIUM: { label: 'Medium priority', color: '#ef6c00' },
    LOW: { label: 'Low priority', color: '#2e7d32' },
};
const PRIORITY_ORDER = ['HIGH', 'MEDIUM', 'LOW'];

const scoreColor = (score) => (score >= 80 ? '#2e7d32' : score >= 60 ? '#0277bd' : score >= 40 ? '#ef6c00' : '#c62828');
const scoreLabel = (score) => (score >= 80 ? 'Excellent' : score >= 60 ? 'Good' : score >= 40 ? 'Fair' : 'Needs work');

const Tag = ({ meta, fallback }) => {
    const m = meta ?? { label: fallback, color: '#616161' };
    return (
        <Chip size="small" label={m.label}
              sx={{ bgcolor: `${m.color}14`, color: m.color, fontWeight: 600, height: 22 }} />
    );
};

const ScoreRing = ({ score }) => {
    const color = scoreColor(score);
    return (
        <Box sx={{ position: 'relative', width: 104, height: 104, flexShrink: 0 }}>
            <CircularProgress variant="determinate" value={100} size={104} thickness={4}
                              sx={{ color: 'rgba(0,0,0,0.06)', position: 'absolute' }} />
            <CircularProgress variant="determinate" value={score} size={104} thickness={4}
                              sx={{ color, position: 'absolute', '& circle': { strokeLinecap: 'round' } }} />
            <Box sx={{
                position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
                alignItems: 'center', justifyContent: 'center',
            }}>
                <Typography sx={{ fontSize: 30, fontWeight: 700, lineHeight: 1, color }}>{score}</Typography>
                <Typography variant="caption" color="text.secondary">{scoreLabel(score)}</Typography>
            </Box>
        </Box>
    );
};

const SectionTitle = ({ emoji, children }) => (
    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5, display: 'flex', gap: 1, alignItems: 'center' }}>
        <span>{emoji}</span>{children}
    </Typography>
);

const TipList = ({ items, marker, color }) => (
    <Stack spacing={1}>
        {items.map((item, i) => (
            <Box key={i} sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                <Box sx={{
                    width: 22, height: 22, borderRadius: '50%', flexShrink: 0, mt: '1px',
                    bgcolor: `${color}14`, color, fontSize: 12, fontWeight: 700,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>{marker}</Box>
                <Typography variant="body2" sx={{ lineHeight: 1.6 }}>{item}</Typography>
            </Box>
        ))}
    </Stack>
);

/** The structured report from Gemini. */
const StructuredReport = ({ report, model, createdAt }) => (
    <>
        {/* Headline */}
        <Box sx={{
            display: 'flex', gap: 3, alignItems: 'center', flexWrap: 'wrap',
            p: 2.5, mb: 3, borderRadius: 3,
            background: 'linear-gradient(135deg, rgba(91,33,182,0.06), rgba(8,145,178,0.06))',
        }}>
            {report.score != null && <ScoreRing score={report.score} />}
            <Box sx={{ flex: 1, minWidth: 220 }}>
                <Stack direction="row" spacing={1} sx={{ mb: 1, flexWrap: 'wrap' }}>
                    {report.intensity && <Tag meta={INTENSITY[report.intensity]} fallback={report.intensity} />}
                </Stack>
                <Typography sx={{ fontSize: 17, lineHeight: 1.6 }}>{report.summary}</Typography>
            </Box>
        </Box>

        {/* Insights */}
        {report.analysis.length > 0 && (
            <Box sx={{ mb: 3 }}>
                <SectionTitle emoji="📊">Analysis</SectionTitle>
                <Grid2 container spacing={1.5}>
                    {report.analysis.map((a, i) => (
                        <Grid2 key={i} size={{ xs: 12, sm: 6 }}>
                            <Box sx={{ p: 2, height: '100%', borderRadius: 2, bgcolor: 'background.default' }}>
                                <Typography variant="body2" sx={{ fontWeight: 700, mb: 0.5 }}>{a.title}</Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                                    {a.detail}
                                </Typography>
                            </Box>
                        </Grid2>
                    ))}
                </Grid2>
            </Box>
        )}

        {/* Strengths */}
        {report.strengths.length > 0 && (
            <Box sx={{ mb: 3 }}>
                <SectionTitle emoji="💪">What went well</SectionTitle>
                <TipList items={report.strengths} marker="✓" color="#2e7d32" />
            </Box>
        )}

        {/* Improvements */}
        {report.improvements.length > 0 && (
            <Box sx={{ mb: 3 }}>
                <SectionTitle emoji="🎯">How to improve</SectionTitle>
                <Stack spacing={1.5}>
                    {[...report.improvements]
                        .sort((a, b) => PRIORITY_ORDER.indexOf(a.priority) - PRIORITY_ORDER.indexOf(b.priority))
                        .map((imp, i) => {
                        const color = PRIORITY[imp.priority]?.color ?? '#616161';
                        return (
                            <Box key={i} sx={{
                                p: 2, borderRadius: 2, border: '1px solid rgba(0,0,0,0.08)',
                                borderLeft: `4px solid ${color}`,
                            }}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1, mb: 0.5, flexWrap: 'wrap' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 700 }}>{imp.area}</Typography>
                                    {imp.priority && <Tag meta={PRIORITY[imp.priority]} fallback={imp.priority} />}
                                </Box>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                                    {imp.recommendation}
                                </Typography>
                            </Box>
                        );
                    })}
                </Stack>
            </Box>
        )}

        {/* Next workouts */}
        {report.nextWorkouts.length > 0 && (
            <Box sx={{ mb: 3 }}>
                <SectionTitle emoji="📅">Suggested next workouts</SectionTitle>
                <Grid2 container spacing={1.5}>
                    {report.nextWorkouts.map((w, i) => (
                        <Grid2 key={i} size={{ xs: 12, sm: 6, md: 4 }}>
                            <Card variant="outlined" sx={{ height: '100%', boxShadow: 'none' }}>
                                <CardContent>
                                    <Typography sx={{ fontWeight: 700, mb: 1 }}>{w.name}</Typography>
                                    <Stack direction="row" spacing={1} sx={{ mb: 1.5, flexWrap: 'wrap', rowGap: 1 }}>
                                        {w.durationMinutes != null && (
                                            <Chip size="small" label={`⏱ ${w.durationMinutes} min`} sx={{ height: 22 }} />
                                        )}
                                        {w.intensity && <Tag meta={INTENSITY[w.intensity]} fallback={w.intensity} />}
                                    </Stack>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                                        {w.description}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid2>
                    ))}
                </Grid2>
            </Box>
        )}

        {/* Safety and recovery */}
        <Grid2 container spacing={3} sx={{ mb: 2 }}>
            {report.safety.length > 0 && (
                <Grid2 size={{ xs: 12, md: 6 }}>
                    <SectionTitle emoji="🛡️">Safety</SectionTitle>
                    <TipList items={report.safety} marker="!" color="#ef6c00" />
                </Grid2>
            )}
            {report.recovery.length > 0 && (
                <Grid2 size={{ xs: 12, md: 6 }}>
                    <SectionTitle emoji="😴">Recovery</SectionTitle>
                    <TipList items={report.recovery} marker="+" color="#0277bd" />
                </Grid2>
            )}
        </Grid2>

        <Divider sx={{ my: 2 }} />
        <Typography variant="caption" color="text.secondary">
            Generated by {model || 'Gemini'}
            {createdAt && ` · ${new Date(createdAt).toLocaleString()}`}
            {' · '}AI advice is general guidance, not medical advice.
        </Typography>
    </>
);

/** Reports saved before the structured format existed. */
const LegacyReport = ({ rec }) => {
    const section = (title, items) => (
        <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1 }}>{title}</Typography>
            {items?.length
                ? items.map((item, i) => <Typography key={i} variant="body2" paragraph>• {item}</Typography>)
                : <Typography variant="body2" color="text.secondary">Nothing reported.</Typography>}
        </Box>
    );
    return (
        <>
            <Typography paragraph sx={{ whiteSpace: 'pre-line' }}>{rec.recommendation}</Typography>
            {section('Improvements', rec.improvements)}
            {section('Suggestions', rec.suggestions)}
            {section('Safety Guidelines', rec.safety)}
        </>
    );
};

/** Card showing the AI report for an activity, or its loading / failed state. */
const AiReport = ({ recommendation, waiting }) => (
    <Card>
        <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
            <Typography variant="h5" sx={{ mb: 2 }}>AI Report</Typography>

            {recommendation?.status === 'FAILED' ? (
                <Alert severity="warning">
                    The AI report could not be generated. {recommendation.errorMessage}
                </Alert>
            ) : recommendation?.report ? (
                <StructuredReport report={recommendation.report}
                                  model={recommendation.model}
                                  createdAt={recommendation.createdAt} />
            ) : recommendation ? (
                <LegacyReport rec={recommendation} />
            ) : waiting ? (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 2 }}>
                    <CircularProgress size={20} />
                    <Typography color="text.secondary">
                        Gemini is analysing your workout… this usually takes a few seconds.
                    </Typography>
                </Box>
            ) : (
                <Typography color="text.secondary">
                    No report has been generated for this activity yet. Check that the AI service is running.
                </Typography>
            )}
        </CardContent>
    </Card>
);

export default AiReport;
