package com.qwibbler.salarm.namaz;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.DialogAddRelationalAlarmBinding;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelationalAlarmDialogFragment extends DialogFragment {

    public static final String TAG = "RelationalAlarmDialogFragment";
    public static final String REQUEST_KEY = "relational_rule_saved";
    private static final String ARG_RULE_ID = "arg_rule_id";

    private static final String KEY_PRIMARY_VALUE = "primary_value";
    private static final String KEY_PRIMARY_OFFSET_IDX = "primary_offset_idx";
    private static final String KEY_PRIMARY_EVENT = "primary_event";
    private static final String KEY_PRIMARY_ROUND_IDX = "primary_round_idx";
    private static final String KEY_PRIMARY_ROUND_VALUE = "primary_round_value";

    private static final String KEY_SAFETY_ENABLED = "safety_enabled";
    private static final String KEY_SAFETY_BOUND_IDX = "safety_bound_idx";
    private static final String KEY_SAFETY_VALUE = "safety_value";
    private static final String KEY_SAFETY_OFFSET_IDX = "safety_offset_idx";
    private static final String KEY_SAFETY_EVENT = "safety_event";
    private static final String KEY_SAFETY_ROUND_IDX = "safety_round_idx";
    private static final String KEY_SAFETY_ROUND_VALUE = "safety_round_value";

    private static final String KEY_AUTOSTOP_ENABLED = "autostop_enabled";
    private static final String KEY_AUTOSTOP_VALUE = "autostop_value";
    private static final String KEY_AUTOSTOP_OFFSET_IDX = "autostop_offset_idx";
    private static final String KEY_AUTOSTOP_EVENT = "autostop_event";

    private static final String KEY_ADJUSTMENT_VALUE = "adjustment_value";
    private static final String KEY_ADJUSTMENT_IDX = "adjustment_idx";

    private DialogAddRelationalAlarmBinding mBinding;

    private String[] mOffsetTypes;
    private String[] mRoundTypes;
    private String[] mBoundaryTypes;
    private String[] mAdjustmentTypes;

    private int mPrimaryOffsetIdx = 1; // default "after"
    private int mPrimaryRoundIdx = 0; // default "to nearest"
    private int mSafetyBoundIdx = 1; // default "later than" (NO_LATER_THAN)
    private int mSafetyOffsetIdx = 0; // default "before"
    private int mSafetyRoundIdx = 0; // default "to nearest"
    private int mAutostopOffsetIdx = 0; // default "before"
    private int mAdjustmentIdx = 0; // default "earlier"

    public static RelationalAlarmDialogFragment newInstance() {
        return new RelationalAlarmDialogFragment();
    }

    public static RelationalAlarmDialogFragment newInstance(long ruleId) {
        RelationalAlarmDialogFragment fragment = new RelationalAlarmDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_RULE_ID, ruleId);
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(FragmentManager manager) {
        Utils.showDialogFragment(manager, newInstance(), TAG);
    }

    public static void show(FragmentManager manager, long ruleId) {
        Utils.showDialogFragment(manager, newInstance(ruleId), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DialogAddRelationalAlarmBinding.inflate(getLayoutInflater());

        mOffsetTypes = getResources().getStringArray(R.array.relational_alarm_offset_type);
        mRoundTypes = getResources().getStringArray(R.array.relational_alarm_round_type);
        mBoundaryTypes = getResources().getStringArray(R.array.relational_alarm_boundary_type);
        mAdjustmentTypes = getResources().getStringArray(R.array.relational_alarm_adjustment_type);

        setupAdapters();
        setupListeners();

        updateRoundingVisibility();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            setInitialDefaults();

            Bundle args = getArguments();
            if (args != null && args.containsKey(ARG_RULE_ID)) {
                long ruleId = args.getLong(ARG_RULE_ID);
                final Context appContext = requireContext().getApplicationContext();
                AppExecutors.getDiskIO().execute(() -> {
                    RelationalRule rule = NamazDatabase.getDatabase(appContext).relationalRuleDao().getById(ruleId);
                    if (rule != null) {
                        AppExecutors.getMainThread().post(() -> {
                            if (isAdded() && mBinding != null) {
                                populateUiFromRule(rule);
                            }
                        });
                    }
                });
            }
        }

        return CustomDialog.create(
            requireContext(),
            R.style.RelationalAlarmDialogTheme,
            null,
            getString(R.string.dialog_add_relational_alarm_title),
            null,
            mBinding.getRoot(),
            getString(R.string.relational_alarm_save),
            (dialog, which) -> saveRelationalAlarm(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mBinding != null) {
            outState.putString(KEY_PRIMARY_VALUE, getText(mBinding.primaryOffsetValue));
            outState.putInt(KEY_PRIMARY_OFFSET_IDX, mPrimaryOffsetIdx);
            outState.putString(KEY_PRIMARY_EVENT, getText(mBinding.primaryEvent));
            outState.putInt(KEY_PRIMARY_ROUND_IDX, mPrimaryRoundIdx);
            outState.putString(KEY_PRIMARY_ROUND_VALUE, getText(mBinding.primaryRoundValue));

            outState.putBoolean(KEY_SAFETY_ENABLED, mBinding.safetyEnabledCheckbox.isChecked());
            outState.putInt(KEY_SAFETY_BOUND_IDX, mSafetyBoundIdx);
            outState.putString(KEY_SAFETY_VALUE, getText(mBinding.safetyOffsetValue));
            outState.putInt(KEY_SAFETY_OFFSET_IDX, mSafetyOffsetIdx);
            outState.putString(KEY_SAFETY_EVENT, getText(mBinding.safetyEvent));
            outState.putInt(KEY_SAFETY_ROUND_IDX, mSafetyRoundIdx);
            outState.putString(KEY_SAFETY_ROUND_VALUE, getText(mBinding.safetyRoundValue));

            outState.putBoolean(KEY_AUTOSTOP_ENABLED, mBinding.autostopEnabledCheckbox.isChecked());
            outState.putString(KEY_AUTOSTOP_VALUE, getText(mBinding.autostopOffsetValue));
            outState.putInt(KEY_AUTOSTOP_OFFSET_IDX, mAutostopOffsetIdx);
            outState.putString(KEY_AUTOSTOP_EVENT, getText(mBinding.autostopEvent));

            outState.putString(KEY_ADJUSTMENT_VALUE, getText(mBinding.adjustmentValue));
            outState.putInt(KEY_ADJUSTMENT_IDX, mAdjustmentIdx);
        }
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void setupAdapters() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_NAMAZ_SHOW_BOTH_ASR_IN_ALARM_EVENTS, false);

        List<String> eventsList = new ArrayList<>();
        String[] originalEvents = getResources().getStringArray(R.array.relational_alarm_prayer_events);
        if (showBothAsr) {
            eventsList.addAll(Arrays.asList(originalEvents));
        } else {
            for (int i = 0; i < originalEvents.length; i++) {
                if (i == 3) { // ASR_SHAFI
                    eventsList.add(getString(R.string.asr_label));
                } else if (i == 4) { // ASR_HANAFI
                    continue; // Skip the second Asr
                } else {
                    eventsList.add(originalEvents[i]);
                }
            }
        }

        ArrayAdapter<String> prayerEventsAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, eventsList);
        mBinding.primaryEvent.setAdapter(prayerEventsAdapter);
        mBinding.safetyEvent.setAdapter(prayerEventsAdapter);
        mBinding.autostopEvent.setAdapter(prayerEventsAdapter);

        mBinding.primaryEvent.setOnItemClickListener((parent, view, position, id) -> updateAutostopEvent(position));
    }

    private void setInitialDefaults() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        mBinding.primaryOffsetValue.setText("5");
        updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
        updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mPrimaryRoundIdx);
        mBinding.primaryRoundValue.setText("0");

        mBinding.safetyEnabledCheckbox.setChecked(false);
        updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
        mBinding.safetyOffsetValue.setText("5");
        updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
        updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
        mBinding.safetyRoundValue.setText("0");

        boolean autoDismissByDefault = SettingsDAO.isNamazAutoDismissByDefaultEnabled(prefs);
        mBinding.autostopEnabledCheckbox.setChecked(autoDismissByDefault);
        mBinding.autostopOffsetValue.setText("0");
        updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);

        mBinding.adjustmentEnabledCheckbox.setChecked(false);
        mBinding.adjustmentValue.setText("0");
        updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
        updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);

        if (mBinding.primaryEvent.getAdapter().getCount() > 0) {
            mBinding.primaryEvent.setText(mBinding.primaryEvent.getAdapter().getItem(0).toString(), false);
        }
        updateAutostopEvent(0);
        mBinding.safetyEvent.setText(mBinding.safetyEvent.getAdapter().getItem(1).toString(), false);
    }

    private void restoreState(@NonNull Bundle savedInstanceState) {
        mBinding.primaryOffsetValue.setText(savedInstanceState.getString(KEY_PRIMARY_VALUE));
        mPrimaryOffsetIdx = savedInstanceState.getInt(KEY_PRIMARY_OFFSET_IDX);
        updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
        mBinding.primaryEvent.setText(savedInstanceState.getString(KEY_PRIMARY_EVENT), false);
        mPrimaryRoundIdx = savedInstanceState.getInt(KEY_PRIMARY_ROUND_IDX);
        updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mPrimaryRoundIdx);
        mBinding.primaryRoundValue.setText(savedInstanceState.getString(KEY_PRIMARY_ROUND_VALUE));

        mBinding.safetyEnabledCheckbox.setChecked(savedInstanceState.getBoolean(KEY_SAFETY_ENABLED));
        mSafetyBoundIdx = savedInstanceState.getInt(KEY_SAFETY_BOUND_IDX);
        updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
        mBinding.safetyOffsetValue.setText(savedInstanceState.getString(KEY_SAFETY_VALUE));
        mSafetyOffsetIdx = savedInstanceState.getInt(KEY_SAFETY_OFFSET_IDX);
        updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
        mBinding.safetyEvent.setText(savedInstanceState.getString(KEY_SAFETY_EVENT), false);
        mSafetyRoundIdx = savedInstanceState.getInt(KEY_SAFETY_ROUND_IDX);
        updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
        mBinding.safetyRoundValue.setText(savedInstanceState.getString(KEY_SAFETY_ROUND_VALUE));

        mBinding.autostopEnabledCheckbox.setChecked(savedInstanceState.getBoolean(KEY_AUTOSTOP_ENABLED));
        mBinding.autostopOffsetValue.setText(savedInstanceState.getString(KEY_AUTOSTOP_VALUE));
        mAutostopOffsetIdx = savedInstanceState.getInt(KEY_AUTOSTOP_OFFSET_IDX);
        updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);
        mBinding.adjustmentEnabledCheckbox.setChecked(savedInstanceState.getBoolean(KEY_ADJUSTMENT_VALUE, false)); // We don't have boolean anymore, just check value
        mBinding.adjustmentValue.setText(savedInstanceState.getString(KEY_ADJUSTMENT_VALUE));
        mAdjustmentIdx = savedInstanceState.getInt(KEY_ADJUSTMENT_IDX);
        updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);

        String adjVal = getText(mBinding.adjustmentValue);
        mBinding.adjustmentEnabledCheckbox.setChecked(!adjVal.equals("0") && !adjVal.isEmpty());

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
        updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
    }

    private void updateAutostopEvent(int primaryPosition) {
        int nextPosition = (primaryPosition + 1) % mBinding.primaryEvent.getAdapter().getCount();
        mBinding.autostopEvent.setText(mBinding.primaryEvent.getAdapter().getItem(nextPosition).toString(), false);
    }

    private void updateRoundingVisibility() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        boolean showRounding = prefs.getBoolean(PreferencesKeys.KEY_NAMAZ_ALARM_SHOW_ROUNDING, false);
        boolean showAdjustment = prefs.getBoolean(PreferencesKeys.KEY_NAMAZ_ADJUST_ALARM_AFTER_ROUNDING, false);

        int roundingVisibility = showRounding ? View.VISIBLE : View.GONE;
        mBinding.primaryRoundTitle.setVisibility(roundingVisibility);
        mBinding.primaryRoundContainer.setVisibility(roundingVisibility);
        mBinding.safetyRoundTitle.setVisibility(roundingVisibility);
        mBinding.safetyRoundContainer.setVisibility(roundingVisibility);

        int adjustmentVisibility = (showRounding && showAdjustment) ? View.VISIBLE : View.GONE;
        mBinding.adjustmentEnabledCheckbox.setVisibility(adjustmentVisibility);
        mBinding.adjustmentContainer.setVisibility(adjustmentVisibility);
    }

    private void setupListeners() {
        mBinding.safetyEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.safetyContent));
        mBinding.autostopEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.autostopContent));

        mBinding.primaryOffsetToggle.setOnClickListener(v -> {
            mPrimaryOffsetIdx = (mPrimaryOffsetIdx + 1) % mOffsetTypes.length;
            updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);

            // Auto-adjust safety boundary default: After -> No Later Than, Before -> No Earlier Than
            mSafetyBoundIdx = (mPrimaryOffsetIdx == 1) ? 1 : 0;
            updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
        });

        mBinding.primaryRoundToggle.setOnClickListener(v -> {
            mPrimaryRoundIdx = (mPrimaryRoundIdx + 1) % mRoundTypes.length;
            updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mPrimaryRoundIdx);
        });

        mBinding.safetyBoundaryToggle.setOnClickListener(v -> {
            mSafetyBoundIdx = (mSafetyBoundIdx + 1) % mBoundaryTypes.length;
            updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
        });

        mBinding.safetyOffsetToggle.setOnClickListener(v -> {
            mSafetyOffsetIdx = (mSafetyOffsetIdx + 1) % mOffsetTypes.length;
            updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
        });

        mBinding.safetyRoundToggle.setOnClickListener(v -> {
            mSafetyRoundIdx = (mSafetyRoundIdx + 1) % mRoundTypes.length;
            updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
        });

        mBinding.autostopOffsetToggle.setOnClickListener(v -> {
            mAutostopOffsetIdx = (mAutostopOffsetIdx + 1) % mOffsetTypes.length;
            updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);
        });

        mBinding.adjustmentEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.adjustmentContainer));
        mBinding.adjustmentToggle.setOnClickListener(v -> {
            mAdjustmentIdx = (mAdjustmentIdx + 1) % mAdjustmentTypes.length;
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);
        });
    }

    private void updateToggle(TextView view, String[] values, int index) {
        if (index >= 0 && index < values.length) {
            view.setText(values[index]);
        }
    }

    private void updateBlockState(boolean enabled, ViewGroup content) {
        content.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.4f;
        content.setAlpha(alpha);
        setViewsEnabled(content, enabled);
    }

    private void setViewsEnabled(ViewGroup layout, boolean enabled) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup group) {
                setViewsEnabled(group, enabled);
            }
        }
    }

    private String getText(TextView textView) {
        return textView != null && textView.getText() != null ? textView.getText().toString() : "";
    }

    private void populateUiFromRule(RelationalRule rule) {
        if (mBinding == null) return;

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) mBinding.primaryEvent.getAdapter();
        String primaryEventName = getEventNameForRule(rule.anchorEvent);
        int primaryPos = adapter.getPosition(primaryEventName);
        if (primaryPos >= 0) {
            mBinding.primaryEvent.setText(adapter.getItem(primaryPos), false);
        }

        mBinding.primaryOffsetValue.setText(String.valueOf(Math.abs(rule.offsetMinutes)));
        mPrimaryOffsetIdx = rule.offsetMinutes >= 0 ? 1 : 0;
        updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
        mPrimaryRoundIdx = rule.roundingDirection != null ? rule.roundingDirection.ordinal() : 0;
        updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mPrimaryRoundIdx);
        mBinding.primaryRoundValue.setText(String.valueOf(rule.roundingIntervalMinutes));

        if (rule.clip != null) {
            mBinding.safetyEnabledCheckbox.setChecked(true);
            mSafetyBoundIdx = rule.clip.boundaryType == RelationalRule.BoundaryType.NO_EARLIER_THAN ? 0 : 1;
            updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);

            String safetyEventName = getEventNameForRule(rule.clip.clipEvent);
            int safetyPos = adapter.getPosition(safetyEventName);
            if (safetyPos >= 0) {
                mBinding.safetyEvent.setText(adapter.getItem(safetyPos), false);
            }

            mBinding.safetyOffsetValue.setText(String.valueOf(Math.abs(rule.clip.clipOffsetMinutes)));
            mSafetyOffsetIdx = rule.clip.clipOffsetMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
            mSafetyRoundIdx = rule.clip.clipRoundingDirection != null ? rule.clip.clipRoundingDirection.ordinal() : 0;
            updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
            mBinding.safetyRoundValue.setText(String.valueOf(rule.clip.clipRoundingIntervalMinutes));
        } else {
            mBinding.safetyEnabledCheckbox.setChecked(false);
        }

        if (rule.autostopEnabled) {
            mBinding.autostopEnabledCheckbox.setChecked(true);
            if (rule.autostopAnchorEvent != null) {
                String autostopEventName = getEventNameForRule(rule.autostopAnchorEvent);
                int autostopPos = adapter.getPosition(autostopEventName);
                if (autostopPos >= 0) {
                    mBinding.autostopEvent.setText(adapter.getItem(autostopPos), false);
                }
            }
            mBinding.autostopOffsetValue.setText(String.valueOf(Math.abs(rule.autostopOffsetMinutes)));
            mAutostopOffsetIdx = rule.autostopOffsetMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);
        } else {
            mBinding.autostopEnabledCheckbox.setChecked(false);
        }

        if (rule.adjustmentMinutes != 0) {
            mBinding.adjustmentEnabledCheckbox.setChecked(true);
            mBinding.adjustmentValue.setText(String.valueOf(Math.abs(rule.adjustmentMinutes)));
            mAdjustmentIdx = rule.adjustmentMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);
        } else {
            mBinding.adjustmentEnabledCheckbox.setChecked(false);
            mBinding.adjustmentValue.setText("0");
        }

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
        updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
    }

    private String getEventNameForRule(RelationalRule.AnchorEvent event) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_NAMAZ_SHOW_BOTH_ASR_IN_ALARM_EVENTS, false);
        if (!showBothAsr && (event == RelationalRule.AnchorEvent.ASR_HANAFI || event == RelationalRule.AnchorEvent.ASR_SHAFI)) {
            return getString(R.string.asr_label);
        }
        String[] originalEvents = getResources().getStringArray(R.array.relational_alarm_prayer_events);
        return originalEvents[event.ordinal()];
    }

    private void saveRelationalAlarm() {
        if (mBinding == null || !isAdded()) return;

        try {
            RelationalRule rule = parseRuleFromUi();
            Bundle args = getArguments();
            if (args != null && args.containsKey(ARG_RULE_ID)) {
                rule.id = args.getLong(ARG_RULE_ID);
            }

            android.net.Uri defaultRingtone = com.qwibbler.salarm.data.DataModel.getDataModel().getAlarmRingtoneUriFromSettings(true);

            // Disable button to prevent double-save and indicate activity
            if (getDialog() instanceof androidx.appcompat.app.AlertDialog dialog) {
                View saveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (saveButton != null) saveButton.setEnabled(false);
            }

            RelationalAlarmSyncManager.saveRule(requireContext().getApplicationContext(), rule, false, defaultRingtone);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, new Bundle());
            dismiss();

        } catch (Exception e) {
            LogUtils.e("Error parsing relational alarm data", e);
            android.widget.Toast.makeText(requireContext(), R.string.namaz_error_saving, android.widget.Toast.LENGTH_LONG).show();
            // Re-enable button on failure
            if (getDialog() instanceof androidx.appcompat.app.AlertDialog dialog) {
                View saveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (saveButton != null) saveButton.setEnabled(true);
            }
        }
    }

    private int parseMinutes(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getPrayerEventIndex(String eventName) {
        if (eventName == null) return 0;
        Context context = getContext();
        if (context == null) return 0;
        if (getString(R.string.asr_label).equals(eventName)) {
            Madhab madhab = NamazUtils.getMadhab(context);
            return madhab == Madhab.HANAFI ? 4 : 3; // 4: ASR_HANAFI, 3: ASR_SHAFI
        }
        String[] events = getResources().getStringArray(R.array.relational_alarm_prayer_events);
        for (int i = 0; i < events.length; i++) {
            if (events[i].equals(eventName)) return i;
        }
        return 0;
    }

    private RelationalRule parseRuleFromUi() {
        RelationalRule rule = new RelationalRule();
        rule.enabled = true;
        rule.anchorEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(getText(mBinding.primaryEvent))];

        int primaryOffset = parseMinutes(getText(mBinding.primaryOffsetValue));
        if (mPrimaryOffsetIdx == 0) primaryOffset *= -1;
        rule.offsetMinutes = primaryOffset;

        rule.roundingDirection = RelationalRule.RoundingDirection.values()[mPrimaryRoundIdx];
        rule.roundingIntervalMinutes = parseMinutes(getText(mBinding.primaryRoundValue));

        if (mBinding.safetyEnabledCheckbox.isChecked()) {
            RelationalRule.Clip clip = new RelationalRule.Clip();
            clip.boundaryType = mSafetyBoundIdx == 0 ? RelationalRule.BoundaryType.NO_EARLIER_THAN : RelationalRule.BoundaryType.NO_LATER_THAN;
            clip.clipEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(getText(mBinding.safetyEvent))];

            int safetyOffset = parseMinutes(getText(mBinding.safetyOffsetValue));
            if (mSafetyOffsetIdx == 0) safetyOffset *= -1;
            clip.clipOffsetMinutes = safetyOffset;

            clip.clipRoundingDirection = RelationalRule.RoundingDirection.values()[mSafetyRoundIdx];
            clip.clipRoundingIntervalMinutes = parseMinutes(getText(mBinding.safetyRoundValue));
            rule.clip = clip;
        }

        if (mBinding.autostopEnabledCheckbox.isChecked()) {
            rule.autostopEnabled = true;
            rule.autostopAnchorEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(getText(mBinding.autostopEvent))];

            int autostopOffset = parseMinutes(getText(mBinding.autostopOffsetValue));
            if (mAutostopOffsetIdx == 0) autostopOffset *= -1;
            rule.autostopOffsetMinutes = autostopOffset;
        }

        if (mBinding.adjustmentEnabledCheckbox.isChecked()) {
            int adjustment = parseMinutes(getText(mBinding.adjustmentValue));
            if (mAdjustmentIdx == 0) adjustment *= -1;
            rule.adjustmentMinutes = adjustment;
        } else {
            rule.adjustmentMinutes = 0;
        }
        return rule;
    }
}
