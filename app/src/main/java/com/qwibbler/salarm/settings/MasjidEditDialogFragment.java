package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.Utils;

import java.util.List;

public class MasjidEditDialogFragment extends DialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private long mMasjidId;
    private Masjid mMasjid;

    private TextInputEditText mNameEdit;
    private TextView mIntervalsSummary;

    public static void show(FragmentManager manager, long masjidId) {
        MasjidEditDialogFragment fragment = new MasjidEditDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASJID_ID, masjidId);
        fragment.setArguments(args);
        Utils.showDialogFragment(manager, fragment, "MasjidEditDialogFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_masjid, null);
        mNameEdit = view.findViewById(R.id.masjid_name_edit);
        mIntervalsSummary = view.findViewById(R.id.masjid_intervals_summary);
        View intervalsRow = view.findViewById(R.id.masjid_intervals_row);

        mMasjidId = getArguments().getLong(ARG_MASJID_ID);

        if (mMasjidId > 0) {
            AppExecutors.getDiskIO().execute(() -> {
                mMasjid = SalahDatabase.getDatabase(requireContext()).masjidDao().getById(mMasjidId);
                AppExecutors.getMainThread().post(() -> {
                    if (isAdded() && mMasjid != null) {
                        mNameEdit.setText(mMasjid.name);
                        updateIntervalsSummary();
                    }
                });
            });
        } else {
            mMasjid = new Masjid();
            mMasjid.name = getString(R.string.salah_masjid_default_name);
            mNameEdit.setText(mMasjid.name);
            updateIntervalsSummary();
        }

        intervalsRow.setOnClickListener(v -> {
            MasjidIntervalsDialogFragment.show(getChildFragmentManager(), mMasjid, (updatedMasjid) -> {
                mMasjid = updatedMasjid;
                updateIntervalsSummary();
            });
        });

        return CustomDialog.create(
                requireContext(),
                R.style.RelationalAlarmDialogTheme,
                null,
                getString(mMasjidId > 0 ? R.string.salah_masjid_edit_title : R.string.salah_add_new_masjid),
                null,
                view,
                getString(R.string.salah_masjid_save),
                (dialog, which) -> saveMasjid(),
                getString(android.R.string.cancel),
                null,
                mMasjidId > 0 ? getString(R.string.delete) : null,
                (dialog, which) -> deleteMasjid(),
                null,
                CustomDialog.SoftInputMode.NONE
        );
    }

    private void updateIntervalsSummary() {
        if (mMasjid != null) {
            mIntervalsSummary.setText(mMasjid.getIntervalsSummary(requireContext()));
        }
    }

    private void saveMasjid() {
        if (mMasjid == null) return;
        mMasjid.name = mNameEdit.getText().toString();
        AppExecutors.getDiskIO().execute(() -> {
            if (mMasjid.id == 0) {
                SalahDatabase.getDatabase(requireContext()).masjidDao().insert(mMasjid);
            } else {
                SalahDatabase.getDatabase(requireContext()).masjidDao().update(mMasjid);
            }
        });
    }

    private void deleteMasjid() {
        if (mMasjidId > 0) {
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase.getDatabase(requireContext()).masjidDao().deleteById(mMasjidId);
                // Also delete related rules?
                List<RelationalRule> rules = SalahDatabase.getDatabase(requireContext()).relationalRuleDao().getAll();
                for (RelationalRule r : rules) {
                    if (r.masjidId == mMasjidId) {
                        SalahDatabase.getDatabase(requireContext()).relationalRuleDao().delete(r);
                    }
                }
                SalahAlarmSyncManager.syncSelectedRulesToAlarms(requireContext());
            });
        }
    }
}
