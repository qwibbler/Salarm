package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.settings.PreferencesKeys;

import java.util.Date;
import java.util.Locale;

/**
 * Represents a rule for calculating an alarm time relative to a prayer event.
 */
@Entity(tableName = "relational_rules")
public class RelationalRule {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public boolean enabled;

    // Metadata for identifying "tracked" alarms (Default and Masjid prayer alarms)
    public long masjidId; // 0 for local/default prayer
    public String masjidSalahType; // e.g. "FAJR"
    public boolean isSelected;
    public boolean isManuallyDeselected;

    @Embedded
    public RelationalDefinition definition;

    public boolean autostopEnabled;
    public int autostopOffsetMinutes;
    @Embedded(prefix = "autostop_")
    public RelationalAnchor autostopAnchor;

    public RelationalRule() {
        this.enabled = true;
        this.definition = new RelationalDefinition();
        this.autostopAnchor = new RelationalAnchor();
    }

    public RelationalRule(RelationalRule other) {
        this.id = other.id;
        this.enabled = other.enabled;
        this.masjidId = other.masjidId;
        this.masjidSalahType = other.masjidSalahType;
        this.isSelected = other.isSelected;
        this.isManuallyDeselected = other.isManuallyDeselected;
        this.definition = new RelationalDefinition(other.definition);
        this.autostopEnabled = other.autostopEnabled;
        this.autostopOffsetMinutes = other.autostopOffsetMinutes;
        this.autostopAnchor = new RelationalAnchor(other.autostopAnchor);
    }

    public static String getAnchorDescription(Context context, RelationalAnchor anchor) {
        if (anchor.masjidEventId != null) {
            MasjidEvent masjidEvent = SalahDatabase.getDatabase(context).masjidEventDao().getById(anchor.masjidEventId);
            if (masjidEvent != null) {
                Masjid masjid = SalahDatabase.getDatabase(context).masjidDao().getById(masjidEvent.masjidId);
                String masjidName = masjid != null ? masjid.name : "Masjid";
                return SalahUtils.getLocalizedSalahName(context, masjidEvent.salahType) + " - " + masjidName;
            }
        }
        if (anchor.basicEvent != null) {
            return getEventName(context, anchor.basicEvent);
        }
        return "";
    }

    public static String getEventName(Context context, AnchorEvent event) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR, false) && prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR_IN_ALARM_EVENTS, false);

        if (!showBothAsr) {
            if (event == AnchorEvent.ASR_HANAFI || event == AnchorEvent.ASR_SHAFI) {
                return context.getString(R.string.asr_label);
            }
        }

        int resId = switch (event) {
            case FAJR -> R.string.fajr_label;
            case SUNRISE -> R.string.sunrise_label;
            case DHUHR -> R.string.dhuhr_label;
            case ASR_SHAFI -> R.string.asr_shafii_label;
            case ASR_HANAFI -> R.string.asr_hanafi_label;
            case MAGHRIB -> R.string.maghrib_label;
            case ISHA -> R.string.isha_label;
        };
        return context.getString(resId);
    }

    /**
     * Evaluates the trigger time for this rule based on calculated prayer times.
     */
    public static Date evaluateTime(Context context, @NonNull RelationalRule rule, @NonNull SalahCalculator.SalahTimes times, Date date) {
        EvaluationResult result = evaluateTimeWithConflictCheck(context, rule, times, date);
        return result.time() > 0 ? new Date(result.time()) : null;
    }

    @NonNull
    public static EvaluationResult evaluateTimeWithConflictCheck(Context context, @NonNull RelationalRule rule, @NonNull SalahCalculator.SalahTimes times, Date date) {
        long targetMillis = SalahCalculator.evaluateDefinition(context, rule.definition, times, date);
        if (targetMillis <= 0) {
            return new EvaluationResult(0, -1);
        }

        int autoSilenceSeconds = -1; // -1 means use default
        if (rule.autostopEnabled) {
            long autostopAnchorTime = SalahCalculator.evaluateAnchor(context, rule.autostopAnchor, times, date);
            if (autostopAnchorTime > 0) {
                long autostopMillis = SalahCalculator.applyCalculation(autostopAnchorTime, rule.autostopOffsetMinutes, null, 0, 0);
                if (autostopMillis > targetMillis) {
                    autoSilenceSeconds = (int) ((autostopMillis - targetMillis) / 1000);
                }
            }
        }

        return new EvaluationResult(targetMillis, autoSilenceSeconds);
    }

    public String getRuleDescription(Context context) {
        if (context == null) return "";
        String anchorName = getAnchorDescription(context, definition.anchor);
        if (anchorName.isEmpty()) {
            if (masjidSalahType != null) {
                anchorName = SalahUtils.getLocalizedSalahName(context, masjidSalahType);
            } else {
                return "";
            }
        }

        String summary = definition.getSummary(context, anchorName, false);

        // Autostop: auto dismissed at sunrise
        if (autostopEnabled) {
            String autostopEventName = getAnchorDescription(context, autostopAnchor);
            if (!autostopEventName.isEmpty()) {
                String autostopOffsetPart = RelationalDefinition.getOffsetPart(context, autostopOffsetMinutes);
                summary += context.getString(R.string.salah_rule_autostop, autostopOffsetPart, autostopEventName);
            }
        }

        if (summary.isEmpty()) return "";
        return summary.substring(0, 1).toUpperCase(Locale.getDefault()) + summary.substring(1);
    }

    public enum AnchorEvent {
        FAJR, SUNRISE, DHUHR, ASR_SHAFI, ASR_HANAFI, MAGHRIB, ISHA
    }

    public enum RoundingDirection {
        NEAREST, UP, DOWN
    }

    public enum BoundaryType {
        NO_LATER_THAN, NO_EARLIER_THAN
    }

    public record EvaluationResult(long time, int autoSilenceSeconds) {
    }
}
