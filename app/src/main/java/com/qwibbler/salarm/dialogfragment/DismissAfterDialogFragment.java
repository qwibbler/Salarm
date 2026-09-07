// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.dialogfragment;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.AlarmDismissAfterDialogBinding;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;

public class DismissAfterDialogFragment extends DialogFragment {

    private static final String TAG = "dismiss_after_dialog";

    private static final String ARG_DISMISS_AFTER = "arg_dismiss_after";
    private static final String ARG_PREF_KEY = "arg_pref_key";
    public static final String REQUEST_KEY = "dismiss_after_request_key";
    public static final String DISMISS_AFTER_VALUE = "dismiss_after_value";
    public static final String RESULT_PREF_KEY = "result_pref_key";

    private AlarmDismissAfterDialogBinding mBinding;
    private String mPrefKey;
    private Button mOkButton;
    private InputMethodManager mInput;
    private boolean isUpdatingInputs = false;

    public static DismissAfterDialogFragment newInstance(int dismissAfter) {
        return newInstance(null, dismissAfter);
    }

    public static DismissAfterDialogFragment newInstance(String key, int dismissAfter) {
        Bundle args = new Bundle();
        args.putString(ARG_PREF_KEY, key);
        args.putInt(ARG_DISMISS_AFTER, dismissAfter);
        DismissAfterDialogFragment frag = new DismissAfterDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    public static void show(FragmentManager manager, DismissAfterDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        final Typeface typeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();
        mPrefKey = args.getString(ARG_PREF_KEY, null);
        int dismissAfter = args.getInt(ARG_DISMISS_AFTER, 0);

        if (savedInstanceState != null) {
            dismissAfter = savedInstanceState.getInt(ARG_DISMISS_AFTER, dismissAfter);
        }

        mInput = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mBinding = AlarmDismissAfterDialogBinding.inflate(getLayoutInflater());

        mBinding.dismissNeverCheckbox.setTypeface(typeFace);
        mBinding.editValue.setTypeface(typeFace);

        if (dismissAfter == 0) {
            mBinding.dismissNeverCheckbox.setChecked(true);
            mBinding.minutesCheckbox.setChecked(false);
            mBinding.snoozesCheckbox.setChecked(false);
            mBinding.editValue.setText("");
        } else if (dismissAfter > 0) {
            mBinding.dismissNeverCheckbox.setChecked(false);
            mBinding.minutesCheckbox.setChecked(true);
            mBinding.snoozesCheckbox.setChecked(false);
            mBinding.editValue.setText(String.valueOf(dismissAfter));
        } else {
            mBinding.dismissNeverCheckbox.setChecked(false);
            mBinding.minutesCheckbox.setChecked(false);
            mBinding.snoozesCheckbox.setChecked(true);
            mBinding.editValue.setText(String.valueOf(Math.abs(dismissAfter)));
        }

        updateInputState();

        mBinding.dismissNeverCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingInputs) return;
            isUpdatingInputs = true;
            if (isChecked) {
                mBinding.minutesCheckbox.setChecked(false);
                mBinding.snoozesCheckbox.setChecked(false);
                mBinding.editValue.setText("");
            } else {
                // At least one must be checked
                mBinding.dismissNeverCheckbox.setChecked(true);
            }
            updateInputState();
            isUpdatingInputs = false;
        });

        mBinding.minutesCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingInputs) return;
            isUpdatingInputs = true;
            if (isChecked) {
                mBinding.dismissNeverCheckbox.setChecked(false);
                mBinding.snoozesCheckbox.setChecked(false);
                updateInputState();
                mBinding.editValue.requestFocus();
                if (mInput != null) {
                    mInput.showSoftInput(mBinding.editValue, InputMethodManager.SHOW_IMPLICIT);
                }
            } else if (!mBinding.dismissNeverCheckbox.isChecked() && !mBinding.snoozesCheckbox.isChecked()) {
                mBinding.dismissNeverCheckbox.setChecked(true);
                mBinding.editValue.setText("");
                updateInputState();
            }
            isUpdatingInputs = false;
        });

        mBinding.snoozesCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingInputs) return;
            isUpdatingInputs = true;
            if (isChecked) {
                mBinding.dismissNeverCheckbox.setChecked(false);
                mBinding.minutesCheckbox.setChecked(false);
                updateInputState();
                mBinding.editValue.requestFocus();
                if (mInput != null) {
                    mInput.showSoftInput(mBinding.editValue, InputMethodManager.SHOW_IMPLICIT);
                }
            } else if (!mBinding.dismissNeverCheckbox.isChecked() && !mBinding.minutesCheckbox.isChecked()) {
                mBinding.dismissNeverCheckbox.setChecked(true);
                mBinding.editValue.setText("");
                updateInputState();
            }
            isUpdatingInputs = false;
        });

        mBinding.editValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingInputs) return;
                updateOkButton();
            }
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingInputs || s.toString().isEmpty()) return;
                try {
                    int val = Integer.parseInt(s.toString());
                    if (mBinding.minutesCheckbox.isChecked() && val > 120) {
                        isUpdatingInputs = true;
                        mBinding.editValue.setText("120");
                        mBinding.editValue.setSelection(3);
                        isUpdatingInputs = false;
                    } else if (mBinding.snoozesCheckbox.isChecked() && val > 24) {
                        isUpdatingInputs = true;
                        mBinding.editValue.setText("24");
                        mBinding.editValue.setSelection(2);
                        isUpdatingInputs = false;
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        mBinding.editValue.setOnEditorActionListener(new ImeDoneListener());

        return CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_alarm_off),
            getString(R.string.dismiss_after_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> setDismissAfterValue(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            alertDialog -> {
                mOkButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                updateOkButton();
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    private void updateInputState() {
        boolean minutesChecked = mBinding.minutesCheckbox.isChecked();
        boolean snoozesChecked = mBinding.snoozesCheckbox.isChecked();

        mBinding.textInputLayout.setEnabled(minutesChecked || snoozesChecked);
        if (minutesChecked) {
            mBinding.textInputLayout.setHint(getString(R.string.label_minutes));
            mBinding.editValue.setHint(getString(R.string.label_minutes));
        } else if (snoozesChecked) {
            mBinding.textInputLayout.setHint(getString(R.string.label_snoozes));
            mBinding.editValue.setHint(getString(R.string.label_snoozes));
        } else {
            mBinding.textInputLayout.setHint(null);
            mBinding.editValue.setHint(null);
        }
        updateOkButton();
    }

    private void updateOkButton() {
        if (mOkButton == null) return;
        boolean never = mBinding.dismissNeverCheckbox.isChecked();
        boolean valueFilled = !TextUtils.isEmpty(mBinding.editValue.getText());

        mOkButton.setEnabled(never || valueFilled);
    }

    private void setDismissAfterValue() {
        int value = 0;
        if (mBinding.minutesCheckbox.isChecked()) {
            value = parseInput(mBinding.editValue.getText());
        } else if (mBinding.snoozesCheckbox.isChecked()) {
            value = -parseInput(mBinding.editValue.getText());
        }

        Bundle result = new Bundle();
        result.putInt(DISMISS_AFTER_VALUE, value);

        if (mPrefKey != null) {
            result.putString(RESULT_PREF_KEY, mPrefKey);
        }

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private int parseInput(CharSequence text) {
        if (TextUtils.isEmpty(text)) return 0;
        try {
            return Integer.parseInt(text.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private class ImeDoneListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mOkButton != null && mOkButton.isEnabled()) {
                    setDismissAfterValue();
                    dismiss();
                    return true;
                }
            }
            return false;
        }
    }
}
