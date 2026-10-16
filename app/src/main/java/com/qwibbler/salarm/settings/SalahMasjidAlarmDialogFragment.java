package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SalahMasjidAlarmDialogFragment extends DialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private static final String ARG_SALAH_TYPE = "arg_salah_type";

    private long mMasjidId;
    private String mSalahType;
    private MasjidEvent mEvent;

    private DialogMasjidAlarmBinding mBinding;

    private String[] mOffsetTypes;
    private String[] mRoundTypes;
    private String[] mAdjustmentTypes;
    private String[] mExtremeTypes;

    private int mPrimaryOffsetIdx = 1; // after
    private boolean mPrimaryIsMax = true;
    private int mRoundIdx = 0;
    private int mAdjustmentIdx = 0;

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
        RelationalAlarmUiHelper.setupNumericDropdowns(mBinding.primaryOffsetValue, mBinding.primaryRoundValue, mBinding.adjustmentValue);

        final Context appContext = requireContext().getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            mEvent = db.masjidEventDao().getMasjidEvent(mMasjidId, mSalahType);

            Masjid masjid = db.masjidDao().getById(mMasjidId);
            double[] coords = (masjid != null && (masjid.latitude != 0.0 || masjid.longitude != 0.0))
                    ? new double[]{masjid.latitude, masjid.longitude}
                    : SalahUtils.getCoordinates(appContext);
            CalculationMethod method = (masjid != null) ? masjid.getCalculationMethod(appContext) : SalahUtils.getCalculationMethod(appContext);
            SalahCalculator.SalahTimes salahTimes = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], method);

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

    private void populateUi() {
        initializeUiWithDefaults();

        if (mEvent != null && mEvent.definition != null) {
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
        updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
        updateAdjustmentVisibility();
    }

    private void saveEvent() {
        MasjidEvent event = mEvent != null ? mEvent : new MasjidEvent();
        event.masjidId = mMasjidId;
        event.salahType = mSalahType;
        if (event.definition == null) event.definition = new RelationalDefinition();
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

        event.definition.clip = null;

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
            Madhab madhab;
            if (mMasjidId > 0) {
                Masjid masjid = SalahDatabase.getDatabase(requireContext()).masjidDao().getById(mMasjidId);
                madhab = (masjid != null) ? masjid.getMadhab(requireContext()) : SalahUtils.getMadhab(requireContext());
            } else {
                madhab = SalahUtils.getMadhab(requireContext());
            }
            return madhab == Madhab.HANAFI ? RelationalRule.AnchorEvent.ASR_HANAFI : RelationalRule.AnchorEvent.ASR_SHAFI;
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
