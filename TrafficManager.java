import java.time.LocalTime;

class TrafficManager {
    private static final LocalTime MORNING_PEAK_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_PEAK_END = LocalTime.of(10, 0);
    private static final LocalTime EVENING_PEAK_START = LocalTime.of(16, 0);
    private static final LocalTime EVENING_PEAK_END = LocalTime.of(18, 0);
    private static final double PEAK_MULTIPLIER = 0.5; // speed halved during peak hours

    public double getTrafficMultiplier(LocalTime time) {
        if (isPeakHour(time)) {
            return PEAK_MULTIPLIER; // speed is halved during peak hours
        }
        return 1.0; // normal speed outside peak hours
    }

    public boolean isPeakHour(LocalTime time) {
        return (time.isAfter(MORNING_PEAK_START) && time.isBefore(MORNING_PEAK_END)) ||
               (time.isAfter(EVENING_PEAK_START) && time.isBefore(EVENING_PEAK_END));
    }
}
