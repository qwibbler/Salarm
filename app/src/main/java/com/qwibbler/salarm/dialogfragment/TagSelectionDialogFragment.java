/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.dialogfragment;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.DialogTagSelectionBinding;
import com.qwibbler.salarm.databinding.ItemTagSelectionBinding;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class TagSelectionDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "tag_selection_request";
    public static final String RESULT_TAGS = "result_tags";
    private static final String ARG_INITIAL_TAGS = "arg_initial_tags";

    private DialogTagSelectionBinding mBinding;
    private TagAdapter mAdapter;
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
        mBinding = DialogTagSelectionBinding.inflate(getLayoutInflater());

        ArrayList<String> initial = requireArguments().getStringArrayList(ARG_INITIAL_TAGS);
        mSelectedTags = initial != null ? new ArrayList<>(initial) : new ArrayList<>();

        mAllTags = Alarm.getAllTags(requireContext().getContentResolver());

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

        mAdapter = new TagAdapter();
        mBinding.tagsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.tagsList.setAdapter(mAdapter);

        mBinding.newTagInput.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext()))));

        mBinding.addTagButton.setOnClickListener(v -> {
            String name = mBinding.newTagInput.getText().toString().trim();
            if (!name.isEmpty()) {
                if (!containsCaseInsensitive(mSelectedTags, name)) {
                    mSelectedTags.add(name);
                    if (!containsCaseInsensitive(mAllTags, name)) {
                        mAllTags.add(name);
                    }
                    mAdapter.notifyDataSetChanged();
                }
                mBinding.newTagInput.setText("");
            }
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
                Bundle result = new Bundle();
                result.putStringArrayList(RESULT_TAGS, new ArrayList<>(mSelectedTags));
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    private boolean containsCaseInsensitive(List<String> list, String value) {
        for (String s : list) {
            if (s.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private class TagAdapter extends RecyclerView.Adapter<TagViewHolder> {
        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new TagViewHolder(ItemTagSelectionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
            String tag = mAllTags.get(position);
            holder.binding.tagCheckbox.setText(tag);
            holder.binding.tagCheckbox.setOnCheckedChangeListener(null);
            holder.binding.tagCheckbox.setChecked(containsCaseInsensitive(mSelectedTags, tag));
            holder.binding.tagCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!containsCaseInsensitive(mSelectedTags, tag)) mSelectedTags.add(tag);
                } else {
                    removeFromListCaseInsensitive(mSelectedTags, tag);
                }
            });
        }

        private void removeFromListCaseInsensitive(List<String> list, String value) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equalsIgnoreCase(value)) {
                    list.remove(i);
                    i--;
                }
            }
        }

        @Override
        public int getItemCount() {
            return mAllTags.size();
        }
    }

    private static class TagViewHolder extends RecyclerView.ViewHolder {
        ItemTagSelectionBinding binding;
        public TagViewHolder(ItemTagSelectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
