package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.uidata.UiDataModel.Tab.SALAH;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Objects;

import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SalahTimesFragment extends SalarmFragment {

    private TextView mCountdownText;
    private TextView mLocalTimeText;
    private SalahAdapter mSalahAdapter;
    private TextView mSelectedDateText;
    private TextView mLocationInfoText;
    private TextView mTimezoneInfoText;
    private TextView mTimezoneWarningText;
    private final Calendar mSelectedDate = Calendar.getInstance();

    private void updateTimezone() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
        String tzId = prefs.getString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        TimeZone tz = TimeZone.getTimeZone(tzId);
        mSelectedDate.setTimeZone(tz);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener = (prefs, key) -> {
        if (PreferencesKeys.KEY_SALAH_CITY_NAME.equals(key) || PreferencesKeys.KEY_SALAH_LATITUDE.equals(key)) {
            updateUI();
        }
    };
    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            mHandler.postDelayed(this, 60000);
        }
    };

    public SalahTimesFragment() {
        super(SALAH);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.salah_times_fragment, container, false);
        mCountdownText = view.findViewById(R.id.salah_countdown);
        mLocalTimeText = view.findViewById(R.id.salah_local_time);
        RecyclerView recyclerView = view.findViewById(R.id.salah_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mSalahAdapter = new SalahAdapter();
        recyclerView.setAdapter(mSalahAdapter);

        mSelectedDateText = view.findViewById(R.id.selected_date_text);
        mLocationInfoText = view.findViewById(R.id.location_info_text);
        mTimezoneInfoText = view.findViewById(R.id.timezone_info_text);
        mTimezoneWarningText = view.findViewById(R.id.salah_timezone_warning);

        view.findViewById(R.id.prev_date_button).setOnClickListener(v -> changeDate(-1));
        view.findViewById(R.id.next_date_button).setOnClickListener(v -> changeDate(1));
        view.findViewById(R.id.date_picker_trigger).setOnClickListener(v -> showDatePicker());
        view.findViewById(R.id.salah_settings_button).setOnClickListener(v -> showSettingsDialog());

        setupHeaderSwipe(view.findViewById(R.id.date_navigation_header));

        return view;
    }

    private void setupHeaderSwipe(View header) {
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) {
                        changeDate(-1);
                    } else {
                        changeDate(1);
                    }
                    return true;
                }
                return false;
            }
        });
        header.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
    }

    private void changeDate(int days) {
        mSelectedDate.add(Calendar.DAY_OF_YEAR, days);
        updateUI();
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date_picker_dialog_title)
                .setSelection(mSelectedDate.getTimeInMillis())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            mSelectedDate.setTimeInMillis(selection);
            updateUI();
        });
        datePicker.show(getParentFragmentManager(), "SALAH_DATE_PICKER");
    }

    private void showSettingsDialog() {
        SalahSettingsDialog dialog = new SalahSettingsDialog();
        dialog.setOnSettingsChangedListener(this::updateUI);
        dialog.show(getParentFragmentManager(), "SALAH_SETTINGS_DIALOG");
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            updateUI();
        } else if (isAdded()) {
            revertToDefaultState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
        updateUI();
        mHandler.post(mUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        prefs.unregisterOnSharedPreferenceChangeListener(mPreferenceListener);

        revertToDefaultState();

        mHandler.removeCallbacks(mUpdateRunnable);
    }

    private void revertToDefaultState() {
        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
        // Revert temporary view overrides when leaving the page
        prefs.edit()
                .remove(PreferencesKeys.KEY_SALAH_VIEW_LATITUDE)
                .remove(PreferencesKeys.KEY_SALAH_VIEW_LONGITUDE)
                .remove(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME)
                .remove(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE)
                .apply();
        mSelectedDate.setTimeInMillis(System.currentTimeMillis());
    }

    private void updateUI() {
        Context context = getContext();
        if (context == null) return;

        updateTimezone();
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);

        // Use local VIEW overrides if present, otherwise fall back to global settings
        String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_VIEW_LATITUDE,
                prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, "0.0"));
        String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_VIEW_LONGITUDE,
                prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, "0.0"));

        double lat, lon;
        try {
            lat = Double.parseDouble(latStr);
            lon = Double.parseDouble(lonStr);
        } catch (NumberFormatException e) {
            lat = 0.0;
            lon = 0.0;
        }

        String methodStr = prefs.getString(PreferencesKeys.KEY_SALAH_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
        CalculationMethod method;
        try {
            method = CalculationMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE;
        }

        SalahCalculator.SalahTimes salahTimes = SalahCalculator.calculateAllSalahTimes(context, mSelectedDate.getTime(), lat, lon, method);

        boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR, false)
                && prefs.getBoolean(PreferencesKeys.KEY_SALAH_SHOW_BOTH_ASR_IN_SALAH_TIMES, false);
        String madhab = prefs.getString(PreferencesKeys.KEY_SALAH_MADHAB, "HANAFI");

        List<PrayerItem> items = new ArrayList<>();
        items.add(new PrayerItem(context.getString(R.string.fajr_label), salahTimes.fajr));
        items.add(new PrayerItem(context.getString(R.string.sunrise_label), salahTimes.sunrise));
        items.add(new PrayerItem(context.getString(R.string.dhuhr_label), salahTimes.dhuhr));

        if (showBothAsr) {
            items.add(new PrayerItem(context.getString(R.string.asr_shafii_label), salahTimes.asrShafi));
            items.add(new PrayerItem(context.getString(R.string.asr_hanafi_label), salahTimes.asrHanafi));
        } else {
            Date asrTime = "HANAFI".equals(madhab) ? salahTimes.asrHanafi : salahTimes.asrShafi;
            items.add(new PrayerItem(context.getString(R.string.asr_label), asrTime));
        }

        items.add(new PrayerItem(context.getString(R.string.maghrib_label), salahTimes.maghrib));
        items.add(new PrayerItem(context.getString(R.string.isha_label), salahTimes.isha));

        mSalahAdapter.setItems(items);

        // Countdown always uses TODAY and GLOBAL settings
        updateCountdown();
        updateDateHeader();
        updateFooter();
    }

    private void updateDateHeader() {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());
        String tzId = prefs.getString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        TimeZone tz = TimeZone.getTimeZone(tzId);

        Calendar today = Calendar.getInstance(tz);
        String label;
        if (isSameDay(mSelectedDate, today)) {
            label = getString(R.string.salarm_today_prefix) + formatDateLabel(mSelectedDate);
        } else {
            Calendar yesterday = Calendar.getInstance(tz);
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(mSelectedDate, yesterday)) {
                label = getString(R.string.salarm_yesterday_prefix) + formatDateLabel(mSelectedDate);
            } else {
                Calendar tomorrow = Calendar.getInstance(tz);
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                if (isSameDay(mSelectedDate, tomorrow)) {
                    label = getString(R.string.salarm_tomorrow_prefix) + formatDateLabel(mSelectedDate);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.salarm_full_date_format), Locale.getDefault());
                    sdf.setTimeZone(tz);
                    label = sdf.format(mSelectedDate.getTime());
                }
            }
        }
        mSelectedDateText.setText(label);
    }

    private String formatDateLabel(Calendar date) {
        return new SimpleDateFormat(getString(R.string.salarm_short_date_format), Locale.getDefault()).format(date.getTime());
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void updateFooter() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);

        String cityName = prefs.getString(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME,
                prefs.getString(PreferencesKeys.KEY_SALAH_CITY_NAME, context.getString(R.string.salarm_unknown_location)));

        SalahLocationManager.LocationMode mode = SalahLocationManager.getLocationMode(context);
        boolean isOverride = prefs.contains(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME);

        String tzId = prefs.getString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        TimeZone tz = TimeZone.getTimeZone(tzId);
        String tzName = tz.getDisplayName(false, TimeZone.SHORT);

        mLocationInfoText.setText(String.format("%s (%s%s)", cityName, mode.name(), isOverride ? "/View" : ""));
        mTimezoneInfoText.setText(String.format("%s • %s", tzId, tzName));

        // Timezone Warning Logic
        // Compare current viewing timezone with the phone's system timezone.
        // We show a warning if they differ, as times shown won't match the phone's clock.
        String systemTzId = TimeZone.getDefault().getID();
        boolean showWarning = !Objects.equals(tzId, systemTzId);

        if (showWarning) {
            mTimezoneWarningText.setText(getString(R.string.salah_timezone_warning, tzId, systemTzId));
            mTimezoneWarningText.setVisibility(View.VISIBLE);
        } else {
            mTimezoneWarningText.setVisibility(View.GONE);
        }
    }

    private void updateCountdown() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);

        String tzId = prefs.getString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        TimeZone tz = TimeZone.getTimeZone(tzId);

        // Countdown uses the location currently being viewed (preview or global)
        String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_VIEW_LATITUDE,
                prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, "0.0"));
        String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_VIEW_LONGITUDE,
                prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, "0.0"));

        double lat, lon;
        try {
            lat = Double.parseDouble(latStr);
            lon = Double.parseDouble(lonStr);
        } catch (NumberFormatException e) {
            lat = 0.0;
            lon = 0.0;
        }

        String methodStr = prefs.getString(PreferencesKeys.KEY_SALAH_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
        CalculationMethod method = CalculationMethod.valueOf(methodStr);

        Calendar todayCal = Calendar.getInstance(tz);
        SalahCalculator.SalahTimes todaySalahTimes = SalahCalculator.calculateAllSalahTimes(context, todayCal.getTime(), lat, lon, method);

        Date now = new Date();
        PrayerItem nextPrayer;

        boolean isViewingToday = isSameDay(mSelectedDate, todayCal);
        mCountdownText.setAlpha(isViewingToday ? 1.0f : 0.5f);

        String madhabStr = prefs.getString(PreferencesKeys.KEY_SALAH_MADHAB, "HANAFI");
        boolean isShafi = "SHAFI".equals(madhabStr);
        Date asrTime = isShafi ? todaySalahTimes.asrShafi : todaySalahTimes.asrHanafi;

        if (now.before(todaySalahTimes.fajr)) nextPrayer = new PrayerItem(context.getString(R.string.fajr_label), todaySalahTimes.fajr);
        else if (now.before(todaySalahTimes.sunrise)) nextPrayer = new PrayerItem(context.getString(R.string.sunrise_label), todaySalahTimes.sunrise);
        else if (now.before(todaySalahTimes.dhuhr)) nextPrayer = new PrayerItem(context.getString(R.string.dhuhr_label), todaySalahTimes.dhuhr);
        else if (now.before(asrTime)) nextPrayer = new PrayerItem(context.getString(R.string.asr_label), asrTime);
        else if (now.before(todaySalahTimes.maghrib)) nextPrayer = new PrayerItem(context.getString(R.string.maghrib_label), todaySalahTimes.maghrib);
        else if (now.before(todaySalahTimes.isha)) nextPrayer = new PrayerItem(context.getString(R.string.isha_label), todaySalahTimes.isha);
        else {
            // Next day's Fajr
            Calendar tomorrow = Calendar.getInstance(tz);
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            SalahCalculator.SalahTimes tomorrowSalahTimes = SalahCalculator.calculateAllSalahTimes(context, tomorrow.getTime(), lat, lon, method);
            nextPrayer = new PrayerItem(context.getString(R.string.fajr_label), tomorrowSalahTimes.fajr);
        }

        long diff = nextPrayer.time.getTime() - now.getTime();
        long hours = diff / (3600 * 1000);
        long minutes = (diff / (60 * 1000)) % 60;
        String countdown = getString(R.string.salarm_salah_countdown_format, hours, minutes, nextPrayer.name);
        mCountdownText.setText(countdown);

        // Local time in preview timezone
        String systemTzId = TimeZone.getDefault().getID();
        if (!Objects.equals(tzId, systemTzId)) {
            java.text.DateFormat localFormat = android.text.format.DateFormat.getTimeFormat(context);
            localFormat.setTimeZone(tz);
            String localTime = localFormat.format(now);
            mLocalTimeText.setText(String.format("(%s in %s)", localTime, tzId));
            mLocalTimeText.setVisibility(View.VISIBLE);
        } else {
            mLocalTimeText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setVisibility(View.GONE);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setVisibility(View.GONE);
        right.setVisibility(View.GONE);
    }

    @Override
    public void onFabClick() {
    }

    private static class PrayerItem {
        String name;
        Date time;

        PrayerItem(String name, Date time) {
            this.name = name;
            this.time = time;
        }
    }

    private class SalahAdapter extends RecyclerView.Adapter<SalahAdapter.ViewHolder> {
        private List<PrayerItem> mItems = new ArrayList<>();

        void setItems(final List<PrayerItem> items) {
            final List<PrayerItem> oldItems = mItems;
            mItems = items;
            androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldItems.size();
                }

                @Override
                public int getNewListSize() {
                    return mItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldItems.get(oldItemPosition).name.equals(mItems.get(newItemPosition).name);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    PrayerItem oldItem = oldItems.get(oldItemPosition);
                    PrayerItem newItem = mItems.get(newItemPosition);
                    return oldItem.name.equals(newItem.name) && oldItem.time.equals(newItem.time);
                }
            }).dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.prayer_time_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PrayerItem item = mItems.get(position);
            holder.name.setText(item.name);
            holder.itemView.setOnClickListener(v -> { /* Just to trigger ripple */ });

            Context context = getContext();
            if (context == null) return;

            SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
            String tzId = prefs.getString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
            TimeZone tz = TimeZone.getTimeZone(tzId);
            java.text.DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
            format.setTimeZone(tz);
            holder.time.setText(format.format(item.time));

            // Highlight current prayer
            Date now = new Date();
            boolean isCurrent;
            if (position < mItems.size() - 1) {
                isCurrent = now.after(item.time) && now.before(mItems.get(position + 1).time);
            } else {
                // Cyclical highlight: Isha is active if it's after Isha time OR before the first prayer of the day (Fajr)
                isCurrent = now.after(item.time) || now.before(mItems.get(0).time);
            }

            // Only highlight if it's today in the preview timezone
            Calendar previewNow = Calendar.getInstance(tz);
            boolean isToday = isSameDay(mSelectedDate, previewNow);

            if (isToday && isCurrent) {
                @ColorInt int highlightColor = MaterialColors.getColor(context,
                        com.google.android.material.R.attr.colorPrimaryContainer, Color.BLUE);
                @ColorInt int onHighlightColor = MaterialColors.getColor(context,
                        com.google.android.material.R.attr.colorOnPrimaryContainer, Color.WHITE);
                holder.itemView.setBackground(ThemeUtils.rippleDrawable(context, new ColorDrawable(highlightColor)));
                holder.name.setTextColor(onHighlightColor);
                holder.time.setTextColor(onHighlightColor);
            } else {
                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                holder.itemView.setBackgroundResource(outValue.resourceId);

                @ColorInt int defaultTextColor = MaterialColors.getColor(context,
                        android.R.attr.textColorPrimary, Color.BLACK);
                holder.name.setTextColor(defaultTextColor);
                holder.time.setTextColor(defaultTextColor);
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView time;

            ViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.prayer_name);
                time = view.findViewById(R.id.prayer_time);
            }
        }
    }
}
