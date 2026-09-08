package com.qwibbler.salarm.salah;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.alarms.AlarmEditBottomSheetFragment;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.DialogAddRelationalAlarmBinding;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SalahAlarmDialogFragment extends DialogFragment {

    public static final String TAG = "SalahAlarmDialogFragment";
    public static final String REQUEST_KEY = "relational_rule_saved";
    private static final String ARG_RULE_ID = "arg_rule_id";

    private static final String KEY_PRIMARY_VALUE = "primary_value";
    private static final String KEY_PRIMARY_OFFSET_IDX = "primary_offset_idx";
    private static final String KEY_PRIMARY_EVENT = "primary_event";
    private static final String KEY_PRIMARY_ROUND_ENABLED = "primary_round_enabled";
    private static final String KEY_PRIMARY_ROUND_IDX = "primary_round_idx";
    private static final String KEY_PRIMARY_ROUND_VALUE = "primary_round_value";

    private static final String KEY_SAFETY_ENABLED = "safety_enabled";
    private static final String KEY_SAFETY_BOUND_IDX = "safety_bound_idx";
    private static final String KEY_SAFETY_VALUE = "safety_value";
    private static final String KEY_SAFETY_OFFSET_IDX = "safety_offset_idx";
    private static final String KEY_SAFETY_EVENT = "safety_event";
    private static final String KEY_SAFETY_ROUND_ENABLED = "safety_round_enabled";
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

    private final List<MasjidEvent> mMasjidEvents = new ArrayList<>();
    private final List<Masjid> mMasjids = new ArrayList<>();

    public static SalahAlarmDialogFragment newInstance() {
        return new SalahAlarmDialogFragment();
    }

    public static SalahAlarmDialogFragment newInstance(long ruleId) {
        SalahAlarmDialogFragment fragment = new SalahAlarmDialogFragment();
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

        setupAdapters(savedInstanceState);
        setupListeners();

        updateRoundingVisibility();

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
            outState.putBoolean(KEY_PRIMARY_ROUND_ENABLED, mBinding.primaryRoundEnabledCheckbox.isChecked());
            outState.putInt(KEY_PRIMARY_ROUND_IDX, mPrimaryRoundIdx);
            outState.putString(KEY_PRIMARY_ROUND_VALUE, getText(mBinding.primaryRoundValue));

            outState.putBoolean(KEY_SAFETY_ENABLED, mBinding.safetyEnabledCheckbox.isChecked());
            outState.putInt(KEY_SAFETY_BOUND_IDX, mSafetyBoundIdx);
            outState.putString(KEY_SAFETY_VALUE, getText(mBinding.safetyOffsetValue));
            outState.putInt(KEY_SAFETY_OFFSET_IDX, mSafetyOffsetIdx);
            outState.putString(KEY_SAFETY_EVENT, getText(mBinding.safetyEvent));
            outState.putBoolean(KEY_SAFETY_ROUND_ENABLED, mBinding.safetyRoundEnabledCheckbox.isChecked());
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

    private void setupAdapters(Bundle savedInstanceState) {
        RelationalAlarmUiHelper.setupNumericDropdowns(
            mBinding.primaryOffsetValue,
            mBinding.primaryRoundValue,
            mBinding.adjustmentValue,
            mBinding.safetyOffsetValue,
            mBinding.safetyRoundValue,
            mBinding.autostopOffsetValue
        );

        final Context appContext = requireContext().getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            List<MasjidEvent> masjidEvents = db.masjidEventDao().getAll();
            List<Masjid> masjids = db.masjidDao().getAll();

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || mBinding == null) return;

                mMasjidEvents.clear();
                mMasjids.clear();
                mMasjidEvents.addAll(masjidEvents);
                mMasjids.addAll(masjids);

                SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
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

                for (MasjidEvent e : masjidEvents) {
                    if (e.isSelected) {
                        Masjid masjid = null;
                        for (Masjid m : masjids) {
                            if (m.id == e.masjidId) {
                                masjid = m;
                                break;
                            }
                        }
                        if (masjid != null) {
                            String masjidLabel = SalahUtils.getLocalizedSalahName(requireContext(), e.salahType) + " - " + masjid.name;
                            eventsList.add(masjidLabel);
                        }
                    }
                }

                ArrayAdapter<String> prayerEventsAdapter = new ArrayAdapter<>(requireContext(),
                        R.layout.dropdown_item_relational, eventsList);
                mBinding.primaryEvent.setAdapter(prayerEventsAdapter);
                mBinding.safetyEvent.setAdapter(prayerEventsAdapter);
                mBinding.autostopEvent.setAdapter(prayerEventsAdapter);

                mBinding.primaryEvent.setOnItemClickListener((parent, view, position, id) -> updateAutostopEvent(position));

                // Now that adapters are set, initialize UI
                if (savedInstanceState != null) {
                    restoreState(savedInstanceState);
                } else {
                    setInitialDefaults();

                    // Re-populate if editing
                    Bundle args = getArguments();
                    if (args != null && args.containsKey(ARG_RULE_ID)) {
                        long ruleId = args.getLong(ARG_RULE_ID);
                        AppExecutors.getDiskIO().execute(() -> {
                            RelationalRule rule = SalahDatabase.getDatabase(appContext).relationalRuleDao().getById(ruleId);
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
            });
        });
    }

    private void setInitialDefaults() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        mBinding.primaryOffsetValue.setText("5");
        updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
        mBinding.primaryRoundEnabledCheckbox.setChecked(false);
        updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mPrimaryRoundIdx);
        mBinding.primaryRoundValue.setText("0");

        mBinding.safetyEnabledCheckbox.setChecked(false);
        updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);
        mBinding.safetyOffsetValue.setText("5");
        updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
        mBinding.safetyRoundEnabledCheckbox.setChecked(false);
        updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
        mBinding.safetyRoundValue.setText("0");

        boolean autoDismissByDefault = SettingsDAO.isSalahAutoDismissByDefaultEnabled(prefs);
        mBinding.autostopEnabledCheckbox.setChecked(autoDismissByDefault);
        mBinding.autostopOffsetValue.setText("0");
        updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);

        mBinding.adjustmentEnabledCheckbox.setChecked(false);
        mBinding.adjustmentValue.setText("0");
        updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
        updateBlockState(mBinding.safetyRoundEnabledCheckbox.isChecked(), mBinding.safetyRoundContainer);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
        updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
        updateAdjustmentVisibility();

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
        mBinding.primaryRoundEnabledCheckbox.setChecked(savedInstanceState.getBoolean(KEY_PRIMARY_ROUND_ENABLED));
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
        mBinding.safetyRoundEnabledCheckbox.setChecked(savedInstanceState.getBoolean(KEY_SAFETY_ROUND_ENABLED));
        mSafetyRoundIdx = savedInstanceState.getInt(KEY_SAFETY_ROUND_IDX);
        updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
        mBinding.safetyRoundValue.setText(savedInstanceState.getString(KEY_SAFETY_ROUND_VALUE));

        mBinding.autostopEnabledCheckbox.setChecked(savedInstanceState.getBoolean(KEY_AUTOSTOP_ENABLED));
        mBinding.autostopOffsetValue.setText(savedInstanceState.getString(KEY_AUTOSTOP_VALUE));
        mAutostopOffsetIdx = savedInstanceState.getInt(KEY_AUTOSTOP_OFFSET_IDX);
        updateToggle(mBinding.autostopOffsetToggle, mOffsetTypes, mAutostopOffsetIdx);
        mBinding.adjustmentValue.setText(savedInstanceState.getString(KEY_ADJUSTMENT_VALUE));
        mAdjustmentIdx = savedInstanceState.getInt(KEY_ADJUSTMENT_IDX);
        updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);

        String adjVal = getText(mBinding.adjustmentValue);
        mBinding.adjustmentEnabledCheckbox.setChecked(!adjVal.equals("0") && !adjVal.isEmpty());

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
        updateBlockState(mBinding.safetyRoundEnabledCheckbox.isChecked(), mBinding.safetyRoundContainer);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
        updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
        updateAdjustmentVisibility();
    }

    private void updateAutostopEvent(int primaryPosition) {
        int nextPosition = (primaryPosition + 1) % mBinding.primaryEvent.getAdapter().getCount();
        mBinding.autostopEvent.setText(mBinding.primaryEvent.getAdapter().getItem(nextPosition).toString(), false);
    }

    private void updateRoundingVisibility() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        boolean showRounding = prefs.getBoolean(PreferencesKeys.KEY_SALAH_ALARM_SHOW_ROUNDING, false);

        int roundingVisibility = showRounding ? View.VISIBLE : View.GONE;
        mBinding.primaryRoundEnabledCheckbox.setVisibility(roundingVisibility);
        if (!showRounding) {
            mBinding.primaryRoundContainer.setVisibility(View.GONE);
        }
        mBinding.safetyRoundEnabledCheckbox.setVisibility(roundingVisibility);
        if (!showRounding) {
            mBinding.safetyRoundContainer.setVisibility(View.GONE);
        }

        updateAdjustmentVisibility();
    }

    private void updateAdjustmentVisibility() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        boolean showRounding = prefs.getBoolean(PreferencesKeys.KEY_SALAH_ALARM_SHOW_ROUNDING, false);
        boolean showAdjustment = prefs.getBoolean(PreferencesKeys.KEY_SALAH_ADJUST_ALARM_AFTER_ROUNDING, false);

        boolean roundingEnabled = mBinding.primaryRoundEnabledCheckbox.isChecked();
        boolean shouldShowAdjustmentCheckbox = showRounding && showAdjustment && roundingEnabled;

        mBinding.adjustmentEnabledCheckbox.setVisibility(shouldShowAdjustmentCheckbox ? View.VISIBLE : View.GONE);
        if (!shouldShowAdjustmentCheckbox) {
            mBinding.adjustmentContainer.setVisibility(View.GONE);
        } else {
            updateBlockState(mBinding.adjustmentEnabledCheckbox.isChecked(), mBinding.adjustmentContainer);
        }
    }

    private void setupListeners() {
        mBinding.safetyEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.safetyContent));
        mBinding.autostopEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.autostopContent));
        mBinding.primaryRoundEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateBlockState(isChecked, mBinding.primaryRoundContainer);
            updateAdjustmentVisibility();
        });
        mBinding.safetyRoundEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateBlockState(isChecked, mBinding.safetyRoundContainer));

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

    private void updateBlockState(boolean enabled, View content) {
        content.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private String getText(TextView textView) {
        return textView != null && textView.getText() != null ? textView.getText().toString() : "";
    }

    private void populateUiFromRule(RelationalRule rule) {
        if (mBinding == null) return;

        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) mBinding.primaryEvent.getAdapter();
        if (adapter != null) {
            String primaryLabel = getLabelForAnchor(rule.definition.anchor);
            int primaryPos = adapter.getPosition(primaryLabel);
            if (primaryPos >= 0) {
                mBinding.primaryEvent.setText(adapter.getItem(primaryPos), false);
            }
        }

        mBinding.primaryOffsetValue.setText(String.valueOf(Math.abs(rule.definition.calculation.offsetMinutes)));
        mPrimaryOffsetIdx = rule.definition.calculation.offsetMinutes >= 0 ? 1 : 0;
        updateToggle(mBinding.primaryOffsetToggle, mOffsetTypes, mPrimaryOffsetIdx);
        mBinding.primaryRoundEnabledCheckbox.setChecked(rule.definition.calculation.roundingIntervalMinutes > 0);
        mPrimaryRoundIdx = rule.definition.calculation.roundingDirection != null ? rule.definition.calculation.roundingDirection.ordinal() : 0;
        updateToggle(mBinding.primaryRoundToggle, mRoundTypes, mPrimaryRoundIdx);
        mBinding.primaryRoundValue.setText(String.valueOf(rule.definition.calculation.roundingIntervalMinutes));

        if (rule.definition.clip != null) {
            mBinding.safetyEnabledCheckbox.setChecked(true);
            mSafetyBoundIdx = rule.definition.clip.boundaryType == RelationalRule.BoundaryType.NO_EARLIER_THAN ? 0 : 1;
            updateToggle(mBinding.safetyBoundaryToggle, mBoundaryTypes, mSafetyBoundIdx);

            if (adapter != null) {
                String safetyLabel = getLabelForAnchor(rule.definition.clip.anchor);
                int safetyPos = adapter.getPosition(safetyLabel);
                if (safetyPos >= 0) {
                    mBinding.safetyEvent.setText(adapter.getItem(safetyPos), false);
                }
            }

            mBinding.safetyOffsetValue.setText(String.valueOf(Math.abs(rule.definition.clip.offsetMinutes)));
            mSafetyOffsetIdx = rule.definition.clip.offsetMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.safetyOffsetToggle, mOffsetTypes, mSafetyOffsetIdx);
            mBinding.safetyRoundEnabledCheckbox.setChecked(rule.definition.clip.roundingIntervalMinutes > 0);
            mSafetyRoundIdx = rule.definition.clip.roundingDirection != null ? rule.definition.clip.roundingDirection.ordinal() : 0;
            updateToggle(mBinding.safetyRoundToggle, mRoundTypes, mSafetyRoundIdx);
            mBinding.safetyRoundValue.setText(String.valueOf(rule.definition.clip.roundingIntervalMinutes));
        } else {
            mBinding.safetyEnabledCheckbox.setChecked(false);
            mBinding.safetyRoundEnabledCheckbox.setChecked(false);
        }

        if (rule.autostopEnabled) {
            mBinding.autostopEnabledCheckbox.setChecked(true);
            if (adapter != null) {
                String autostopEventName = getLabelForAnchor(rule.autostopAnchor);
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

        if (rule.definition.calculation.adjustmentMinutes != 0) {
            mBinding.adjustmentEnabledCheckbox.setChecked(true);
            mBinding.adjustmentValue.setText(String.valueOf(Math.abs(rule.definition.calculation.adjustmentMinutes)));
            mAdjustmentIdx = rule.definition.calculation.adjustmentMinutes >= 0 ? 1 : 0;
            updateToggle(mBinding.adjustmentToggle, mAdjustmentTypes, mAdjustmentIdx);
        } else {
            mBinding.adjustmentEnabledCheckbox.setChecked(false);
            mBinding.adjustmentValue.setText("0");
        }

        updateBlockState(mBinding.safetyEnabledCheckbox.isChecked(), mBinding.safetyContent);
        updateBlockState(mBinding.primaryRoundEnabledCheckbox.isChecked(), mBinding.primaryRoundContainer);
        updateBlockState(mBinding.safetyRoundEnabledCheckbox.isChecked(), mBinding.safetyRoundContainer);
        updateBlockState(mBinding.autostopEnabledCheckbox.isChecked(), mBinding.autostopContent);
        updateAdjustmentVisibility();
    }

    private String getLabelForAnchor(RelationalAnchor anchor) {
        if (anchor.masjidEventId != null) {
            for (MasjidEvent e : mMasjidEvents) {
                if (e.id == anchor.masjidEventId) {
                    for (Masjid m : mMasjids) {
                        if (m.id == e.masjidId) {
                            return SalahUtils.getLocalizedSalahName(requireContext(), e.salahType) + " - " + m.name;
                        }
                    }
                }
            }
        }
        if (anchor.basicEvent != null) {
            return RelationalRule.getEventName(requireContext(), anchor.basicEvent);
        }
        return "";
    }

    private void saveRelationalAlarm() {
        if (mBinding == null || !isAdded()) return;

        try {
            RelationalRule rule = parseRuleFromUi();
            Bundle args = getArguments();
            if (args != null && args.containsKey(ARG_RULE_ID)) {
                rule.id = args.getLong(ARG_RULE_ID);
            }

            Uri defaultRingtone = DataModel.getDataModel().getAlarmRingtoneUriFromSettings(true);
            boolean isNewRule = rule.id == 0;
            final FragmentManager fm = getParentFragmentManager();

            if (getDialog() instanceof AlertDialog dialog) {
                View saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (saveButton != null) saveButton.setEnabled(false);
            }

            SalahAlarmSyncManager.saveRule(requireContext().getApplicationContext(), rule, defaultRingtone, syncedAlarm -> {
                if (isAdded() && syncedAlarm != null) {
                    Bundle result = new Bundle();
                    result.putLong(AlarmEditBottomSheetFragment.ARG_ALARM_ID, syncedAlarm.id);
                    result.putBoolean(AlarmEditBottomSheetFragment.ARG_IS_NEW_ALARM, isNewRule);
                    fm.setFragmentResult(REQUEST_KEY, result);
                }
            });

            dismiss();

        } catch (Exception e) {
            LogUtils.e("Error parsing relational alarm data", e);
            Toast.makeText(requireContext(), R.string.salah_error_saving, Toast.LENGTH_LONG).show();
            if (getDialog() instanceof AlertDialog dialog) {
                View saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
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
            Madhab madhab = SalahUtils.getMadhab(context);
            return madhab == Madhab.HANAFI ? 4 : 3;
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

        String primaryLabel = getText(mBinding.primaryEvent);
        setAnchorFromLabel(rule.definition.anchor, primaryLabel);

        int primaryOffset = parseMinutes(getText(mBinding.primaryOffsetValue));
        if (mPrimaryOffsetIdx == 0) primaryOffset *= -1;
        rule.definition.calculation.offsetMinutes = primaryOffset;

        rule.definition.calculation.roundingDirection = RelationalRule.RoundingDirection.values()[mPrimaryRoundIdx];
        rule.definition.calculation.roundingIntervalMinutes = mBinding.primaryRoundEnabledCheckbox.isChecked() ? parseMinutes(getText(mBinding.primaryRoundValue)) : 0;

        if (mBinding.safetyEnabledCheckbox.isChecked()) {
            RelationalClip clip = new RelationalClip();
            clip.boundaryType = mSafetyBoundIdx == 0 ? RelationalRule.BoundaryType.NO_EARLIER_THAN : RelationalRule.BoundaryType.NO_LATER_THAN;

            String safetyLabel = getText(mBinding.safetyEvent);
            setAnchorFromLabel(clip.anchor, safetyLabel);

            int safetyOffset = parseMinutes(getText(mBinding.safetyOffsetValue));
            if (mSafetyOffsetIdx == 0) safetyOffset *= -1;
            clip.offsetMinutes = safetyOffset;

            clip.roundingDirection = RelationalRule.RoundingDirection.values()[mSafetyRoundIdx];
            clip.roundingIntervalMinutes = mBinding.safetyRoundEnabledCheckbox.isChecked() ? parseMinutes(getText(mBinding.safetyRoundValue)) : 0;
            rule.definition.clip = clip;
        }

        if (mBinding.autostopEnabledCheckbox.isChecked()) {
            rule.autostopEnabled = true;
            String autostopLabel = getText(mBinding.autostopEvent);
            setAnchorFromLabel(rule.autostopAnchor, autostopLabel);

            int autostopOffset = parseMinutes(getText(mBinding.autostopOffsetValue));
            if (mAutostopOffsetIdx == 0) autostopOffset *= -1;
            rule.autostopOffsetMinutes = autostopOffset;
        }

        if (mBinding.adjustmentEnabledCheckbox.isChecked()) {
            int adjustment = parseMinutes(getText(mBinding.adjustmentValue));
            if (mAdjustmentIdx == 0) adjustment *= -1;
            rule.definition.calculation.adjustmentMinutes = adjustment;
        } else {
            rule.definition.calculation.adjustmentMinutes = 0;
        }
        return rule;
    }

    private void setAnchorFromLabel(RelationalAnchor anchor, String label) {
        for (MasjidEvent e : mMasjidEvents) {
            Masjid masjid = null;
            for (Masjid m : mMasjids) {
                if (m.id == e.masjidId) {
                    masjid = m;
                    break;
                }
            }
            if (masjid != null) {
                String masjidLabel = SalahUtils.getLocalizedSalahName(requireContext(), e.salahType) + " - " + masjid.name;
                if (masjidLabel.equals(label)) {
                    anchor.masjidEventId = e.id;
                    anchor.basicEvent = null;
                    return;
                }
            }
        }

        anchor.basicEvent = RelationalRule.AnchorEvent.values()[getPrayerEventIndex(label)];
        anchor.masjidEventId = null;
    }
}
