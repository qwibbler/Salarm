package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.RangeSlider;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.Utils;

import java.util.Arrays;
import java.util.List;

public class MasjidIntervalsDialogFragment extends DialogFragment {

    public interface OnIntervalsUpdatedListener {
        void onIntervalsUpdated(Masjid masjid);
    }

    private Masjid mMasjid;
    private OnIntervalsUpdatedListener mListener;

    private MaterialButtonToggleGroup mToggleGroup;
    private RangeSlider mSlider;
    private TextView mSummaryText;

    public static void show(FragmentManager manager, Masjid masjid, OnIntervalsUpdatedListener listener) {
        MasjidIntervalsDialogFragment fragment = new MasjidIntervalsDialogFragment();
        fragment.mMasjid = masjid;
        fragment.mListener = listener;
        Utils.showDialogFragment(manager, fragment, "MasjidIntervalsDialogFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_masjid_intervals, null);
        mToggleGroup = view.findViewById(R.id.interval_toggle_group);
        mSlider = view.findViewById(R.id.interval_slider);
        mSummaryText = view.findViewById(R.id.intervals_text_summary);

        setupUI();

        return CustomDialog.create(
                requireContext(),
                R.style.RelationalAlarmDialogTheme,
                null,
                getString(R.string.salah_masjid_intervals_dialog_title),
                null,
                view,
                getString(android.R.string.ok),
                (dialog, which) -> {
                    if (mListener != null) {
                        mListener.onIntervalsUpdated(mMasjid);
                    }
                },
                getString(android.R.string.cancel),
                null,
                null,
                null,
                null,
                CustomDialog.SoftInputMode.NONE
        );
    }

    private void setupUI() {
        mToggleGroup.check(mMasjid.intervalsCount == 2 ? R.id.button_2_intervals : R.id.button_3_intervals);

        mToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.button_2_intervals) {
                    mMasjid.intervalsCount = 2;
                    mMasjid.boundary1 = 16;
                    mMasjid.boundary2 = 31;
                } else {
                    mMasjid.intervalsCount = 3;
                    mMasjid.boundary1 = 10;
                    mMasjid.boundary2 = 20;
                }
                updateSliderFromMasjid();
                updateSummary();
            }
        });

        mSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                List<Float> values = slider.getValues();
                if (mMasjid.intervalsCount == 2) {
                    mMasjid.boundary1 = values.get(0).intValue();
                } else {
                    mMasjid.boundary1 = values.get(0).intValue();
                    mMasjid.boundary2 = values.get(1).intValue();
                }
                updateSummary();
            }
        });

        updateSliderFromMasjid();
        updateSummary();
    }

    private void updateSliderFromMasjid() {
        if (mMasjid.intervalsCount == 2) {
            mSlider.setValues((float) mMasjid.boundary1);
        } else {
            mSlider.setValues((float) mMasjid.boundary1, (float) mMasjid.boundary2);
        }
    }

    private void updateSummary() {
        mSummaryText.setText(mMasjid.getIntervalsSummary(requireContext()));
    }
}
