package com.fitness.activityservice.analytics;

import java.time.LocalDate;
import java.util.List;

/** Response shapes of the analytics API. All values are already aggregated by MongoDB. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    // --- weekly duration ---

    public record WeekPoint(LocalDate weekStart, long minutes, long workouts, long calories) {
    }

    public record WeeklyDuration(int weeks, long totalMinutes, double averageMinutesPerWeek, List<WeekPoint> points) {
    }

    // --- monthly calories ---

    /** month is "YYYY-MM". */
    public record MonthPoint(String month, long calories, long workouts, long minutes) {
    }

    public record MonthlyCalories(int months, long totalCalories, double averageCaloriesPerMonth, List<MonthPoint> points) {
    }

    // --- frequency ---

    public record TypeCount(String type, long workouts, long minutes, long calories, double share) {
    }

    /** day is 1 = Monday ... 7 = Sunday (ISO). */
    public record WeekdayCount(int day, String label, long workouts) {
    }

    public record Frequency(int days, long totalWorkouts, double workoutsPerWeek, String mostFrequentType,
                            List<TypeCount> byType, List<WeekdayCount> byWeekday) {
    }

    // --- intensity ---

    public record WeekIntensity(LocalDate weekStart, Double caloriesPerMinute, String level, long workouts) {
    }

    public record LevelCount(String level, String label, long workouts) {
    }

    public record Intensity(int weeks, Double averageCaloriesPerMinute, String level, String levelLabel,
                            List<WeekIntensity> weekly, List<LevelCount> distribution) {
    }

    // --- personal bests ---

    public record BestWorkout(String activityId, String type, double value, String unit, LocalDate date) {
    }

    /** period is the week start date ("YYYY-MM-DD") or the month ("YYYY-MM"). */
    public record BestPeriod(String period, double value, String unit) {
    }

    public record TypeBest(String type, long workouts, long longestMinutes, long mostCalories) {
    }

    public record PersonalBests(BestWorkout longestWorkout, BestWorkout mostCalories, BestWorkout highestIntensity,
                                BestPeriod bestWeek, BestPeriod bestMonth, List<TypeBest> byType) {
    }

    // --- streaks ---

    public record DayActivity(LocalDate date, long workouts, long minutes) {
    }

    public record Streaks(int currentStreakDays, int longestStreakDays, LocalDate longestStreakStart,
                          LocalDate longestStreakEnd, int currentWeeklyStreak, int totalActiveDays,
                          LocalDate lastWorkoutDate, boolean workedOutToday,
                          /** Every day of the last 12 weeks (for a calendar heatmap), oldest first. */
                          List<DayActivity> recentDays) {
    }

    // --- month comparison ---

    public record MonthStats(String month, long workouts, long minutes, long calories, long activeDays,
                             Double caloriesPerMinute) {
    }

    /** changePercent is null when the previous value is 0. */
    public record MetricChange(String metric, String label, double current, double previous,
                               double change, Double changePercent) {
    }

    /**
     * monthToDate: the month is still running, so both sides cover only days
     * 1..throughDay (throughDay is null for finished months).
     */
    public record MonthComparison(MonthStats current, MonthStats previous, boolean monthToDate,
                                  Integer throughDay, List<MetricChange> changes) {
    }

    // --- dashboard (everything in one call) ---

    public record Totals(long workouts, long minutes, long calories, LocalDate firstWorkoutDate) {
    }

    public record Dashboard(Totals totals, WeeklyDuration weeklyDuration, MonthlyCalories monthlyCalories,
                            Frequency frequency, Intensity intensity, PersonalBests personalBests,
                            Streaks streaks, MonthComparison monthComparison) {
    }
}
