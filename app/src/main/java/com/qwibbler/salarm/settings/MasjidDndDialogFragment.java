package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.databinding.DialogMasjidDndBinding;
import com.qwibbler.salarm.salah.MasjidEvent;
import com.qwibbler.salarm.salah.RelationalAlarmUiHelper;
import com.qwibbler.salarm.salah.RelationalDefinition;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.Utils;

public class MasjidDndDialogFragment extends DialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private static final String ARG_SALAH_TYPE = "arg_salah_type";
    private static final String ARG_FIXED_HOUR = "arg_fixed_hour";
    private static final String ARG_FIXED_MINUTE = "arg_fixed_minute";

    private long mMasjidId;
    private String mSalahType;
    private int mFixedHour;
    private int mFixedMinute;
    private MasjidEvent mEvent;
    private DialogMasjidDndBinding mBinding;

    public static void show(FragmentManager manager, long masjidId, String salahType, int fixedHour, int fixedMinute) {
        MasjidDndDialogFragment fragment = new MasjidDndDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASJID_ID, masjidId);
        args.putString(ARG_SALAH_TYPE, salahType);
        args.putInt(ARG_FIXED_HOUR, fixedHour);
        args.putInt(ARG_FIXED_MINUTE, fixedMinute);
        fragment.setArguments(args);
        Utils.showDialogFragment(manager, fragment, "MasjidDndDialogFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DialogMasjidDndBinding.inflate(getLayoutInflater());
        Bundle args = getArguments();
        if (args != null) {
            mMasjidId = args.getLong(ARG_MASJID_ID);
            mSalahType = args.getString(ARG_SALAH_TYPE);
            mFixedHour = args.getInt(ARG_FIXED_HOUR);
            mFixedMinute = args.getInt(ARG_FIXED_MINUTE);
        }

        RelationalAlarmUiHelper.setupNumericDropdowns(mBinding.dndBeforeValue, mBinding.dndAfterValue);

        final Context context = requireContext();
        final Context appContext = context.getApplicationContext();

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            mEvent = db.masjidEventDao().getMasjidEvent(mMasjidId, mSalahType);
            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || mBinding == null) return;
                if (mEvent != null) {
                    mBinding.dndEnabledCheckbox.setChecked(mEvent.dndEnabled);
                    mBinding.dndBeforeValue.setText(String.valueOf(mEvent.dndBeforeMinutes));
                    mBinding.dndAfterValue.setText(String.valueOf(mEvent.dndAfterMinutes));
                    updateBlockState(mEvent.dndEnabled, mBinding.dndContainer);
                } else {
                    mBinding.dndEnabledCheckbox.setChecked(false);
                    mBinding.dndBeforeValue.setText(String.valueOf(0));
                    mBinding.dndAfterValue.setText(String.valueOf(10));
                    updateBlockState(false, mBinding.dndContainer);
                }
            });
        });

        mBinding.dndEnabledCheckbox.setOnCheckedChangeListener((b, checked) -> updateBlockState(checked, mBinding.dndContainer));

        return CustomDialog.create(
            context,
            R.style.RelationalAlarmDialogTheme,
            null,
            getString(R.string.masjid_dnd_title),
            null,
            mBinding.getRoot(),
            getString(R.string.masjid_save),
            (dialog, which) -> saveEvent(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    private void updateBlockState(boolean enabled, View v) {
        if (v != null) v.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void saveEvent() {
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(requireContext());
            MasjidEvent event = mEvent != null ? mEvent : db.masjidEventDao().getMasjidEvent(mMasjidId, mSalahType);
            if (event == null) {
                event = new MasjidEvent();
                event.masjidId = mMasjidId;
                event.salahType = mSalahType;
                event.isSelected = true;
            }
            if (event.definition == null) {
                event.definition = new RelationalDefinition();
            }

            event.definition.isFixedTime = true;
            event.definition.fixedHour = mFixedHour;
            event.definition.fixedMinute = mFixedMinute;
            event.definition.anchor = null;
            event.definition.calculation = null;
            event.definition.clip = null;

            event.dndEnabled = mBinding.dndEnabledCheckbox.isChecked();
            event.dndBeforeMinutes = parseSafeInt(mBinding.dndBeforeValue.getText().toString(), 0);
            event.dndAfterMinutes = parseSafeInt(mBinding.dndAfterValue.getText().toString(), 10);

            if (event.dndEnabled) {
                NotificationManager nm = requireContext().getSystemService(NotificationManager.class);
                if (nm != null && !nm.isNotificationPolicyAccessGranted()) {
                    AppExecutors.getMainThread().post(() -> {
                        Toast.makeText(requireContext(), R.string.dnd_permission_required, Toast.LENGTH_LONG).show();
                        try {
                            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                        } catch (Exception ignored) {}
                    });
                }
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
}
