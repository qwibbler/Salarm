package com.best.deskclock.namaz;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.best.deskclock.R;
import com.best.deskclock.utils.LogUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class RelationalAlarmDialogFragment extends DialogFragment {

    public static final String TAG = "RelationalAlarmDialogFragment";

    private TextInputEditText mPrimaryOffsetValue;
    private MaterialButton mPrimaryOffsetToggle;
    private AutoCompleteTextView mPrimaryPrayerEvent;
    private MaterialButtonToggleGroup mPrimaryRoundGroup;
    private TextInputEditText mPrimaryRoundValue;

    private CheckBox mBlock2Checkbox;
    private LinearLayout mBlock2Content;
    private MaterialButton mSafetyBoundaryToggle;
    private TextInputEditText mSafetyOffsetValue;
    private MaterialButton mSafetyOffsetToggle;
    private AutoCompleteTextView mSafetyPrayerEvent;
    private MaterialButtonToggleGroup mSafetyRoundGroup;
    private TextInputEditText mSafetyRoundValue;

    private CheckBox mBlock3Checkbox;
    private LinearLayout mBlock3Content;
    private TextInputEditText mAutostopOffsetValue;
    private MaterialButton mAutostopOffsetToggle;
    private AutoCompleteTextView mAutostopPrayerEvent;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_relational_alarm, null);

        initViews(view);
        setupAdapters();
        setupListeners();
        setInitialDefaults();

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_add_relational_alarm_title)
                .setView(view)
                .setPositiveButton(R.string.relational_alarm_save, (dialog, which) -> saveRelationalAlarm())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void initViews(View view) {
        mPrimaryOffsetValue = view.findViewById(R.id.primary_offset_value);
        mPrimaryOffsetToggle = view.findViewById(R.id.primary_offset_toggle);
        mPrimaryPrayerEvent = view.findViewById(R.id.primary_prayer_event);
        mPrimaryRoundGroup = view.findViewById(R.id.primary_round_group);
        mPrimaryRoundValue = view.findViewById(R.id.primary_round_value);

        mBlock2Checkbox = view.findViewById(R.id.block2_checkbox);
        mBlock2Content = view.findViewById(R.id.block2_content);
        mSafetyBoundaryToggle = view.findViewById(R.id.safety_boundary_toggle);
        mSafetyOffsetValue = view.findViewById(R.id.safety_offset_value);
        mSafetyOffsetToggle = view.findViewById(R.id.safety_offset_toggle);
        mSafetyPrayerEvent = view.findViewById(R.id.safety_prayer_event);
        mSafetyRoundGroup = view.findViewById(R.id.safety_round_group);
        mSafetyRoundValue = view.findViewById(R.id.safety_round_value);

        mBlock3Checkbox = view.findViewById(R.id.block3_checkbox);
        mBlock3Content = view.findViewById(R.id.block3_content);
        mAutostopOffsetValue = view.findViewById(R.id.autostop_offset_value);
        mAutostopOffsetToggle = view.findViewById(R.id.autostop_offset_toggle);
        mAutostopPrayerEvent = view.findViewById(R.id.autostop_prayer_event);
    }

    private void setupAdapters() {
        ArrayAdapter<CharSequence> prayerEventsAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.relational_alarm_prayer_events, android.R.layout.simple_dropdown_item_1line);
        mPrimaryPrayerEvent.setAdapter(prayerEventsAdapter);
        mSafetyPrayerEvent.setAdapter(prayerEventsAdapter);
        mAutostopPrayerEvent.setAdapter(prayerEventsAdapter);

        // Selection listeners for dropdowns to handle auto-updates
        mPrimaryPrayerEvent.setOnItemClickListener((parent, view, position, id) -> updateAutostopEvent(position));
    }

    private void setInitialDefaults() {
        // Block 1: 5 minutes after, Near, 0
        mPrimaryOffsetValue.setText("5");
        mPrimaryOffsetToggle.setText(R.string.relational_alarm_after);
        mPrimaryRoundGroup.check(R.id.primary_round_nearest);
        mPrimaryRoundValue.setText("0");

        // Block 2: 5 minutes before, Near, 0, Unchecked
        mBlock2Checkbox.setChecked(false);
        mSafetyBoundaryToggle.setText(R.string.relational_alarm_later_than);
        mSafetyOffsetValue.setText("5");
        mSafetyOffsetToggle.setText(R.string.relational_alarm_before);
        mSafetyRoundGroup.check(R.id.safety_round_nearest);
        mSafetyRoundValue.setText("0");

        // Block 3: 0 min before, Checked
        mBlock3Checkbox.setChecked(true);
        mAutostopOffsetValue.setText("0");
        mAutostopOffsetToggle.setText(R.string.relational_alarm_before);

        // Sync visibility
        updateBlockState(mBlock2Checkbox.isChecked(), mBlock2Content);
        updateBlockState(mBlock3Checkbox.isChecked(), mBlock3Content);

        // Set initial prayer event and trigger autostop logic
        mPrimaryPrayerEvent.setText(mPrimaryPrayerEvent.getAdapter().getItem(0).toString(), false);
        updateAutostopEvent(0);
        mSafetyPrayerEvent.setText(mSafetyPrayerEvent.getAdapter().getItem(1).toString(), false);
    }

    private void updateAutostopEvent(int primaryPosition) {
        int nextPosition = (primaryPosition + 1) % mPrimaryPrayerEvent.getAdapter().getCount();
        mAutostopPrayerEvent.setText(mAutostopPrayerEvent.getAdapter().getItem(nextPosition).toString(), false);
    }

    private void setupListeners() {
        mBlock2Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBlock2Content));
        mBlock3Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBlock3Content));

        mPrimaryOffsetToggle.setOnClickListener(v -> toggleButtonText(mPrimaryOffsetToggle,
                R.string.relational_alarm_before, R.string.relational_alarm_after));

        mSafetyOffsetToggle.setOnClickListener(v -> toggleButtonText(mSafetyOffsetToggle,
                R.string.relational_alarm_before, R.string.relational_alarm_after));

        mAutostopOffsetToggle.setOnClickListener(v -> toggleButtonText(mAutostopOffsetToggle,
                R.string.relational_alarm_before, R.string.relational_alarm_after));

        mSafetyBoundaryToggle.setOnClickListener(v -> toggleButtonText(mSafetyBoundaryToggle,
                R.string.relational_alarm_earlier_than, R.string.relational_alarm_later_than));
    }

    private void toggleButtonText(MaterialButton button, int stringRes1, int stringRes2) {
        String text1 = getString(stringRes1);
        String currentText = button.getText().toString();
        button.setText(currentText.equals(text1) ? getString(stringRes2) : text1);
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

    private void saveRelationalAlarm() {
        // Collect all data
        String primaryValue = mPrimaryOffsetValue.getText() != null ? mPrimaryOffsetValue.getText().toString() : "";
        String primaryOffset = mPrimaryOffsetToggle.getText().toString();
        String primaryEvent = mPrimaryPrayerEvent.getText().toString();
        int primaryRoundId = mPrimaryRoundGroup.getCheckedButtonId();
        String primaryRoundType = "nearest";
        if (primaryRoundId == R.id.primary_round_up) primaryRoundType = "up";
        else if (primaryRoundId == R.id.primary_round_down) primaryRoundType = "down";
        String primaryRoundValue = mPrimaryRoundValue.getText() != null ? mPrimaryRoundValue.getText().toString() : "";

        boolean safetyEnabled = mBlock2Checkbox.isChecked();
        String safetyBound = mSafetyBoundaryToggle.getText().toString();
        String safetyValue = mSafetyOffsetValue.getText() != null ? mSafetyOffsetValue.getText().toString() : "";
        String safetyOffset = mSafetyOffsetToggle.getText().toString();
        String safetyEvent = mSafetyPrayerEvent.getText().toString();
        int safetyRoundId = mSafetyRoundGroup.getCheckedButtonId();
        String safetyRoundType = "nearest";
        if (safetyRoundId == R.id.safety_round_up) safetyRoundType = "up";
        else if (safetyRoundId == R.id.safety_round_down) safetyRoundType = "down";
        String safetyRoundValue = mSafetyRoundValue.getText() != null ? mSafetyRoundValue.getText().toString() : "";

        boolean autostopEnabled = mBlock3Checkbox.isChecked();
        String autostopValue = mAutostopOffsetValue.getText() != null ? mAutostopOffsetValue.getText().toString() : "";
        String autostopOffset = mAutostopOffsetToggle.getText().toString();
        String autostopEvent = mAutostopPrayerEvent.getText().toString();

        LogUtils.i("Saving Relational Alarm:");
        LogUtils.i("Primary: %s min %s %s, Round: %s %s min", primaryValue, primaryOffset, primaryEvent, primaryRoundType, primaryRoundValue);
        if (safetyEnabled) {
            LogUtils.i("Safety: %s %s min %s %s, Round: %s %s min", safetyBound, safetyValue, safetyOffset, safetyEvent, safetyRoundType, safetyRoundValue);
        }
        if (autostopEnabled) {
            LogUtils.i("Autostop: %s min %s %s", autostopValue, autostopOffset, autostopEvent);
        }
    }
}
