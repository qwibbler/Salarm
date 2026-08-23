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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TagSelectionDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "tag_selection_request";
    public static final String RESULT_TAGS = "result_tags";
    private static final String ARG_INITIAL_TAGS = "arg_initial_tags";

    private AlarmTagSelectionDialogBinding mBinding;
    private List<String> mAllTags;
    private Set<String> mSelectedTags;
    private ArrayAdapter<String> mAutoAdapter;

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
        mSelectedTags = initial != null ? new LinkedHashSet<>(initial) : new LinkedHashSet<>();

        mAllTags = Alarm.getTagsByUsage(requireContext().getContentResolver());

        // Ensure all currently selected tags are in mAllTags for display
        Set<String> allTagsSet = new LinkedHashSet<>(mAllTags);
        allTagsSet.addAll(mSelectedTags);
        mAllTags = new ArrayList<>(allTagsSet);

        refreshChips();

        mBinding.newTagInput.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext()))));
        mAutoAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, mAllTags);
        mBinding.newTagInput.setAdapter(mAutoAdapter);
        mBinding.newTagInput.setOnItemClickListener((parent, view, position, id) -> {
            String item = mAutoAdapter.getItem(position);
            if (item != null) {
                addTag(item);
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
            (d, w) -> {
                String name = mBinding.newTagInput.getText().toString().trim();
                if (!name.isEmpty()) {
                    addTag(name);
                }
                notifyChange();
            },
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
                chip.setChecked(mSelectedTags.contains(tag));

                com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                    new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                params.setMargins(hMargin, vMargin, hMargin, vMargin);
                chip.setLayoutParams(params);

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        mSelectedTags.add(tag);
                    } else {
                        mSelectedTags.remove(tag);
                    }
                    notifyChange();
                });
                mBinding.tagsFlexbox.addView(chip);
            }
        }
    }

    private void addTag(String name) {
        if (!name.isEmpty()) {
            if (mSelectedTags.add(name)) {
                if (!mAllTags.contains(name)) {
                    mAllTags.add(0, name); // Add to top if new
                }
                refreshChips();

                // Update autocomplete adapter
                if (mAutoAdapter != null) {
                    mAutoAdapter.clear();
                    mAutoAdapter.addAll(mAllTags);
                    mAutoAdapter.notifyDataSetChanged();
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
}
