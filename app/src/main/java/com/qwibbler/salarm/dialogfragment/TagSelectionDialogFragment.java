/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.dialogfragment;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.widget.ArrayAdapter;
import com.google.android.material.chip.Chip;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.AlarmTagSelectionDialogBinding;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class TagSelectionDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "tag_selection_request";
    public static final String RESULT_TAGS = "result_tags";
    private static final String ARG_INITIAL_TAGS = "arg_initial_tags";

    private AlarmTagSelectionDialogBinding mBinding;
    private List<String> mAllTags;
    private List<String> mSelectedTags;

    public static TagSelectionDialogFragment newInstance(ArrayList<String> initialTags) {
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_INITIAL_TAGS, initialTags);
        TagSelectionDialogFragment fragment = new TagSelectionDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = AlarmTagSelectionDialogBinding.inflate(getLayoutInflater());

        ArrayList<String> initial = requireArguments().getStringArrayList(ARG_INITIAL_TAGS);
        mSelectedTags = initial != null ? new ArrayList<>(initial) : new ArrayList<>();

        mAllTags = Alarm.getTagsByUsage(requireContext().getContentResolver());

        // Ensure all currently selected tags are in mAllTags for display
        for (String st : mSelectedTags) {
            boolean exists = false;
            for (String t : mAllTags) {
                if (t.equalsIgnoreCase(st)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                mAllTags.add(st);
            }
        }

        refreshChips();

        mBinding.newTagInput.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext()))));
        mBinding.newTagInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, mAllTags));
        mBinding.newTagInput.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getAdapter().getItem(position);
            if (item != null) {
                addTag(item.toString());
            }
        });
        mBinding.newTagInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                addTag(mBinding.newTagInput.getText().toString().trim());
                return true;
            }
            return false;
        });

        mBinding.newTagInputLayout.setEndIconOnClickListener(v -> {
            String name = mBinding.newTagInput.getText().toString().trim();
            addTag(name);
        });

        return CustomDialog.create(
            requireContext(),
            null,
            null,
            getString(R.string.select_tags_dialog_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> notifyChange(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    private void refreshChips() {
        mBinding.tagsFlexbox.removeAllViews();
        if (mAllTags.isEmpty()) {
            mBinding.emptyTags.setVisibility(android.view.View.VISIBLE);
        } else {
            mBinding.emptyTags.setVisibility(android.view.View.GONE);
            int hMargin = (int) (4 * getResources().getDisplayMetrics().density);
            int vMargin = (int) (0 * getResources().getDisplayMetrics().density);
            for (String tag : mAllTags) {
                Chip chip = new Chip(requireContext());
                chip.setText(tag);
                chip.setCheckable(true);
                chip.setChecked(containsCaseInsensitive(mSelectedTags, tag));

                com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                    new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                params.setMargins(hMargin, vMargin, hMargin, vMargin);
                chip.setLayoutParams(params);

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!containsCaseInsensitive(mSelectedTags, tag)) mSelectedTags.add(tag);
                    } else {
                        removeFromListCaseInsensitive(mSelectedTags, tag);
                    }
                    notifyChange();
                });
                mBinding.tagsFlexbox.addView(chip);
            }
        }
    }

    private void addTag(String name) {
        if (!name.isEmpty()) {
            if (!containsCaseInsensitive(mSelectedTags, name)) {
                mSelectedTags.add(name);
                if (!containsCaseInsensitive(mAllTags, name)) {
                    mAllTags.add(0, name); // Add to top if new
                }
                refreshChips();

                // Update autocomplete adapter
                ArrayAdapter<String> autoAdapter = (ArrayAdapter<String>) mBinding.newTagInput.getAdapter();
                if (autoAdapter != null) {
                    autoAdapter.clear();
                    autoAdapter.addAll(mAllTags);
                    autoAdapter.notifyDataSetChanged();
                }
                notifyChange();
            }
            mBinding.newTagInput.setText("");
        }
    }

    private void notifyChange() {
        Bundle result = new Bundle();
        result.putStringArrayList(RESULT_TAGS, new ArrayList<>(mSelectedTags));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private boolean containsCaseInsensitive(List<String> list, String value) {
        for (String s : list) {
            if (s.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private void removeFromListCaseInsensitive(List<String> list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(value)) {
                list.remove(i);
                i--;
            }
        }
    }
}
