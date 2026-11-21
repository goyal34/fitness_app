package com.fitness.activityservice.analytics;

import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record WeekPoint(LocalDate weekStart, long minutes, long workouts, long calories) {
    }

    public record WeeklyDuration(int weeks, long totalMinutes, double averageMinutesPerWeek, List<WeekPoint> points) {
    }

    public record MonthPoint(String month, long calories, long workouts, long minutes) {
    }

    public record MonthlyCalories(int months, long totalCalories, double averageCaloriesPerMonth, List<MonthPoint> points) {
    }

    public record TypeCount(String type, long workouts, long minutes, long calories, double share) {
    }

    public record WeekdayCount(int day, String label, long workouts) {
    }

    public record Frequency(int days, long totalWorkouts, double workoutsPerWeek, String mostFrequentType,
                            List<TypeCount> byType, List<WeekdayCount> byWeekday) {
    }

    public record WeekIntensity(LocalDate weekStart, Double caloriesPerMinute, String level, long workouts) {
    }

    public record LevelCount(String level, String label, long workouts) {
    }

    public record Intensity(int weeks, Double averageCaloriesPerMinute, String level, String levelLabel,
                            List<WeekIntensity> weekly, List<LevelCount> distribution) {
    }

    public record BestWorkout(String activityId, String type, double value, String unit, LocalDate date) {
    }

    public record BestPeriod(String period, double value, String unit) {
    }

    public record TypeBest(String type, long workouts, long longestMinutes, long mostCalories) {
    }

    public record PersonalBests(BestWorkout longestWorkout, BestWorkout mostCalories, BestWorkout highestIntensity,
                                BestPeriod bestWeek, BestPeriod bestMonth, List<TypeBest> byType) {
    }

    public record DayActivity(LocalDate date, long workouts, long minutes) {
    }

    public record Streaks(int currentStreakDays, int longestStreakDays, LocalDate longestStreakStart,
                          LocalDate longestStreakEnd, int currentWeeklyStreak, int totalActiveDays,
                          LocalDate lastWorkoutDate, boolean workedOutToday,
                          List<DayActivity> recentDays) {
    }

    public record MonthStats(String month, long workouts, long minutes, long calories, long activeDays,
                             Double caloriesPerMinute) {
    }

    public record MetricChange(String metric, String label, double current, double previous,
                               double change, Double changePercent) {
    }

    public record MonthComparison(MonthStats current, MonthStats previous, boolean monthToDate,
                                  Integer throughDay, List<MetricChange> changes) {
    }

    public record Totals(long workouts, long minutes, long calories, LocalDate firstWorkoutDate) {
    }

    public record Dashboard(Totals totals, WeeklyDuration weeklyDuration, MonthlyCalories monthlyCalories,
                            Frequency frequency, Intensity intensity, PersonalBests personalBests,
                            Streaks streaks, MonthComparison monthComparison) {
    }
}
