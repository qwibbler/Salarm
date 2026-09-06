package com.qwibbler.salarm.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
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
            MasjidPreferenceCategory masjidSection = new MasjidPreferenceCategory(context, masjid.id, getParentFragmentManager());
            masjidSection.setTitle(masjid.name);
            screen.addPreference(masjidSection);

            SwitchPreferenceCompat showEventsToggle = getSwitchPreferenceCompat(masjid, context);
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

    @NonNull
    private SwitchPreferenceCompat getSwitchPreferenceCompat(Masjid masjid, Context context) {
        SwitchPreferenceCompat showEventsToggle = new SwitchPreferenceCompat(context);
        showEventsToggle.setTitle(R.string.salah_masjid_show_events_title);
        showEventsToggle.setLayoutResource(R.layout.settings_preference_layout);
        showEventsToggle.setIconSpaceReserved(false);
        showEventsToggle.setChecked(masjid.showMasjidEvents);
        showEventsToggle.setOnPreferenceChangeListener((preference, newValue) -> {
            masjid.showMasjidEvents = (boolean) newValue;
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase db = SalahDatabase.getDatabase(requireContext());
                db.masjidDao().update(masjid);

                List<RelationalRule> rules = db.relationalRuleDao().getByMasjidId(masjid.id);
                for (RelationalRule rule : rules) {
                    if (masjid.showMasjidEvents) {
                        if (!rule.isManuallyDeselected) {
                            rule.isSelected = true;
                        }
                    } else {
                        rule.isSelected = false;
                    }
                    db.relationalRuleDao().update(rule);
                }

                SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
            });
            return true;
        });
        return showEventsToggle;
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
        pref.setIconSpaceReserved(false);

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

    private static class MasjidPreferenceCategory extends PreferenceCategory {
        private final long masjidId;
        private final FragmentManager fragmentManager;

        public MasjidPreferenceCategory(Context context, long masjidId, FragmentManager fragmentManager) {
            super(context);
            this.masjidId = masjidId;
            this.fragmentManager = fragmentManager;
            setLayoutResource(R.layout.settings_masjid_category);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            View editButton = holder.findViewById(R.id.edit_masjid_button);
            if (editButton != null) {
                editButton.setOnClickListener(v -> {
                    MasjidEditDialogFragment.show(fragmentManager, masjidId);
                });
            }
        }
    }
}
