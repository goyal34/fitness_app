package com.fitness.activityservice.analytics;

import com.fitness.activityservice.analytics.AnalyticsDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Workout analytics for the logged-in user (the "sub" of their token).
 * /dashboard returns everything the dashboard needs in one request; the
 * other endpoints return one section each, with adjustable ranges.
 */
@RestController
@RequestMapping("/api/activities/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analytics;

    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        return analytics.dashboard(jwt.getSubject());
    }

    @GetMapping("/weekly-duration")
    public WeeklyDuration weeklyDuration(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(defaultValue = "12") int weeks) {
        return analytics.weeklyDuration(jwt.getSubject(), inRange("weeks", weeks, 1, 52));
    }

    @GetMapping("/monthly-calories")
    public MonthlyCalories monthlyCalories(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "12") int months) {
        return analytics.monthlyCalories(jwt.getSubject(), inRange("months", months, 1, 24));
    }

    @GetMapping("/frequency")
    public Frequency frequency(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(defaultValue = "90") int days) {
        return analytics.frequency(jwt.getSubject(), inRange("days", days, 7, 365));
    }

    @GetMapping("/intensity")
    public Intensity intensity(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(defaultValue = "12") int weeks) {
        return analytics.intensity(jwt.getSubject(), inRange("weeks", weeks, 1, 52));
    }

    @GetMapping("/personal-bests")
    public PersonalBests personalBests(@AuthenticationPrincipal Jwt jwt) {
        return analytics.personalBests(jwt.getSubject());
    }

    @GetMapping("/streaks")
    public Streaks streaks(@AuthenticationPrincipal Jwt jwt) {
        return analytics.streaks(jwt.getSubject());
    }

    /** Compares a month (YYYY-MM, default: this month) with the month before it. */
    @GetMapping("/month-comparison")
    public MonthComparison monthComparison(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) String month) {
        YearMonth target;
        try {
            target = month == null ? YearMonth.now(ZoneId.systemDefault()) : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must look like 2026-11");
        }
        return analytics.monthComparison(jwt.getSubject(), target);
    }

    private static int inRange(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    name + " must be between " + min + " and " + max);
        }
        return value;
    }
}
