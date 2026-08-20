/*
 * Copyright (C) 2026 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.settings;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.provider.ClockContract.AlarmLogColumns;
import com.qwibbler.salarm.uicomponents.CollapsingToolbarBaseActivity;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.AlarmUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmLogFragment extends BaseSettingsScreenFragment {

    private LogAdapter mAdapter;
    private Spinner mWindowSpinner;
    private long mWindowMillis = 24 * 60 * 60 * 1000L;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.alarm_log_title);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // No-op for this fragment as it doesn't use standard preferences
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm_log, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.log_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mAdapter = new LogAdapter();
        recyclerView.setAdapter(mAdapter);

        mWindowSpinner = view.findViewById(R.id.window_spinner);
        setupSpinner();

        view.findViewById(R.id.clear_button).setOnClickListener(v -> showClearConfirmDialog());

        loadLogs();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Ensure the toolbar title is set correctly
        CollapsingToolbarBaseActivity activity = (CollapsingToolbarBaseActivity) requireActivity();
        if (activity.getBaseBinding() != null) {
            activity.getBaseBinding().collapsingToolbar.setTitle(getFragmentTitle());
            activity.getBaseBinding().appBar.setExpanded(true, true);
        }
    }

    private void setupSpinner() {
        String[] options = {
                getString(R.string.alarm_log_window_24h),
                getString(R.string.alarm_log_window_3d),
                getString(R.string.alarm_log_window_7d)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mWindowSpinner.setAdapter(adapter);

        mWindowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0 -> mWindowMillis = 24 * 60 * 60 * 1000L;
                    case 1 -> mWindowMillis = 3 * 24 * 60 * 60 * 1000L;
                    case 2 -> mWindowMillis = 7 * 24 * 60 * 60 * 1000L;
                }
                loadLogs();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showClearConfirmDialog() {
        CustomDialog.create(
                requireContext(),
                null,
                null,
                getString(R.string.warning),
                getString(R.string.alarm_log_clear_confirm),
                null,
                getString(R.string.delete),
                (dialog, which) -> clearLogs(),
                getString(android.R.string.cancel),
                null,
                null,
                null,
                null,
                CustomDialog.SoftInputMode.NONE
        ).show();
    }

    private void clearLogs() {
        requireContext().getContentResolver().delete(AlarmLogColumns.CONTENT_URI, null, null);
        loadLogs();
    }

    private void loadLogs() {
        long cutoff = System.currentTimeMillis() - mWindowMillis;
        String selection = AlarmLogColumns.EVENT_TIME + " > ?";
        String[] selectionArgs = {String.valueOf(cutoff)};
        String sortOrder = AlarmLogColumns.EVENT_TIME + " DESC";

        List<Object> items = new ArrayList<>();
        Context context = getContext();
        if (context == null) return;

        try (Cursor cursor = context.getContentResolver().query(
                AlarmLogColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                String lastDay = "";
                do {
                    long eventTime = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmLogColumns.EVENT_TIME));
                    String currentDay = getDayString(eventTime);
                    if (!currentDay.equals(lastDay)) {
                        items.add(new HeaderItem(currentDay));
                        lastDay = currentDay;
                    }
                    items.add(new LogEntry(cursor, context));
                } while (cursor.moveToNext());
            }
        }
        mAdapter.setItems(items);
    }

    private String getDayString(long timeMillis) {
        if (DateUtils.isToday(timeMillis)) {
            return getString(R.string.today);
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        if (DateUtils.isToday(timeMillis + DateUtils.DAY_IN_MILLIS)) {
            return getString(R.string.yesterday);
        }
        return new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date(timeMillis));
    }

    private static class HeaderItem {
        final String title;
        HeaderItem(String title) { this.title = title; }
    }

    private static class LogEntry {
        final String time;
        final String text;

        LogEntry(Cursor cursor, Context context) {
            long eventTime = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmLogColumns.EVENT_TIME));
            this.time = AlarmUtils.getFormattedTime(context, eventTime);

            String label = cursor.getString(cursor.getColumnIndexOrThrow(AlarmLogColumns.LABEL));
            long firedTime = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmLogColumns.FIRED_TIME));
            if (label == null || label.isEmpty()) {
                String originalTime = AlarmUtils.getFormattedTime(context, firedTime);
                label = context.getString(R.string.default_label) + " (" + originalTime + ")";
            }
            final String finalLabel = label;

            int eventType = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmLogColumns.EVENT_TYPE));
            int snoozes = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmLogColumns.SNOOZE_COUNT));
            long extraInfo = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmLogColumns.EXTRA_INFO));

            String eventStr;
            switch (eventType) {
                case AlarmLogColumns.EVENT_FIRED -> eventStr = context.getString(R.string.alarm_log_fired);
                case AlarmLogColumns.EVENT_SNOOZED -> {
                    if (extraInfo > 0) {
                        String untilTime = AlarmUtils.getFormattedTime(context, extraInfo);
                        eventStr = context.getString(R.string.alarm_log_snoozed_until, untilTime);
                    } else {
                        eventStr = context.getString(R.string.alarm_log_snoozed);
                    }
                }
                case AlarmLogColumns.EVENT_MISSED -> eventStr = context.getString(R.string.alarm_log_missed);
                case AlarmLogColumns.EVENT_TIMED_OUT -> eventStr = context.getString(R.string.alarm_log_timed_out);
                case AlarmLogColumns.EVENT_DISMISSED -> eventStr = context.getString(R.string.alarm_log_dismissed);
                default -> eventStr = "Unknown event";
            }

            if ((eventType == AlarmLogColumns.EVENT_DISMISSED || eventType == AlarmLogColumns.EVENT_MISSED || eventType == AlarmLogColumns.EVENT_TIMED_OUT) && snoozes > 0) {
                eventStr = context.getString(R.string.alarm_log_snoozed_count, snoozes) + ", " + eventStr;
            }

            this.text = finalLabel + " - " + eventStr;
        }
    }

    private static class LogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ENTRY = 1;
        private final List<Object> mItems = new ArrayList<>();

        void setItems(List<Object> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return (mItems.get(position) instanceof HeaderItem) ? TYPE_HEADER : TYPE_ENTRY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_alarm_log, parent, false);
                return new LogViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = mItems.get(position);
            if (holder instanceof HeaderViewHolder headerHolder) {
                headerHolder.title.setText(((HeaderItem) item).title);
                headerHolder.title.setTextSize(14);
                headerHolder.title.setPadding(0, 16, 0, 8);
                headerHolder.title.setAlpha(0.7f);
            } else if (holder instanceof LogViewHolder logHolder) {
                LogEntry entry = (LogEntry) item;
                logHolder.time.setText(entry.time);
                logHolder.text.setText(entry.text);
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
        }
    }

    private static class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView time;
        final TextView text;

        LogViewHolder(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.log_item_time);
            text = itemView.findViewById(R.id.log_item_text);
        }
    }
}
