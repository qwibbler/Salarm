package com.best.deskclock.namaz;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.databinding.DialogAddRelationalAlarmBinding;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;

public class RelationalAlarmDialogFragment extends DialogFragment {

    public static final String TAG = "RelationalAlarmDialogFragment";

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

    private DialogAddRelationalAlarmBinding mBinding;

    private String[] mOffsetTypes;
    private String[] mRoundTypes;
    private String[] mBoundaryTypes;

    private int mPrimaryOffsetIdx = 1; // default "after"
    private int mPrimaryRoundIdx = 0; // default "to nearest"
    private int mSafetyBoundIdx = 1; // default "later than"
    private int mSafetyOffsetIdx = 0; // default "before"
    private int mSafetyRoundIdx = 0; // default "to nearest"
    private int mAutostopOffsetIdx = 0; // default "before"

    public static RelationalAlarmDialogFragment newInstance() {
        return new RelationalAlarmDialogFragment();
    }

    public static void show(FragmentManager manager) {
        Utils.showDialogFragment(manager, newInstance(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DialogAddRelationalAlarmBinding.inflate(getLayoutInflater());

        mOffsetTypes = getResources().getStringArray(R.array.relational_alarm_offset_type);
        mRoundTypes = getResources().getStringArray(R.array.relational_alarm_round_type);
        mBoundaryTypes = getResources().getStringArray(R.array.relational_alarm_boundary_type);

        setupAdapters();
        setupListeners();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            setInitialDefaults();
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
        }
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void setupAdapters() {
        ArrayAdapter<CharSequence> prayerEventsAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.relational_alarm_prayer_events, android.R.layout.simple_dropdown_item_1line);
        mBinding.primaryEvent.setAdapter(prayerEventsAdapter);
        mBinding.safetyEvent.setAdapter(prayerEventsAdapter);
        mBinding.autostopEvent.setAdapter(prayerEventsAdapter);

        mBinding.primaryEvent.setOnItemClickListener((parent, view, position, id) -> updateAutostopEvent(position));
    }

    private void setInitialDefaults() {
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

        mBinding.autostopEnabledCheckbox.setChecked(true);
        mBinding.autostopOffsetValue.setText("0");
        updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);

        mBinding.primaryEvent.setText(mBinding.primaryEvent.getAdapter().getItem(0).toString(), false);
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
        mBinding.autostopEvent.setText(savedInstanceState.getString(KEY_AUTOSTOP_EVENT), false);

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
    }

    private void updateAutostopEvent(int primaryPosition) {
        int nextPosition = (primaryPosition + 1) % mBinding.primaryEvent.getAdapter().getCount();
        mBinding.autostopEvent.setText(mBinding.primaryEvent.getAdapter().getItem(nextPosition).toString(), false);
    }

    private void setupListeners() {
        mBinding.safetyEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.safetyContent));
        mBinding.autostopEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.autostopContent));

        mBinding.primaryOffsetToggle.setOnClickListener(v -> {
            mPrimaryOffsetIdx = (mPrimaryOffsetIdx + 1) % mOffsetTypes.length;
            updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
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

    private void saveRelationalAlarm() {
        if (mBinding == null) return;

        try {
            RelationalRule rule = new RelationalRule();
            rule.enabled = true;
            rule.anchorEvent = RelationalRule.AnchorEvent.values()[mBinding.primaryEvent.getAdapter().getCount() - 1]; // Placeholder logic, fix below

            // Map UI to Rule
            int primaryEventIdx = getPrayerEventIndex(getText(mBinding.primaryEvent));
            rule.anchorEvent = RelationalRule.AnchorEvent.values()[primaryEventIdx];

            int primaryOffset = Integer.parseInt(getText(mBinding.primaryOffsetValue));
            if (mPrimaryOffsetIdx == 0) primaryOffset *= -1; // Before
            rule.offsetMinutes = primaryOffset;

            rule.roundingDirection = RelationalRule.RoundingDirection.values()[mPrimaryRoundIdx];
            rule.roundingIntervalMinutes = Integer.parseInt(getText(mBinding.primaryRoundValue));

            if (mBinding.safetyEnabledCheckbox.isChecked()) {
                RelationalRule.Clip clip = new RelationalRule.Clip();
                clip.boundaryType = mSafetyBoundIdx == 0 ? RelationalRule.BoundaryType.MAX : RelationalRule.BoundaryType.MIN; // Earlier Than -> MAX, Later Than -> MIN

                int safetyEventIdx = getPrayerEventIndex(getText(mBinding.safetyEvent));
                clip.clipEvent = RelationalRule.AnchorEvent.values()[safetyEventIdx];

                int safetyOffset = Integer.parseInt(getText(mBinding.safetyOffsetValue));
                if (mSafetyOffsetIdx == 0) safetyOffset *= -1; // Before
                clip.clipOffsetMinutes = safetyOffset;

                clip.clipRoundingDirection = RelationalRule.RoundingDirection.values()[mSafetyRoundIdx];
                clip.clipRoundingIntervalMinutes = Integer.parseInt(getText(mBinding.safetyRoundValue));
                rule.clip = clip;
            }

            if (mBinding.autostopEnabledCheckbox.isChecked()) {
                rule.autostopEnabled = true;
                int autostopEventIdx = getPrayerEventIndex(getText(mBinding.autostopEvent));
                rule.autostopAnchorEvent = RelationalRule.AnchorEvent.values()[autostopEventIdx];

                int autostopOffset = Integer.parseInt(getText(mBinding.autostopOffsetValue));
                if (mAutostopOffsetIdx == 0) autostopOffset *= -1; // Before
                rule.autostopOffsetMinutes = autostopOffset;
            }

            RelationalAlarmSyncManager.saveRule(requireContext().getApplicationContext(), rule);
            dismiss();

        } catch (RelationalRule.InvalidRuleException e) {
            showConflictConfirmation(e);
        } catch (Exception e) {
            LogUtils.e("Error parsing relational alarm data", e);
        }
    }

    private int getPrayerEventIndex(String eventName) {
        String[] events = getResources().getStringArray(R.array.relational_alarm_prayer_events);
        for (int i = 0; i < events.length; i++) {
            if (events[i].equals(eventName)) return i;
        }
        return 0;
    }

    private void showConflictConfirmation(RelationalRule.InvalidRuleException e) {
        CustomDialog.create(
            requireContext(),
            R.style.AppAlertDialogTheme,
            null,
            getString(R.string.warning),
            e.getMessage(),
            null,
            getString(android.R.string.ok),
            (dialog, which) -> {
                try {
                    RelationalRule rule = parseRuleFromUi();
                    RelationalAlarmSyncManager.saveRule(requireContext(), rule, true);
                    dismiss();
                } catch (Exception ex) {
                    LogUtils.e("Error force saving rule", ex);
                }
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        ).show();
    }

    private RelationalRule parseRuleFromUi() {
        RelationalRule rule = new RelationalRule();
        rule.enabled = true;
        rule.anchorEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(getText(mBinding.primaryEvent))];
        int primaryOffset = Integer.parseInt(getText(mBinding.primaryOffsetValue));
        if (mPrimaryOffsetIdx == 0) primaryOffset *= -1;
        rule.offsetMinutes = primaryOffset;
        rule.roundingDirection = RelationalRule.RoundingDirection.values()[mPrimaryRoundIdx];
        rule.roundingIntervalMinutes = Integer.parseInt(getText(mBinding.primaryRoundValue));

        if (mBinding.safetyEnabledCheckbox.isChecked()) {
            RelationalRule.Clip clip = new RelationalRule.Clip();
            clip.boundaryType = mSafetyBoundIdx == 0 ? RelationalRule.BoundaryType.MAX : RelationalRule.BoundaryType.MIN;
            clip.clipEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(getText(mBinding.safetyEvent))];
            int safetyOffset = Integer.parseInt(getText(mBinding.safetyOffsetValue));
            if (mSafetyOffsetIdx == 0) safetyOffset *= -1;
            clip.clipOffsetMinutes = safetyOffset;
            clip.clipRoundingDirection = RelationalRule.RoundingDirection.values()[mSafetyRoundIdx];
            clip.clipRoundingIntervalMinutes = Integer.parseInt(getText(mBinding.safetyRoundValue));
            rule.clip = clip;
        }

        if (mBinding.autostopEnabledCheckbox.isChecked()) {
            rule.autostopEnabled = true;
            rule.autostopAnchorEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(getText(mBinding.autostopEvent))];
            int autostopOffset = Integer.parseInt(getText(mBinding.autostopOffsetValue));
            if (mAutostopOffsetIdx == 0) autostopOffset *= -1;
            rule.autostopOffsetMinutes = autostopOffset;
        }
        return rule;
    }
}
