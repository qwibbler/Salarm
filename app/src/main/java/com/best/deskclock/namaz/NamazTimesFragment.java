package com.best.deskclock.namaz;

import static com.best.deskclock.uidata.UiDataModel.Tab.NAMAZ;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.base.DeskClockFragment;
import com.best.deskclock.settings.PreferencesKeys;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NamazTimesFragment extends DeskClockFragment {

    private TextView mCountdownText;
    private RecyclerView mRecyclerView;
    private NamazAdapter mAdapter;
    private TextView mSelectedDateText;
    private TextView mLocationInfoText;
    private final Calendar mSelectedDate = Calendar.getInstance();

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener = (prefs, key) -> {
        if (PreferencesKeys.KEY_NAMAZ_CITY_NAME.equals(key) || PreferencesKeys.KEY_NAMAZ_LATITUDE.equals(key)) {
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

    public NamazTimesFragment() {
        super(NAMAZ);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.namaz_times_fragment, container, false);
        mCountdownText = view.findViewById(R.id.namaz_countdown);
        mRecyclerView = view.findViewById(R.id.namaz_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new NamazAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mSelectedDateText = view.findViewById(R.id.selected_date_text);
        mLocationInfoText = view.findViewById(R.id.location_info_text);

        view.findViewById(R.id.prev_date_button).setOnClickListener(v -> changeDate(-1));
        view.findViewById(R.id.next_date_button).setOnClickListener(v -> changeDate(1));
        view.findViewById(R.id.date_picker_trigger).setOnClickListener(v -> showDatePicker());
        view.findViewById(R.id.namaz_settings_button).setOnClickListener(v -> showSettingsDialog());

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
                .setTitleText("Select Date")
                .setSelection(mSelectedDate.getTimeInMillis())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            mSelectedDate.setTimeInMillis(selection);
            updateUI();
        });
        datePicker.show(getParentFragmentManager(), "NAMAZ_DATE_PICKER");
    }

    private void showSettingsDialog() {
        NamazSettingsDialog dialog = new NamazSettingsDialog();
        dialog.setOnSettingsChangedListener(this::updateUI);
        dialog.show(getParentFragmentManager(), "NAMAZ_SETTINGS_DIALOG");
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(requireContext());
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
        updateUI();
        mHandler.post(mUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(requireContext());
        prefs.unregisterOnSharedPreferenceChangeListener(mPreferenceListener);

        // Revert temporary view overrides when leaving the page
        prefs.edit()
                .remove(PreferencesKeys.KEY_NAMAZ_VIEW_LATITUDE)
                .remove(PreferencesKeys.KEY_NAMAZ_VIEW_LONGITUDE)
                .remove(PreferencesKeys.KEY_NAMAZ_VIEW_CITY_NAME)
                .remove(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE)
                .apply();
        mSelectedDate.setTimeInMillis(System.currentTimeMillis());

        mHandler.removeCallbacks(mUpdateRunnable);
    }

    private void updateUI() {
        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);

        // Use local VIEW overrides if present, otherwise fall back to global settings
        String latStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_VIEW_LATITUDE,
                prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, "0.0"));
        String lonStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_VIEW_LONGITUDE,
                prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, "0.0"));

        double lat, lon;
        try {
            lat = Double.parseDouble(latStr);
            lon = Double.parseDouble(lonStr);
        } catch (NumberFormatException e) {
            lat = 0.0;
            lon = 0.0;
        }

        String methodStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
        CalculationMethod method;
        try {
            method = CalculationMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE;
        }

        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(mSelectedDate.getTime(), lat, lon, method);

        boolean showBothAsr = prefs.getBoolean(PreferencesKeys.KEY_NAMAZ_SHOW_BOTH_ASR_IN_NAMAZ_TIMES, false);
        String madhab = prefs.getString(PreferencesKeys.KEY_NAMAZ_MADHAB, "HANAFI");

        List<PrayerItem> items = new ArrayList<>();
        items.add(new PrayerItem("Fajr", times.fajr));
        items.add(new PrayerItem("Sunrise", times.sunrise));
        items.add(new PrayerItem("Dhuhr", times.dhuhr));

        if (showBothAsr) {
            items.add(new PrayerItem(context.getString(R.string.asr_shafii_label), times.asrShafi));
            items.add(new PrayerItem(context.getString(R.string.asr_hanafi_label), times.asrHanafi));
        } else {
            Date asrTime = madhab.equals("HANAFI") ? times.asrHanafi : times.asrShafi;
            items.add(new PrayerItem(context.getString(R.string.asr_label), asrTime));
        }

        items.add(new PrayerItem("Maghrib", times.maghrib));
        items.add(new PrayerItem("Isha", times.isha));

        mAdapter.setItems(items);

        // Countdown always uses TODAY and GLOBAL settings
        updateCountdown();
        updateDateHeader();
        updateFooter();
    }

    private void updateDateHeader() {
        Calendar today = Calendar.getInstance();
        String label;
        if (isSameDay(mSelectedDate, today)) {
            label = "Today, " + formatDateLabel(mSelectedDate);
        } else {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(mSelectedDate, yesterday)) {
                label = "Yesterday, " + formatDateLabel(mSelectedDate);
            } else {
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                if (isSameDay(mSelectedDate, tomorrow)) {
                    label = "Tomorrow, " + formatDateLabel(mSelectedDate);
                } else {
                    label = new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(mSelectedDate.getTime());
                }
            }
        }
        mSelectedDateText.setText(label);
    }

    private String formatDateLabel(Calendar date) {
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date.getTime());
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void updateFooter() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);

        String cityName = prefs.getString(PreferencesKeys.KEY_NAMAZ_VIEW_CITY_NAME,
                prefs.getString(PreferencesKeys.KEY_NAMAZ_CITY_NAME, "Unknown"));

        NamazLocationManager.LocationMode mode = NamazLocationManager.getLocationMode(context);
        boolean isOverride = prefs.contains(PreferencesKeys.KEY_NAMAZ_VIEW_CITY_NAME);

        String tzId = prefs.getString(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        TimeZone tz = TimeZone.getTimeZone(tzId);
        String tzName = tz.getDisplayName(false, TimeZone.SHORT);

        String footerText = String.format("%s (%s%s) • %s", cityName, mode.name(), isOverride ? "/View" : "", tzName);
        mLocationInfoText.setText(footerText);
    }

    private void updateCountdown() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);

        // Countdown uses GLOBAL coordinates and TODAY's date
        String latStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, "0.0");
        String lonStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, "0.0");
        double lat = Double.parseDouble(latStr);
        double lon = Double.parseDouble(lonStr);

        String methodStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
        CalculationMethod method = CalculationMethod.valueOf(methodStr);

        Calendar todayCal = Calendar.getInstance();
        NamazCalculator.NamazTimes todayTimes = NamazCalculator.calculateAllTimes(todayCal.getTime(), lat, lon, method);

        Date now = new Date();
        PrayerItem nextPrayer = null;

        boolean isViewingToday = isSameDay(mSelectedDate, todayCal);
        mCountdownText.setAlpha(isViewingToday ? 1.0f : 0.5f);

        if (now.before(todayTimes.fajr)) nextPrayer = new PrayerItem("Fajr", todayTimes.fajr);
        else if (now.before(todayTimes.sunrise)) nextPrayer = new PrayerItem("Sunrise", todayTimes.sunrise);
        else if (now.before(todayTimes.dhuhr)) nextPrayer = new PrayerItem("Dhuhr", todayTimes.dhuhr);
        else if (now.before(todayTimes.asrHanafi)) nextPrayer = new PrayerItem("Asr", todayTimes.asrHanafi);
        else if (now.before(todayTimes.maghrib)) nextPrayer = new PrayerItem("Maghrib", todayTimes.maghrib);
        else if (now.before(todayTimes.isha)) nextPrayer = new PrayerItem("Isha", todayTimes.isha);
        else {
            // Next day's Fajr (still based on GLOBAL location)
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            NamazCalculator.NamazTimes tomorrowTimes = NamazCalculator.calculateAllTimes(tomorrow.getTime(), lat, lon, method);
            nextPrayer = new PrayerItem("Fajr", tomorrowTimes.fajr);
        }

        if (nextPrayer != null) {
            long diff = nextPrayer.time.getTime() - now.getTime();
            long hours = diff / (3600 * 1000);
            long minutes = (diff / (60 * 1000)) % 60;
            String countdown = String.format(Locale.getDefault(), "%dh:%02dm To %s", hours, minutes, nextPrayer.name);
            mCountdownText.setText(countdown);
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

    private class NamazAdapter extends RecyclerView.Adapter<NamazAdapter.ViewHolder> {
        private List<PrayerItem> mItems = new ArrayList<>();

        void setItems(List<PrayerItem> items) {
            mItems = items;
            notifyDataSetChanged();
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

            Context context = getContext();
            if (context == null) return;

            SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);
            String tzId = prefs.getString(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
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

            // Only highlight if it's today
            boolean isToday = isSameDay(mSelectedDate, Calendar.getInstance());

            if (isToday && isCurrent && getContext() != null) {
                holder.itemView.setBackgroundColor(getContext().getColor(R.color.md_theme_primaryContainer));
            } else {
                holder.itemView.setBackground(null);
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
