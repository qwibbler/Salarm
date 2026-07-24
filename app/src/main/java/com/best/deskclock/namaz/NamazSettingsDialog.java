package com.best.deskclock.namaz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.settings.PreferencesKeys;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        AutoCompleteTextView locationPicker = view.findViewById(R.id.location_mode_picker);
        String[] locationModes = {NamazLocationManager.LocationMode.GPS.name(), NamazLocationManager.LocationMode.MANUAL.name()};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, locationModes);
        locationPicker.setAdapter(locationAdapter);
        locationPicker.setText(NamazLocationManager.getLocationMode(context).name(), false);
        locationPicker.setOnItemClickListener((parent, v, position, id) -> {
            NamazLocationManager.LocationMode mode = NamazLocationManager.LocationMode.valueOf(locationModes[position]);
            NamazLocationManager.setLocationMode(context, mode);
            if (mode == NamazLocationManager.LocationMode.MANUAL) {
                new CitySearchDialogFragment().show(getParentFragmentManager(), "city_search");
            }
            if (mListener != null) mListener.onSettingsChanged();
            updateResetButtonVisibility(view, context, prefs);
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
            updateResetButtonVisibility(view, context, prefs);
        });

        MaterialButton resetButton = view.findViewById(R.id.reset_location_button);
        resetButton.setOnClickListener(v -> {
            NamazLocationManager.setLocationMode(context, NamazLocationManager.LocationMode.GPS);
            prefs.edit().remove(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE).apply();
            if (mListener != null) mListener.onSettingsChanged();
            dismiss();
        });

        updateResetButtonVisibility(view, context, prefs);

        return view;
    }

    private void updateResetButtonVisibility(View view, Context context, SharedPreferences prefs) {
        MaterialButton resetButton = view.findViewById(R.id.reset_location_button);
        boolean isOverride = NamazLocationManager.getLocationMode(context) != NamazLocationManager.LocationMode.GPS
                || prefs.contains(PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE);
        resetButton.setVisibility(isOverride ? View.VISIBLE : View.GONE);
    }
}
