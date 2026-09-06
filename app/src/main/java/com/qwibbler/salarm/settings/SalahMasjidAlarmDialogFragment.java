package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.databinding.DialogMasjidAlarmBinding;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.RelationalAlarmUiHelper;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahUtils;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SalahMasjidAlarmDialogFragment extends DialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private static final String ARG_SALAH_TYPE = "arg_salah_type";

    private long mMasjidId;
    private String mSalahType;
    private RelationalRule mRule;

    private DialogMasjidAlarmBinding mBinding;

    private String[] mOffsetTypes;
    private String[] mRoundTypes;
    private String[] mBoundaryTypes;
    private String[] mAdjustmentTypes;
    private String[] mExtremeTypes;

    private int mPrimaryOffsetIdx = 1; // after
    private boolean mPrimaryIsMax = true;
    private int mRoundIdx = 0;
    private int mAdjustmentIdx = 0;
    private int mSafetyBoundIdx = 1; // later than
    private int mSafetyOffsetIdx = 0; // before
    private boolean mSafetyIsMax = false;

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

        mOffsetTypes = getResources().getStringArray(R.array.relational_alarm_offset_type);
        mRoundTypes = getResources().getStringArray(R.array.relational_alarm_round_type);
        mBoundaryTypes = getResources().getStringArray(R.array.relational_alarm_boundary_type);
        mAdjustmentTypes = getResources().getStringArray(R.array.relational_alarm_adjustment_type);
        mExtremeTypes = new String[]{getString(R.string.salah_extreme_min), getString(R.string.salah_extreme_max)};

        setupAdapters();
        setupListeners();

        loadRule();

        return CustomDialog.create(
                requireContext(),
                R.style.RelationalAlarmDialogTheme,
                null,
                getString(R.string.salah_masjid_timing_title_format, SalahUtils.getLocalizedSalahName(requireContext(), mSalahType)),
                null,
                mBinding.getRoot(),
                getString(R.string.salah_masjid_save),
                (dialog, which) -> saveRule(),
                getString(android.R.string.cancel),
                null,
                getString(R.string.delete),
                (dialog, which) -> deleteRule(),
                null,
                CustomDialog.SoftInputMode.NONE
        );
    }

    private void setupAdapters() {
        RelationalAlarmUiHelper.setupNumericDropdowns(mBinding.primaryOffsetValue, mBinding.primaryRoundValue, mBinding.adjustmentValue, mBinding.safetyOffsetValue);

        android.content.SharedPreferences prefs = com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences(requireContext());
        boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR, false)
                && prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR_IN_ALARM_EVENTS, false);

        List<String> eventsList = new ArrayList<>();
        String[] originalEvents = getResources().getStringArray(R.array.relational_alarm_prayer_events);
        if (showBothAsr) {
            eventsList.addAll(Arrays.asList(originalEvents));
        } else {
            for (int i = 0; i < originalEvents.length; i++) {
                if (i == 3) { // ASR_SHAFI
                    eventsList.add(getString(R.string.asr_label));
                } else if (i != 4) { // Not ASR_HANAFI
                    eventsList.add(originalEvents[i]);
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item_relational, eventsList);
        mBinding.primaryEvent.setAdapter(adapter);
        mBinding.primaryEvent.setThreshold(100);
        mBinding.primaryEvent.setOnClickListener(v -> mBinding.primaryEvent.showDropDown());

        mBinding.safetyEvent.setAdapter(adapter);
        mBinding.safetyEvent.setThreshold(100);
        mBinding.safetyEvent.setOnClickListener(v -> mBinding.safetyEvent.showDropDown());
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

        mBinding.primaryRoundEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> updateBlockState(checked, mBinding.primaryRoundContainer));
        mBinding.primaryRoundToggle.setOnClickListener(v -> {
            mRoundIdx = (mRoundIdx + 1) % mRoundTypes.length;
            updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mRoundIdx);
        });

        mBinding.adjustmentEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> updateBlockState(checked, mBinding.adjustmentContainer));
        mBinding.adjustmentToggle.setOnClickListener(v -> {
            mAdjustmentIdx = (mAdjustmentIdx + 1) % mAdjustmentTypes.length;
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);
        });

        mBinding.safetyEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> updateBlockState(checked, mBinding.safetyContent));
        mBinding.safetyBoundaryToggle.setOnClickListener(v -> {
            mSafetyBoundIdx = (mSafetyBoundIdx + 1) % mBoundaryTypes.length;
            updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
        });
        mBinding.safetyOffsetToggle.setOnClickListener(v -> {
            mSafetyOffsetIdx = (mSafetyOffsetIdx + 1) % mOffsetTypes.length;
            updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
        });
        mBinding.safetyExtremeToggle.setOnClickListener(v -> {
            mSafetyIsMax = !mSafetyIsMax;
            updateToggle(mBinding.safetyExtremeToggle, mExtremeTypes, mSafetyIsMax ? 1 : 0);
        });
    }

    private void updateToggle(TextView v, String[] vals, int idx) {
        if (idx >= 0 && idx < vals.length) v.setText(vals[idx]);
    }

    private void updateBlockState(boolean enabled, View v) {
        if (v != null) v.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void loadRule() {
        AppExecutors.getDiskIO().execute(() -> {
            List<RelationalRule> rules = SalahDatabase.getDatabase(requireContext()).relationalRuleDao().getAll();
            for (RelationalRule r : rules) {
                if (r.masjidId == mMasjidId && mSalahType.equals(r.masjidSalahType)) {
                    mRule = r;
                    break;
                }
            }
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    populateUi();
                }
            });
        });
    }

    private void populateUi() {
        if (mRule == null) {
            // Defaults
            mBinding.primaryOffsetValue.setText("0");
            updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, 1);
            updateToggle(mBinding.primaryExtremeToggle, mExtremeTypes, 1);
            mBinding.primaryEvent.setText(SalahUtils.getLocalizedSalahName(requireContext(), mSalahType), false);

            mBinding.primaryRoundEnabledCheckbox.setChecked(false);
            updateToggle(mBinding.primaryRoundToggle, mRoundTypes, 0);
            mBinding.primaryRoundValue.setText("0");

            mBinding.adjustmentEnabledCheckbox.setChecked(false);
            mBinding.adjustmentValue.setText("0");
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, 0);

            mBinding.safetyEnabledCheckbox.setChecked(false);
            updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, 1);
            mBinding.safetyOffsetValue.setText("0");
            updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, 0);
            updateToggle(mBinding.safetyExtremeToggle, mExtremeTypes, 0);
            // Default next event logic
            mBinding.safetyEvent.setText(getDefaultNextEvent(mSalahType), false);
        } else {
            mBinding.primaryOffsetValue.setText(String.valueOf(Math.abs(mRule.offsetMinutes)));
            mPrimaryOffsetIdx = mRule.offsetMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
            mPrimaryIsMax = mRule.anchorIsMax;
            updateToggle(mBinding.primaryExtremeToggle, mExtremeTypes, mPrimaryIsMax ? 1 : 0);
            mBinding.primaryEvent.setText(RelationalRule.getEventName(requireContext(), mRule.anchorEvent), false);

            mBinding.primaryRoundEnabledCheckbox.setChecked(mRule.roundingIntervalMinutes > 0);
            mRoundIdx = mRule.roundingDirection != null ? mRule.roundingDirection.ordinal() : 0;
            updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mRoundIdx);
            mBinding.primaryRoundValue.setText(String.valueOf(mRule.roundingIntervalMinutes));

            mBinding.adjustmentEnabledCheckbox.setChecked(mRule.adjustmentMinutes != 0);
            mBinding.adjustmentValue.setText(String.valueOf(Math.abs(mRule.adjustmentMinutes)));
            mAdjustmentIdx = mRule.adjustmentMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);

            if (mRule.clip != null) {
                mBinding.safetyEnabledCheckbox.setChecked(true);
                mSafetyBoundIdx = mRule.clip.boundaryType == RelationalRule.BoundaryType.NO_LATER_THAN ? 1 : 0;
                updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
                mBinding.safetyOffsetValue.setText(String.valueOf(Math.abs(mRule.clip.clipOffsetMinutes)));
                mSafetyOffsetIdx = mRule.clip.clipOffsetMinutes >= 0 ? 1 : 0;
                updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
                mSafetyIsMax = mRule.clipIsMax;
                updateToggle(mBinding.safetyExtremeToggle, mExtremeTypes, mSafetyIsMax ? 1 : 0);
                mBinding.safetyEvent.setText(RelationalRule.getEventName(requireContext(), mRule.clip.clipEvent), false);
            }
        }
        updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
        updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
    }

    private String getDefaultNextEvent(String current) {
        Context context = getContext();
        if (context == null) return "Sunrise";
        return switch (current) {
            case "FAJR" -> SalahUtils.getLocalizedSalahName(context, "SUNRISE");
            case "DHUHR" -> SalahUtils.getLocalizedSalahName(context, "ASR");
            case "ASR" -> SalahUtils.getLocalizedSalahName(context, "MAGHRIB");
            case "MAGHRIB" -> SalahUtils.getLocalizedSalahName(context, "ISHA");
            case "ISHA" -> SalahUtils.getLocalizedSalahName(context, "FAJR");
            default -> SalahUtils.getLocalizedSalahName(context, "SUNRISE");
        };
    }

    private void saveRule() {
        RelationalRule rule = mRule != null ? mRule : new RelationalRule();
        rule.masjidId = mMasjidId;
        rule.masjidSalahType = mSalahType;
        rule.enabled = true;

        rule.anchorEvent = getAnchorEvent(mBinding.primaryEvent.getText().toString());
        int offset = Integer.parseInt(mBinding.primaryOffsetValue.getText().toString());
        rule.offsetMinutes = mPrimaryOffsetIdx == 1 ? offset : -offset;
        rule.anchorIsMax = mPrimaryIsMax;

        if (mBinding.primaryRoundEnabledCheckbox.isChecked()) {
            rule.roundingIntervalMinutes = Integer.parseInt(mBinding.primaryRoundValue.getText().toString());
            rule.roundingDirection = RelationalRule.RoundingDirection.values()[mRoundIdx];
        } else {
            rule.roundingIntervalMinutes = 0;
        }

        if (mBinding.adjustmentEnabledCheckbox.isChecked()) {
            int adj = Integer.parseInt(mBinding.adjustmentValue.getText().toString());
            rule.adjustmentMinutes = mAdjustmentIdx == 1 ? adj : -adj;
        } else {
            rule.adjustmentMinutes = 0;
        }

        if (mBinding.safetyEnabledCheckbox.isChecked()) {
            RelationalRule.Clip clip = new RelationalRule.Clip();
            clip.boundaryType = mSafetyBoundIdx == 1 ? RelationalRule.BoundaryType.NO_LATER_THAN : RelationalRule.BoundaryType.NO_EARLIER_THAN;
            clip.clipEvent = getAnchorEvent(mBinding.safetyEvent.getText().toString());
            clip.clipOffsetMinutes = Integer.parseInt(mBinding.safetyOffsetValue.getText().toString());
            if (mSafetyOffsetIdx == 0) clip.clipOffsetMinutes *= -1;
            rule.clip = clip;
            rule.clipIsMax = mSafetyIsMax;
        } else {
            rule.clip = null;
        }

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(requireContext());
            Masjid masjid = db.masjidDao().getById(mMasjidId);
            if (rule.id == 0 && masjid != null) {
                rule.isSelected = masjid.showMasjidEvents;
            }

            if (rule.id == 0) {
                db.relationalRuleDao().insert(rule);
            } else {
                db.relationalRuleDao().update(rule);
            }
            SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
            getParentFragmentManager().setFragmentResult("masjid_updated", new Bundle());
        });
    }

    private void deleteRule() {
        if (mRule != null) {
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase.getDatabase(requireContext()).relationalRuleDao().delete(mRule);
                SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
                getParentFragmentManager().setFragmentResult("masjid_updated", new Bundle());
            });
        }
    }

    private RelationalRule.AnchorEvent getAnchorEvent(String name) {
        if (getString(R.string.asr_label).equals(name)) {
            com.batoulapps.adhan.Madhab madhab = com.qwibbler.salarm.salah.SalahUtils.getMadhab(requireContext());
            return madhab == com.batoulapps.adhan.Madhab.HANAFI ? RelationalRule.AnchorEvent.ASR_HANAFI : RelationalRule.AnchorEvent.ASR_SHAFI;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}
