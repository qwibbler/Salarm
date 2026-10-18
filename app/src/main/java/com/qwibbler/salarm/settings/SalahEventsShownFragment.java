package com.qwibbler.salarm.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.MasjidEvent;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahCalculator;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalahEventsShownFragment extends BaseSettingsScreenFragment {

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.masjid_events_shown_title);
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
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
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
            List<MasjidEvent> masjidEvents = SalahDatabase.getDatabase(appContext).masjidEventDao().getAll();

            Map<Long, String> eventSummaries = new HashMap<>();
            for (MasjidEvent e : masjidEvents) {
                eventSummaries.put(e.id, calculateEventTimeToday(appContext, e));
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    populateScreen(masjids, masjidEvents, eventSummaries);
                }
            });
        });
    }

    private void populateScreen(List<Masjid> masjids, List<MasjidEvent> masjidEvents, Map<Long, String> eventSummaries) {
        Context context = getContext();
        if (context == null || getPreferenceManager() == null) return;

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        String[] salahTypes = {"FAJR", "DHUHR", "JUMA", "ASR", "MAGHRIB", "ISHA"};

        for (String type : salahTypes) {
            List<Pair<MasjidEvent, Masjid>> activeMasjidEvents = new ArrayList<>();
            for (Masjid masjid : masjids) {
                if (masjid.showMasjidEvents) {
                    MasjidEvent event = findMasjidEvent(masjidEvents, masjid.id, type);
                    if (event != null) {
                        activeMasjidEvents.add(new Pair<>(event, masjid));
                    }
                }
            }

            if (!activeMasjidEvents.isEmpty()) {
                PreferenceCategory category = new PreferenceCategory(context);
                category.setTitle(SalahUtils.getLocalizedSalahName(context, type));
                screen.addPreference(category);

                for (Pair<MasjidEvent, Masjid> p : activeMasjidEvents) {
                    addMasjidEventPreference(category, SalahUtils.getLocalizedSalahName(context, type), p.second.name, p.first, eventSummaries.get(p.first.id));
                }
            }
        }
        setPreferenceScreen(screen);
        setDivider(null);
        setDividerHeight(0);
    }

    private MasjidEvent findMasjidEvent(List<MasjidEvent> events, long masjidId, String type) {
        for (MasjidEvent e : events) {
            if (e.masjidId == masjidId && type.equals(e.salahType)) {
                return e;
            }
        }
        return null;
    }

    private void addMasjidEventPreference(PreferenceCategory category, String localizedTitle, String label, MasjidEvent event, String summary) {
        Context context = getContext();
        if (context == null) return;

        CheckBoxPreference pref = new CheckBoxPreference(context);
        pref.setLayoutResource(R.layout.settings_preference_layout);
        pref.setTitle(localizedTitle + " - " + label);

        pref.setChecked(event.isSelected);
        pref.setSummary(summary);
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            event.isSelected = (boolean) newValue;
            event.isManuallyDeselected = !event.isSelected;
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase.getDatabase(requireContext()).masjidEventDao().update(event);
                SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
            });
            return true;
        });

        category.addPreference(pref);
    }

    private String calculateEventTimeToday(Context context, MasjidEvent event) {
        if (context == null) return "";
        double[] coords = SalahUtils.getCoordinates(context);
        Date today = new Date();
        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(context, today, coords[0], coords[1], SalahUtils.getCalculationMethod(context));

        long evaluated = SalahCalculator.evaluateMasjidEvent(context, event, times, today, true);
        if (evaluated > 0) {
            return DateFormat.getTimeFormat(context).format(new Date(evaluated));
        }
        return "";
    }

    private record Pair<F, S>(F first, S second) {
    }
}
