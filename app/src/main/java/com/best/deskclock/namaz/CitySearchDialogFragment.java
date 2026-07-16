package com.best.deskclock.namaz;

import static com.best.deskclock.settings.PreferencesKeys.KEY_NAMAZ_CITY_NAME;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CitySearchDialogFragment extends DialogFragment {

    private static class CitySearchResult {
        String city;
        String province;
        String country;
        double latitude;
        double longitude;

        CitySearchResult(String city, String province, String country, double lat, double lon) {
            this.city = city;
            this.province = province != null ? province : "";
            this.country = country != null ? country : "";
            this.latitude = lat;
            this.longitude = lon;
        }

        public String getFullLocation() {
            StringBuilder sb = new StringBuilder();
            if (!province.isEmpty()) sb.append(province);
            if (!country.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(country);
            }
            return sb.toString();
        }

        public boolean matches(String query) {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            return city.toLowerCase(Locale.getDefault()).contains(lowerQuery)
                    || province.toLowerCase(Locale.getDefault()).contains(lowerQuery)
                    || country.toLowerCase(Locale.getDefault()).contains(lowerQuery);
        }

        @NonNull
        @Override
        public String toString() {
            return city + " " + province + " " + country;
        }
    }

    private final List<CitySearchResult> mBuiltInResults = new ArrayList<>();
    private final List<CitySearchResult> mDisplayResults = new ArrayList<>();
    private CityAdapter mAdapter;
    private ProgressBar mProgressBar;
    private static final Map<String, CitySearchResult> sResolvedCache = new ConcurrentHashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<City> cities = DataModel.getDataModel().getAllCities();
        for (City c : cities) {
            CitySearchResult cached = sResolvedCache.get(c.getName());
            if (cached != null) {
                mBuiltInResults.add(cached);
            } else {
                mBuiltInResults.add(new CitySearchResult(c.getName(), "", "", 0, 0));
            }
        }

        resolveBuiltInCities();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.namaz_city_search_dialog, null);

        SearchView searchView = view.findViewById(R.id.city_search_view);
        ListView listView = view.findViewById(R.id.city_results_list);
        mProgressBar = view.findViewById(R.id.city_search_progress);

        mAdapter = new CityAdapter(requireContext(), mDisplayResults);
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
                if (newText.length() > 1) {
                    filterAndSearch(newText);
                } else if (newText.isEmpty()) {
                    populateInitialList();
                }
                return true;
            }
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            CitySearchResult selected = mDisplayResults.get(position);
            selectCity(selected);
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.namaz_city_search_title)
                .setView(view)
                .create();
    }

    private void resolveBuiltInCities() {
        AppExecutors.getDiskIO().execute(() -> {
            if (!Geocoder.isPresent()) return;
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            for (CitySearchResult res : mBuiltInResults) {
                if (res.country.isEmpty()) {
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(res.city, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address addr = addresses.get(0);
                            res.province = addr.getAdminArea();
                            res.country = addr.getCountryName();
                            res.latitude = addr.getLatitude();
                            res.longitude = addr.getLongitude();
                            sResolvedCache.put(res.city, res);
                        }
                    } catch (IOException ignored) {}
                }
            }
            AppExecutors.getMainThread().post(() -> {
                if (mAdapter != null) mAdapter.notifyDataSetChanged();
            });
        });
    }

    private void populateInitialList() {
        mDisplayResults.clear();
        mDisplayResults.addAll(mBuiltInResults);
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    private void filterAndSearch(String query) {
        mDisplayResults.clear();
        for (CitySearchResult res : mBuiltInResults) {
            if (res.matches(query)) {
                mDisplayResults.add(res);
            }
        }
        mAdapter.notifyDataSetChanged();
        performGeocoderSearch(query);
    }

    private void performGeocoderSearch(String query) {
        if (!Geocoder.isPresent()) return;

        mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getDiskIO().execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 10);
                AppExecutors.getMainThread().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    if (addresses != null) {
                        for (Address addr : addresses) {
                            String cityName = addr.getLocality();
                            if (cityName == null) cityName = addr.getFeatureName();

                            if (cityName != null && !cityName.equalsIgnoreCase(addr.getCountryName())) {
                                CitySearchResult res = new CitySearchResult(
                                        cityName,
                                        addr.getAdminArea(),
                                        addr.getCountryName(),
                                        addr.getLatitude(),
                                        addr.getLongitude()
                                );

                                boolean duplicate = false;
                                for (CitySearchResult d : mDisplayResults) {
                                    if (d.city.equalsIgnoreCase(res.city) && d.country.equalsIgnoreCase(res.country)) {
                                        duplicate = true;
                                        break;
                                    }
                                }
                                if (!duplicate) mDisplayResults.add(res);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                });
            } catch (IOException e) {
                AppExecutors.getMainThread().post(() -> mProgressBar.setVisibility(View.GONE));
            }
        });
    }

    private void selectCity(CitySearchResult city) {
        if (city.latitude == 0 && city.longitude == 0) {
            resolveAndSelect(city);
        } else {
            saveAndDismiss(city);
        }
    }

    private void resolveAndSelect(CitySearchResult city) {
        mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getDiskIO().execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(city.city, 1);
                AppExecutors.getMainThread().post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        city.latitude = addr.getLatitude();
                        city.longitude = addr.getLongitude();
                        city.province = addr.getAdminArea();
                        city.country = addr.getCountryName();
                        saveAndDismiss(city);
                    }
                });
            } catch (IOException e) {
                AppExecutors.getMainThread().post(() -> mProgressBar.setVisibility(View.GONE));
            }
        });
    }

    private void saveAndDismiss(CitySearchResult city) {
        NamazLocationManager.handleNewLocation(requireContext(), city.latitude, city.longitude, false);
        String finalDisplayName = city.city + (city.getFullLocation().isEmpty() ? "" : ", " + city.getFullLocation());
        DeskClockApplication.getDefaultSharedPreferences(requireContext())
                .edit()
                .putString(KEY_NAMAZ_CITY_NAME, finalDisplayName)
                .apply();
        dismiss();
    }

    private static class CityAdapter extends ArrayAdapter<CitySearchResult> {
        CityAdapter(Context context, List<CitySearchResult> cities) {
            super(context, 0, cities);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.namaz_city_list_item, parent, false);
            }

            CitySearchResult city = getItem(position);
            TextView nameView = view.findViewById(R.id.city_name);
            TextView locView = view.findViewById(R.id.city_location);

            if (city != null) {
                nameView.setText(city.city);
                String fullLoc = city.getFullLocation();
                locView.setText(fullLoc);
                locView.setVisibility(fullLoc.isEmpty() ? View.GONE : View.VISIBLE);
            }

            return view;
        }
    }
}
