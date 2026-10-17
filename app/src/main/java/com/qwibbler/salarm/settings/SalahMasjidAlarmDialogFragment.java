package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.databinding.DialogMasjidAlarmBinding;
import com.qwibbler.salarm.dialogfragment.MaterialTimePickerDialogFragment;
import com.qwibbler.salarm.dialogfragment.SpinnerTimePickerDialogFragment;
import com.qwibbler.salarm.data.SettingsDAO;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.MasjidEvent;
import com.qwibbler.salarm.salah.RelationalAlarmUiHelper;
import com.qwibbler.salarm.salah.RelationalAnchor;
import com.qwibbler.salarm.salah.RelationalCalculation;
import com.qwibbler.salarm.salah.RelationalDefinition;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahCalculator;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahUtils;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.Utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalahMasjidAlarmDialogFragment extends DialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private static final String ARG_SALAH_TYPE = "arg_salah_type";

    private long mMasjidId;
    private String mSalahType;
    private MasjidEvent mEvent;
    private Madhab mMadhab = Madhab.SHAFI;

    private DialogMasjidAlarmBinding mBinding;

    private String[] mOffsetTypes;
    private String[] mRoundTypes;
    private String[] mAdjustmentTypes;
    private String[] mExtremeTypes;

    private int mPrimaryOffsetIdx = 1; // after
    private boolean mPrimaryIsMax = true;
    private int mRoundIdx = 0;
    private int mAdjustmentIdx = 0;

    private int mFixedHour = 12;
    private int mFixedMinute = 30;

    private final List<SelectableEvent> mSelectableEvents = new ArrayList<>();

    public static void show(FragmentManager manager, long masjidId, String salahType) {
        SalahMasjidAlarmDialogFragment fragment = new SalahMasjidAlarmDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASJID_ID, masjidId);
        args.putString(ARG_SALAH_TYPE, salahType);
        fragment.setArguments(args);
        Utils.showDialogFragment(manager, fragment, "SalahMasjidAlarmDialogFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DialogMasjidAlarmBinding.inflate(getLayoutInflater());

        mMasjidId = getArguments().getLong(ARG_MASJID_ID);
        mSalahType = getArguments().getString(ARG_SALAH_TYPE);

        final Context context = requireContext();

        mOffsetTypes = getResources().getStringArray(R.array.relational_alarm_offset_type);
        mRoundTypes = getResources().getStringArray(R.array.relational_alarm_round_type);
        mAdjustmentTypes = getResources().getStringArray(R.array.relational_alarm_adjustment_type);
        mExtremeTypes = new String[]{getString(R.string.masjid_salah_extreme_min), getString(R.string.masjid_salah_extreme_max)};

        setupInitialization();
        setupListeners();

        return CustomDialog.create(
            context,
            R.style.RelationalAlarmDialogTheme,
            null,
            getString(R.string.masjid_timing_title_format, SalahUtils.getLocalizedSalahName(context, mSalahType)),
            null,
            mBinding.getRoot(),
            getString(R.string.masjid_save),
            (dialog, which) -> saveEvent(),
            getString(android.R.string.cancel),
            null,
            getString(R.string.delete),
            (dialog, which) -> deleteEvent(),
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    private void setupInitialization() {
        RelationalAlarmUiHelper.setupNumericDropdowns(mBinding.primaryOffsetValue, mBinding.primaryRoundValue, mBinding.adjustmentValue, mBinding.dndBeforeValue, mBinding.dndAfterValue);

        final Context appContext = requireContext().getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            mEvent = db.masjidEventDao().getMasjidEvent(mMasjidId, mSalahType);

            Masjid masjid = db.masjidDao().getById(mMasjidId);
            Madhab madhab = (masjid != null) ? masjid.getMadhab(appContext) : SalahUtils.getMadhab(appContext);
            double[] coords = (masjid != null && (masjid.latitude != 0.0 || masjid.longitude != 0.0))
                    ? new double[]{masjid.latitude, masjid.longitude}
                    : SalahUtils.getCoordinates(appContext);
            CalculationMethod method = (masjid != null) ? masjid.getCalculationMethod(appContext) : SalahUtils.getCalculationMethod(appContext);
            SalahCalculator.SalahTimes salahTimes = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], method);

            if ("DHUHR".equals(mSalahType) && salahTimes.dhuhr != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(salahTimes.dhuhr);
                mFixedHour = cal.get(Calendar.HOUR_OF_DAY);
                mFixedMinute = cal.get(Calendar.MINUTE);
            } else if ("JUMA".equals(mSalahType) && salahTimes.dhuhr != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(salahTimes.dhuhr);
                mFixedHour = cal.get(Calendar.HOUR_OF_DAY);
                mFixedMinute = cal.get(Calendar.MINUTE);
            }

            SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(appContext);
            boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR, false)
                    && prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR_IN_ALARM_EVENTS, false);

            List<SelectableEvent> selectableEvents = new ArrayList<>();
            String[] originalEvents = appContext.getResources().getStringArray(R.array.relational_alarm_prayer_events);

            for (int i = 0; i < originalEvents.length; i++) {
                RelationalRule.AnchorEvent event = RelationalRule.AnchorEvent.values()[i];
                Date time = SalahCalculator.getEventTime(event, salahTimes);
                String label;
                if (!showBothAsr) {
                    if (event == RelationalRule.AnchorEvent.ASR_SHAFI) {
                        label = appContext.getString(R.string.asr_label);
                    } else if (event == RelationalRule.AnchorEvent.ASR_HANAFI) {
                        continue;
                    } else {
                        label = originalEvents[i];
                    }
                } else {
                    label = originalEvents[i];
                }
                selectableEvents.add(new SelectableEvent(label, time != null ? time.getTime() : 0, true));
            }

            Collections.sort(selectableEvents, (a, b) -> Long.compare(a.time, b.time));

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || mBinding == null) return;
                mMadhab = madhab;

                mSelectableEvents.clear();
                mSelectableEvents.addAll(selectableEvents);

                List<String> eventsList = new ArrayList<>();
                for (SelectableEvent se : mSelectableEvents) {
                    eventsList.add(se.label);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item_relational, eventsList);
                mBinding.primaryEvent.setAdapter(adapter);
                mBinding.primaryEvent.setThreshold(100);
                mBinding.primaryEvent.setOnClickListener(v -> mBinding.primaryEvent.showDropDown());

                mBinding.primaryEvent.setOnItemClickListener((parent, view, position, id) -> {
                    String selected = (String) parent.getItemAtPosition(position);
                    if (getAnchorEvent(selected) == RelationalRule.AnchorEvent.MAGHRIB) {
                        mBinding.primaryExtremeToggle.setVisibility(View.GONE);
                    } else {
                        mBinding.primaryExtremeToggle.setVisibility(View.VISIBLE);
                    }
                });

                if (mEvent != null) {
                    populateUi();
                } else {
                    initializeUiWithDefaults();
                }
            });
        });
    }

    private void setupListeners() {
        mBinding.fixedTimeButton.setOnClickListener(v -> showTimePicker());

        mBinding.primaryOffsetToggle.setOnClickListener(v -> {
            mPrimaryOffsetIdx = (mPrimaryOffsetIdx + 1) % mOffsetTypes.length;
            updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
        });
        mBinding.primaryExtremeToggle.setOnClickListener(v -> {
            mPrimaryIsMax = !mPrimaryIsMax;
            updateToggle(mBinding.primaryExtremeToggle, mExtremeTypes, mPrimaryIsMax ? 1 : 0);
        });

        mBinding.primaryRoundEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> {
            updateBlockState(checked, mBinding.primaryRoundContainer);
            updateAdjustmentVisibility();
        });
        mBinding.primaryRoundToggle.setOnClickListener(v -> {
            mRoundIdx = (mRoundIdx + 1) % mRoundTypes.length;
            updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mRoundIdx);
        });

        mBinding.adjustmentEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> updateBlockState(checked, mBinding.adjustmentContainer));
        mBinding.adjustmentToggle.setOnClickListener(v -> {
            mAdjustmentIdx = (mAdjustmentIdx + 1) % mAdjustmentTypes.length;
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);
        });

        mBinding.dndEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> updateBlockState(checked, mBinding.dndContainer));
    }

    private void showTimePicker() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        if (SPINNER_TIME_PICKER_STYLE.equals(SettingsDAO.getMaterialTimePickerStyle(prefs))) {
            final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(mFixedHour, mFixedMinute);
            getChildFragmentManager().setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                mFixedHour = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                mFixedMinute = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateFixedTimeButton();
            });
            SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment);
        } else {
            getChildFragmentManager().setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                mFixedHour = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                mFixedMinute = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateFixedTimeButton();
            });
            MaterialTimePickerDialogFragment.show(requireContext(), getChildFragmentManager(), "fixed_time_picker", mFixedHour, mFixedMinute, prefs);
        }
    }

    private void updateFixedTimeButton() {
        if (mBinding == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, mFixedHour);
        cal.set(Calendar.MINUTE, mFixedMinute);
        DateFormat df = android.text.format.DateFormat.getTimeFormat(requireContext());
        if (df != null) {
            mBinding.fixedTimeButton.setText(df.format(cal.getTime()));
        } else {
            mBinding.fixedTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", mFixedHour, mFixedMinute));
        }
    }

    private void updateToggle(TextView v, String[] vals, int idx) {
        if (idx >= 0 && idx < vals.length) v.setText(vals[idx]);
    }

    private void updateBlockState(boolean enabled, View v) {
        if (v != null) v.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void updateAdjustmentVisibility() {
        boolean roundingEnabled = mBinding.primaryRoundEnabledCheckbox.isChecked();
        mBinding.adjustmentEnabledCheckbox.setVisibility(roundingEnabled ? View.VISIBLE : View.GONE);
        if (!roundingEnabled) {
            mBinding.adjustmentEnabledCheckbox.setChecked(false);
            mBinding.adjustmentContainer.setVisibility(View.GONE);
        } else {
            updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
        }
    }

    private void initializeUiWithDefaults() {
        boolean isFixed = "DHUHR".equals(mSalahType) || "JUMA".equals(mSalahType) || (mEvent != null && mEvent.definition != null && mEvent.definition.isFixedTime);

        if (isFixed) {
            mBinding.fixedTimeContainer.setVisibility(View.VISIBLE);
            mBinding.primaryOffsetContainer.setVisibility(View.GONE);
            mBinding.primaryRoundEnabledCheckbox.setVisibility(View.GONE);
            mBinding.primaryRoundContainer.setVisibility(View.GONE);
            mBinding.adjustmentEnabledCheckbox.setVisibility(View.GONE);
            mBinding.adjustmentContainer.setVisibility(View.GONE);
            updateFixedTimeButton();
        } else {
            mBinding.fixedTimeContainer.setVisibility(View.GONE);
            mBinding.primaryOffsetContainer.setVisibility(View.VISIBLE);
            mBinding.primaryRoundEnabledCheckbox.setVisibility(View.VISIBLE);
            if ("MAGHRIB".equals(mSalahType)) {
                mBinding.primaryOffsetValue.setText("8");
                mBinding.primaryExtremeToggle.setVisibility(View.GONE);
            } else {
                mBinding.primaryOffsetValue.setText("0");
                mBinding.primaryExtremeToggle.setVisibility(View.VISIBLE);
            }
            updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, 1);
            updateToggle(mBinding.primaryExtremeToggle, mExtremeTypes, 1);
            mBinding.primaryEvent.setText(SalahUtils.getLocalizedSalahName(requireContext(), mSalahType), false);

            mBinding.primaryRoundEnabledCheckbox.setChecked(false);
            updateToggle(mBinding.primaryRoundToggle, mRoundTypes, 0);
            mBinding.primaryRoundValue.setText("0");

            mBinding.adjustmentEnabledCheckbox.setChecked(false);
            mBinding.adjustmentValue.setText("0");
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, 0);

            updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
            updateAdjustmentVisibility();
        }

        // DND defaults
        mBinding.dndEnabledCheckbox.setChecked(false);
        mBinding.dndBeforeValue.setText(String.valueOf(0));
        mBinding.dndAfterValue.setText(String.valueOf(10));
        updateBlockState(false, mBinding.dndContainer);
    }

    private void populateUi() {
        initializeUiWithDefaults();

        if (mEvent != null && mEvent.definition != null) {
            if (mEvent.definition.isFixedTime) {
                mFixedHour = mEvent.definition.fixedHour;
                mFixedMinute = mEvent.definition.fixedMinute;
                updateFixedTimeButton();
            } else {
                if (mEvent.definition.calculation != null) {
                    mBinding.primaryOffsetValue.setText(String.valueOf(Math.abs(mEvent.definition.calculation.offsetMinutes)));
                    mPrimaryOffsetIdx = mEvent.definition.calculation.offsetMinutes >= 0 ? 1 : 0;
                    updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
                }
                if (mEvent.definition.anchor != null) {
                    mPrimaryIsMax = mEvent.definition.anchor.isMax;
                    updateToggle(mBinding.primaryExtremeToggle, mExtremeTypes, mPrimaryIsMax ? 1 : 0);
                    if (mEvent.definition.anchor.basicEvent != null) {
                        mBinding.primaryEvent.setText(RelationalRule.getEventName(requireContext(), mEvent.definition.anchor.basicEvent), false);

                        if ("MAGHRIB".equals(mSalahType) || mEvent.definition.anchor.basicEvent == RelationalRule.AnchorEvent.MAGHRIB) {
                            mBinding.primaryExtremeToggle.setVisibility(View.GONE);
                        } else {
                            mBinding.primaryExtremeToggle.setVisibility(View.VISIBLE);
                        }
                    }
                }

                if (mEvent.definition.calculation != null) {
                    boolean hasRounding = mEvent.definition.calculation.roundingIntervalMinutes > 0 && mEvent.definition.calculation.roundingDirection != null;
                    mBinding.primaryRoundEnabledCheckbox.setChecked(hasRounding);
                    mRoundIdx = mEvent.definition.calculation.roundingDirection != null ? mEvent.definition.calculation.roundingDirection.ordinal() : 0;
                    updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mRoundIdx);
                    mBinding.primaryRoundValue.setText(String.valueOf(mEvent.definition.calculation.roundingIntervalMinutes));

                    if (hasRounding) {
                        mBinding.adjustmentEnabledCheckbox.setChecked(mEvent.definition.calculation.adjustmentMinutes != 0);
                        mBinding.adjustmentValue.setText(String.valueOf(Math.abs(mEvent.definition.calculation.adjustmentMinutes)));
                        mAdjustmentIdx = mEvent.definition.calculation.adjustmentMinutes >= 0 ? 1 : 0;
                        updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);
                    } else {
                        mBinding.adjustmentEnabledCheckbox.setChecked(false);
                        mBinding.adjustmentValue.setText("0");
                    }
                }
            }

            mBinding.dndEnabledCheckbox.setChecked(mEvent.dndEnabled);
            mBinding.dndBeforeValue.setText(String.valueOf(mEvent.dndBeforeMinutes));
            mBinding.dndAfterValue.setText(String.valueOf(mEvent.dndAfterMinutes));
            updateBlockState(mEvent.dndEnabled, mBinding.dndContainer);
        }
        if (!("DHUHR".equals(mSalahType) || "JUMA".equals(mSalahType) || (mEvent != null && mEvent.definition != null && mEvent.definition.isFixedTime))) {
            updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
            updateAdjustmentVisibility();
        }
    }

    private void saveEvent() {
        MasjidEvent event = mEvent != null ? mEvent : new MasjidEvent();
        event.masjidId = mMasjidId;
        event.salahType = mSalahType;
        if (event.definition == null) event.definition = new RelationalDefinition();

        boolean isFixed = "DHUHR".equals(mSalahType) || "JUMA".equals(mSalahType);
        event.definition.isFixedTime = isFixed;

        if (isFixed) {
            event.definition.fixedHour = mFixedHour;
            event.definition.fixedMinute = mFixedMinute;
            event.definition.anchor = null;
            event.definition.calculation = null;
        } else {
            if (event.definition.anchor == null) event.definition.anchor = new RelationalAnchor();
            if (event.definition.calculation == null) event.definition.calculation = new RelationalCalculation();

            event.definition.anchor.basicEvent = getAnchorEvent(mBinding.primaryEvent.getText().toString());
            int offset = parseSafeInt(mBinding.primaryOffsetValue.getText().toString(), 0);
            event.definition.calculation.offsetMinutes = mPrimaryOffsetIdx == 1 ? offset : -offset;
            event.definition.anchor.isMax = mPrimaryIsMax;

            if (mBinding.primaryRoundEnabledCheckbox.isChecked()) {
                event.definition.calculation.roundingIntervalMinutes = parseSafeInt(mBinding.primaryRoundValue.getText().toString(), 0);
                if (mRoundIdx >= 0 && mRoundIdx < RelationalRule.RoundingDirection.values().length) {
                    event.definition.calculation.roundingDirection = RelationalRule.RoundingDirection.values()[mRoundIdx];
                }
                if (mBinding.adjustmentEnabledCheckbox.isChecked()) {
                    int adj = parseSafeInt(mBinding.adjustmentValue.getText().toString(), 0);
                    event.definition.calculation.adjustmentMinutes = mAdjustmentIdx == 1 ? adj : -adj;
                } else {
                    event.definition.calculation.adjustmentMinutes = 0;
                }
            } else {
                event.definition.calculation.roundingIntervalMinutes = 0;
                event.definition.calculation.roundingDirection = null;
                event.definition.calculation.adjustmentMinutes = 0;
            }
        }

        event.definition.clip = null;

        event.dndEnabled = mBinding.dndEnabledCheckbox.isChecked();
        event.dndBeforeMinutes = parseSafeInt(mBinding.dndBeforeValue.getText().toString(), 0);
        event.dndAfterMinutes = parseSafeInt(mBinding.dndAfterValue.getText().toString(), 10);

        if (event.dndEnabled) {
            NotificationManager nm = requireContext().getSystemService(NotificationManager.class);
            if (nm != null && !nm.isNotificationPolicyAccessGranted()) {
                Toast.makeText(requireContext(), R.string.dnd_permission_required, Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                } catch (Exception ignored) {}
            }
        }

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(requireContext());
            Masjid masjid = db.masjidDao().getById(mMasjidId);
            if (event.id == 0 && masjid != null) {
                event.isSelected = masjid.showMasjidEvents;
            }

            if (event.id == 0) {
                event.id = db.masjidEventDao().insert(event);
            } else {
                db.masjidEventDao().update(event);
            }

            SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
            getParentFragmentManager().setFragmentResult("masjid_updated", new Bundle());
        });
    }

    private void deleteEvent() {
        if (mEvent != null) {
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase.getDatabase(requireContext()).masjidEventDao().delete(mEvent);
                SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
                getParentFragmentManager().setFragmentResult("masjid_updated", new Bundle());
            });
        }
    }

    private RelationalRule.AnchorEvent getAnchorEvent(String name) {
        if (name == null || name.isEmpty() || !isAdded()) return RelationalRule.AnchorEvent.FAJR;
        if (getString(R.string.asr_label).equals(name)) {
            return mMadhab == Madhab.HANAFI ? RelationalRule.AnchorEvent.ASR_HANAFI : RelationalRule.AnchorEvent.ASR_SHAFI;
        }
        String[] events = getResources().getStringArray(R.array.relational_alarm_prayer_events);
        for (int i = 0; i < events.length; i++) {
            if (events[i].equals(name)) {
                return RelationalRule.AnchorEvent.values()[i];
            }
        }
        // Fallback checks using localized names
        Context context = requireContext();
        if (name.equals(SalahUtils.getLocalizedSalahName(context, "FAJR"))) return RelationalRule.AnchorEvent.FAJR;
        if (name.equals(SalahUtils.getLocalizedSalahName(context, "SUNRISE"))) return RelationalRule.AnchorEvent.SUNRISE;
        if (name.equals(SalahUtils.getLocalizedSalahName(context, "DHUHR"))) return RelationalRule.AnchorEvent.DHUHR;
        if (name.equals(SalahUtils.getLocalizedSalahName(context, "ASR"))) return RelationalRule.AnchorEvent.ASR_HANAFI;
        if (name.equals(SalahUtils.getLocalizedSalahName(context, "MAGHRIB"))) return RelationalRule.AnchorEvent.MAGHRIB;
        if (name.equals(SalahUtils.getLocalizedSalahName(context, "ISHA"))) return RelationalRule.AnchorEvent.ISHA;

        return RelationalRule.AnchorEvent.FAJR;
    }

    private static int parseSafeInt(String str, int defaultValue) {
        if (str == null || str.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private static class SelectableEvent {
        final String label;
        final long time;
        final boolean isBasic;

        SelectableEvent(String label, long time, boolean isBasic) {
            this.label = label;
            this.time = time;
            this.isBasic = isBasic;
        }
    }
}
