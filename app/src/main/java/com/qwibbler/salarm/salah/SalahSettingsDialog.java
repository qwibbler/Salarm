package com.qwibbler.salarm.salah;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class SalahSettingsDialog extends BottomSheetDialogFragment {

    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    private OnSettingsChangedListener mListener;
    private AutoCompleteTextView mTimezonePicker;
    private TextView mCitySummary;

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("city_picked", this, (requestKey, result) -> {
            String name = result.getString("city_name");
            if (name == null) return;

            double lat = result.getDouble("latitude");
            double lon = result.getDouble("longitude");
            String tzId = result.getString("timezone_id");

            Context context = getContext();
            if (context == null) return;

            SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
            prefs.edit()
                    .putString(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME, name)
                    .putString(PreferencesKeys.KEY_SALAH_VIEW_LATITUDE, String.valueOf(lat))
                    .putString(PreferencesKeys.KEY_SALAH_VIEW_LONGITUDE, String.valueOf(lon))
                    .apply();

            if (tzId == null) {
                tzId = SalahTimezoneUtils.guessTimezone(name);
            }

            if (tzId != null) {
                prefs.edit().putString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, tzId).apply();
            }

            if (mTimezonePicker != null && tzId != null) {
                mTimezonePicker.setText(tzId, false);
            }
            if (mCitySummary != null) {
                updateCitySummary(mCitySummary, prefs);
            }

            if (mListener != null) mListener.onSettingsChanged();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.salah_settings_dialog, container, false);

        Context context = requireContext();
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);

        mCitySummary = view.findViewById(R.id.current_view_city_summary);
        updateCitySummary(mCitySummary, prefs);

        TextView recentHeader = view.findViewById(R.id.recent_locations_header);
        View recentScroll = view.findViewById(R.id.recent_locations_scroll);
        ChipGroup chipGroup = view.findViewById(R.id.recent_locations_chip_group);

        List<RecentLocation> recents = RecentLocationsManager.getRecentLocations(context);
        if (recentHeader != null && recentScroll != null && chipGroup != null && !recents.isEmpty()) {
            recentHeader.setVisibility(View.VISIBLE);
            recentScroll.setVisibility(View.VISIBLE);
            chipGroup.removeAllViews();
            for (RecentLocation loc : recents) {
                Chip chip = new Chip(context);
                chip.setText(loc.getCityName());
                chip.setClickable(true);
                chip.setCheckable(false);
                chip.setOnClickListener(v -> {
                    prefs.edit()
                            .putString(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME, loc.getCityName())
                            .putString(PreferencesKeys.KEY_SALAH_VIEW_LATITUDE, String.valueOf(loc.getLatitude()))
                            .putString(PreferencesKeys.KEY_SALAH_VIEW_LONGITUDE, String.valueOf(loc.getLongitude()))
                            .apply();

                    if (loc.getTimeZoneId() != null && !loc.getTimeZoneId().isEmpty()) {
                        prefs.edit().putString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, loc.getTimeZoneId()).apply();
                    }

                    RecentLocationsManager.addRecentLocation(context, loc);
                    if (mListener != null) mListener.onSettingsChanged();
                    dismiss();
                });
                chipGroup.addView(chip);
            }
        }

        view.findViewById(R.id.use_current_location_button).setOnClickListener(v -> {
            prefs.edit()
                    .remove(PreferencesKeys.KEY_SALAH_VIEW_LATITUDE)
                    .remove(PreferencesKeys.KEY_SALAH_VIEW_LONGITUDE)
                    .remove(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME)
                    .remove(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE)
                    .apply();
            SalahLocationManager.requestGpsUpdate(context);
            if (mListener != null) mListener.onSettingsChanged();
            dismiss();
        });

        view.findViewById(R.id.pick_city_button).setOnClickListener(v -> {
            CitySearchDialogFragment picker = new CitySearchDialogFragment();
            picker.setPickerMode(true);
            picker.show(getParentFragmentManager(), "city_picker");
        });

        mTimezonePicker = view.findViewById(R.id.timezone_picker);
        String[] ids = TimeZone.getAvailableIDs();
        List<String> timezoneList = new ArrayList<>();
        Collections.addAll(timezoneList, ids);
        Collections.sort(timezoneList);
        ArrayAdapter<String> timezoneAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, timezoneList);
        mTimezonePicker.setAdapter(timezoneAdapter);

        String currentTimezone = prefs.getString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        mTimezonePicker.setText(currentTimezone, false);
        mTimezonePicker.setOnItemClickListener((parent, v, position, id) -> {
            prefs.edit().putString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, timezoneList.get(position)).apply();
            if (mListener != null) mListener.onSettingsChanged();
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTimezonePicker = null;
        mCitySummary = null;
    }

    private void updateCitySummary(TextView summary, SharedPreferences prefs) {
        if (summary == null || prefs == null) return;
        String name = prefs.getString(PreferencesKeys.KEY_SALAH_VIEW_CITY_NAME, null);
        if (name == null) {
            name = prefs.getString(PreferencesKeys.KEY_SALAH_CITY_NAME, getString(R.string.salarm_current_location));
        }
        summary.setText(getString(R.string.salarm_currently_viewing_prefix, name));
    }
}
