package com.fitness.activityservice.analytics;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.analytics.AnalyticsDtos.*;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Runs the analytics pipelines on a real MongoDB and checks every number
 * against the same figure computed in plain Java from the same workouts.
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/unused",
        "rabbitmq.exchange.name=fitness.exchange",
        "rabbitmq.queue.name=activity.queue",
        "rabbitmq.routing.key=activity.tracking",
        "outbox.publisher.interval-ms=3600000",
})
class AnalyticsIntegrationTest {

    private static final String USER = "user-1";

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired AnalyticsService analytics;
    @Autowired ActivityRepository repository;

    @MockitoBean UserValidationService userValidationService;

    private final LocalDate today = LocalDate.now();
    private List<Activity> mine;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        List<Activity> data = new ArrayList<>(List.of(
                activity(0, ActivityType.RUNNING, 30, 300),       // 10 kcal/min  HIGH
                activity(1, ActivityType.CYCLING, 60, 480),       //  8           HIGH
                activity(2, ActivityType.YOGA, 45, 135),          //  3           LOW
                activity(10, ActivityType.RUNNING, 50, 600),      // 12           VERY_HIGH
                activity(11, ActivityType.RUNNING, 20, 120),      //  6           MODERATE
                activity(12, ActivityType.WALKING, 40, 160),      //  4           LOW
                activity(13, ActivityType.WALKING, 30, 120),      //  4           LOW
                activity(45, ActivityType.SWIMMING, 35, 350),     // 10           HIGH
                activity(400, ActivityType.HIIT, 25, 375)));      // over a year ago: only in all-time stats
        // An old record without startTime: its createdAt (now) is used instead.
        Activity legacy = activity(0, ActivityType.STRETCHING, 10, 30);
        legacy.setStartTime(null);
        data.add(legacy);
        mine = repository.saveAll(data);

        // Someone else's workout must never show up.
        Activity other = activity(0, ActivityType.RUNNING, 999, 9999);
        other.setUserId("someone-else");
        repository.save(other);
    }

    @Test
    void totalsCountOnlyTheUsersOwnWorkouts() {
        Totals totals = analytics.totals(USER);
        assertThat(totals.workouts()).isEqualTo(10);
        assertThat(totals.minutes()).isEqualTo(sum(a -> true, Activity::getDuration));
        assertThat(totals.calories()).isEqualTo(sum(a -> true, Activity::getCaloriesBurned));
        assertThat(totals.firstWorkoutDate()).isEqualTo(today.minusDays(400));
    }

    @Test
    void weeklyDurationGroupsByMondayWeeksAndFillsEmptyWeeks() {
        WeeklyDuration result = analytics.weeklyDuration(USER, 8);

        assertThat(result.points()).hasSize(8);
        assertThat(result.points().get(7).weekStart()).isEqualTo(weekStart(today));
        for (WeekPoint p : result.points()) {
            assertThat(p.weekStart().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(p.minutes()).as("minutes in week %s", p.weekStart())
                    .isEqualTo(sum(a -> weekStart(date(a)).equals(p.weekStart()), Activity::getDuration));
            assertThat(p.workouts()).isEqualTo(count(a -> weekStart(date(a)).equals(p.weekStart())));
        }
        assertThat(result.totalMinutes()).isEqualTo(result.points().stream().mapToLong(WeekPoint::minutes).sum());
    }

    @Test
    void monthlyCaloriesGroupsByCalendarMonth() {
        MonthlyCalories result = analytics.monthlyCalories(USER, 4);

        assertThat(result.points()).hasSize(4);
        assertThat(result.points().get(3).month()).isEqualTo(YearMonth.from(today).toString());
        for (MonthPoint p : result.points()) {
            YearMonth month = YearMonth.parse(p.month());
            assertThat(p.calories()).as("calories in %s", month)
                    .isEqualTo(sum(a -> YearMonth.from(date(a)).equals(month), Activity::getCaloriesBurned));
        }
    }

    @Test
    void frequencyCountsByTypeAndWeekday() {
        Frequency result = analytics.frequency(USER, 90);
        Predicate<Activity> inRange = a -> !date(a).isBefore(today.minusDays(89));

        assertThat(result.totalWorkouts()).isEqualTo(count(inRange));
        assertThat(result.mostFrequentType()).isEqualTo("RUNNING");
        assertThat(result.byType().get(0).workouts()).isEqualTo(3);
        assertThat(result.byType()).noneMatch(t -> t.type().equals("HIIT")); // older than 90 days
        assertThat(result.byWeekday()).hasSize(7);
        for (WeekdayCount day : result.byWeekday()) {
            assertThat(day.workouts()).as(day.label())
                    .isEqualTo(count(inRange.and(a -> date(a).getDayOfWeek().getValue() == day.day())));
        }
    }

    @Test
    void intensityIsCaloriesPerMinuteWeightedByDuration() {
        Intensity result = analytics.intensity(USER, 4);
        Predicate<Activity> inRange = a -> !date(a).isBefore(weekStart(today).minusWeeks(3));

        double expected = (double) sum(inRange, Activity::getCaloriesBurned) / sum(inRange, Activity::getDuration);
        assertThat(result.averageCaloriesPerMinute()).isCloseTo(expected, within(0.01));
        assertThat(result.level()).isEqualTo(IntensityLevel.of(expected).name());
        for (LevelCount level : result.distribution()) {
            assertThat(level.workouts()).as(level.level()).isEqualTo(count(inRange.and(a ->
                    IntensityLevel.of((double) a.getCaloriesBurned() / a.getDuration()).name().equals(level.level()))));
        }
    }

    @Test
    void personalBestsFindTheTopWorkouts() {
        PersonalBests bests = analytics.personalBests(USER);

        assertThat(bests.longestWorkout().value()).isEqualTo(60);
        assertThat(bests.longestWorkout().type()).isEqualTo("CYCLING");
        assertThat(bests.mostCalories().value()).isEqualTo(600);
        assertThat(bests.highestIntensity().value()).isEqualTo(15.0); // HIIT: 375 / 25
        assertThat(bests.highestIntensity().date()).isEqualTo(today.minusDays(400));
        assertThat(bests.byType()).anySatisfy(t -> {
            assertThat(t.type()).isEqualTo("RUNNING");
            assertThat(t.workouts()).isEqualTo(3);
            assertThat(t.longestMinutes()).isEqualTo(50);
            assertThat(t.mostCalories()).isEqualTo(600);
        });
        assertThat(bests.bestWeek()).isNotNull();
        assertThat(bests.bestMonth()).isNotNull();
    }

    @Test
    void streaksCountConsecutiveDays() {
        Streaks streaks = analytics.streaks(USER);

        assertThat(streaks.currentStreakDays()).isEqualTo(3);         // today, -1, -2
        assertThat(streaks.longestStreakDays()).isEqualTo(4);         // -13 .. -10
        assertThat(streaks.longestStreakStart()).isEqualTo(today.minusDays(13));
        assertThat(streaks.longestStreakEnd()).isEqualTo(today.minusDays(10));
        assertThat(streaks.workedOutToday()).isTrue();
        assertThat(streaks.totalActiveDays()).isEqualTo(9);
        assertThat(streaks.lastWorkoutDate()).isEqualTo(today);
        assertThat(streaks.recentDays().get(streaks.recentDays().size() - 1).date()).isEqualTo(today);
        assertThat(streaks.recentDays().get(0).date().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(streaks.recentDays().stream().filter(d -> d.date().equals(today)).findFirst().orElseThrow().workouts())
                .isEqualTo(2); // the RUNNING workout and the legacy one
    }

    @Test
    void currentMonthIsComparedMonthToDate() {
        YearMonth month = YearMonth.from(today);
        YearMonth previous = month.minusMonths(1);
        int throughDay = today.getDayOfMonth();
        MonthComparison result = analytics.monthComparison(USER, month);

        assertThat(result.monthToDate()).isTrue();
        assertThat(result.throughDay()).isEqualTo(throughDay);
        assertThat(result.current().month()).isEqualTo(month.toString());
        assertThat(result.previous().month()).isEqualTo(previous.toString());

        // This month so far vs the same days of last month.
        Predicate<Activity> thisMonth = a -> YearMonth.from(date(a)).equals(month);
        Predicate<Activity> lastMonthSameDays = a -> YearMonth.from(date(a)).equals(previous)
                && date(a).getDayOfMonth() <= throughDay;
        assertThat(result.current().workouts()).isEqualTo(count(thisMonth));
        assertThat(result.previous().workouts()).isEqualTo(count(lastMonthSameDays));
        assertThat(result.previous().calories()).isEqualTo(sum(lastMonthSameDays, Activity::getCaloriesBurned));
        assertThat(result.current().activeDays())
                .isEqualTo(mine.stream().filter(thisMonth).map(this::date).distinct().count());

        MetricChange workouts = result.changes().get(0);
        assertThat(workouts.change()).isEqualTo(result.current().workouts() - result.previous().workouts());
        if (result.previous().workouts() == 0) {
            assertThat(workouts.changePercent()).isNull();
        } else {
            assertThat(workouts.changePercent()).isCloseTo(
                    100.0 * (result.current().workouts() - result.previous().workouts()) / result.previous().workouts(),
                    within(0.1));
        }
    }

    @Test
    void pastMonthsAreComparedInFull() {
        YearMonth month = YearMonth.from(today).minusMonths(1);
        MonthComparison result = analytics.monthComparison(USER, month);

        assertThat(result.monthToDate()).isFalse();
        assertThat(result.throughDay()).isNull();
        assertThat(result.current().workouts()).isEqualTo(count(a -> YearMonth.from(date(a)).equals(month)));
        assertThat(result.previous().minutes())
                .isEqualTo(sum(a -> YearMonth.from(date(a)).equals(month.minusMonths(1)), Activity::getDuration));
    }

    @Test
    void dashboardWorksForAUserWithNoWorkouts() {
        Dashboard empty = analytics.dashboard("new-user");

        assertThat(empty.totals().workouts()).isZero();
        assertThat(empty.weeklyDuration().points()).hasSize(12).allMatch(p -> p.minutes() == 0);
        assertThat(empty.intensity().averageCaloriesPerMinute()).isNull();
        assertThat(empty.personalBests().longestWorkout()).isNull();
        assertThat(empty.streaks().currentStreakDays()).isZero();
        assertThat(empty.monthComparison().changes()).allMatch(c -> c.changePercent() == null);
    }

    // ---------------------------------------------------------------- helpers

    private Activity activity(int daysAgo, ActivityType type, int minutes, int calories) {
        return Activity.builder()
                .userId(USER)
                .type(type)
                .duration(minutes)
                .caloriesBurned(calories)
                .startTime(today.minusDays(daysAgo).atTime(8, 0))
                .build();
    }

    private LocalDate date(Activity a) {
        LocalDateTime at = a.getStartTime() != null ? a.getStartTime() : a.getCreatedAt();
        return at.toLocalDate();
    }

    private long count(Predicate<Activity> filter) {
        return mine.stream().filter(filter).count();
    }

    private long sum(Predicate<Activity> filter, ToLongFunction<Activity> value) {
        return mine.stream().filter(filter).mapToLong(value).sum();
    }

    private static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
