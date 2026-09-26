package com.safesphere.event;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimelineLogEvent {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String category;
    private final String message;
    private final String timestampFormatted;
    private final long epochMillis;

    public TimelineLogEvent(String category, String message) {
        this.category = category;
        this.message = message;
        this.epochMillis = System.currentTimeMillis();
        this.timestampFormatted = LocalTime.now().format(TIME_FORMATTER);
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestampFormatted() {
        return timestampFormatted;
    }

    public long getEpochMillis() {
        return epochMillis;
    }

    public String toLogLine() {
        return String.format("[%s] [%-11s] %s", timestampFormatted, category, message);
    }

    @Override
    public String toString() {
        return toLogLine();
    }
}
