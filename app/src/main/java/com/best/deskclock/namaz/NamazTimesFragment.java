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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.base.DeskClockFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.PreferencesKeys;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NamazTimesFragment extends DeskClockFragment {

    private TextView mCountdownText;
    private RecyclerView mRecyclerView;
    private NamazAdapter mAdapter;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        mHandler.post(mUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateRunnable);
    }

    private void updateUI() {
        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);
        String latStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, "0.0");
        String lonStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, "0.0");
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

        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(new Date(), lat, lon, method);

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
        updateCountdown(times);
    }

    private void updateCountdown(NamazCalculator.NamazTimes times) {
        Date now = new Date();
        PrayerItem nextPrayer = null;

        if (now.before(times.fajr)) nextPrayer = new PrayerItem("Fajr", times.fajr);
        else if (now.before(times.sunrise)) nextPrayer = new PrayerItem("Sunrise", times.sunrise);
        else if (now.before(times.dhuhr)) nextPrayer = new PrayerItem("Dhuhr", times.dhuhr);
        else if (now.before(times.asrHanafi)) nextPrayer = new PrayerItem("Asr", times.asrHanafi);
        else if (now.before(times.maghrib)) nextPrayer = new PrayerItem("Maghrib", times.maghrib);
        else if (now.before(times.isha)) nextPrayer = new PrayerItem("Isha", times.isha);
        else {
            // Next day's Fajr
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Context context = getContext();
            if (context != null) {
                SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);
                String latStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, "0.0");
                String lonStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, "0.0");
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
                NamazCalculator.NamazTimes tomorrowTimes = NamazCalculator.calculateAllTimes(cal.getTime(), lat, lon, method);
                nextPrayer = new PrayerItem("Fajr", tomorrowTimes.fajr);
            }
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
            holder.time.setText(DateFormat.getTimeFormat(getContext()).format(item.time));

            // Highlight current prayer
            Date now = new Date();
            boolean isCurrent;
            if (position < mItems.size() - 1) {
                isCurrent = now.after(item.time) && now.before(mItems.get(position + 1).time);
            } else {
                // Cyclical highlight: Isha is active if it's after Isha time OR before the first prayer of the day (Fajr)
                isCurrent = now.after(item.time) || now.before(mItems.get(0).time);
            }

            if (isCurrent && getContext() != null) {
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
