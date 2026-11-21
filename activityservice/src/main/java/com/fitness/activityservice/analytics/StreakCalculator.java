package com.fitness.activityservice.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NavigableSet;
import java.util.TreeSet;

public final class StreakCalculator {

    private StreakCalculator() {
    }

    public record Result(int current, int longest, LocalDate longestStart, LocalDate longestEnd, int currentWeekly) {
    }

    public static Result compute(NavigableSet<LocalDate> days, LocalDate today) {
        if (days.isEmpty()) {
            return new Result(0, 0, null, null, 0);
        }

        int longest = 0;
        LocalDate longestStart = null;
        LocalDate longestEnd = null;
        int run = 0;
        LocalDate runStart = null;
        LocalDate previous = null;
        for (LocalDate day : days) {
            if (previous != null && day.equals(previous.plusDays(1))) {
                run++;
            } else {
                run = 1;
                runStart = day;
            }
            if (run > longest) {
                longest = run;
                longestStart = runStart;
                longestEnd = day;
            }
            previous = day;
        }

        int current = 0;
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        while (days.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }

        NavigableSet<LocalDate> weeks = new TreeSet<>();
        for (LocalDate day : days) {
            weeks.add(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        }
        LocalDate thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate week = weeks.contains(thisWeek) ? thisWeek : thisWeek.minusWeeks(1);
        int currentWeekly = 0;
        while (weeks.contains(week)) {
            currentWeekly++;
            week = week.minusWeeks(1);
        }

        return new Result(current, longest, longestStart, longestEnd, currentWeekly);
    }
}
