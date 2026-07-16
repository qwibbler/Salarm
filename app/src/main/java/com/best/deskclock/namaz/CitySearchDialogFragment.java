package com.best.deskclock.namaz;

import static com.best.deskclock.settings.PreferencesKeys.KEY_NAMAZ_CITY_NAME;

import android.app.Dialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import com.best.deskclock.DeskClockApplication;
import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitySearchDialogFragment extends DialogFragment {

    private final List<City> mAllCities = new ArrayList<>();
    private final List<String> mDisplayNames = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private ProgressBar mProgressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAllCities.addAll(DataModel.getDataModel().getAllCities());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.namaz_city_search_dialog, container, false);

        SearchView searchView = view.findViewById(R.id.city_search_view);
        ListView listView = view.findViewById(R.id.city_results_list);
        mProgressBar = view.findViewById(R.id.city_search_progress);

        mAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, mDisplayNames);
        listView.setAdapter(mAdapter);

        populateInitialList();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performGeocoderSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCities(newText);
                return true;
            }
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedName = mAdapter.getItem(position);
            resolveCoordinatesAndSelect(selectedName);
        });

        return view;
    }

    private void populateInitialList() {
        mDisplayNames.clear();
        for (City city : mAllCities) {
            mDisplayNames.add(city.getName());
        }
        mAdapter.notifyDataSetChanged();
    }

    private void filterCities(String query) {
        mDisplayNames.clear();
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        for (City city : mAllCities) {
            if (city.getName().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                mDisplayNames.add(city.getName());
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void performGeocoderSearch(String query) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(requireContext(), "Geocoder service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getDiskIO().execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                AppExecutors.getMainThread().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    if (addresses != null && !addresses.isEmpty()) {
                        mDisplayNames.clear();
                        for (Address addr : addresses) {
                            String name = addr.getLocality();
                            if (name == null) name = addr.getSubAdminArea();
                            if (name == null) name = addr.getAdminArea();
                            if (name == null) name = addr.getFeatureName();

                            String country = addr.getCountryName();
                            String fullName = (name != null ? name : "Unknown") + (country != null ? ", " + country : "");
                            mDisplayNames.add(fullName);
                        }
                        mAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(requireContext(), "No results found for " + query, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                LogUtils.e("Geocoder search failed", e);
                AppExecutors.getMainThread().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Search failed. Check connection.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resolveCoordinatesAndSelect(String cityName) {
        mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getDiskIO().execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
                AppExecutors.getMainThread().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        NamazLocationManager.handleNewLocation(requireContext(), addr.getLatitude(), addr.getLongitude(), false);
                        DeskClockApplication.getDefaultSharedPreferences(requireContext())
                                .edit()
                                .putString(KEY_NAMAZ_CITY_NAME, cityName)
                                .apply();
                        dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Could not resolve coordinates for " + cityName, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                LogUtils.e("Coordinate resolution failed", e);
                AppExecutors.getMainThread().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to get coordinates. Check connection.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}
