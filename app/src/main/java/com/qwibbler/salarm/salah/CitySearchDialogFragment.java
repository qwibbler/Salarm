package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_INTERNET_ACCESS_ALLOWED;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_INTERNET_ACCESS_ALLOWED;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_SALAH_CITY_NAME;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.data.City;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.settings.PermissionsManagementActivity;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.utils.LogUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CitySearchDialogFragment extends DialogFragment {

    private static Map<String, Object[]> sBuiltInCoords;
    private final List<CitySearchResult> mBuiltInResults = new ArrayList<>();
    private final List<CitySearchResult> mDisplayResults = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private CityAdapter mAdapter;
    private ProgressBar mProgressBar;
    private MaterialButton mBtnRefineOnline;
    private MaterialButton mBtnCustomLocation;
    private String mCurrentQuery = "";
    private Runnable mSearchRunnable;
    private boolean mIsSelecting = false;
    private boolean mIsPickerMode = false;

    private static Map<String, Object[]> getBuiltInCoords(Context context) {
        if (sBuiltInCoords == null) {
            sBuiltInCoords = new HashMap<>();
            try (InputStream is = context.getAssets().open("city_coordinates.json")) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String json = result.toString("UTF-8");
                JSONObject obj = new JSONObject(json);
                Iterator<String> keys = obj.keys();
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
                LogUtils.e("Error loading built-in coordinates", e);
            }
        }
        return sBuiltInCoords;
    }

    private String normalizeKey(String city, String province, String country) {
        String key = (city != null ? city : "") + "|" + (province != null ? province : "") + "|" + (country != null ? country : "");
        return key.replaceAll("\\s+", " ").trim();
    }

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
            mBuiltInResults.add(new CitySearchResult(c.getId(), c.getName(), "", c.getCountry(), "", tzId, c.getAlternativeNames(), lat, lon));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.salah_city_search_dialog, null);

        SearchView searchView = view.findViewById(R.id.city_search_view);
        ListView listView = view.findViewById(R.id.city_results_list);

        mProgressBar = view.findViewById(R.id.city_search_progress);
        mBtnRefineOnline = view.findViewById(R.id.btn_refine_online);
        mBtnCustomLocation = view.findViewById(R.id.btn_custom_location);

        if (mBtnCustomLocation != null) {
            mBtnCustomLocation.setOnClickListener(v -> showCustomLocationDialog());
        }

        mAdapter = new CityAdapter(requireContext(), mDisplayResults);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if (position >= 0 && position < mDisplayResults.size()) {
                CitySearchResult selected = mDisplayResults.get(position);
                if (!selected.isHeader && !selected.isSeparator) {
                    selectCity(selected);
                }
            }
        });

        populateInitialList();

        if (mBtnCustomLocation != null) {
            mBtnCustomLocation.setVisibility(View.VISIBLE);
        }
        if (mBtnRefineOnline != null) {
            mBtnRefineOnline.setVisibility(View.GONE);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mCurrentQuery = query.trim();
                if (!mCurrentQuery.isEmpty()) {
                    if (mBtnCustomLocation != null) mBtnCustomLocation.setVisibility(View.GONE);
                    if (mBtnRefineOnline != null) mBtnRefineOnline.setVisibility(View.VISIBLE);
                    filterAndSearch(mCurrentQuery);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mCurrentQuery = newText.trim();
                if (mSearchRunnable != null) {
                    mHandler.removeCallbacks(mSearchRunnable);
                }

                if (!mCurrentQuery.isEmpty()) {
                    if (mBtnCustomLocation != null) mBtnCustomLocation.setVisibility(View.GONE);
                    if (mBtnRefineOnline != null) mBtnRefineOnline.setVisibility(View.VISIBLE);
                    if (mCurrentQuery.length() > 1) {
                        mSearchRunnable = () -> filterAndSearch(mCurrentQuery);
                        mHandler.postDelayed(mSearchRunnable, 500);
                    }
                } else {
                    populateInitialList();
                    if (mBtnCustomLocation != null) mBtnCustomLocation.setVisibility(View.VISIBLE);
                    if (mBtnRefineOnline != null) mBtnRefineOnline.setVisibility(View.GONE);
                }
                return true;
            }
        });

        mBtnRefineOnline.setOnClickListener(v -> performOnlineSearch(mCurrentQuery));

        return new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.salarm_city_search_title)
            .setView(view)
            .create();
    }

    private void showCustomLocationDialog() {
        Context context = getContext();
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_location, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.custom_location_name_input);
        TextInputEditText latInput = dialogView.findViewById(R.id.custom_location_lat_input);
        TextInputEditText lonInput = dialogView.findViewById(R.id.custom_location_lon_input);
        AutoCompleteTextView tzInput = dialogView.findViewById(R.id.custom_location_tz_input);

        String[] ids = TimeZone.getAvailableIDs();
        List<String> timezoneList = new ArrayList<>();
        Collections.addAll(timezoneList, ids);
        Collections.sort(timezoneList);
        ArrayAdapter<String> tzAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, timezoneList);
        tzInput.setAdapter(tzAdapter);
        tzInput.setText(SalahTimezoneUtils.getPhoneTimeZoneId(), false);

        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.salarm_custom_location_dialog_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String nameStr = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String latStr = latInput.getText() != null ? latInput.getText().toString().trim() : "";
                String lonStr = lonInput.getText() != null ? lonInput.getText().toString().trim() : "";
                String tzStr = tzInput.getText() != null ? tzInput.getText().toString().trim() : "";

                double lat, lon;
                try {
                    lat = Double.parseDouble(latStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.salarm_custom_location_invalid_lat, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    lon = Double.parseDouble(lonStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.salarm_custom_location_invalid_lon, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (lat < -90.0 || lat > 90.0) {
                    Toast.makeText(context, R.string.salarm_custom_location_invalid_lat, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (lon < -180.0 || lon > 180.0) {
                    Toast.makeText(context, R.string.salarm_custom_location_invalid_lon, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (nameStr.isEmpty()) {
                    nameStr = String.format(Locale.US, "Custom (%.4f, %.4f)", lat, lon);
                }

                if (tzStr.isEmpty()) {
                    tzStr = SalahTimezoneUtils.getPhoneTimeZoneId();
                }

                CitySearchResult customRes = new CitySearchResult(null, nameStr, "Custom Location", "", "", tzStr, null, lat, lon);
                customRes.isCustom = true;

                selectCity(customRes);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void populateInitialList() {
        mDisplayResults.clear();

        Context context = getContext();
        if (context != null) {
            List<RecentLocation> recents = RecentLocationsManager.getRecentLocations(context);
            if (!recents.isEmpty()) {
                mDisplayResults.add(CitySearchResult.createHeader(getString(R.string.salarm_recent_locations_title)));
                for (RecentLocation rl : recents) {
                    CitySearchResult res = new CitySearchResult(null, rl.getCityName(), rl.getProvince(), rl.getCountry(),
                        rl.getCountryCode(), rl.getTimeZoneId(), null, rl.getLatitude(), rl.getLongitude());
                    res.isRecent = true;
                    res.isCustom = rl.isCustom();
                    mDisplayResults.add(res);
                }

                // Header separating Recent from the full list
                mDisplayResults.add(CitySearchResult.createHeader(getString(R.string.salarm_all_locations_title)));
            } else {
                mDisplayResults.add(CitySearchResult.createHeader(getString(R.string.salarm_all_locations_title)));
            }
        }

        // Recent entries intentionally appear again in the full list.
        mDisplayResults.addAll(mBuiltInResults);

        mAdapter.notifyDataSetChanged();

        final Context appContext = requireContext().getApplicationContext();
        AppExecutors.getNetworkIO().execute(() -> {
            List<CityCoordinate> cachedResults = SalahDatabase.getDatabase(appContext).cityCoordinateDao().searchByName("");
            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || mAdapter == null) return;

                List<CitySearchResult> toAdd = new ArrayList<>();
                for (CityCoordinate cached : cachedResults) {
                    CitySearchResult res = new CitySearchResult(
                        null, cached.cityName, cached.province, cached.country, cached.countryCode, cached.timeZoneId, null, cached.latitude, cached.longitude
                    );

                    boolean duplicate = false;
                    for (CitySearchResult d : mDisplayResults) {
                        if (!d.isHeader && !d.isSeparator && d.city.equalsIgnoreCase(res.city) && d.country.equalsIgnoreCase(res.country)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) toAdd.add(res);
                }
                mDisplayResults.addAll(toAdd);
                mAdapter.notifyDataSetChanged();
            });
        });
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
            // Offline search: Query database cache
            List<CityCoordinate> cachedResults = SalahDatabase.getDatabase(appContext).cityCoordinateDao().searchByName(query);
            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || mAdapter == null || mProgressBar == null) return;
                mProgressBar.setVisibility(View.GONE);

                List<CitySearchResult> toAdd = new ArrayList<>();
                for (CityCoordinate cached : cachedResults) {
                    CitySearchResult res = new CitySearchResult(
                        null, cached.cityName, cached.province, cached.country, cached.countryCode, cached.timeZoneId, null, cached.latitude, cached.longitude
                    );

                    boolean duplicate = false;
                    for (CitySearchResult d : mDisplayResults) {
                        if (!d.isHeader && !d.isSeparator && d.city.equalsIgnoreCase(res.city) && d.country.equalsIgnoreCase(res.country)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) toAdd.add(res);
                }
                mDisplayResults.addAll(toAdd);
                mAdapter.notifyDataSetChanged();
                mBtnRefineOnline.setVisibility(View.VISIBLE);
            });
        });
    }

    private void performOnlineSearch(String query) {
        final Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        if (!getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)) {
            showInternetDisabledDialog();
            return;
        }

        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        AppExecutors.getNetworkIO().execute(() -> {
            List<OpenMeteoClient.SearchResult> openMeteoResults = OpenMeteoClient.search(query);
            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || mAdapter == null || mProgressBar == null) return;
                mProgressBar.setVisibility(View.GONE);
                for (OpenMeteoClient.SearchResult nr : openMeteoResults) {
                    CitySearchResult res = new CitySearchResult(
                        null, nr.name, nr.province, nr.country, nr.countryCode, nr.timezone, null, nr.latitude, nr.longitude
                    );

                    boolean duplicate = false;
                    for (CitySearchResult d : mDisplayResults) {
                        if (!d.isHeader && !d.isSeparator && d.city.equalsIgnoreCase(res.city) && d.country.equalsIgnoreCase(res.country)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) mDisplayResults.add(res);
                }
                mAdapter.notifyDataSetChanged();
                if (openMeteoResults.isEmpty()) {
                    Toast.makeText(appContext, R.string.no_results, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showInternetDisabledDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.online_search_internet_disabled_title)
            .setMessage(R.string.online_search_internet_disabled_message)
            .setPositiveButton(R.string.online_search_internet_settings_button, (dialog, which) ->
                startActivity(new Intent(requireContext(), PermissionsManagementActivity.class)))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
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
            city.timeZoneId = SalahTimezoneUtils.fetchTimeZoneId(appContext, city.latitude, city.longitude);
            if (city.timeZoneId == null) {
                city.timeZoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
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
            CityCoordinate cached = SalahDatabase.getDatabase(appContext).cityCoordinateDao().findByKey(
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

            // 3. Perform online lookup if allowed
            if (getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)
                && Geocoder.isPresent()) {
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
                            city.timeZoneId = SalahTimezoneUtils.fetchTimeZoneId(appContext, city.latitude, city.longitude);

                            // 2. Fallback to phone's TZ
                            if (city.timeZoneId == null) {
                                city.timeZoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
                            }

                            // Save to cache in background
                            final CityCoordinate toCache = new CityCoordinate(
                                normalizeKey(city.city, city.province, city.country),
                                city.city, city.province, city.country,
                                city.latitude, city.longitude, city.countryCode, city.timeZoneId
                            );
                            AppExecutors.getNetworkIO().execute(() ->
                                SalahDatabase.getDatabase(appContext).cityCoordinateDao().insert(toCache));

                            saveAndDismiss(city);
                        } else {
                            mIsSelecting = false;
                            Toast.makeText(appContext, R.string.salarm_error_resolve_city, Toast.LENGTH_SHORT).show();
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
                    if (!getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)) {
                        showInternetDisabledDialog();
                    } else {
                        Toast.makeText(appContext, R.string.salarm_error_resolve_city, Toast.LENGTH_SHORT).show();
                    }
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

        RecentLocation recent = new RecentLocation(city.city, city.province, city.country, city.countryCode,
            city.latitude, city.longitude, city.timeZoneId, city.isCustom, System.currentTimeMillis());
        RecentLocationsManager.addRecentLocation(context, recent);

        // Cache coordinates if resolved online (custom city)
        if (city.cityId == null && city.latitude != 0 && city.longitude != 0) {
            final Context appContext = context.getApplicationContext();
            final String key = normalizeKey(city.city, city.province, city.country);
            AppExecutors.getNetworkIO().execute(() -> {
                CityCoordinate toCache = new CityCoordinate(key, city.city, city.province, city.country, city.latitude, city.longitude, city.countryCode, city.timeZoneId);
                SalahDatabase.getDatabase(appContext).cityCoordinateDao().insert(toCache);
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
            SalahLocationManager.handleNewLocation(context, city.latitude, city.longitude, false, true);
            SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
            editor.putString(KEY_SALAH_CITY_NAME, finalDisplayName);
            if (city.timeZoneId != null) {
                editor.putString(PreferencesKeys.KEY_SALAH_DISPLAY_TIMEZONE, city.timeZoneId);
            }
            editor.apply();
        }
        dismiss();
    }

    private static class CitySearchResult {
        String cityId;
        String city;
        String province;
        String country;
        String countryCode;
        String timeZoneId;
        List<String> altNames;
        double latitude;
        double longitude;
        boolean isRecent = false;
        boolean isCustom = false;
        boolean isHeader = false;
        boolean isSeparator = false;
        String headerTitle;

        CitySearchResult(String city, String province, String country, String countryCode, double lat, double lon) {
            this(null, city, province, country, countryCode, null, null, lat, lon);
        }

        CitySearchResult(String cityId, String city, String province, String country, String countryCode, String tzId, List<String> altNames, double lat, double lon) {
            this.cityId = cityId;
            this.city = city != null ? city : "";
            this.province = province != null ? province : "";
            this.country = country != null ? country : "";
            this.countryCode = countryCode != null ? countryCode : "";
            this.timeZoneId = tzId;
            this.altNames = altNames;
            this.latitude = lat;
            this.longitude = lon;
        }

        static CitySearchResult createHeader(String title) {
            CitySearchResult res = new CitySearchResult("", "", "", "", 0.0, 0.0);
            res.isHeader = true;
            res.headerTitle = title;
            return res;
        }

        static CitySearchResult createSeparator() {
            CitySearchResult res = new CitySearchResult("", "", "", "", 0.0, 0.0);
            res.isSeparator = true;
            return res;
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
            if (isHeader || isSeparator) return false;
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            if (city.toLowerCase(Locale.getDefault()).contains(lowerQuery)
                || province.toLowerCase(Locale.getDefault()).contains(lowerQuery)
                || country.toLowerCase(Locale.getDefault()).contains(lowerQuery)
                || (timeZoneId != null && timeZoneId.toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                return true;
            }

            if (altNames != null) {
                for (String alt : altNames) {
                    if (alt.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @NonNull
        @Override
        public String toString() {
            return city + " " + province + " " + country;
        }
    }

    private static class CityAdapter extends ArrayAdapter<CitySearchResult> {

        private static final int VIEW_TYPE_CITY = 0;
        private static final int VIEW_TYPE_HEADER = 1;
        private static final int VIEW_TYPE_SEPARATOR = 2;

        CityAdapter(Context context, List<CitySearchResult> cities) {
            super(context, 0, cities);
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            CitySearchResult item = getItem(position);
            if (item != null) {
                if (item.isHeader) return VIEW_TYPE_HEADER;
                if (item.isSeparator) return VIEW_TYPE_SEPARATOR;
            }
            return VIEW_TYPE_CITY;
        }

        @Override
        public boolean isEnabled(int position) {
            CitySearchResult item = getItem(position);
            if (item != null && (item.isHeader || item.isSeparator)) {
                return false;
            }
            return super.isEnabled(position);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            int viewType = getItemViewType(position);
            View view = convertView;

            if (viewType == VIEW_TYPE_HEADER) {
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.salah_city_header_item, parent, false);
                }
                TextView headerView = view.findViewById(R.id.section_header_text);
                CitySearchResult item = getItem(position);
                if (item != null && headerView != null) {
                    headerView.setText(item.headerTitle);
                }
                return view;
            }

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.salah_city_list_item, parent, false);
            }
            CitySearchResult city = getItem(position);

            TextView nameView = view.findViewById(R.id.city_name);
            TextView locView = view.findViewById(R.id.city_location);

            if (city != null) {
                if (city.isRecent) {
                    nameView.setText(city.city);
                    String fullLoc = city.getFullLocation();
                    String tzInfo = (city.timeZoneId != null && !city.timeZoneId.isEmpty()) ? " • " + city.timeZoneId : "";
                    if (fullLoc.isEmpty()) {
                        locView.setText(getContext().getString(R.string.salarm_recent_label) + String.format(Locale.US, " • %.4f, %.4f", city.latitude, city.longitude) + tzInfo);
                    } else {
                        locView.setText(getContext().getString(R.string.salarm_recent_label) + " • " + fullLoc + tzInfo);
                    }
                    locView.setVisibility(View.VISIBLE);
                } else {
                    nameView.setText(city.city);
                    String fullLoc = city.getFullLocation();
                    locView.setText(fullLoc);
                    locView.setVisibility(fullLoc.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }

            return view;
        }
    }
}
