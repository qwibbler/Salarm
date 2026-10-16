package com.qwibbler.salarm.salah;

import android.content.Context;

import androidx.room.Embedded;
import androidx.room.Ignore;

import com.qwibbler.salarm.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

/**
 * Complete definition of a time calculation (Fixed or Relational).
 */
public class RelationalDefinition {
    public boolean isFixedTime;
    public int fixedHour;
    public int fixedMinute;

    @Embedded(prefix = "anchor_")
    public RelationalAnchor anchor;

    @Embedded(prefix = "calc_")
    public RelationalCalculation calculation;

    @Embedded(prefix = "clip_")
    public RelationalClip clip;

    public String getSummary(Context context, String anchorName, boolean isMasjidDefinition) {
        if (context == null) return "";
        if (isFixedTime) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, fixedHour);
                cal.set(Calendar.MINUTE, fixedMinute);
                DateFormat df = android.text.format.DateFormat.getTimeFormat(context);
                if (df != null) return df.format(cal.getTime());
            } catch (Exception ignored) {}
            return String.format(Locale.getDefault(), "%02d:%02d", fixedHour, fixedMinute);
        }

        if (anchorName == null || anchorName.isEmpty()) return "";
        if (calculation == null || anchor == null) return "";

        StringBuilder sb = new StringBuilder();
        String offsetPart = getOffsetPart(context, calculation.offsetMinutes);

        int baseResId;
        if (isMasjidDefinition) {
            if (anchor.basicEvent != null && anchor.basicEvent != RelationalRule.AnchorEvent.MAGHRIB) {
                baseResId = anchor.isMax ? R.string.masjid_rule_description_base_max : R.string.masjid_rule_description_base_min;
            } else {
                baseResId = R.string.masjid_rule_description_base;
            }
        } else {
            if (anchor.basicEvent != null) {
                baseResId = anchor.isMax ? R.string.salah_alarm_rule_description_base_max : R.string.salah_alarm_rule_description_base_min;
            } else {
                baseResId = R.string.salah_rule_description_base;
            }
        }
        sb.append(context.getString(baseResId, offsetPart, anchorName));

        boolean hasRounding = (calculation.roundingIntervalMinutes > 0 && calculation.roundingDirection != null);
        sb.append(getRoundingPart(context, calculation.roundingDirection, calculation.roundingIntervalMinutes));

        if (hasRounding && calculation.adjustmentMinutes != 0) {
            String adjPart = getAdjustmentPart(context, calculation.adjustmentMinutes);
            sb.append(context.getString(R.string.salah_rule_adjusted, adjPart));
        }

        if (clip != null) {
            String clipOffsetPart = getOffsetPart(context, clip.offsetMinutes);
            String clipEventName = clip.anchor.basicEvent != null
                    ? RelationalRule.getEventName(context, clip.anchor.basicEvent)
                    : ""; // Logic for masjid-anchored clips can be added here if needed

            if (!clipEventName.isEmpty()) {
                int clipResId = clip.boundaryType == RelationalRule.BoundaryType.NO_LATER_THAN
                        ? R.string.salah_rule_no_later
                        : R.string.salah_rule_no_earlier;
                sb.append(context.getString(clipResId, clipOffsetPart, clipEventName));
                sb.append(getRoundingPart(context, clip.roundingDirection, clip.roundingIntervalMinutes));
            }
        }

        String description = sb.toString();
        if (description.isEmpty()) return "";
        return description.substring(0, 1).toUpperCase(Locale.getDefault()) + description.substring(1);
    }

    public static String getOffsetPart(Context context, int offset) {
        if (offset == 0) return context.getString(R.string.salah_rule_at);
        int resId = offset > 0 ? R.string.salah_rule_offset_after : R.string.salah_rule_offset_before;
        return context.getString(resId, Math.abs(offset));
    }

    public static String getRoundingPart(Context context, RelationalRule.RoundingDirection direction, int interval) {
        if (interval <= 0 || direction == null) return "";
        int resId = switch (direction) {
            case NEAREST -> R.string.salah_rule_rounded_nearest;
            case UP -> R.string.salah_rule_rounded_up;
            case DOWN -> R.string.salah_rule_rounded_down;
        };
        return context.getString(resId, interval);
    }

    public static String getAdjustmentPart(Context context, int offset) {
        int resId = offset >= 0 ? R.string.salah_rule_adjustment_later : R.string.salah_rule_adjustment_earlier;
        return context.getString(resId, Math.abs(offset));
    }

    public RelationalDefinition() {
        this.anchor = new RelationalAnchor();
        this.calculation = new RelationalCalculation();
    }

    @Ignore
    public RelationalDefinition(RelationalDefinition other) {
        if (other != null) {
            this.isFixedTime = other.isFixedTime;
            this.fixedHour = other.fixedHour;
            this.fixedMinute = other.fixedMinute;
            this.anchor = new RelationalAnchor(other.anchor);
            this.calculation = new RelationalCalculation(other.calculation);
            if (other.clip != null) {
                this.clip = new RelationalClip(other.clip);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationalDefinition that = (RelationalDefinition) o;
        return isFixedTime == that.isFixedTime &&
                fixedHour == that.fixedHour &&
                fixedMinute == that.fixedMinute &&
                Objects.equals(anchor, that.anchor) &&
                Objects.equals(calculation, that.calculation) &&
                Objects.equals(clip, that.clip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFixedTime, fixedHour, fixedMinute, anchor, calculation, clip);
    }
}
