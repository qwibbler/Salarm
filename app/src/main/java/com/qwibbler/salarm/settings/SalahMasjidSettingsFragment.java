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

import com.google.android.material.button.MaterialButton;
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

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.salah_masjid_timings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.salah_masjid_settings);

        getParentFragmentManager().setFragmentResultListener("masjid_updated", this, (requestKey, result) -> loadMasjids());

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
            MasjidPreferenceCategory masjidSection = new MasjidPreferenceCategory(context, masjid, getParentFragmentManager());
            masjidSection.setTitle(masjid.name);
            screen.addPreference(masjidSection);

            addSalahPreference(masjidSection, masjid, "FAJR");
            addSalahPreference(masjidSection, masjid, "DHUHR");
            addSalahPreference(masjidSection, masjid, "JUMA");
            addSalahPreference(masjidSection, masjid, "ASR");
            addSalahPreference(masjidSection, masjid, "MAGHRIB");
            addSalahPreference(masjidSection, masjid, "ISHA");
        }
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
        Preference pref = new Preference(requireContext());
        pref.setTitle(SalahUtils.getLocalizedSalahName(requireContext(), type));
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
            if ("DHUHR".equals(type) || "JUMA".equals(type)) {
                showFixedTimeDialogue(masjid, type);
            } else {
                SalahMasjidAlarmDialogFragment.show(getParentFragmentManager(), masjid.id, type);
            }
            return true;
        });
        category.addPreference(pref);
    }

    private void showFixedTimeDialogue(Masjid masjid, String type) {
        final Context appContext = requireContext().getApplicationContext();

        // Verify type is supported for fixed time (Dhuhr or Juma only)
        if (!"DHUHR".equals(type) && !"JUMA".equals(type)) {
            LogUtils.w("Attempted to show fixed time dialogue for unsupported type: " + type);
            return;
        }

        // Fetch ringtone on main thread to avoid enforceMainLooper crash
        final Uri ringtone = DataModel.getDataModel().getAlarmRingtoneUriFromSettings(true);

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            if (db == null) {
                LogUtils.e("SalahDatabase is null in showFixedTimeDialogue");
                return;
            }
            RelationalRule rule = db.relationalRuleDao().getMasjidEventRule(masjid.id, type);
            if (rule == null) {
                rule = new RelationalRule();
                rule.masjidId = masjid.id;
                rule.masjidSalahType = type;
                rule.isFixedTime = true;

                // Try to set a sensible default from current prayer times
                double[] coords = SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], SalahUtils.getCalculationMethod(appContext));
                Date prayerTime = switch (type) {
                    case "DHUHR", "JUMA" -> st.dhuhr;
                    default -> null;
                };

                if (prayerTime != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(prayerTime);
                    rule.fixedHour = cal.get(Calendar.HOUR_OF_DAY);
                    rule.fixedMinute = cal.get(Calendar.MINUTE);
                } else {
                    rule.fixedHour = 12;
                    rule.fixedMinute = 0;
                }
                rule.id = db.relationalRuleDao().insert(rule);
            } else if (!rule.isFixedTime) {
                // Migrate to fixed time: use current evaluation if possible
                double[] coords = SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], SalahUtils.getCalculationMethod(appContext));
                Date evaluated = RelationalRule.evaluateTime(appContext, rule, st, new Date(), masjid);
                if (evaluated != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(evaluated);
                    rule.fixedHour = cal.get(Calendar.HOUR_OF_DAY);
                    rule.fixedMinute = cal.get(Calendar.MINUTE);
                } else {
                    rule.fixedHour = 12;
                    rule.fixedMinute = 0;
                }
                rule.isFixedTime = true;
                db.relationalRuleDao().update(rule);
            }

            final RelationalRule finalRule = rule;

            // Find or create native alarm
            List<Alarm> alarms = Alarm.getAlarms(appContext.getContentResolver(), Alarm.RELATIONAL_RULE_ID + "=?", String.valueOf(finalRule.id));
            if (alarms.isEmpty()) {
                Alarm nativeAlarm = Alarm.createDefaultAlarm(appContext, finalRule.fixedHour, finalRule.fixedMinute, ringtone, true);
                nativeAlarm.relationalRuleId = finalRule.id;
                nativeAlarm.enabled = false;
                nativeAlarm.label = masjid.name + " " + SalahUtils.getLocalizedSalahName(appContext, type);
                if (!nativeAlarm.tags.contains(".masjid_definition")) {
                    nativeAlarm.tags.add(".masjid_definition");
                }
                nativeAlarm.addAlarm(appContext.getContentResolver());
            } else {
                Alarm nativeAlarm = alarms.get(0);
                if (nativeAlarm != null && !nativeAlarm.tags.contains(".masjid_definition")) {
                    nativeAlarm.tags.add(".masjid_definition");
                    nativeAlarm.updateAlarm(appContext.getContentResolver());
                }
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    showTimePicker(finalRule);
                }
            });
        });
    }

    private void showTimePicker(RelationalRule rule) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());

        if (SPINNER_TIME_PICKER_STYLE.equals(SettingsDAO.getMaterialTimePickerStyle(prefs))) {
            final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(rule.fixedHour, rule.fixedMinute);
            getChildFragmentManager().setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                int h = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateRuleTime(rule, h, m);
            });
            SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment);
        } else {
            getChildFragmentManager().setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                int h = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateRuleTime(rule, h, m);
            });
            MaterialTimePickerDialogFragment.show(requireContext(), getChildFragmentManager(), "fixed_time_picker", rule.fixedHour, rule.fixedMinute, prefs);
        }
    }

    private void updateRuleTime(RelationalRule rule, int hour, int minute) {
        rule.fixedHour = hour;
        rule.fixedMinute = minute;
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(requireContext());
            db.relationalRuleDao().update(rule);

            // Also update the native alarm if it exists
            List<Alarm> alarms = Alarm.getAlarms(requireContext().getContentResolver(), Alarm.RELATIONAL_RULE_ID + "=?", String.valueOf(rule.id));
            if (!alarms.isEmpty()) {
                Alarm nativeAlarm = alarms.get(0);
                nativeAlarm.hour = hour;
                nativeAlarm.minutes = minute;
                nativeAlarm.updateAlarm(requireContext().getContentResolver());
            }

            SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    loadMasjids();
                }
            });
        });
    }

    private static class MasjidPreferenceCategory extends PreferenceCategory {
        private final Masjid masjid;
        private final FragmentManager fragmentManager;

        public MasjidPreferenceCategory(Context context, Masjid masjid, FragmentManager fragmentManager) {
            super(context);
            this.masjid = masjid;
            this.fragmentManager = fragmentManager;
            setLayoutResource(R.layout.settings_masjid_category);
        }

        @Override
        public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            View editButton = holder.findViewById(R.id.edit_masjid_button);
            if (editButton != null) {
                editButton.setOnClickListener(v -> MasjidEditDialogFragment.show(fragmentManager, masjid.id));
            }

            MaterialButton visibilityButton = (MaterialButton) holder.findViewById(R.id.visibility_masjid_button);
            if (visibilityButton != null) {
                updateVisibilityIcon(visibilityButton);
                visibilityButton.setOnClickListener(v -> {
                    masjid.showMasjidEvents = !masjid.showMasjidEvents;
                    updateVisibilityIcon(visibilityButton);

                    final Context context = getContext().getApplicationContext();
                    AppExecutors.getDiskIO().execute(() -> {
                        SalahDatabase db = SalahDatabase.getDatabase(context);
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

                        SalahAlarmSyncManager.syncSelectedRulesToAlarms(context);
                    });
                });
            }
        }

        private void updateVisibilityIcon(MaterialButton button) {
            button.setIconResource(masjid.showMasjidEvents ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        }
    }
}
