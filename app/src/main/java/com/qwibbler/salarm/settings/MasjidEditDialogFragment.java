package com.qwibbler.salarm.settings;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_INTERNET_ACCESS_ALLOWED;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_INTERNET_ACCESS_ALLOWED;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.salah.AndroidLocationProvider;
import com.qwibbler.salarm.salah.CitySearchDialogFragment;
import com.qwibbler.salarm.salah.MapPickerDialogFragment;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.MasjidEvent;
import com.qwibbler.salarm.salah.NominatimClient;
import com.qwibbler.salarm.salah.RelationalDefinition;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.SalahTimezoneUtils;
import com.qwibbler.salarm.salah.SalahUtils;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MasjidEditDialogFragment extends DialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private long mMasjidId;
    private Masjid mMasjid;
    private boolean mHasLocation = false;

    private ImageView mIconPreview;
    private View mBtnRemoveIcon;
    private TextInputEditText mNameEdit;
    private TextView mCoordinatesText;
    private TextInputEditText mAddressEdit;
    private TextInputEditText mCityEdit;
    private TextInputEditText mCountryEdit;
    private TextView mTimezoneText;
    private TextView mIntervalsSummary;

    private ActivityResultLauncher<String> mPickIconLauncher;

    public static void show(FragmentManager manager, long masjidId) {
        MasjidEditDialogFragment fragment = new MasjidEditDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASJID_ID, masjidId);
        fragment.setArguments(args);
        Utils.showDialogFragment(manager, fragment, "MasjidEditDialogFragment");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPickIconLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                processSelectedIcon(uri);
            }
        });

        getChildFragmentManager().setFragmentResultListener("city_picked", this, (requestKey, result) -> {
            double lat = result.getDouble("latitude", 0.0);
            double lon = result.getDouble("longitude", 0.0);
            boolean hasLoc = result.containsKey("latitude") || (lat != 0.0 || lon != 0.0);
            String cityName = result.getString("city_name", "");
            String country = result.getString("country", "");
            String tzId = result.getString("timezone_id", null);

            if (mMasjid != null && hasLoc && MapPickerDialogFragment.isValidCoordinates(lat, lon)) {
                mMasjid.latitude = lat;
                mMasjid.longitude = lon;
                mHasLocation = true;

                if (cityName != null && !cityName.trim().isEmpty()) {
                    String[] parts = cityName.split(",");
                    mCityEdit.setText(parts[0].trim());
                }
                if (country != null && !country.trim().isEmpty()) {
                    mCountryEdit.setText(country.trim());
                }

                if (tzId != null && !tzId.trim().isEmpty()) {
                    mMasjid.timezoneId = tzId;
                } else {
                    resolveLocationDetails(lat, lon);
                }

                updateLocationUI();
            }
        });

        getChildFragmentManager().setFragmentResultListener("map_location_picked", this, (requestKey, result) -> {
            double lat = result.getDouble("latitude", 0.0);
            double lon = result.getDouble("longitude", 0.0);
            boolean hasLoc = result.getBoolean("has_location", true);
            String address = result.getString("address", null);
            String cityName = result.getString("city", null);
            String country = result.getString("country", null);

            if (mMasjid != null && hasLoc && MapPickerDialogFragment.isValidCoordinates(lat, lon)) {
                mMasjid.latitude = lat;
                mMasjid.longitude = lon;
                mHasLocation = true;

                if (address != null && !address.trim().isEmpty()) {
                    mAddressEdit.setText(address.trim());
                }
                if (cityName != null && !cityName.trim().isEmpty()) {
                    String[] parts = cityName.split(",");
                    mCityEdit.setText(parts[0].trim());
                }
                if (country != null && !country.trim().isEmpty()) {
                    mCountryEdit.setText(country.trim());
                }

                resolveLocationDetails(lat, lon);
                updateLocationUI();
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_masjid, null);
        mNameEdit = view.findViewById(R.id.masjid_name_edit);
        mIconPreview = view.findViewById(R.id.masjid_icon_preview);
        mBtnRemoveIcon = view.findViewById(R.id.btn_remove_masjid_icon);
        mCoordinatesText = view.findViewById(R.id.masjid_coordinates_text);
        mAddressEdit = view.findViewById(R.id.masjid_address_edit);
        mCityEdit = view.findViewById(R.id.masjid_city_edit);
        mCountryEdit = view.findViewById(R.id.masjid_country_edit);
        mTimezoneText = view.findViewById(R.id.masjid_timezone_text);
        mIntervalsSummary = view.findViewById(R.id.masjid_intervals_summary);

        View intervalsRow = view.findViewById(R.id.masjid_intervals_row);
        View timezoneRow = view.findViewById(R.id.masjid_timezone_row);

        mMasjidId = getArguments() != null ? getArguments().getLong(ARG_MASJID_ID) : 0;

        if (mMasjidId > 0) {
            AppExecutors.getDiskIO().execute(() -> {
                mMasjid = SalahDatabase.getDatabase(requireContext()).masjidDao().getById(mMasjidId);
                AppExecutors.getMainThread().post(() -> {
                    if (isAdded() && mMasjid != null) {
                        mHasLocation = MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)
                            && (mMasjid.latitude != 0.0 || mMasjid.longitude != 0.0);
                        mNameEdit.setText(mMasjid.name);
                        if (mMasjid.address != null) mAddressEdit.setText(mMasjid.address);
                        if (mMasjid.cityName != null) mCityEdit.setText(mMasjid.cityName);
                        if (mMasjid.countryName != null) mCountryEdit.setText(mMasjid.countryName);
                        updateIconUI();
                        updateLocationUI();
                        updateIntervalsSummary();
                    }
                });
            });
        } else {
            mMasjid = new Masjid();
            mMasjid.name = getString(R.string.salah_masjid_default_name);
            mMasjid.timezoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
            mHasLocation = false;
            mNameEdit.setText(mMasjid.name);
            updateIconUI();
            updateLocationUI();
            updateIntervalsSummary();
        }

        View iconContainer = view.findViewById(R.id.masjid_icon_container);
        if (iconContainer != null) {
            iconContainer.setOnClickListener(v -> mPickIconLauncher.launch("image/*"));
        }
        if (mIconPreview != null) {
            mIconPreview.setOnClickListener(v -> mPickIconLauncher.launch("image/*"));
        }
        if (mBtnRemoveIcon != null) {
            mBtnRemoveIcon.setOnClickListener(v -> {
                if (mMasjid != null) {
                    mMasjid.iconUri = null;
                    updateIconUI();
                }
            });
        }

        view.findViewById(R.id.btn_select_location).setOnClickListener(v -> showLocationOptionsDialog());

        timezoneRow.setOnClickListener(v -> showTimezonePicker());

        intervalsRow.setOnClickListener(v ->
            MasjidIntervalsDialogFragment.show(getChildFragmentManager(), mMasjid, (updatedMasjid) -> {
                mMasjid = updatedMasjid;
                updateIntervalsSummary();
            })
        );

        List<CustomDialog.DialogButton> buttons = new ArrayList<>();
        if (mMasjidId > 0) {
            buttons.add(new CustomDialog.DialogButton(getString(R.string.delete), (dialog, which) -> deleteMasjid()));
        }
        buttons.add(new CustomDialog.DialogButton(getString(android.R.string.cancel), null));
        if (mMasjidId > 0) {
            buttons.add(new CustomDialog.DialogButton(getString(R.string.salah_masjid_copy), (dialog, which) -> copyMasjid()));
        }
        buttons.add(new CustomDialog.DialogButton(getString(R.string.salah_masjid_save), (dialog, which) -> saveMasjid()));

        return CustomDialog.create(
            requireContext(),
            R.style.RelationalAlarmDialogTheme,
            null,
            getString(mMasjidId > 0 ? R.string.salah_masjid_edit_title : R.string.salah_add_new_masjid),
            null,
            view,
            null,
            CustomDialog.SoftInputMode.NONE,
            buttons.toArray(new CustomDialog.DialogButton[0])
        );
    }

    private void showLocationOptionsDialog() {
        CharSequence[] options = new CharSequence[]{
            getString(R.string.salah_masjid_location_use_current),
            getString(R.string.salah_masjid_location_search_city),
            getString(R.string.salah_masjid_location_search_map),
            getString(R.string.salah_masjid_location_enter_manual)
        };

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.salah_masjid_select_location_dialog_title)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0 -> useCurrentLocation();
                    case 1 -> searchCity();
                    case 2 -> searchLocationOnMap();
                    case 3 -> enterManualCoordinates();
                }
            })
            .show();
    }

    private void updateIconUI() {
        if (mMasjid != null && mMasjid.iconUri != null && !mMasjid.iconUri.isEmpty()) {
            try {
                mIconPreview.setImageURI(Uri.parse(mMasjid.iconUri));
                if (mIconPreview.getDrawable() == null) {
                    mIconPreview.setImageResource(R.drawable.ic_masjid_timings);
                    if (mBtnRemoveIcon != null) mBtnRemoveIcon.setVisibility(View.GONE);
                } else if (mBtnRemoveIcon != null) {
                    mBtnRemoveIcon.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                mIconPreview.setImageResource(R.drawable.ic_masjid_timings);
                if (mBtnRemoveIcon != null) mBtnRemoveIcon.setVisibility(View.GONE);
            }
        } else {
            mIconPreview.setImageResource(R.drawable.ic_masjid_timings);
            if (mBtnRemoveIcon != null) {
                mBtnRemoveIcon.setVisibility(View.GONE);
            }
        }
    }

    private void updateLocationUI() {
        if (mMasjid != null) {
            if (mHasLocation && MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)) {
                mCoordinatesText.setText(String.format(Locale.getDefault(), getString(R.string.salah_masjid_location_coordinates_format), mMasjid.latitude, mMasjid.longitude));
            } else {
                mCoordinatesText.setText(R.string.salah_masjid_location_required_error);
            }

            if (mMasjid.timezoneId != null && !mMasjid.timezoneId.isEmpty()) {
                mTimezoneText.setText(mMasjid.timezoneId);
            } else {
                mTimezoneText.setText(SalahTimezoneUtils.getPhoneTimeZoneId());
            }
        }
    }

    private void processSelectedIcon(Uri sourceUri) {
        AppExecutors.getDiskIO().execute(() -> {
            try {
                Context context = requireContext();
                File iconDir = new File(context.getFilesDir(), "masjid_icons");
                if (!iconDir.exists()) iconDir.mkdirs();

                File iconFile = new File(iconDir, "masjid_icon_" + System.currentTimeMillis() + ".png");
                try (InputStream is = context.getContentResolver().openInputStream(sourceUri);
                     OutputStream os = new FileOutputStream(iconFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }

                String savedUri = Uri.fromFile(iconFile).toString();
                AppExecutors.getMainThread().post(() -> {
                    if (isAdded() && mMasjid != null) {
                        mMasjid.iconUri = savedUri;
                        updateIconUI();
                    }
                });
            } catch (Exception e) {
                LogUtils.e("Error saving masjid icon image", e);
            }
        });
    }

    private void useCurrentLocation() {
        Context context = requireContext();
        AndroidLocationProvider.requestSingleUpdate(context, 15000, location -> {
            if (location != null && mMasjid != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                if (MapPickerDialogFragment.isValidCoordinates(lat, lon)) {
                    mMasjid.latitude = lat;
                    mMasjid.longitude = lon;
                    mHasLocation = true;
                    resolveLocationDetails(lat, lon);
                    updateLocationUI();
                }
            } else {
                Toast.makeText(context, R.string.salah_detecting_location, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchCity() {
        CitySearchDialogFragment dialog = new CitySearchDialogFragment();
        dialog.setPickerMode(true);
        dialog.show(getChildFragmentManager(), "CitySearchDialogFragment");
    }

    private void searchLocationOnMap() {
        double initLat = (mMasjid != null && mHasLocation) ? mMasjid.latitude : 0.0;
        double initLon = (mMasjid != null && mHasLocation) ? mMasjid.longitude : 0.0;
        MapPickerDialogFragment dialog = MapPickerDialogFragment.newInstance(initLat, initLon, mHasLocation);
        dialog.show(getChildFragmentManager(), "MapPickerDialogFragment");
    }

    private void enterManualCoordinates() {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText latInput = new EditText(context);
        latInput.setHint(R.string.salah_masjid_latitude);
        latInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        if (mMasjid != null && mHasLocation) {
            latInput.setText(String.valueOf(mMasjid.latitude));
        }
        layout.addView(latInput);

        final EditText lonInput = new EditText(context);
        lonInput.setHint(R.string.salah_masjid_longitude);
        lonInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        if (mMasjid != null && mHasLocation) {
            lonInput.setText(String.valueOf(mMasjid.longitude));
        }
        layout.addView(lonInput);

        new AlertDialog.Builder(context)
            .setTitle(R.string.salah_masjid_manual_coords_dialog_title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                try {
                    double lat = Double.parseDouble(latInput.getText().toString().trim());
                    double lon = Double.parseDouble(lonInput.getText().toString().trim());
                    if (!MapPickerDialogFragment.isValidCoordinates(lat, lon)) {
                        Toast.makeText(context, R.string.salah_masjid_invalid_coords, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mMasjid != null) {
                        mMasjid.latitude = lat;
                        mMasjid.longitude = lon;
                        mHasLocation = true;
                        resolveLocationDetails(lat, lon);
                        updateLocationUI();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.salah_masjid_invalid_coords, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void resolveLocationDetails(double lat, double lon) {
        Context context = requireContext();
        final Context appContext = context.getApplicationContext();

        AppExecutors.getNetworkIO().execute(() -> {
            String city = null;
            String country = null;
            String tz = null;

            // 1. Offline city lookup
            SalahUtils.OfflineCity match = SalahUtils.findNearestOfflineCity(appContext, lat, lon);
            if (match != null) {
                city = match.name;
                tz = match.timezoneId;
            }

            // 2. System Geocoder if available
            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        if (addr.getLocality() != null) city = addr.getLocality();
                        else if (addr.getSubAdminArea() != null) city = addr.getSubAdminArea();
                        if (addr.getCountryName() != null) country = addr.getCountryName();
                    }
                } catch (Exception ignored) {}
            }

            // 3. Online fallback if needed and allowed
            if ((city == null || country == null) && getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)) {
                NominatimClient.SearchResult res = NominatimClient.reverseGeocodeFull(lat, lon);
                if (res != null) {
                    if (city == null && res.name != null) city = res.name;
                    if (country == null && res.country != null) country = res.country;
                }
            }

            // 4. Timezone lookup
            if (tz == null) {
                tz = SalahTimezoneUtils.fetchTimeZoneId(appContext, lat, lon);
            }
            if (tz == null) {
                tz = SalahTimezoneUtils.getPhoneTimeZoneId();
            }

            final String finalCity = city;
            final String finalCountry = country;
            final String finalTz = tz;

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    if (finalCity != null) {
                        mCityEdit.setText(finalCity);
                    }
                    if (finalCountry != null) {
                        mCountryEdit.setText(finalCountry);
                    }
                    if (finalTz != null) {
                        mMasjid.timezoneId = finalTz;
                        mTimezoneText.setText(finalTz);
                    }
                }
            });
        });
    }

    private void showTimezonePicker() {
        String[] availableIds = TimeZone.getAvailableIDs();
        Arrays.sort(availableIds);

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.salah_masjid_select_timezone_title)
            .setItems(availableIds, (dialog, which) -> {
                String selectedTz = availableIds[which];
                if (mMasjid != null) {
                    mMasjid.timezoneId = selectedTz;
                    mTimezoneText.setText(selectedTz);
                }
            })
            .show();
    }

    private void updateIntervalsSummary() {
        if (mMasjid != null) {
            mIntervalsSummary.setText(mMasjid.getIntervalsSummary(requireContext()));
        }
    }

    private void saveMasjid() {
        if (mMasjid == null) return;

        if (!mHasLocation || !MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)) {
            Toast.makeText(requireContext(), R.string.salah_masjid_location_required_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mNameEdit.getText() == null) return;
        mMasjid.name = mNameEdit.getText().toString().trim();

        if (mAddressEdit.getText() != null) mMasjid.address = mAddressEdit.getText().toString().trim();
        if (mCityEdit.getText() != null) mMasjid.cityName = mCityEdit.getText().toString().trim();
        if (mCountryEdit.getText() != null) mMasjid.countryName = mCountryEdit.getText().toString().trim();

        if (mMasjid.timezoneId == null || mMasjid.timezoneId.isEmpty()) {
            mMasjid.timezoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
        }

        final Context appContext = requireContext().getApplicationContext();
        final FragmentManager fragmentManager = getParentFragmentManager();
        AppExecutors.getDiskIO().execute(() -> {
            if (mMasjid.id == 0) {
                SalahDatabase.getDatabase(appContext).masjidDao().insert(mMasjid);
            } else {
                SalahDatabase.getDatabase(appContext).masjidDao().update(mMasjid);
            }
            fragmentManager.setFragmentResult("masjid_updated", new Bundle());
        });
    }

    private void deleteMasjid() {
        if (mMasjidId > 0) {
            final Context context = requireContext();
            final Context appContext = context.getApplicationContext();
            final FragmentManager fragmentManager = getParentFragmentManager();
            CustomDialog.create(
                context,
                null,
                null,
                getString(R.string.delete),
                getString(R.string.salah_masjid_delete_confirm),
                null,
                null,
                CustomDialog.SoftInputMode.NONE,
                new CustomDialog.DialogButton(getString(android.R.string.cancel), null),
                new CustomDialog.DialogButton(getString(R.string.delete), (dialog, which) -> {
                    AppExecutors.getDiskIO().execute(() -> {
                        SalahDatabase db = SalahDatabase.getDatabase(appContext);
                        db.masjidDao().deleteById(mMasjidId);

                        // Delete associated events
                        List<MasjidEvent> events = db.masjidEventDao().getByMasjidId(mMasjidId);
                        for (MasjidEvent e : events) {
                            db.masjidEventDao().delete(e);
                        }

                        List<RelationalRule> rules = db.relationalRuleDao().getAll();
                        for (RelationalRule r : rules) {
                            if (r.masjidId == mMasjidId) {
                                db.relationalRuleDao().delete(r);
                            }
                        }
                        SalahAlarmSyncManager.syncSelectedRulesToAlarms(appContext);
                        fragmentManager.setFragmentResult("masjid_updated", new Bundle());
                    });
                    dismiss();
                })
            ).show();
        }
    }

    private void copyMasjid() {
        if (mMasjid == null || mMasjidId <= 0) return;

        final Context appContext = requireContext().getApplicationContext();
        final FragmentManager fragmentManager = getParentFragmentManager();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);

            // 1. Copy Masjid
            Masjid newMasjid = new Masjid();
            newMasjid.name = mMasjid.name + ' ' + getString(R.string.salah_masjid_copy_suffix);
            newMasjid.showMasjidEvents = mMasjid.showMasjidEvents;
            newMasjid.intervalsCount = mMasjid.intervalsCount;
            newMasjid.boundary1 = mMasjid.boundary1;
            newMasjid.boundary2 = mMasjid.boundary2;
            newMasjid.iconUri = mMasjid.iconUri;
            newMasjid.latitude = mMasjid.latitude;
            newMasjid.longitude = mMasjid.longitude;
            newMasjid.address = mMasjid.address;
            newMasjid.cityName = mMasjid.cityName;
            newMasjid.countryName = mMasjid.countryName;
            newMasjid.timezoneId = mMasjid.timezoneId;

            long newId = db.masjidDao().insert(newMasjid);

            // 2. Copy MasjidEvents
            List<MasjidEvent> oldEvents = db.masjidEventDao().getByMasjidId(mMasjidId);
            Map<Long, Long> eventIdMap = new HashMap<>();
            for (MasjidEvent oldEvent : oldEvents) {
                MasjidEvent newEvent = new MasjidEvent();
                newEvent.masjidId = newId;
                newEvent.salahType = oldEvent.salahType;
                newEvent.isSelected = oldEvent.isSelected;
                newEvent.isManuallyDeselected = oldEvent.isManuallyDeselected;
                newEvent.definition = new RelationalDefinition(oldEvent.definition);

                long newEventId = db.masjidEventDao().insert(newEvent);
                eventIdMap.put(oldEvent.id, newEventId);
            }

            // 3. Copy Rules
            List<RelationalRule> rules = db.relationalRuleDao().getByMasjidId(mMasjidId);
            for (RelationalRule rule : rules) {
                RelationalRule newRule = new RelationalRule(rule);
                newRule.id = 0; // New record
                newRule.masjidId = newId;

                // Update anchors if they point to masjid events of the same masjid
                if (newRule.definition.anchor.masjidEventId != null && eventIdMap.containsKey(newRule.definition.anchor.masjidEventId)) {
                    newRule.definition.anchor.masjidEventId = eventIdMap.get(newRule.definition.anchor.masjidEventId);
                }
                if (newRule.definition.clip != null && newRule.definition.clip.anchor.masjidEventId != null && eventIdMap.containsKey(newRule.definition.clip.anchor.masjidEventId)) {
                    newRule.definition.clip.anchor.masjidEventId = eventIdMap.get(newRule.definition.clip.anchor.masjidEventId);
                }
                if (newRule.autostopAnchor.masjidEventId != null && eventIdMap.containsKey(newRule.autostopAnchor.masjidEventId)) {
                    newRule.autostopAnchor.masjidEventId = eventIdMap.get(newRule.autostopAnchor.masjidEventId);
                }

                db.relationalRuleDao().insert(newRule);
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    fragmentManager.setFragmentResult("masjid_updated", new Bundle());
                    dismiss();
                }
            });
        });
    }
}
