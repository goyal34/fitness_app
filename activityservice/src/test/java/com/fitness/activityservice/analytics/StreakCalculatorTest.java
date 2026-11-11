package com.fitness.activityservice.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    // A Wednesday, so "this week" started on Monday 2026-11-09.
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    private static StreakCalculator.Result compute(LocalDate... days) {
        return StreakCalculator.compute(new TreeSet<>(List.of(days)), TODAY);
    }

    @Test
    void noWorkoutsMeansNoStreaks() {
        StreakCalculator.Result r = compute();
        assertThat(r.current()).isZero();
        assertThat(r.longest()).isZero();
        assertThat(r.currentWeekly()).isZero();
    }

    @Test
    void currentStreakCountsBackFromToday() {
        StreakCalculator.Result r = compute(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(4));
        assertThat(r.current()).isEqualTo(3);
    }

    @Test
    void streakStaysAliveUntilTheEndOfToday() {
        // Worked out yesterday and the day before, not yet today.
        StreakCalculator.Result r = compute(TODAY.minusDays(1), TODAY.minusDays(2));
        assertThat(r.current()).isEqualTo(2);
    }

    @Test
    void streakIsBrokenAfterAMissedDay() {
        StreakCalculator.Result r = compute(TODAY.minusDays(2), TODAY.minusDays(3));
        assertThat(r.current()).isZero();
        assertThat(r.longest()).isEqualTo(2);
    }

    @Test
    void longestStreakReportsItsDates() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        StreakCalculator.Result r = compute(start, start.plusDays(1), start.plusDays(2), start.plusDays(3),
                start.plusDays(10), TODAY);
        assertThat(r.longest()).isEqualTo(4);
        assertThat(r.longestStart()).isEqualTo(start);
        assertThat(r.longestEnd()).isEqualTo(start.plusDays(3));
        assertThat(r.current()).isEqualTo(1);
    }

    @Test
    void streakCrossesMonthAndYearBoundaries() {
        StreakCalculator.Result r = StreakCalculator.compute(new TreeSet<>(List.of(
                LocalDate.of(2025, 12, 30), LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1))),
                LocalDate.of(2026, 1, 1));
        assertThat(r.current()).isEqualTo(3);
        assertThat(r.longest()).isEqualTo(3);
    }

    @Test
    void weeklyStreakCountsConsecutiveWeeks() {
        // This week (Mon 11-09), last week, and the week before; then a gap.
        StreakCalculator.Result r = compute(TODAY, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 10, 5));
        assertThat(r.currentWeekly()).isEqualTo(3);
    }

    @Test
    void weeklyStreakStaysAliveIfOnlyLastWeekHadAWorkout() {
        StreakCalculator.Result r = compute(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 10, 28));
        assertThat(r.currentWeekly()).isEqualTo(2);
    }
}
