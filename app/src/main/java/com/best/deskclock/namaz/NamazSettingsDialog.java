package com.best.deskclock.namaz;

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

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.settings.PreferencesKeys;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NamazSettingsDialog extends BottomSheetDialogFragment {

    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    private OnSettingsChangedListener mListener;

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.namaz_settings_dialog, container, false);

        Context context = requireContext();
        SharedPreferences prefs = DeskClockApplication.getDefaultSharedPreferences(context);

        TextView citySummary = view.findViewById(R.id.current_view_city_summary);
        updateCitySummary(citySummary, prefs);

        view.findViewById(R.id.use_current_location_button).setOnClickListener(v -> {
            prefs.edit()
                    .remove(PreferencesKeys.KEY_NAMAZ_VIEW_LATITUDE)
                    .remove(PreferencesKeys.KEY_NAMAZ_VIEW_LONGITUDE)
                    .remove(PreferencesKeys.KEY_NAMAZ_VIEW_CITY_NAME)
                    .remove(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE)
                    .apply();
            NamazLocationManager.requestGpsUpdate(context);
            if (mListener != null) mListener.onSettingsChanged();
            dismiss();
        });

        view.findViewById(R.id.pick_city_button).setOnClickListener(v -> {
            CitySearchDialogFragment picker = new CitySearchDialogFragment();
            picker.setPickerMode(true);
            picker.show(getParentFragmentManager(), "city_picker");
        });

        getParentFragmentManager().setFragmentResultListener("city_picked", this, (requestKey, result) -> {
            String name = result.getString("city_name");
            double lat = result.getDouble("latitude");
            double lon = result.getDouble("longitude");
            String country = result.getString("country");

            prefs.edit()
                    .putString(PreferencesKeys.KEY_NAMAZ_VIEW_CITY_NAME, name)
                    .putString(PreferencesKeys.KEY_NAMAZ_VIEW_LATITUDE, String.valueOf(lat))
                    .putString(PreferencesKeys.KEY_NAMAZ_VIEW_LONGITUDE, String.valueOf(lon))
                    .apply();

            autoDetectTimezone(prefs, name, country);
            updateCitySummary(citySummary, prefs);
            if (mListener != null) mListener.onSettingsChanged();
        });

        AutoCompleteTextView timezonePicker = view.findViewById(R.id.timezone_picker);
        String[] ids = TimeZone.getAvailableIDs();
        List<String> timezoneList = new ArrayList<>();
        Collections.addAll(timezoneList, ids);
        Collections.sort(timezoneList);
        ArrayAdapter<String> timezoneAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, timezoneList);
        timezonePicker.setAdapter(timezoneAdapter);

        String currentTimezone = prefs.getString(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, TimeZone.getDefault().getID());
        timezonePicker.setText(currentTimezone, false);
        timezonePicker.setOnItemClickListener((parent, v, position, id) -> {
            prefs.edit().putString(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, timezoneList.get(position)).apply();
            if (mListener != null) mListener.onSettingsChanged();
        });

        return view;
    }

    private void updateCitySummary(TextView summary, SharedPreferences prefs) {
        String name = prefs.getString(PreferencesKeys.KEY_NAMAZ_VIEW_CITY_NAME, null);
        if (name == null) {
            name = prefs.getString(PreferencesKeys.KEY_NAMAZ_CITY_NAME, "Current Location");
        }
        summary.setText("Currently viewing: " + name);
    }

    private void autoDetectTimezone(SharedPreferences prefs, String cityName, String countryName) {
        String[] ids = TimeZone.getAvailableIDs();
        String cityLower = cityName.split(",")[0].trim().toLowerCase(Locale.getDefault());
        String countryLower = countryName != null ? countryName.toLowerCase(Locale.getDefault()) : "";

        // 1. Try to match city name in IDs
        for (String id : ids) {
            if (id.toLowerCase(Locale.getDefault()).contains(cityLower)) {
                prefs.edit().putString(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, id).apply();
                return;
            }
        }

        // 2. Try to match country name in IDs (often Continent/Capital or Continent/Country)
        if (!countryLower.isEmpty()) {
            for (String id : ids) {
                if (id.toLowerCase(Locale.getDefault()).contains(countryLower)) {
                    prefs.edit().putString(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, id).apply();
                    return;
                }
            }
        }
    }
}
