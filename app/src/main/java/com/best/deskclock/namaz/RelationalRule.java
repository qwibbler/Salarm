package com.best.deskclock.namaz;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a rule for calculating an alarm time relative to a prayer event.
 */
@Entity(tableName = "relational_rules")
public class RelationalRule {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public boolean enabled;

    public enum AnchorEvent {
        FAJR, SUNRISE, DHUHR, ASR_SHAFI, ASR_HANAFI, MAGHRIB, ISHA
    }

    public enum RoundingDirection {
        NEAREST, UP, DOWN
    }

    public enum BoundaryType {
        MIN, MAX
    }

    public static class Clip {
        public AnchorEvent clipEvent;
        public int clipOffsetMinutes;
        public RoundingDirection clipRoundingDirection;
        public int clipRoundingIntervalMinutes;
        public BoundaryType boundaryType;

        public Clip() {}

        public Clip(AnchorEvent clipEvent, int clipOffsetMinutes, RoundingDirection clipRoundingDirection,
                    int clipRoundingIntervalMinutes, BoundaryType boundaryType) {
            this.clipEvent = clipEvent;
            this.clipOffsetMinutes = clipOffsetMinutes;
            this.clipRoundingDirection = clipRoundingDirection;
            this.clipRoundingIntervalMinutes = clipRoundingIntervalMinutes;
            this.boundaryType = boundaryType;
        }
    }

    public AnchorEvent anchorEvent;
    public int offsetMinutes;
    public RoundingDirection roundingDirection;
    public int roundingIntervalMinutes;

    @Embedded(prefix = "clip_")
    public Clip clip;

    public RelationalRule() {}

    public RelationalRule(AnchorEvent anchorEvent, int offsetMinutes, RoundingDirection roundingDirection,
                          int roundingIntervalMinutes) {
        this(anchorEvent, offsetMinutes, roundingDirection, roundingIntervalMinutes, null);
    }

    public RelationalRule(AnchorEvent anchorEvent, int offsetMinutes, RoundingDirection roundingDirection,
                          int roundingIntervalMinutes, Clip clip) {
        this.anchorEvent = anchorEvent;
        this.offsetMinutes = offsetMinutes;
        this.roundingDirection = roundingDirection;
        this.roundingIntervalMinutes = roundingIntervalMinutes;
        this.clip = clip;
        this.enabled = true;
    }

    /**
     * Evaluates the trigger time for this rule based on calculated prayer times.
     */
    public static Date evaluateTime(RelationalRule rule, NamazCalculator.NamazTimes times, Date date) {
        Date anchorTime = getEventTime(rule.anchorEvent, times);
        long targetMillis = applyRule(anchorTime, rule.offsetMinutes, rule.roundingDirection, rule.roundingIntervalMinutes);

        if (rule.clip != null) {
            Date clipAnchorTime = getEventTime(rule.clip.clipEvent, times);
            long clipMillis = applyRule(clipAnchorTime, rule.clip.clipOffsetMinutes, rule.clip.clipRoundingDirection,
                    rule.clip.clipRoundingIntervalMinutes);

            switch (rule.clip.boundaryType) {
                case MIN -> {
                    if (targetMillis < clipMillis) {
                        targetMillis = clipMillis;
                    }
                }
                case MAX -> {
                    if (targetMillis > clipMillis) {
                        targetMillis = clipMillis;
                    }
                }
            }
        }

        return new Date(targetMillis);
    }

    private static Date getEventTime(AnchorEvent event, NamazCalculator.NamazTimes times) {
        return switch (event) {
            case FAJR -> times.fajr;
            case SUNRISE -> times.sunrise;
            case DHUHR -> times.dhuhr;
            case ASR_SHAFI -> times.asrShafi;
            case ASR_HANAFI -> times.asrHanafi;
            case MAGHRIB -> times.maghrib;
            case ISHA -> times.isha;
        };
    }

    private static long applyRule(Date baseTime, int offsetMinutes, RoundingDirection direction, int intervalMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseTime);
        cal.add(Calendar.MINUTE, offsetMinutes);

        if (intervalMinutes <= 0 || direction == null) {
            return cal.getTimeInMillis();
        }

        long millis = cal.getTimeInMillis();
        long intervalMillis = intervalMinutes * 60 * 1000L;

        if (direction == RoundingDirection.NEAREST) {
            return Math.round((double) millis / intervalMillis) * intervalMillis;
        } else if (direction == RoundingDirection.UP) {
            return (long) Math.ceil((double) millis / intervalMillis) * intervalMillis;
        } else if (direction == RoundingDirection.DOWN) {
            return (long) Math.floor((double) millis / intervalMillis) * intervalMillis;
        }

        return millis;
    }
}
