package com.qwibbler.salarm.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahCalculator;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SalahEventsShownFragment extends BaseSettingsScreenFragment {

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.salah_masjid_events_shown_title);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(requireContext()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("masjid_updated", this, (requestKey, result) -> loadData());
        loadData();
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
        setDividerHeight(0);
    }

    private void loadData() {
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            List<Masjid> masjids = SalahDatabase.getDatabase(appContext).masjidDao().getAll();
            List<RelationalRule> rules = SalahDatabase.getDatabase(appContext).relationalRuleDao().getAll();

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    populateScreen(masjids, rules);
                }
            });
        });
    }

    private void populateScreen(List<Masjid> masjids, List<RelationalRule> rules) {
        Context context = getContext();
        if (context == null || getPreferenceManager() == null) return;

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        String[] salahTypes = {"FAJR", "DHUHR", "JUMA", "ASR", "MAGHRIB", "ISHA"};

        for (String type : salahTypes) {
            // 1. Actual event
            RelationalRule defaultRule = findRule(rules, 0, type);

            // 2. Masjid events (only enabled masjids)
            List<Pair<RelationalRule, Masjid>> activeMasjidRules = new ArrayList<>();
            for (Masjid masjid : masjids) {
                if (masjid.showMasjidEvents) {
                    RelationalRule masjidRule = findRule(rules, masjid.id, type);
                    if (masjidRule != null) {
                        activeMasjidRules.add(new Pair<>(masjidRule, masjid));
                    }
                }
            }

            if (defaultRule != null || !activeMasjidRules.isEmpty()) {
                PreferenceCategory category = new PreferenceCategory(context);
                category.setTitle(SalahUtils.getLocalizedSalahName(context, type));
                screen.addPreference(category);

                if (defaultRule != null) {
                    addEventPreference(category, SalahUtils.getLocalizedSalahName(context, type), getString(R.string.salah_masjid_events_default), defaultRule, null);
                }

                for (Pair<RelationalRule, Masjid> p : activeMasjidRules) {
                    addEventPreference(category, SalahUtils.getLocalizedSalahName(context, type), p.second.name, p.first, p.second);
                }
            }
        }
        setPreferenceScreen(screen);
        setDivider(null);
        setDividerHeight(0);
    }

    private RelationalRule findRule(List<RelationalRule> rules, long masjidId, String type) {
        for (RelationalRule r : rules) {
            if (r.masjidId == masjidId && type.equals(r.masjidSalahType)) {
                return r;
            }
        }
        return null;
    }

    private void addEventPreference(PreferenceCategory category, String localizedTitle, String label, RelationalRule rule, Masjid masjid) {
        Context context = getContext();
        if (context == null) return;

        CheckBoxPreference pref = new CheckBoxPreference(context);
        pref.setLayoutResource(R.layout.settings_preference_layout);
        if (masjid == null) {
            pref.setTitle(localizedTitle);
        } else {
            pref.setTitle(localizedTitle + " - " + label);
        }

        if (rule != null) {
            pref.setChecked(rule.isSelected);
            pref.setSummary(calculateTimeToday(rule, masjid));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                rule.isSelected = (boolean) newValue;
                rule.isManuallyDeselected = !rule.isSelected;
                AppExecutors.getDiskIO().execute(() -> {
                    SalahDatabase.getDatabase(requireContext()).relationalRuleDao().update(rule);
                    SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
                });
                return true;
            });
        } else {
            // If no rule, it might be just the raw Salah time?
            // Requirement 4 says "Delete: Unsets the event time for that Salah. Removes the saved relational configuration."
            // This suggests that if there's no rule, there's no event.
            // But "Default" should probably always be available.
            if (masjid == null) {
                pref.setEnabled(false);
                pref.setSummary(R.string.salah_masjid_not_configured);
            } else {
                return; // Don't show unconfigured masjid events
            }
        }

        category.addPreference(pref);
    }

    private String calculateTimeToday(RelationalRule rule, Masjid masjid) {
        Context context = getContext();
        if (context == null) return "";
        double[] coords = SalahUtils.getCoordinates(context);
        Date today = new Date();
        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(context, today, coords[0], coords[1], SalahUtils.getCalculationMethod(context));

        // This is a simplified calculation for the summary
        Date evaluated = RelationalRule.evaluateTime(context, rule, times, today, masjid);
        if (evaluated != null) {
            return android.text.format.DateFormat.getTimeFormat(context).format(evaluated);
        }
        return "";
    }

    private record Pair<F, S>(F first, S second) {
    }
}
