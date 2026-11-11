package com.fitness.activityservice.analytics;

/**
 * Workout intensity, measured as calories burned per minute.
 * Rough guide: an easy walk is ~3-4 kcal/min, a steady run ~10-12.
 */
public enum IntensityLevel {
    LOW("Low", 0),
    MODERATE("Moderate", 5),
    HIGH("High", 8),
    VERY_HIGH("Very high", 11);

    private final String label;
    /** Lowest calories-per-minute value that counts as this level. */
    private final double minCaloriesPerMinute;

    IntensityLevel(String label, double minCaloriesPerMinute) {
        this.label = label;
        this.minCaloriesPerMinute = minCaloriesPerMinute;
    }

    public String label() {
        return label;
    }

    public double minCaloriesPerMinute() {
        return minCaloriesPerMinute;
    }

    public static IntensityLevel of(double caloriesPerMinute) {
        IntensityLevel result = LOW;
        for (IntensityLevel level : values()) {
            if (caloriesPerMinute >= level.minCaloriesPerMinute) {
                result = level;
            }
        }
        return result;
    }
}
