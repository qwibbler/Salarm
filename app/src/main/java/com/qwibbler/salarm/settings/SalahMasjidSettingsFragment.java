package com.qwibbler.salarm.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahUtils;

import java.util.List;

public class SalahMasjidSettingsFragment extends BaseSettingsScreenFragment {

    private static final String KEY_EVENTS_SHOWN = "key_masjid_events_shown";
    private static final String KEY_ADD_MASJID = "key_add_new_masjid";
    private static final String KEY_DOWNLOAD_CARD = "key_download_card";

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.salah_masjid_timings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.salah_masjid_settings);

        getParentFragmentManager().setFragmentResultListener("masjid_updated", this, (requestKey, result) -> {
            loadMasjids();
        });

        Preference eventsShown = findPreference(KEY_EVENTS_SHOWN);
        if (eventsShown != null) {
            eventsShown.setOnPreferenceClickListener(preference -> {
                animateAndShowFragment(new SalahEventsShownFragment());
                return true;
            });
        }

        Preference addMasjid = findPreference(KEY_ADD_MASJID);
        if (addMasjid != null) {
            addMasjid.setOnPreferenceClickListener(preference -> {
                MasjidEditDialogFragment.show(getParentFragmentManager(), 0);
                return true;
            });
        }

        Preference downloadCard = findPreference(KEY_DOWNLOAD_CARD);
        if (downloadCard != null) {
            downloadCard.setOnPreferenceClickListener(preference -> {
                // Placeholder
                return true;
            });
        }

        loadMasjids();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
        setDividerHeight(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        setDivider(null);
        setDividerHeight(0);
        loadMasjids();
    }

    private void loadMasjids() {
        AppExecutors.getDiskIO().execute(() -> {
            List<Masjid> masjids = SalahDatabase.getDatabase(requireContext()).masjidDao().getAll();
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    populateMasjids(masjids);
                }
            });
        });
    }

    private void populateMasjids(List<Masjid> masjids) {
        Context context = getContext();
        if (context == null) return;

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);
        addPreferencesFromResource(R.xml.salah_masjid_settings);
        setDivider(null);
        setDividerHeight(0);

        // Re-setup listeners because we reset the screen
        setupFixedPreferences();

        for (Masjid masjid : masjids) {
            PreferenceCategory masjidSection = new PreferenceCategory(context);
            masjidSection.setTitle(masjid.name);
            screen.addPreference(masjidSection);

            Preference editMasjidPref = new Preference(context);
            editMasjidPref.setTitle(R.string.salah_masjid_edit_title);
            editMasjidPref.setLayoutResource(R.layout.settings_preference_layout);
            editMasjidPref.setIcon(R.drawable.ic_settings);
            editMasjidPref.setOnPreferenceClickListener(preference -> {
                MasjidEditDialogFragment.show(getParentFragmentManager(), masjid.id);
                return true;
            });
            masjidSection.addPreference(editMasjidPref);

            SwitchPreferenceCompat showEventsToggle = new SwitchPreferenceCompat(context);
            showEventsToggle.setTitle(R.string.salah_masjid_show_events_title);
            showEventsToggle.setLayoutResource(R.layout.settings_preference_layout);
            showEventsToggle.setChecked(masjid.showMasjidEvents);
            showEventsToggle.setOnPreferenceChangeListener((preference, newValue) -> {
                masjid.showMasjidEvents = (boolean) newValue;
                AppExecutors.getDiskIO().execute(() -> {
                    SalahDatabase.getDatabase(requireContext()).masjidDao().update(masjid);
                    SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
                });
                return true;
            });
            masjidSection.addPreference(showEventsToggle);

            addSalahPreference(masjidSection, masjid, "FAJR");
            addSalahPreference(masjidSection, masjid, "DHUHR");
            addSalahPreference(masjidSection, masjid, "ASR");
            addSalahPreference(masjidSection, masjid, "MAGHRIB");
            addSalahPreference(masjidSection, masjid, "ISHA");
        }

        // Add download card at the bottom
        Preference downloadCard = new Preference(context);
        downloadCard.setKey(KEY_DOWNLOAD_CARD);
        downloadCard.setTitle(R.string.salah_download_card);
        downloadCard.setLayoutResource(R.layout.settings_preference_layout);
        screen.addPreference(downloadCard);
    }

    private void setupFixedPreferences() {
        Preference eventsShown = findPreference(KEY_EVENTS_SHOWN);
        if (eventsShown != null) {
            eventsShown.setOnPreferenceClickListener(preference -> {
                animateAndShowFragment(new SalahEventsShownFragment());
                return true;
            });
        }

        Preference addMasjid = findPreference(KEY_ADD_MASJID);
        if (addMasjid != null) {
            addMasjid.setOnPreferenceClickListener(preference -> {
                MasjidEditDialogFragment.show(getParentFragmentManager(), 0);
                return true;
            });
        }
    }

    private void addSalahPreference(PreferenceCategory category, Masjid masjid, String type) {
        Context context = getContext();
        if (context == null) return;

        Preference pref = new Preference(context);
        pref.setTitle(SalahUtils.getLocalizedSalahName(context, type));
        pref.setLayoutResource(R.layout.settings_preference_layout);

        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            List<RelationalRule> rules = SalahDatabase.getDatabase(appContext).relationalRuleDao().getAll();
            RelationalRule masjidRule = null;
            for (RelationalRule r : rules) {
                if (r.masjidId == masjid.id && type.equals(r.masjidSalahType)) {
                    masjidRule = r;
                    break;
                }
            }
            final RelationalRule finalRule = masjidRule;
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    if (finalRule != null) {
                        pref.setSummary(finalRule.getRuleDescription(requireContext()));
                    } else {
                        pref.setSummary("");
                    }
                }
            });
        });

        pref.setOnPreferenceClickListener(preference -> {
            SalahMasjidAlarmDialogFragment.show(getParentFragmentManager(), masjid.id, type);
            return true;
        });
        category.addPreference(pref);
    }
}
