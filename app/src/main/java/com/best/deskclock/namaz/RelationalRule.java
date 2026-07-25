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

    /**
     * Thrown when a rule's parameters result in an impossible time (e.g. clipping conflict).
     */
    public static class InvalidRuleException extends Exception {
        public final long fallbackTime;
        public final String alarmName;

        public InvalidRuleException(String message, String alarmName, long fallbackTime) {
            super(message);
            this.alarmName = alarmName;
            this.fallbackTime = fallbackTime;
        }
    }

    public record EvaluationResult(long time, boolean conflict, long fallbackTime, int autoSilenceSeconds) {
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

    public boolean autostopEnabled;
    public int autostopOffsetMinutes;
    public AnchorEvent autostopAnchorEvent;

    public RelationalRule() {
        this.enabled = true;
    }

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

    public String getRuleDescription(android.content.Context context) {
        StringBuilder sb = new StringBuilder();

        // Base: alarm [at / 5m after / 10m before] Fajr
        String offsetPart = getOffsetPart(context, offsetMinutes);
        String eventName = getEventName(context, anchorEvent);
        sb.append(context.getString(com.best.deskclock.R.string.namaz_rule_description_base, offsetPart, eventName));

        // Rounding: rounded to the nearest 5m
        if (roundingIntervalMinutes > 0) {
            sb.append(" (").append(context.getString(com.best.deskclock.R.string.namaz_rule_rounded, roundingIntervalMinutes)).append(")");
        }

        // Clip: no later than 15m before sunrise
        if (clip != null) {
            String clipOffsetPart = getOffsetPart(context, clip.clipOffsetMinutes);
            String clipEventName = getEventName(context, clip.clipEvent);
            int clipResId = clip.boundaryType == BoundaryType.MAX ? com.best.deskclock.R.string.namaz_rule_no_later : com.best.deskclock.R.string.namaz_rule_no_earlier;
            sb.append(context.getString(clipResId, clipOffsetPart, clipEventName));
        }

        // Autostop: auto dismissed at sunrise
        if (autostopEnabled && autostopAnchorEvent != null) {
            String autostopOffsetPart = getOffsetPart(context, autostopOffsetMinutes);
            String autostopEventName = getEventName(context, autostopAnchorEvent);
            sb.append(context.getString(com.best.deskclock.R.string.namaz_rule_autostop, autostopOffsetPart, autostopEventName));
        }

        String description = sb.toString();
        if (description.isEmpty()) return "";
        return description.substring(0, 1).toUpperCase() + description.substring(1);
    }

    private String getOffsetPart(android.content.Context context, int offset) {
        if (offset == 0) {
            return context.getString(com.best.deskclock.R.string.namaz_rule_at);
        } else if (offset > 0) {
            return context.getString(com.best.deskclock.R.string.namaz_rule_offset_after, offset);
        } else {
            return context.getString(com.best.deskclock.R.string.namaz_rule_offset_before, Math.abs(offset));
        }
    }

    public static String getEventName(android.content.Context context, AnchorEvent event) {
        int resId = switch (event) {
            case FAJR -> com.best.deskclock.R.string.fajr_label;
            case SUNRISE -> com.best.deskclock.R.string.sunrise_label;
            case DHUHR -> com.best.deskclock.R.string.dhuhr_label;
            case ASR_SHAFI -> com.best.deskclock.R.string.asr_shafii_label;
            case ASR_HANAFI -> com.best.deskclock.R.string.asr_hanafi_label;
            case MAGHRIB -> com.best.deskclock.R.string.maghrib_label;
            case ISHA -> com.best.deskclock.R.string.isha_label;
        };
        return context.getString(resId);
    }

    /**
     * Evaluates the trigger time for this rule based on calculated prayer times.
     */
    public static Date evaluateTime(RelationalRule rule, NamazCalculator.NamazTimes times, Date date) {
        return new Date(evaluateTimeWithConflictCheck(rule, times).time);
    }

    public static EvaluationResult evaluateTimeWithConflictCheck(RelationalRule rule, NamazCalculator.NamazTimes times) {
        Date anchorTime = getEventTime(rule.anchorEvent, times);
        if (anchorTime == null) {
            // Cannot calculate time for this event
            return new EvaluationResult(0, true, 0, -1);
        }
        long targetMillis = applyRule(anchorTime, rule.offsetMinutes, rule.roundingDirection, rule.roundingIntervalMinutes);
        long fallbackTime = anchorTime.getTime(); // Default fallback is the anchor event itself

        boolean conflict = false;
        if (rule.clip != null) {
            Date clipAnchorTime = getEventTime(rule.clip.clipEvent, times);
            if (clipAnchorTime != null) {
                long clipMillis = applyRule(clipAnchorTime, rule.clip.clipOffsetMinutes, rule.clip.clipRoundingDirection,
                        rule.clip.clipRoundingIntervalMinutes);

                switch (rule.clip.boundaryType) {
                    case MIN -> {
                        if (targetMillis < clipMillis) {
                            targetMillis = clipMillis;
                            conflict = true;
                        }
                    }
                    case MAX -> {
                        if (targetMillis > clipMillis) {
                            targetMillis = clipMillis;
                            conflict = true;
                        }
                    }
                }
            }
        }

        int autoSilenceSeconds = -1; // -1 means use default
        if (rule.autostopEnabled && rule.autostopAnchorEvent != null) {
            Date autostopAnchorTime = getEventTime(rule.autostopAnchorEvent, times);
            long autostopMillis = applyRule(autostopAnchorTime, rule.autostopOffsetMinutes, null, 0);
            if (autostopMillis > targetMillis) {
                autoSilenceSeconds = (int) ((autostopMillis - targetMillis) / 1000);
            }
        }

        return new EvaluationResult(targetMillis, conflict, fallbackTime, autoSilenceSeconds);
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
        if (baseTime == null) {
            return 0; // Or throw a custom exception
        }
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
