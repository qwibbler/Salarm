package com.best.deskclock.namaz;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
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

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CitySearchDialogFragment extends DialogFragment {

    private static Map<String, Object[]> sBuiltInCoords;

    private static Map<String, Object[]> getBuiltInCoords(Context context) {
        if (sBuiltInCoords == null) {
            sBuiltInCoords = new HashMap<>();
            try (InputStream is = context.getAssets().open("city_coordinates.json")) {
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String json = result.toString("UTF-8");
                JSONObject obj = new JSONObject(json);
                java.util.Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        JSONObject coord = obj.getJSONObject(key);
                        sBuiltInCoords.put(key, new Object[]{
                                coord.getDouble("lat"),
                                coord.getDouble("lon"),
                                coord.optString("tz", "")
                        });
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                com.best.deskclock.utils.LogUtils.e("Error loading built-in coordinates", e);
            }
        }
        return sBuiltInCoords;
    }

    private String normalizeKey(String city, String province, String country) {
        String key = (city != null ? city : "") + "|" + (province != null ? province : "") + "|" + (country != null ? country : "");
        return key.replaceAll("\\s+", " ").trim();
    }

    private static class CitySearchResult {
        String cityId;
        String city;
        String province;
        String country;
        String countryCode;
        String timeZoneId; // Added
        double latitude;
        double longitude;

        CitySearchResult(String city, String province, String country, String countryCode, double lat, double lon) {
            this(null, city, province, country, countryCode, null, lat, lon);
        }

        CitySearchResult(String cityId, String city, String province, String country, String countryCode, String tzId, double lat, double lon) {
            this.cityId = cityId;
            this.city = city;
            this.province = province != null ? province : "";
            this.country = country != null ? country : "";
            this.countryCode = countryCode != null ? countryCode : "";
            this.timeZoneId = tzId;
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

    private final android.os.Handler mHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable mSearchRunnable;
    private boolean mIsSelecting = false;
    private boolean mIsPickerMode = false;

    public void setPickerMode(boolean isPickerMode) {
        mIsPickerMode = isPickerMode;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<City> cities = DataModel.getDataModel().getAllCities();
        Map<String, Object[]> builtInCoords = getBuiltInCoords(requireContext());
        for (City c : cities) {
            double lat = 0, lon = 0;
            String tzId = c.getTimeZone().getID();
            Object[] data = builtInCoords.get(c.getId());
            if (data != null) {
                lat = (double) data[0];
                lon = (double) data[1];
                if (data.length > 2 && data[2] instanceof String tz && !tz.isEmpty()) {
                    tzId = tz;
                }
            }
            mBuiltInResults.add(new CitySearchResult(c.getId(), c.getName(), "", "", "", tzId, lat, lon));
        }
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
                if (mSearchRunnable != null) {
                    mHandler.removeCallbacks(mSearchRunnable);
                }

                if (newText.length() > 1) {
                    mSearchRunnable = () -> filterAndSearch(newText);
                    mHandler.postDelayed(mSearchRunnable, 500);
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
        final Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getNetworkIO().execute(() -> {
            boolean isOnline = NamazUtils.isNetworkAvailable(appContext);

            if (isOnline && Geocoder.isPresent()) {
                Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(query, 10);
                    AppExecutors.getMainThread().post(() -> {
                        if (!isAdded() || mAdapter == null || mProgressBar == null) return;
                        mProgressBar.setVisibility(View.GONE);
                        if (addresses != null) {
                            for (Address addr : addresses) {
                                String cityName = addr.getLocality();
                                if (cityName == null) cityName = addr.getFeatureName();
                                if (cityName == null) continue;

                                if (!cityName.equalsIgnoreCase(addr.getCountryName())) {
                                    CitySearchResult res = new CitySearchResult(
                                            cityName,
                                            addr.getAdminArea(),
                                            addr.getCountryName(),
                                            addr.getCountryCode(),
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
                    AppExecutors.getMainThread().post(() -> {
                        if (isAdded() && mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                    });
                }
            } else {
                // Offline search: Query database cache
                List<CityCoordinate> cachedResults = NamazDatabase.getDatabase(appContext).cityCoordinateDao().searchByName(query);
                AppExecutors.getMainThread().post(() -> {
                    if (!isAdded() || mAdapter == null || mProgressBar == null) return;
                    mProgressBar.setVisibility(View.GONE);
                    for (CityCoordinate cached : cachedResults) {
                        CitySearchResult res = new CitySearchResult(
                                null, cached.cityName, cached.province, cached.country, cached.countryCode, cached.timeZoneId, cached.latitude, cached.longitude
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
                    mAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void selectCity(CitySearchResult city) {
        if (mIsSelecting) return;
        mIsSelecting = true;

        if (city.latitude == 0 && city.longitude == 0) {
            resolveAndSelect(city);
        } else if (city.cityId == null && city.timeZoneId == null) {
            // Custom city from Geocoder search has coords but needs TZ resolution
            resolveTzAndSelect(city);
        } else {
            saveAndDismiss(city);
        }
    }

    private void resolveTzAndSelect(CitySearchResult city) {
        final Context context = getContext();
        if (context == null) {
            mIsSelecting = false;
            return;
        }
        final Context appContext = context.getApplicationContext();

        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getNetworkIO().execute(() -> {
            if (NamazUtils.isNetworkAvailable(appContext)) {
                city.timeZoneId = NamazTimezoneUtils.fetchTimeZoneId(city.latitude, city.longitude);
            }
            if (city.timeZoneId == null) {
                city.timeZoneId = NamazTimezoneUtils.guessTimezone(city.city + ", " + city.country);
            }

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded()) {
                    mIsSelecting = false;
                    return;
                }
                if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                saveAndDismiss(city);
            });
        });
    }

    private void resolveAndSelect(CitySearchResult city) {
        final Context context = getContext();
        if (context == null) {
            mIsSelecting = false;
            return;
        }
        final Context appContext = context.getApplicationContext();

        // 1. Check built-in dataset
        if (city.latitude != 0 || city.longitude != 0) {
            saveAndDismiss(city);
            return;
        }

        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getNetworkIO().execute(() -> {
            // 2. Check local database cache
            CityCoordinate cached = NamazDatabase.getDatabase(appContext).cityCoordinateDao().findByKey(
                    normalizeKey(city.city, city.province, city.country));

            if (cached != null) {
                AppExecutors.getMainThread().post(() -> {
                    if (!isAdded()) {
                        mIsSelecting = false;
                        return;
                    }
                    if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                    city.latitude = cached.latitude;
                    city.longitude = cached.longitude;
                    city.timeZoneId = cached.timeZoneId;
                    saveAndDismiss(city);
                });
                return;
            }

            // 3. Perform online lookup if available
            if (NamazUtils.isNetworkAvailable(appContext) && Geocoder.isPresent()) {
                Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(city.city, 1);
                    AppExecutors.getMainThread().post(() -> {
                        if (!isAdded()) {
                            mIsSelecting = false;
                            return;
                        }
                        if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address addr = addresses.get(0);
                            city.latitude = addr.getLatitude();
                            city.longitude = addr.getLongitude();
                            city.province = addr.getAdminArea();
                            city.country = addr.getCountryName();
                            city.countryCode = addr.getCountryCode();

                            // 1. Try to fetch actual TimeZone ID online
                            city.timeZoneId = NamazTimezoneUtils.fetchTimeZoneId(city.latitude, city.longitude);

                            // 2. Fallback to guessing if online fetch failed
                            if (city.timeZoneId == null) {
                                city.timeZoneId = NamazTimezoneUtils.guessTimezone(city.city + ", " + city.country);
                            }

                            // Save to cache in background
                            final CityCoordinate toCache = new CityCoordinate(
                                    normalizeKey(city.city, city.province, city.country),
                                    city.city, city.province, city.country,
                                    city.latitude, city.longitude, city.countryCode, city.timeZoneId
                            );
                            AppExecutors.getNetworkIO().execute(() ->
                                    NamazDatabase.getDatabase(appContext).cityCoordinateDao().insert(toCache));

                            saveAndDismiss(city);
                        } else {
                            mIsSelecting = false;
                            android.widget.Toast.makeText(appContext, R.string.salarm_error_resolve_city, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    AppExecutors.getMainThread().post(() -> {
                        mIsSelecting = false;
                        if (isAdded() && mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                    });
                }
            } else {
                AppExecutors.getMainThread().post(() -> {
                    mIsSelecting = false;
                    if (isAdded() && mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                    android.widget.Toast.makeText(appContext, R.string.salarm_error_resolve_city, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveAndDismiss(CitySearchResult city) {
        if (!isAdded()) {
            mIsSelecting = false;
            return;
        }
        final Context context = getContext();
        if (context == null) {
            mIsSelecting = false;
            return;
        }

        // Cache coordinates if resolved online (custom city)
        if (city.cityId == null && city.latitude != 0 && city.longitude != 0) {
            final Context appContext = context.getApplicationContext();
            final String key = normalizeKey(city.city, city.province, city.country);
            AppExecutors.getNetworkIO().execute(() -> {
                CityCoordinate toCache = new CityCoordinate(key, city.city, city.province, city.country, city.latitude, city.longitude, city.countryCode, city.timeZoneId);
                NamazDatabase.getDatabase(appContext).cityCoordinateDao().insert(toCache);
            });
        }

        String finalDisplayName = city.city + (city.getFullLocation().isEmpty() ? "" : ", " + city.getFullLocation());

        if (mIsPickerMode) {
            Bundle result = new Bundle();
            result.putString("city_name", finalDisplayName);
            result.putDouble("latitude", city.latitude);
            result.putDouble("longitude", city.longitude);
            result.putString("country", city.country);
            result.putString("country_code", city.countryCode);
            result.putString("timezone_id", city.timeZoneId);
            getParentFragmentManager().setFragmentResult("city_picked", result);
        } else {
            NamazLocationManager.handleNewLocation(context, city.latitude, city.longitude, false, true);
            android.content.SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
            editor.putString(KEY_NAMAZ_CITY_NAME, finalDisplayName);
            if (city.timeZoneId != null) {
                editor.putString(com.best.deskclock.settings.PreferencesKeys.KEY_NAMAZ_DISPLAY_TIMEZONE, city.timeZoneId);
            }
            editor.apply();
        }
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
