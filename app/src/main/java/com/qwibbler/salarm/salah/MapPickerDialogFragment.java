package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_INTERNET_ACCESS_ALLOWED;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_INTERNET_ACCESS_ALLOWED;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.utils.LogUtils;

import java.util.List;
import java.util.Locale;

public class MapPickerDialogFragment extends DialogFragment {

    private static final String ARG_INIT_LAT = "arg_init_lat";
    private static final String ARG_INIT_LON = "arg_init_lon";
    private static final String ARG_HAS_INIT_LOC = "arg_has_init_loc";

    private double mPickedLat = 0.0;
    private double mPickedLon = 0.0;
    private boolean mHasPickedLocation = false;

    private String mPickedAddress;
    private String mPickedCity;
    private String mPickedCountry;

    private WebView mWebView;
    private View mErrorContainer;
    private TextView mErrorText;
    private View mProgressBar;
    private TextView mCoordsText;
    private TextView mAddressText;

    public static boolean isValidCoordinates(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon)
            && lat >= -90.0 && lat <= 90.0
            && lon >= -180.0 && lon <= 180.0;
    }

    public static MapPickerDialogFragment newInstance(double initLat, double initLon, boolean hasInitLoc) {
        MapPickerDialogFragment fragment = new MapPickerDialogFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_INIT_LAT, initLat);
        args.putDouble(ARG_INIT_LON, initLon);
        args.putBoolean(ARG_HAS_INIT_LOC, hasInitLoc);
        fragment.setArguments(args);
        return fragment;
    }

    public static MapPickerDialogFragment newInstance(double initLat, double initLon) {
        boolean hasLoc = isValidCoordinates(initLat, initLon) && (initLat != 0.0 || initLon != 0.0);
        return newInstance(initLat, initLon, hasLoc);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_map_picker, null);

        SearchView searchView = view.findViewById(R.id.map_search_view);
        mWebView = view.findViewById(R.id.map_web_view);
        mProgressBar = view.findViewById(R.id.map_progress_bar);
        mErrorContainer = view.findViewById(R.id.map_error_container);
        mErrorText = view.findViewById(R.id.map_error_text);
        mCoordsText = view.findViewById(R.id.map_picked_coords_text);
        mAddressText = view.findViewById(R.id.map_picked_address_text);

        if (getArguments() != null) {
            mPickedLat = getArguments().getDouble(ARG_INIT_LAT, 0.0);
            mPickedLon = getArguments().getDouble(ARG_INIT_LON, 0.0);
            mHasPickedLocation = getArguments().getBoolean(ARG_HAS_INIT_LOC, false);
        }

        setupWebView();
        setupSearchView(searchView);

        if (mHasPickedLocation && isValidCoordinates(mPickedLat, mPickedLon)) {
            updateCoordsText(mPickedLat, mPickedLon);
            reverseGeocodePickedLocation(mPickedLat, mPickedLon);
        } else {
            mCoordsText.setText(R.string.salarm_map_tap_to_pick);
            mAddressText.setVisibility(View.GONE);
        }

        view.findViewById(R.id.btn_cancel_map_picker).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_select_map_picker).setOnClickListener(v -> confirmSelection());
        view.findViewById(R.id.btn_map_retry).setOnClickListener(v -> reloadMap());

        return new AlertDialog.Builder(requireContext())
            .setTitle(R.string.masjid_location_search_map)
            .setView(view)
            .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int dialogHeight = (int) (displayHeight * 0.75);
            if (dialogHeight < 800) {
                dialogHeight = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight);
        }
        if (mWebView != null) {
            mWebView.postDelayed(() -> mWebView.evaluateJavascript("if (typeof window.invalidateMapSize === 'function') { window.invalidateMapSize(); }", null), 300);
        }
    }

    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        settings.setUserAgentString("Salarm-Android-App/1.0 (com.qwibbler.salarm)");

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                LogUtils.d("MapWebView JS (" + consoleMessage.messageLevel() + "): "
                    + consoleMessage.message() + " -- Line "
                    + consoleMessage.lineNumber() + " of "
                    + consoleMessage.sourceId());
                return true;
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                mWebView.evaluateJavascript("if (typeof window.invalidateMapSize === 'function') { window.invalidateMapSize(); }", null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    LogUtils.e("MapWebView error: " + error.getDescription() + " (code " + error.getErrorCode() + ")");
                    showError(error.getDescription() != null ? error.getDescription().toString() : "Network error");
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (request != null && request.isForMainFrame()) {
                    LogUtils.e("MapWebView HTTP error: status " + errorResponse.getStatusCode());
                    showError("HTTP " + errorResponse.getStatusCode());
                }
            }
        });

        loadMapHtml();
    }

    private void loadMapHtml() {
        if (mErrorContainer != null) mErrorContainer.setVisibility(View.GONE);
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);

        double initLat = (mHasPickedLocation && isValidCoordinates(mPickedLat, mPickedLon)) ? mPickedLat : 21.4225;
        double initLon = (mHasPickedLocation && isValidCoordinates(mPickedLat, mPickedLon)) ? mPickedLon : 39.8262;

        String html = buildLeafletHtml(initLat, initLon, mHasPickedLocation);
        mWebView.loadDataWithBaseURL("https://basemaps.cartocdn.com", html, "text/html", "UTF-8", null);
    }

    private void reloadMap() {
        loadMapHtml();
    }

    private void showError(String message) {
        if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
        if (mErrorContainer != null) {
            mErrorContainer.setVisibility(View.VISIBLE);
            if (mErrorText != null && message != null) {
                mErrorText.setText(getString(R.string.salarm_map_error_load_with_message,
                    getString(R.string.salarm_map_error_load),
                    message));
            }
        }
    }

    private String buildLeafletHtml(double initLat, double initLon, boolean hasInitLoc) {
        return "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>" +
            "html, body { height: 100%; width: 100%; margin: 0; padding: 0; background: #f8f8f8; }" +
            "#map { height: 100%; width: 100%; position: absolute; top: 0; bottom: 0; left: 0; right: 0; }" +
            "</style>" +
            "</head><body><div id='map'></div><script>" +
            "window.onerror = function(msg, url, line) {" +
            "   if (window.Android && window.Android.onMapError) {" +
            "       window.Android.onMapError('JS Error: ' + msg);" +
            "   }" +
            "   return false;" +
            "};" +
            "document.addEventListener('DOMContentLoaded', function() {" +
            "   try {" +
            "       if (typeof L === 'undefined') {" +
            "           if (window.Android && window.Android.onMapError) {" +
            "               window.Android.onMapError('Leaflet library failed to load.');" +
            "           }" +
            "           return;" +
            "       }" +
            "       var map = L.map('map', {" +
            "           zoomControl: true," +
            "           attributionControl: true" +
            "       }).setView([" + initLat + ", " + initLon + "], " + (hasInitLoc ? 15 : 12) + ");" +
            "       var primaryUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';" +
            "       var tileLayer = L.tileLayer(primaryUrl, {" +
            "           maxZoom: 19," +
            "           attribution: '&copy; OpenStreetMap contributors'" +
            "       }).addTo(map);" +
            "       tileLayer.on('tileerror', function(e) {" +
            "           console.warn('Primary tile load error, falling back to Esri');" +
            "           if (tileLayer.getUrl() !== fallbackUrl) {" +
            "               tileLayer.setUrl(fallbackUrl);" +
            "           }" +
            "       });" +
            "       var marker = " + (hasInitLoc ? "L.marker([" + initLat + ", " + initLon + "]).addTo(map);" : "null;") +
            "       window.setLocation = function(lat, lng) {" +
            "           if (marker) { marker.setLatLng([lat, lng]); } else { marker = L.marker([lat, lng]).addTo(map); }" +
            "           map.setView([lat, lng], 15);" +
            "           setTimeout(function() { map.invalidateSize(); }, 100);" +
            "       };" +
            "       window.invalidateMapSize = function() {" +
            "           if (map) { map.invalidateSize(); }" +
            "       };" +
            "       map.on('click', function(e) {" +
            "           var lat = e.latlng.lat;" +
            "           var lng = e.latlng.lng;" +
            "           setLocation(lat, lng);" +
            "           if (window.Android && window.Android.onLocationPicked) {" +
            "               window.Android.onLocationPicked(lat, lng);" +
            "           }" +
            "       });" +
            "       setTimeout(function() {" +
            "           map.invalidateSize();" +
            "           if (window.Android && window.Android.onMapLoaded) {" +
            "               window.Android.onMapLoaded();" +
            "           }" +
            "       }, 250);" +
            "   } catch (e) {" +
            "       if (window.Android && window.Android.onMapError) {" +
            "           window.Android.onMapError('Map init error: ' + e.message);" +
            "       }" +
            "   }" +
            "});" +
            "</script></body></html>";
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        final Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        AppExecutors.getNetworkIO().execute(() -> {
            double foundLat = Double.NaN;
            double foundLon = Double.NaN;

            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(query, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address a = addresses.get(0);
                        foundLat = a.getLatitude();
                        foundLon = a.getLongitude();
                    }
                } catch (Exception ignored) {}
            }

            if (!isValidCoordinates(foundLat, foundLon) && getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)) {
                List<OpenMeteoClient.SearchResult> results = OpenMeteoClient.search(query);
                if (!results.isEmpty()) {
                    OpenMeteoClient.SearchResult res = results.get(0);
                    foundLat = res.latitude;
                    foundLon = res.longitude;
                }
            }

            final double lat = foundLat;
            final double lon = foundLon;

            AppExecutors.getMainThread().post(() -> {
                if (isAdded() && isValidCoordinates(lat, lon)) {
                    mPickedLat = lat;
                    mPickedLon = lon;
                    mHasPickedLocation = true;
                    updateCoordsText(lat, lon);
                    mWebView.evaluateJavascript("if (typeof window.setLocation === 'function') { setLocation(" + lat + ", " + lon + "); }", null);
                    reverseGeocodePickedLocation(lat, lon);
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.masjid_invalid_coords, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateCoordsText(double lat, double lon) {
        mCoordsText.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", lat, lon));
    }

    private void reverseGeocodePickedLocation(double lat, double lon) {
        final Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        AppExecutors.getNetworkIO().execute(() -> {
            String addrStr = null;
            String city = null;
            String country = null;

            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address a = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i <= a.getMaxAddressLineIndex(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(a.getAddressLine(i));
                        }
                        addrStr = sb.toString();
                        city = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                        country = a.getCountryName();
                    }
                } catch (Exception ignored) {}
            }

            if (addrStr == null && getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)) {
                NominatimClient.SearchResult res = NominatimClient.reverseGeocodeFull(lat, lon);
                if (res != null) {
                    addrStr = res.toString();
                    city = res.name;
                    country = res.country;
                }
            }

            if (city == null) {
                SalahUtils.OfflineCity match = SalahUtils.findNearestOfflineCity(appContext, lat, lon);
                if (match != null) {
                    city = match.name;
                }
            }

            final String finalAddr = addrStr;
            final String finalCity = city;
            final String finalCountry = country;

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    if (finalAddr != null) mPickedAddress = finalAddr;
                    if (finalCity != null) mPickedCity = finalCity;
                    if (finalCountry != null) mPickedCountry = finalCountry;

                    if (mPickedAddress != null && !mPickedAddress.isEmpty()) {
                        mAddressText.setText(mPickedAddress);
                        mAddressText.setVisibility(View.VISIBLE);
                    } else {
                        mAddressText.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void confirmSelection() {
        if (!mHasPickedLocation || !isValidCoordinates(mPickedLat, mPickedLon)) {
            Toast.makeText(requireContext(), R.string.masjid_location_required_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle result = new Bundle();
        result.putDouble("latitude", mPickedLat);
        result.putDouble("longitude", mPickedLon);
        result.putBoolean("has_location", true);
        if (mPickedAddress != null) result.putString("address", mPickedAddress);
        if (mPickedCity != null) result.putString("city", mPickedCity);
        if (mPickedCountry != null) result.putString("country", mPickedCountry);

        getParentFragmentManager().setFragmentResult("map_location_picked", result);
        dismiss();
    }
}
