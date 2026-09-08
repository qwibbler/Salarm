package com.qwibbler.salarm.settings;

import android.app.Dialog;
import android.content.Context;
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
import com.qwibbler.salarm.salah.MasjidEvent;
import com.qwibbler.salarm.salah.RelationalDefinition;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        intervalsRow.setOnClickListener(v ->
            MasjidIntervalsDialogFragment.show(getChildFragmentManager(), mMasjid, (updatedMasjid) -> {
                mMasjid = updatedMasjid;
                updateIntervalsSummary();
            })
        );

        List<CustomDialog.DialogButton> buttons = new ArrayList<>();
        if (mMasjidId > 0) {
            buttons.add(new CustomDialog.DialogButton(getString(R.string.delete), (dialog, which) -> deleteMasjid()));
        }
        buttons.add(new CustomDialog.DialogButton(getString(android.R.string.cancel), null));
        if (mMasjidId > 0) {
            buttons.add(new CustomDialog.DialogButton(getString(R.string.salah_masjid_copy), (dialog, which) -> copyMasjid()));
        }
        buttons.add(new CustomDialog.DialogButton(getString(R.string.salah_masjid_save), (dialog, which) -> saveMasjid()));

        return CustomDialog.create(
                requireContext(),
                R.style.RelationalAlarmDialogTheme,
                null,
                getString(mMasjidId > 0 ? R.string.salah_masjid_edit_title : R.string.salah_add_new_masjid),
                null,
                view,
                null,
                CustomDialog.SoftInputMode.NONE,
                buttons.toArray(new CustomDialog.DialogButton[0])
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
        final Context appContext = requireContext().getApplicationContext();
        final FragmentManager fragmentManager = getParentFragmentManager();
        AppExecutors.getDiskIO().execute(() -> {
            if (mMasjid.id == 0) {
                SalahDatabase.getDatabase(appContext).masjidDao().insert(mMasjid);
            } else {
                SalahDatabase.getDatabase(appContext).masjidDao().update(mMasjid);
            }
            fragmentManager.setFragmentResult("masjid_updated", new Bundle());
        });
    }

    private void deleteMasjid() {
        if (mMasjidId > 0) {
            final Context context = requireContext();
            final Context appContext = context.getApplicationContext();
            final FragmentManager fragmentManager = getParentFragmentManager();
            CustomDialog.create(
                context,
                null,
                null,
                getString(R.string.delete),
                getString(R.string.salah_masjid_delete_confirm),
                null,
                null,
                CustomDialog.SoftInputMode.NONE,
                new CustomDialog.DialogButton(getString(android.R.string.cancel), null),
                new CustomDialog.DialogButton(getString(R.string.delete), (dialog, which) -> {
                    AppExecutors.getDiskIO().execute(() -> {
                        SalahDatabase db = SalahDatabase.getDatabase(appContext);
                        db.masjidDao().deleteById(mMasjidId);

                        // Delete associated events
                        List<MasjidEvent> events = db.masjidEventDao().getByMasjidId(mMasjidId);
                        for (MasjidEvent e : events) {
                            db.masjidEventDao().delete(e);
                        }

                        List<RelationalRule> rules = db.relationalRuleDao().getAll();
                        for (RelationalRule r : rules) {
                            if (r.masjidId == mMasjidId) {
                                db.relationalRuleDao().delete(r);
                            }
                        }
                        SalahAlarmSyncManager.syncSelectedRulesToAlarms(appContext);
                        fragmentManager.setFragmentResult("masjid_updated", new Bundle());
                    });
                    dismiss();
                })
            ).show();
        }
    }

    private void copyMasjid() {
        if (mMasjid == null || mMasjidId <= 0) return;

        final Context appContext = requireContext().getApplicationContext();
        final FragmentManager fragmentManager = getParentFragmentManager();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);

            // 1. Copy Masjid
            Masjid newMasjid = new Masjid();
            newMasjid.name = mMasjid.name + ' ' + getString(R.string.salah_masjid_copy_suffix);
            newMasjid.showMasjidEvents = mMasjid.showMasjidEvents;
            newMasjid.intervalsCount = mMasjid.intervalsCount;
            newMasjid.boundary1 = mMasjid.boundary1;
            newMasjid.boundary2 = mMasjid.boundary2;

            long newId = db.masjidDao().insert(newMasjid);

            // 2. Copy MasjidEvents
            List<MasjidEvent> oldEvents = db.masjidEventDao().getByMasjidId(mMasjidId);
            Map<Long, Long> eventIdMap = new HashMap<>();
            for (MasjidEvent oldEvent : oldEvents) {
                MasjidEvent newEvent = new MasjidEvent();
                newEvent.masjidId = newId;
                newEvent.salahType = oldEvent.salahType;
                newEvent.isSelected = oldEvent.isSelected;
                newEvent.isManuallyDeselected = oldEvent.isManuallyDeselected;
                newEvent.definition = new RelationalDefinition(oldEvent.definition);

                long newEventId = db.masjidEventDao().insert(newEvent);
                eventIdMap.put(oldEvent.id, newEventId);
            }

            // 3. Copy Rules
            List<RelationalRule> rules = db.relationalRuleDao().getByMasjidId(mMasjidId);
            for (RelationalRule rule : rules) {
                RelationalRule newRule = new RelationalRule(rule);
                newRule.id = 0; // New record
                newRule.masjidId = newId;

                // Update anchors if they point to masjid events of the same masjid
                if (newRule.definition.anchor.masjidEventId != null && eventIdMap.containsKey(newRule.definition.anchor.masjidEventId)) {
                    newRule.definition.anchor.masjidEventId = eventIdMap.get(newRule.definition.anchor.masjidEventId);
                }
                if (newRule.definition.clip != null && newRule.definition.clip.anchor.masjidEventId != null && eventIdMap.containsKey(newRule.definition.clip.anchor.masjidEventId)) {
                    newRule.definition.clip.anchor.masjidEventId = eventIdMap.get(newRule.definition.clip.anchor.masjidEventId);
                }
                if (newRule.autostopAnchor.masjidEventId != null && eventIdMap.containsKey(newRule.autostopAnchor.masjidEventId)) {
                    newRule.autostopAnchor.masjidEventId = eventIdMap.get(newRule.autostopAnchor.masjidEventId);
                }

                db.relationalRuleDao().insert(newRule);
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    fragmentManager.setFragmentResult("masjid_updated", new Bundle());
                    dismiss();
                }
            });
        });
    }
}
