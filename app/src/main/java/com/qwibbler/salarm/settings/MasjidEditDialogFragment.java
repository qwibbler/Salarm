package com.qwibbler.salarm.settings;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_INTERNET_ACCESS_ALLOWED;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_INTERNET_ACCESS_ALLOWED;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.RangeSlider;
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
import com.qwibbler.salarm.salah.ui.MasjidCardEditorActivity;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.InsetsUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MasjidEditDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_MASJID_ID = "arg_masjid_id";
    private long mMasjidId;
    private Masjid mMasjid;
    private boolean mHasLocation = false;

    private ImageView mIconPreview;
    private View mBtnRemoveIcon;
    private TextView mNameText;
    private View mNameLayout;
    private TextInputEditText mNameEdit;
    private TextView mCoordinatesText;
    private TextInputEditText mAddressEdit;
    private TextInputEditText mCityEdit;
    private TextInputEditText mCountryEdit;
    private TextView mTimezoneText;
    private View mCalcMethodRow;
    private TextView mCalcMethodText;
    private View mMadhabRow;
    private TextView mMadhabText;
    private TextView mIntervalsSummary;
    private MaterialButtonToggleGroup mIntervalToggleGroup;
    private RangeSlider mIntervalSlider;
    private View mSaveBtn;

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

        getChildFragmentManager().setFragmentResultListener("masjid_imported", this, (requestKey, result) -> {
            long newId = result.getLong("selected_masjid_id", -1);
            if (newId != -1) {
                getParentFragmentManager().setFragmentResult("masjid_updated", result);
                dismiss();
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
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        Window window = dialog.getWindow();
        if (window != null) {
            ThemeUtils.allowDisplayCutout(window);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            Utils.setupTapToDismissKeyboard(window);
        }

        ViewGroup container = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_masjid, container, false);
        dialog.setContentView(view);

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int visualPadding = (int) dpToPx(8, getResources().getDisplayMetrics());

        InsetsUtils.doOnApplyWindowInsets(view, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusBarHeight = statusBars.top;
            behavior.setMaxHeight(screenHeight - statusBarHeight - visualPadding);
        });

        mNameText = view.findViewById(R.id.masjid_name_text);
        mNameLayout = view.findViewById(R.id.masjid_name_layout);
        mNameEdit = view.findViewById(R.id.masjid_name_edit);
        mIconPreview = view.findViewById(R.id.masjid_icon_preview);
        mBtnRemoveIcon = view.findViewById(R.id.btn_remove_masjid_icon);
        mCoordinatesText = view.findViewById(R.id.masjid_coordinates_text);
        mAddressEdit = view.findViewById(R.id.masjid_address_edit);
        mCityEdit = view.findViewById(R.id.masjid_city_edit);
        mCountryEdit = view.findViewById(R.id.masjid_country_edit);
        mTimezoneText = view.findViewById(R.id.masjid_timezone_text);
        mCalcMethodRow = view.findViewById(R.id.masjid_calc_method_row);
        mCalcMethodText = view.findViewById(R.id.masjid_calc_method_text);
        mMadhabRow = view.findViewById(R.id.masjid_madhab_row);
        mMadhabText = view.findViewById(R.id.masjid_madhab_text);
        mIntervalsSummary = view.findViewById(R.id.masjid_intervals_summary);
        mIntervalToggleGroup = view.findViewById(R.id.interval_toggle_group);
        mIntervalSlider = view.findViewById(R.id.interval_slider);

        View deleteBtn = view.findViewById(R.id.delete_button);
        View copyBtn = view.findViewById(R.id.copy_button);
        View previewBtn = view.findViewById(R.id.preview_button);
        mSaveBtn = view.findViewById(R.id.save_button);

        View selectLocationBtn = view.findViewById(R.id.btn_select_location);
        View timezoneRow = view.findViewById(R.id.masjid_timezone_row);

        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());

        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            prefs,
            selectLocationBtn,
            timezoneRow,
            mCalcMethodRow,
            mMadhabRow
        );

        setupNameEditToggle();

        mMasjidId = getArguments() != null ? getArguments().getLong(ARG_MASJID_ID) : 0;

        if (previewBtn instanceof MaterialButton pBtn) {
            if (mMasjidId > 0) {
                pBtn.setIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_preview));
                pBtn.setOnClickListener(v -> openPreview());
            } else {
                pBtn.setIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_cancel));
                pBtn.setOnClickListener(v -> dismiss());
            }
        }

        if (mMasjidId > 0) {
            if (deleteBtn != null) deleteBtn.setVisibility(View.VISIBLE);
            if (copyBtn != null) copyBtn.setVisibility(View.VISIBLE);
            AppExecutors.getDiskIO().execute(() -> {
                mMasjid = SalahDatabase.getDatabase(requireContext()).masjidDao().getById(mMasjidId);
                AppExecutors.getMainThread().post(() -> {
                    if (isAdded() && mMasjid != null) {
                        boolean defaulted = mMasjid.fillMissingValuesWithDefaults(requireContext());
                        if (defaulted) {
                            Toast.makeText(requireContext(), R.string.masjid_missing_values_defaults_toast, Toast.LENGTH_LONG).show();
                            AppExecutors.getDiskIO().execute(() -> SalahDatabase.getDatabase(requireContext().getApplicationContext()).masjidDao().update(mMasjid));
                        }
                        mHasLocation = MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)
                            && (mMasjid.latitude != 0.0 || mMasjid.longitude != 0.0);
                        mNameText.setText(mMasjid.name);
                        mNameEdit.setText(mMasjid.name);
                        if (mMasjid.address != null) mAddressEdit.setText(mMasjid.address);
                        if (mMasjid.cityName != null) mCityEdit.setText(mMasjid.cityName);
                        if (mMasjid.countryName != null) mCountryEdit.setText(mMasjid.countryName);
                        updateIconUI();
                        updateLocationUI();
                        updateCalcMethodUI();
                        updateMadhabUI();
                        setupIntervalsUI(view);
                    }
                });
            });
        } else {
            if (deleteBtn != null) deleteBtn.setVisibility(View.GONE);
            if (copyBtn != null) copyBtn.setVisibility(View.GONE);
            mMasjid = new Masjid();
            mMasjid.fillMissingValuesWithDefaults(requireContext());
            mHasLocation = MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)
                && (mMasjid.latitude != 0.0 || mMasjid.longitude != 0.0);
            mNameText.setText(mMasjid.name);
            mNameEdit.setText(mMasjid.name);
            updateIconUI();
            updateLocationUI();
            updateCalcMethodUI();
            updateMadhabUI();
            setupIntervalsUI(view);
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

        if (selectLocationBtn != null) {
            selectLocationBtn.setOnClickListener(v -> showLocationOptionsDialog());
        }

        if (timezoneRow != null) {
            timezoneRow.setOnClickListener(v -> showTimezonePicker());
        }

        if (mCalcMethodRow != null) {
            mCalcMethodRow.setOnClickListener(v -> showCalcMethodPicker());
        }

        if (mMadhabRow != null) {
            mMadhabRow.setOnClickListener(v -> showMadhabPicker());
        }

        if (deleteBtn != null) deleteBtn.setOnClickListener(v -> deleteMasjid());
        if (copyBtn != null) copyBtn.setOnClickListener(v -> copyMasjid());
        if (mSaveBtn != null) mSaveBtn.setOnClickListener(v -> saveMasjid());

        return dialog;
    }

    private void setupNameEditToggle() {
        if (mNameText == null || mNameLayout == null || mNameEdit == null) return;

        mNameText.setOnClickListener(v -> showNameEdit());

        mNameEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateNameFromEdit();
                updateLocationUI();
            }
        });

        mNameEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideNameEdit();
                return true;
            }
            return false;
        });
    }

    private void showNameEdit() {
        if (mNameText == null || mNameLayout == null || mNameEdit == null) return;
        mNameText.setVisibility(View.GONE);
        mNameLayout.setVisibility(View.VISIBLE);
        mNameEdit.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mNameEdit, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideNameEdit() {
        if (mNameLayout == null || mNameText == null) return;
        if (mNameLayout.getVisibility() == View.VISIBLE) {
            updateNameFromEdit();
            mNameLayout.setVisibility(View.GONE);
            mNameText.setVisibility(View.VISIBLE);
            if (isAdded()) {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && mNameEdit != null) {
                    imm.hideSoftInputFromWindow(mNameEdit.getWindowToken(), 0);
                }
            }
        }
    }

    private void updateNameFromEdit() {
        if (mNameEdit != null && mNameEdit.getText() != null) {
            String newName = mNameEdit.getText().toString().trim();
            if (mMasjid != null) {
                mMasjid.name = newName;
                if (mNameText != null) {
                    mNameText.setText(newName.isEmpty() ? getString(R.string.masjid_name_title) : newName);
                }
            }
        }
    }

    private void setupIntervalsUI(View rootView) {
        if (mMasjid == null || mIntervalToggleGroup == null || mIntervalSlider == null) return;

        if (mMasjid.intervalsCount != 2 && mMasjid.intervalsCount != 3) {
            mMasjid.intervalsCount = 2;
        }
        if (mMasjid.intervalsCount == 2) {
            if (mMasjid.boundary1 <= 0 || mMasjid.boundary1 >= 30) {
                mMasjid.boundary1 = 16;
            }
            mMasjid.boundary2 = 31;
        } else {
            if (mMasjid.boundary1 <= 0) mMasjid.boundary1 = 10;
            if (mMasjid.boundary2 <= mMasjid.boundary1 || mMasjid.boundary2 >= 31) mMasjid.boundary2 = 20;
        }

        MaterialButton btn2 = rootView.findViewById(R.id.button_2_intervals);
        MaterialButton btn3 = rootView.findViewById(R.id.button_3_intervals);

        int checkedId = (mMasjid.intervalsCount == 3) ? R.id.button_3_intervals : R.id.button_2_intervals;
        mIntervalToggleGroup.check(checkedId);
        updateIntervalButtonVisuals(btn2, checkedId == R.id.button_2_intervals);
        updateIntervalButtonVisuals(btn3, checkedId == R.id.button_3_intervals);

        mIntervalToggleGroup.clearOnButtonCheckedListeners();
        mIntervalToggleGroup.addOnButtonCheckedListener((group, checkedButtonId, isChecked) -> {
            if (isChecked) {
                boolean is2 = (checkedButtonId == R.id.button_2_intervals);
                mMasjid.intervalsCount = is2 ? 2 : 3;
                if (is2) {
                    mMasjid.boundary1 = 16;
                    mMasjid.boundary2 = 31;
                } else {
                    mMasjid.boundary1 = 10;
                    mMasjid.boundary2 = 20;
                }
                updateIntervalButtonVisuals(btn2, is2);
                updateIntervalButtonVisuals(btn3, !is2);
                updateSliderFromMasjid();
                updateIntervalsSummary();
            }
        });

        mIntervalSlider.clearOnChangeListeners();
        mIntervalSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && mMasjid != null) {
                List<Float> values = slider.getValues();
                if (mMasjid.intervalsCount == 2) {
                    if (!values.isEmpty()) {
                        mMasjid.boundary1 = values.get(0).intValue();
                    }
                } else {
                    if (values.size() >= 2) {
                        mMasjid.boundary1 = values.get(0).intValue();
                        mMasjid.boundary2 = values.get(1).intValue();
                    }
                }
                updateIntervalsSummary();
            }
        });

        updateSliderFromMasjid();
        updateIntervalsSummary();
    }

    private void updateIntervalButtonVisuals(MaterialButton button, boolean isSelected) {
        if (button == null || !isAdded()) return;

        final int backgroundColor = isSelected
            ? MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.BLACK)
            : Color.TRANSPARENT;

        final ColorStateList strokeColor = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), isSelected
            ? com.google.android.material.R.attr.colorTertiary
            : com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK)
        );

        final int textColor = MaterialColors.getColor(requireContext(), isSelected
            ? com.google.android.material.R.attr.colorSurfaceContainerLowest
            : com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);

        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setStrokeColor(strokeColor);
        button.setTextColor(textColor);
    }

    private void updateSliderFromMasjid() {
        if (mIntervalSlider == null || mMasjid == null) return;
        if (mMasjid.intervalsCount == 2) {
            float b1 = Math.max(1f, Math.min(30f, (float) mMasjid.boundary1));
            mIntervalSlider.setValues(b1);
        } else {
            float b1 = Math.max(1f, Math.min(29f, (float) mMasjid.boundary1));
            float b2 = Math.max(b1 + 1f, Math.min(30f, (float) mMasjid.boundary2));
            mIntervalSlider.setValues(b1, b2);
        }
    }

    private void openPreview() {
        if (mMasjid == null) return;

        updateNameFromEdit();

        boolean defaulted = mMasjid.fillMissingValuesWithDefaults(requireContext());
        if (defaulted) {
            Toast.makeText(requireContext(), R.string.masjid_missing_values_defaults_toast, Toast.LENGTH_LONG).show();
            mHasLocation = MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)
                && (mMasjid.latitude != 0.0 || mMasjid.longitude != 0.0);
            updateLocationUI();
        }

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
                long newId = SalahDatabase.getDatabase(appContext).masjidDao().insert(mMasjid);
                mMasjid.id = newId;
                mMasjidId = newId;
            } else {
                SalahDatabase.getDatabase(appContext).masjidDao().update(mMasjid);
            }
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    Bundle result = new Bundle();
                    result.putLong("selected_masjid_id", mMasjid.id);
                    fragmentManager.setFragmentResult("masjid_updated", result);
                    Intent intent = new Intent(requireContext(), MasjidCardEditorActivity.class);
                    intent.putExtra("masjid_id", mMasjid.id);
                    startActivity(intent);
                }
            });
        });
    }

    private void showLocationOptionsDialog() {
        CharSequence[] options = new CharSequence[]{
            getString(R.string.masjid_location_use_current),
            getString(R.string.masjid_location_search_city),
            getString(R.string.masjid_location_search_map),
            getString(R.string.masjid_location_enter_manual)
        };

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.masjid_select_location_dialog_title)
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
            boolean validLoc = mHasLocation && MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude);
            if (validLoc) {
                mCoordinatesText.setText(String.format(Locale.getDefault(), getString(R.string.masjid_location_coordinates_format), mMasjid.latitude, mMasjid.longitude));
            } else {
                mCoordinatesText.setText("");
            }

            if (mMasjid.timezoneId != null && !mMasjid.timezoneId.isEmpty()) {
                mTimezoneText.setText(mMasjid.timezoneId);
            } else {
                mTimezoneText.setText(SalahTimezoneUtils.getPhoneTimeZoneId());
            }

            if (mSaveBtn != null) {
                mSaveBtn.setEnabled(true);
                mSaveBtn.setAlpha(1.0f);
            }
        }
    }

    private void processSelectedIcon(Uri sourceUri) {
        AppExecutors.getDiskIO().execute(() -> {
            try {
                Context context = requireContext();
                File iconDir = new File(context.getFilesDir(), "masjid_icons");
                if (!iconDir.exists() && !iconDir.mkdirs()) {
                    LogUtils.e("Failed to create icon directory");
                }

                File iconFile = new File(iconDir, "masjid_icon_" + System.currentTimeMillis() + ".png");
                try (InputStream is = context.getContentResolver().openInputStream(sourceUri);
                     OutputStream os = new FileOutputStream(iconFile)) {
                    if (is != null) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
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
                Toast.makeText(context, R.string.salarm_detecting_location, Toast.LENGTH_SHORT).show();
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
        latInput.setHint(R.string.masjid_latitude);
        latInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        if (mMasjid != null && mHasLocation) {
            latInput.setText(String.valueOf(mMasjid.latitude));
        }
        layout.addView(latInput);

        final EditText lonInput = new EditText(context);
        lonInput.setHint(R.string.masjid_longitude);
        lonInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        if (mMasjid != null && mHasLocation) {
            lonInput.setText(String.valueOf(mMasjid.longitude));
        }
        layout.addView(lonInput);

        new AlertDialog.Builder(context)
            .setTitle(R.string.masjid_manual_coords_dialog_title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                try {
                    double lat = Double.parseDouble(latInput.getText().toString().trim());
                    double lon = Double.parseDouble(lonInput.getText().toString().trim());
                    if (!MapPickerDialogFragment.isValidCoordinates(lat, lon)) {
                        Toast.makeText(context, R.string.masjid_invalid_coords, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(context, R.string.masjid_invalid_coords, Toast.LENGTH_SHORT).show();
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
            .setTitle(R.string.masjid_select_timezone_title)
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
        if (mMasjid != null && mIntervalsSummary != null) {
            mIntervalsSummary.setText(mMasjid.getIntervalsSummary(requireContext()));
        }
    }

    private void saveMasjid() {
        if (mMasjid == null) return;

        updateNameFromEdit();

        boolean defaulted = mMasjid.fillMissingValuesWithDefaults(requireContext());
        if (defaulted) {
            Toast.makeText(requireContext(), R.string.masjid_missing_values_defaults_toast, Toast.LENGTH_LONG).show();
            mHasLocation = MapPickerDialogFragment.isValidCoordinates(mMasjid.latitude, mMasjid.longitude)
                && (mMasjid.latitude != 0.0 || mMasjid.longitude != 0.0);
            updateLocationUI();
        }

        if (mAddressEdit.getText() != null) mMasjid.address = mAddressEdit.getText().toString().trim();
        if (mCityEdit.getText() != null) mMasjid.cityName = mCityEdit.getText().toString().trim();
        if (mCountryEdit.getText() != null) mMasjid.countryName = mCountryEdit.getText().toString().trim();

        if (mMasjid.timezoneId == null || mMasjid.timezoneId.isEmpty()) {
            mMasjid.timezoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
        }

        final Context appContext = requireContext().getApplicationContext();
        final FragmentManager fragmentManager = getParentFragmentManager();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            if (mMasjid.id == 0) {
                mMasjid.id = db.masjidDao().insert(mMasjid);
            } else {
                db.masjidDao().update(mMasjid);

                List<MasjidEvent> events = db.masjidEventDao().getByMasjidId(mMasjid.id);
                Madhab madhab = mMasjid.getMadhab(appContext);
                RelationalRule.AnchorEvent targetAsr = (madhab == Madhab.SHAFI) ? RelationalRule.AnchorEvent.ASR_SHAFI : RelationalRule.AnchorEvent.ASR_HANAFI;

                for (MasjidEvent event : events) {
                    if ("ASR".equals(event.salahType)) {
                        boolean changed = false;
                        if (event.definition.anchor.basicEvent == RelationalRule.AnchorEvent.ASR_HANAFI || event.definition.anchor.basicEvent == RelationalRule.AnchorEvent.ASR_SHAFI) {
                            if (event.definition.anchor.basicEvent != targetAsr) {
                                event.definition.anchor.basicEvent = targetAsr;
                                changed = true;
                            }
                        }
                        if (event.definition.clip != null && (event.definition.clip.anchor.basicEvent == RelationalRule.AnchorEvent.ASR_HANAFI || event.definition.clip.anchor.basicEvent == RelationalRule.AnchorEvent.ASR_SHAFI)) {
                            if (event.definition.clip.anchor.basicEvent != targetAsr) {
                                event.definition.clip.anchor.basicEvent = targetAsr;
                                changed = true;
                            }
                        }
                        if (changed) {
                            db.masjidEventDao().update(event);
                        }
                    }
                }
                SalahAlarmSyncManager.recalculateAllActiveRules(appContext);
            }
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    Bundle result = new Bundle();
                    result.putLong("selected_masjid_id", mMasjid.id);
                    fragmentManager.setFragmentResult("masjid_updated", result);
                    dismiss();
                }
            });
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
                getString(R.string.masjid_delete_confirm),
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
                        AppExecutors.getMainThread().post(() -> {
                            if (isAdded()) {
                                fragmentManager.setFragmentResult("masjid_updated", new Bundle());
                            }
                        });
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
            newMasjid.name = mMasjid.name + ' ' + getString(R.string.masjid_copy_suffix);
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
            newMasjid.calcMethod = mMasjid.calcMethod;
            newMasjid.madhab = mMasjid.madhab;

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
                    Bundle result = new Bundle();
                    result.putLong("selected_masjid_id", newId);
                    fragmentManager.setFragmentResult("masjid_updated", result);
                    dismiss();
                }
            });
        });
    }

    private void updateCalcMethodUI() {
        if (mMasjid != null && mCalcMethodText != null && isAdded()) {
            CalculationMethod currentMethod = mMasjid.getCalculationMethod(requireContext());
            mCalcMethodText.setText(getCalcMethodDisplayName(requireContext(), currentMethod));
        }
    }

    private void updateMadhabUI() {
        if (mMasjid != null && mMadhabText != null && isAdded()) {
            Madhab currentMadhab = mMasjid.getMadhab(requireContext());
            mMadhabText.setText(getMadhabDisplayName(requireContext(), currentMadhab));
        }
    }

    private String getCalcMethodDisplayName(Context context, CalculationMethod method) {
        String[] entries = context.getResources().getStringArray(R.array.salah_calc_method_entries);
        String[] values = context.getResources().getStringArray(R.array.salah_calc_method_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(method.name())) {
                return entries[i];
            }
        }
        return method.name();
    }

    private String getMadhabDisplayName(Context context, Madhab madhab) {
        String[] entries = context.getResources().getStringArray(R.array.salarm_madhab_entries);
        String[] values = context.getResources().getStringArray(R.array.salarm_madhab_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(madhab.name())) {
                return entries[i];
            }
        }
        return madhab.name();
    }

    private void showCalcMethodPicker() {
        String[] entries = getResources().getStringArray(R.array.salah_calc_method_entries);
        String[] values = getResources().getStringArray(R.array.salah_calc_method_values);

        CalculationMethod currentMethod = mMasjid != null ? mMasjid.getCalculationMethod(requireContext()) : SalahUtils.getCalculationMethod(requireContext());

        int currentSelection = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentMethod.name())) {
                currentSelection = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.masjid_select_calc_method_title)
            .setSingleChoiceItems(entries, currentSelection, (dialog, which) -> {
                if (mMasjid != null) {
                    mMasjid.calcMethod = values[which];
                    updateCalcMethodUI();
                }
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void showMadhabPicker() {
        String[] entries = getResources().getStringArray(R.array.salarm_madhab_entries);
        String[] values = getResources().getStringArray(R.array.salarm_madhab_values);

        Madhab currentMadhab = mMasjid != null ? mMasjid.getMadhab(requireContext()) : SalahUtils.getMadhab(requireContext());

        int currentSelection = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentMadhab.name())) {
                currentSelection = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.masjid_select_madhab_title)
            .setSingleChoiceItems(entries, currentSelection, (dialog, which) -> {
                if (mMasjid != null) {
                    mMasjid.madhab = values[which];
                    updateMadhabUI();
                }
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
}
