package com.example.myapplication;

import java.util.List;
import java.util.Map;

public class DailiesModels {
    public List<Day> data;
    public static class Day { public String calendarDate; public Daily data; }
    public static class Daily {
        public int steps, stepsGoal;
        public String activityType, calendarDate;
        public int floorsClimbed, floorsClimbedGoal;
        public int maxStressLevel, averageStressLevel;
        public int stressDurationInSeconds, lowStressDurationInSeconds,
                mediumStressDurationInSeconds, highStressDurationInSeconds,
                restStressDurationInSeconds;
        public int bmrKilocalories, activeKilocalories;
        public long distanceInMeters;
        public int durationInSeconds, activeTimeInSeconds;
        public int averageHeartRateInBeatsPerMinute, restingHeartRateInBeatsPerMinute,
                maxHeartRateInBeatsPerMinute, minHeartRateInBeatsPerMinute;
        public Map<String, Integer> timeOffsetHeartRateSamples;
    }
}
