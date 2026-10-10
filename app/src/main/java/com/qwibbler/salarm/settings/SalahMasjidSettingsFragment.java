package com.qwibbler.salarm.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.MasjidEvent;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahUtils;
import com.qwibbler.salarm.salah.SalahCalculator;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.dialogfragment.MaterialTimePickerDialogFragment;
import com.qwibbler.salarm.dialogfragment.SpinnerTimePickerDialogFragment;
import com.qwibbler.salarm.data.SettingsDAO;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;

import com.qwibbler.salarm.salah.ui.MasjidCardEditorActivity;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

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
        // Don't add preferences from resource here, we'll do it in populateMasjids
        // to ensure a clean refresh.

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
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            List<Masjid> masjids = SalahDatabase.getDatabase(appContext).masjidDao().getAll();
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

        setupFixedPreferences();

        for (Masjid masjid : masjids) {
            MasjidPreferenceCategory masjidSection = new MasjidPreferenceCategory(context, masjid, getParentFragmentManager(), this::loadMasjids);
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
        Context context = getContext();
        if (context == null) return;
        Preference pref = new Preference(context);
        pref.setTitle(SalahUtils.getLocalizedSalahName(context, type));
        pref.setLayoutResource(R.layout.settings_preference_layout);
        pref.setIconSpaceReserved(false);

        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            MasjidEvent masjidEvent = SalahDatabase.getDatabase(appContext).masjidEventDao().getMasjidEvent(masjid.id, type);
            String summary = "";
            if (masjidEvent != null) {
                summary = getEventDescription(appContext, masjidEvent);
            }
            final String finalSummary = summary;
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    pref.setSummary(finalSummary);
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

    private String getEventDescription(Context context, MasjidEvent event) {
        String anchorName = RelationalRule.getAnchorDescription(context, event.definition.anchor);
        return event.definition.getSummary(context, anchorName, true);
    }

    private void showFixedTimeDialogue(Masjid masjid, String type) {
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        // Verify type is supported for fixed time (Dhuhr or Juma only)
        if (!"DHUHR".equals(type) && !"JUMA".equals(type)) {
            LogUtils.w("Attempted to show fixed time dialogue for unsupported type: " + type);
            return;
        }

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            MasjidEvent event = db.masjidEventDao().getMasjidEvent(masjid.id, type);
            if (event == null) {
                event = new MasjidEvent();
                event.masjidId = masjid.id;
                event.salahType = type;
                event.definition.isFixedTime = true;
                event.isSelected = masjid.showMasjidEvents;

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
                    event.definition.fixedHour = cal.get(Calendar.HOUR_OF_DAY);
                    event.definition.fixedMinute = cal.get(Calendar.MINUTE);
                } else {
                    event.definition.fixedHour = 12;
                    event.definition.fixedMinute = 0;
                }
                event.id = db.masjidEventDao().insert(event);
            } else if (!event.definition.isFixedTime) {
                double[] coords = SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], SalahUtils.getCalculationMethod(appContext));

                long evaluated = SalahCalculator.evaluateMasjidEvent(appContext, event, st, new Date(), true);
                if (evaluated > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(evaluated);
                    event.definition.fixedHour = cal.get(Calendar.HOUR_OF_DAY);
                    event.definition.fixedMinute = cal.get(Calendar.MINUTE);
                } else {
                    event.definition.fixedHour = 12;
                    event.definition.fixedMinute = 0;
                }
                event.definition.isFixedTime = true;
                db.masjidEventDao().update(event);
            }

            final MasjidEvent finalEvent = event;

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    showTimePicker(finalEvent);
                }
            });
        });
    }

    private void showTimePicker(MasjidEvent event) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());

        if (SPINNER_TIME_PICKER_STYLE.equals(SettingsDAO.getMaterialTimePickerStyle(prefs))) {
            final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(event.definition.fixedHour, event.definition.fixedMinute);
            getChildFragmentManager().setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                int h = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateEventTime(event, h, m);
            });
            SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment);
        } else {
            getChildFragmentManager().setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                int h = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateEventTime(event, h, m);
            });
            MaterialTimePickerDialogFragment.show(requireContext(), getChildFragmentManager(), "fixed_time_picker", event.definition.fixedHour, event.definition.fixedMinute, prefs);
        }
    }

    private void updateEventTime(MasjidEvent event, int hour, int minute) {
        event.definition.fixedHour = hour;
        event.definition.fixedMinute = minute;
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            db.masjidEventDao().update(event);

            SalahAlarmSyncManager.syncSelectedRulesToAlarms(appContext);
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
        private final Runnable updateCallback;

        public MasjidPreferenceCategory(Context context, Masjid masjid, FragmentManager fragmentManager, Runnable updateCallback) {
            super(context);
            this.masjid = masjid;
            this.fragmentManager = fragmentManager;
            this.updateCallback = updateCallback;
            setLayoutResource(R.layout.settings_masjid_category);
        }

        @Override
        public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            View editButton = holder.findViewById(R.id.edit_masjid_button);
            if (editButton != null) {
                editButton.setOnClickListener(v -> MasjidEditDialogFragment.show(fragmentManager, masjid.id));
            }

            View downloadButton = holder.findViewById(R.id.download_card);
            if (downloadButton != null) {
                downloadButton.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), MasjidCardEditorActivity.class);
                    intent.putExtra("masjid_id", masjid.id);
                    getContext().startActivity(intent);
                });
            }

            MaterialButton visibilityButton = (MaterialButton) holder.findViewById(R.id.visibility_masjid_button);
            if (visibilityButton != null) {
                updateVisibilityIcon(visibilityButton);
                visibilityButton.setOnClickListener(v -> {
                    masjid.showMasjidEvents = !masjid.showMasjidEvents;
                    updateVisibilityIcon(visibilityButton);

                    Context ctx = getContext();
                    final Context context = ctx.getApplicationContext();
                    AppExecutors.getDiskIO().execute(() -> {
                        SalahDatabase db = SalahDatabase.getDatabase(context);
                        db.masjidDao().update(masjid);

                        List<MasjidEvent> events = db.masjidEventDao().getByMasjidId(masjid.id);
                        for (MasjidEvent event : events) {
                            if (masjid.showMasjidEvents) {
                                if (!event.isManuallyDeselected) {
                                    event.isSelected = true;
                                }
                            } else {
                                event.isSelected = false;
                            }
                            db.masjidEventDao().update(event);
                        }

                        SalahAlarmSyncManager.syncSelectedRulesToAlarms(context);
                        if (updateCallback != null) {
                            AppExecutors.getMainThread().post(updateCallback);
                        }
                    });
                });
            }
        }

        private void updateVisibilityIcon(MaterialButton button) {
            button.setIconResource(masjid.showMasjidEvents ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        }
    }
}
