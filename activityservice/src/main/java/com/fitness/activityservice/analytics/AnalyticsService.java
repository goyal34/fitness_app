package com.fitness.activityservice.analytics;

import com.fitness.activityservice.analytics.AnalyticsDtos.*;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Workout analytics, computed by MongoDB aggregation pipelines.
 *
 * Dates: a workout's date is its startTime (or createdAt for old records).
 * Both are stored as LocalDateTime, which Spring converts to UTC using the
 * JVM time zone, so grouping with that same zone gives back the user's
 * calendar days and weeks (weeks start on Monday).
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    static final String COLLECTION = "activities";

    private static final Document AT = new Document("$ifNull", List.of("$startTime", "$createdAt"));
    private static final Document COUNT = new Document("$sum", 1);

    private final MongoTemplate mongoTemplate;

    // ---------------------------------------------------------------- weekly duration

    public WeeklyDuration weeklyDuration(String userId, int weeks) {
        LocalDate first = weekStart(today()).minusWeeks(weeks - 1L);
        List<Document> rows = run(pipeline(matchUser(userId, first, null),
                group(trunc("week"))
                        .append("minutes", sumOf("duration"))
                        .append("workouts", COUNT)
                        .append("calories", sumOf("caloriesBurned"))));

        Map<LocalDate, Document> byWeek = new HashMap<>();
        rows.forEach(r -> byWeek.put(toLocalDate(r.get("_id")), r));

        List<WeekPoint> points = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < weeks; i++) {
            LocalDate week = first.plusWeeks(i);
            Document r = byWeek.get(week);
            points.add(new WeekPoint(week, num(r, "minutes"), num(r, "workouts"), num(r, "calories")));
            total += num(r, "minutes");
        }
        return new WeeklyDuration(weeks, total, round1((double) total / weeks), points);
    }

    // ---------------------------------------------------------------- monthly calories

    public MonthlyCalories monthlyCalories(String userId, int months) {
        YearMonth first = YearMonth.now(zone()).minusMonths(months - 1L);
        List<Document> rows = run(pipeline(matchUser(userId, first.atDay(1), null),
                group(trunc("month"))
                        .append("calories", sumOf("caloriesBurned"))
                        .append("workouts", COUNT)
                        .append("minutes", sumOf("duration"))));

        Map<YearMonth, Document> byMonth = new HashMap<>();
        rows.forEach(r -> byMonth.put(YearMonth.from(toLocalDate(r.get("_id"))), r));

        List<MonthPoint> points = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < months; i++) {
            YearMonth month = first.plusMonths(i);
            Document r = byMonth.get(month);
            points.add(new MonthPoint(month.toString(), num(r, "calories"), num(r, "workouts"), num(r, "minutes")));
            total += num(r, "calories");
        }
        return new MonthlyCalories(months, total, round1((double) total / months), points);
    }

    // ---------------------------------------------------------------- frequency

    public Frequency frequency(String userId, int days) {
        LocalDate from = today().minusDays(days - 1L);
        Document isoWeekday = new Document("$isoDayOfWeek", new Document("date", "$at").append("timezone", tz()));

        Document result = runFacet(pipeline(matchUser(userId, from, null), new Document("$facet", new Document()
                .append("byType", List.of(
                        group("$type")
                                .append("workouts", COUNT)
                                .append("minutes", sumOf("duration"))
                                .append("calories", sumOf("caloriesBurned"))
                                .wrap(),
                        new Document("$sort", new Document("workouts", -1).append("_id", 1))))
                .append("byWeekday", List.of(group(isoWeekday).append("workouts", COUNT).wrap())))));

        List<Document> types = result.getList("byType", Document.class);
        long total = types.stream().mapToLong(t -> num(t, "workouts")).sum();
        List<TypeCount> byType = types.stream()
                .map(t -> new TypeCount(String.valueOf(t.get("_id")), num(t, "workouts"), num(t, "minutes"),
                        num(t, "calories"), total == 0 ? 0 : round3((double) num(t, "workouts") / total)))
                .toList();

        Map<Integer, Long> weekdayCounts = new HashMap<>();
        result.getList("byWeekday", Document.class)
                .forEach(d -> weekdayCounts.put(((Number) d.get("_id")).intValue(), num(d, "workouts")));
        List<WeekdayCount> byWeekday = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            byWeekday.add(new WeekdayCount(day.getValue(), day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    weekdayCounts.getOrDefault(day.getValue(), 0L)));
        }

        return new Frequency(days, total, round1(total / (days / 7.0)),
                byType.isEmpty() ? null : byType.get(0).type(), byType, byWeekday);
    }

    // ---------------------------------------------------------------- intensity

    public Intensity intensity(String userId, int weeks) {
        LocalDate first = weekStart(today()).minusWeeks(weeks - 1L);

        Document result = runFacet(pipeline(matchUser(userId, first, null),
                new Document("$match", new Document("duration", new Document("$gt", 0))
                        .append("caloriesBurned", new Document("$gt", 0))),
                new Document("$addFields", new Document("cpm",
                        new Document("$divide", List.of("$caloriesBurned", "$duration")))),
                new Document("$facet", new Document()
                        .append("weekly", List.of(group(trunc("week"))
                                .append("calories", sumOf("caloriesBurned"))
                                .append("minutes", sumOf("duration"))
                                .append("workouts", COUNT).wrap()))
                        .append("levels", List.of(group(levelSwitch()).append("workouts", COUNT).wrap()))
                        .append("overall", List.of(group(null)
                                .append("calories", sumOf("caloriesBurned"))
                                .append("minutes", sumOf("duration")).wrap())))));

        Map<LocalDate, Document> byWeek = new HashMap<>();
        result.getList("weekly", Document.class).forEach(r -> byWeek.put(toLocalDate(r.get("_id")), r));
        List<WeekIntensity> weekly = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            LocalDate week = first.plusWeeks(i);
            Double cpm = caloriesPerMinute(byWeek.get(week));
            weekly.add(new WeekIntensity(week, cpm, cpm == null ? null : IntensityLevel.of(cpm).name(),
                    num(byWeek.get(week), "workouts")));
        }

        Map<String, Long> levelCounts = new HashMap<>();
        result.getList("levels", Document.class)
                .forEach(d -> levelCounts.put(String.valueOf(d.get("_id")), num(d, "workouts")));
        List<LevelCount> distribution = Arrays.stream(IntensityLevel.values())
                .map(l -> new LevelCount(l.name(), l.label(), levelCounts.getOrDefault(l.name(), 0L)))
                .toList();

        List<Document> overall = result.getList("overall", Document.class);
        Double average = overall.isEmpty() ? null : caloriesPerMinute(overall.get(0));
        IntensityLevel level = average == null ? null : IntensityLevel.of(average);
        return new Intensity(weeks, average, level == null ? null : level.name(),
                level == null ? null : level.label(), weekly, distribution);
    }

    // ---------------------------------------------------------------- personal bests

    public PersonalBests personalBests(String userId) {
        Document positiveDuration = new Document("duration", new Document("$gt", 0));
        Document positiveCalories = new Document("caloriesBurned", new Document("$gt", 0));

        Document result = runFacet(pipeline(matchUser(userId, null, null), new Document("$facet", new Document()
                .append("longest", topBy(positiveDuration, "duration"))
                .append("mostCalories", topBy(positiveCalories, "caloriesBurned"))
                .append("highestIntensity", List.of(
                        new Document("$match", new Document(positiveDuration).append("caloriesBurned", new Document("$gt", 0))),
                        new Document("$addFields", new Document("cpm",
                                new Document("$divide", List.of("$caloriesBurned", "$duration")))),
                        new Document("$sort", new Document("cpm", -1).append("at", 1)),
                        new Document("$limit", 1)))
                .append("bestWeek", List.of(
                        group(trunc("week")).append("minutes", sumOf("duration")).wrap(),
                        new Document("$sort", new Document("minutes", -1).append("_id", 1)),
                        new Document("$limit", 1)))
                .append("bestMonth", List.of(
                        group(trunc("month")).append("calories", sumOf("caloriesBurned")).wrap(),
                        new Document("$sort", new Document("calories", -1).append("_id", 1)),
                        new Document("$limit", 1)))
                .append("byType", List.of(
                        group("$type")
                                .append("workouts", COUNT)
                                .append("longest", new Document("$max", "$duration"))
                                .append("most", new Document("$max", "$caloriesBurned"))
                                .wrap(),
                        new Document("$sort", new Document("workouts", -1).append("_id", 1)))))));

        Document week = first(result, "bestWeek");
        Document month = first(result, "bestMonth");
        return new PersonalBests(
                bestWorkout(first(result, "longest"), num(first(result, "longest"), "duration"), "min"),
                bestWorkout(first(result, "mostCalories"), num(first(result, "mostCalories"), "caloriesBurned"), "kcal"),
                bestWorkout(first(result, "highestIntensity"), round2(dbl(first(result, "highestIntensity"), "cpm")), "kcal/min"),
                week == null || num(week, "minutes") == 0 ? null
                        : new BestPeriod(toLocalDate(week.get("_id")).toString(), num(week, "minutes"), "min"),
                month == null || num(month, "calories") == 0 ? null
                        : new BestPeriod(YearMonth.from(toLocalDate(month.get("_id"))).toString(), num(month, "calories"), "kcal"),
                result.getList("byType", Document.class).stream()
                        .map(t -> new TypeBest(String.valueOf(t.get("_id")), num(t, "workouts"),
                                num(t, "longest"), num(t, "most")))
                        .toList());
    }

    // ---------------------------------------------------------------- streaks

    public Streaks streaks(String userId) {
        List<Document> rows = run(pipeline(matchUser(userId, null, null),
                group(dayString()).append("workouts", COUNT).append("minutes", sumOf("duration")).wrap(),
                new Document("$sort", new Document("_id", 1))));

        NavigableSet<LocalDate> days = new TreeSet<>();
        Map<LocalDate, Document> byDay = new HashMap<>();
        for (Document r : rows) {
            LocalDate day = LocalDate.parse(r.getString("_id"));
            days.add(day);
            byDay.put(day, r);
        }

        LocalDate today = today();
        StreakCalculator.Result s = StreakCalculator.compute(days, today);

        List<DayActivity> recent = new ArrayList<>();
        for (LocalDate d = weekStart(today).minusWeeks(11); !d.isAfter(today); d = d.plusDays(1)) {
            recent.add(new DayActivity(d, num(byDay.get(d), "workouts"), num(byDay.get(d), "minutes")));
        }

        return new Streaks(s.current(), s.longest(), s.longestStart(), s.longestEnd(), s.currentWeekly(),
                days.size(), days.isEmpty() ? null : days.last(), days.contains(today), recent);
    }

    // ---------------------------------------------------------------- month comparison

    /**
     * Compares a month with the one before it. For the current (unfinished)
     * month it is month-to-date: the 1st..today of this month against the
     * same days of last month, so a half-finished month never looks like a drop.
     */
    public MonthComparison monthComparison(String userId, YearMonth month) {
        YearMonth previous = month.minusMonths(1);
        LocalDate today = today();
        boolean monthToDate = month.equals(YearMonth.from(today));
        int throughDay = today.getDayOfMonth();

        LocalDate curEnd = monthToDate ? today.plusDays(1) : month.plusMonths(1).atDay(1);
        LocalDate prevEnd = monthToDate
                ? previous.atDay(Math.min(throughDay, previous.lengthOfMonth())).plusDays(1)
                : month.atDay(1);

        Document result = runFacet(pipeline(matchUser(userId, previous.atDay(1), curEnd),
                new Document("$facet", new Document()
                        .append("current", monthStatsStages(month.atDay(1), curEnd))
                        .append("previous", monthStatsStages(previous.atDay(1), prevEnd)))));

        MonthStats cur = monthStats(month, first(result, "current"));
        MonthStats prev = monthStats(previous, first(result, "previous"));

        List<MetricChange> changes = List.of(
                change("workouts", "Workouts", cur.workouts(), prev.workouts()),
                change("minutes", "Active minutes", cur.minutes(), prev.minutes()),
                change("calories", "Calories burned", cur.calories(), prev.calories()),
                change("activeDays", "Active days", cur.activeDays(), prev.activeDays()),
                change("caloriesPerMinute", "Avg intensity (kcal/min)",
                        Objects.requireNonNullElse(cur.caloriesPerMinute(), 0.0),
                        Objects.requireNonNullElse(prev.caloriesPerMinute(), 0.0)));
        return new MonthComparison(cur, prev, monthToDate, monthToDate ? throughDay : null, changes);
    }

    private List<Document> monthStatsStages(LocalDate from, LocalDate toExclusive) {
        return List.of(
                new Document("$match", new Document("at",
                        new Document("$gte", startOf(from)).append("$lt", startOf(toExclusive)))),
                group(null)
                        .append("workouts", COUNT)
                        .append("minutes", sumOf("duration"))
                        .append("calories", sumOf("caloriesBurned"))
                        .append("days", new Document("$addToSet", dayString()))
                        .wrap(),
                new Document("$addFields", new Document("activeDays", new Document("$size", "$days"))));
    }

    // ---------------------------------------------------------------- totals + dashboard

    public Totals totals(String userId) {
        List<Document> rows = run(pipeline(matchUser(userId, null, null),
                group(null)
                        .append("workouts", COUNT)
                        .append("minutes", sumOf("duration"))
                        .append("calories", sumOf("caloriesBurned"))
                        .append("first", new Document("$min", "$at"))
                        .wrap()));
        Document r = rows.isEmpty() ? null : rows.get(0);
        return new Totals(num(r, "workouts"), num(r, "minutes"), num(r, "calories"),
                r == null || r.get("first") == null ? null : toLocalDate(r.get("first")));
    }

    public Dashboard dashboard(String userId) {
        return new Dashboard(
                totals(userId),
                weeklyDuration(userId, 12),
                monthlyCalories(userId, 12),
                frequency(userId, 90),
                intensity(userId, 12),
                personalBests(userId),
                streaks(userId),
                monthComparison(userId, YearMonth.now(zone())));
    }

    // ---------------------------------------------------------------- pipeline helpers

    /** $match on the user, add "at" (the workout date), then keep from <= at < toExclusive. */
    private List<Document> matchUser(String userId, LocalDate from, LocalDate toExclusive) {
        List<Document> stages = new ArrayList<>();
        stages.add(new Document("$match", new Document("userId", userId)));
        stages.add(new Document("$addFields", new Document("at", AT)));
        Document range = new Document();
        if (from != null) {
            range.append("$gte", startOf(from));
        }
        if (toExclusive != null) {
            range.append("$lt", startOf(toExclusive));
        }
        if (!range.isEmpty()) {
            stages.add(new Document("$match", new Document("at", range)));
        }
        return stages;
    }

    private static List<Document> pipeline(List<Document> base, Object... more) {
        List<Document> stages = new ArrayList<>(base);
        for (Object stage : more) {
            stages.add(stage instanceof Group g ? g.wrap() : (Document) stage);
        }
        return stages;
    }

    private List<Document> topBy(Document filter, String field) {
        return List.of(new Document("$match", filter),
                new Document("$sort", new Document(field, -1).append("at", 1)),
                new Document("$limit", 1));
    }

    private Document trunc(String unit) {
        Document args = new Document("date", "$at").append("unit", unit).append("timezone", tz());
        if ("week".equals(unit)) {
            args.append("startOfWeek", "monday");
        }
        return new Document("$dateTrunc", args);
    }

    private Document dayString() {
        return new Document("$dateToString",
                new Document("format", "%Y-%m-%d").append("date", "$at").append("timezone", tz()));
    }

    /** $switch that labels "cpm" with an IntensityLevel name. */
    private static Document levelSwitch() {
        List<Document> branches = new ArrayList<>();
        IntensityLevel[] levels = IntensityLevel.values();
        for (int i = levels.length - 1; i > 0; i--) {
            branches.add(new Document("case", new Document("$gte", List.of("$cpm", levels[i].minCaloriesPerMinute())))
                    .append("then", levels[i].name()));
        }
        return new Document("$switch", new Document("branches", branches).append("default", IntensityLevel.LOW.name()));
    }

    private static Document sumOf(String field) {
        return new Document("$sum", new Document("$ifNull", List.of("$" + field, 0)));
    }

    private static Group group(Object id) {
        return new Group(id);
    }

    /** Small builder so $group stages read top to bottom. */
    private static final class Group extends Document {
        Group(Object id) {
            super("_id", id);
        }

        @Override
        public Group append(String key, Object value) {
            super.append(key, value);
            return this;
        }

        Document wrap() {
            return new Document("$group", new Document(this));
        }
    }

    private List<Document> run(List<Document> pipeline) {
        return mongoTemplate.getCollection(COLLECTION).aggregate(pipeline).into(new ArrayList<>());
    }

    /** A $facet stage always returns exactly one document. */
    private Document runFacet(List<Document> pipeline) {
        return run(pipeline).get(0);
    }

    // ---------------------------------------------------------------- value helpers

    private BestWorkout bestWorkout(Document d, double value, String unit) {
        if (d == null) {
            return null;
        }
        return new BestWorkout(String.valueOf(d.get("_id")), String.valueOf(d.get("type")), value, unit,
                toLocalDate(d.get("at")));
    }

    private static MonthStats monthStats(YearMonth month, Document r) {
        return new MonthStats(month.toString(), num(r, "workouts"), num(r, "minutes"), num(r, "calories"),
                num(r, "activeDays"), caloriesPerMinute(r));
    }

    private static MetricChange change(String metric, String label, double current, double previous) {
        double diff = round2(current - previous);
        Double percent = previous == 0 ? null : round1((current - previous) / previous * 100);
        return new MetricChange(metric, label, current, previous, diff, percent);
    }

    private static Double caloriesPerMinute(Document r) {
        long minutes = num(r, "minutes");
        return minutes == 0 ? null : round2((double) num(r, "calories") / minutes);
    }

    private static Document first(Document facet, String key) {
        List<Document> list = facet.getList(key, Document.class);
        return list.isEmpty() ? null : list.get(0);
    }

    private static long num(Document d, String key) {
        Object v = d == null ? null : d.get(key);
        return v instanceof Number n ? n.longValue() : 0;
    }

    private static double dbl(Document d, String key) {
        Object v = d == null ? null : d.get(key);
        return v instanceof Number n ? n.doubleValue() : 0;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private ZoneId zone() {
        return ZoneId.systemDefault();
    }

    private String tz() {
        return zone().getId();
    }

    private LocalDate today() {
        return LocalDate.now(zone());
    }

    private static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private Date startOf(LocalDate day) {
        return Date.from(day.atStartOfDay(zone()).toInstant());
    }

    private LocalDate toLocalDate(Object mongoDate) {
        return ((Date) mongoDate).toInstant().atZone(zone()).toLocalDate();
    }
}
