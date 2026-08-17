// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.settings;

import static android.app.Activity.RESULT_OK;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.alarms.AlarmUpdateHandler;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.data.DataModel.ClockStyle;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.settings.custompreference.ColorPickerPreference;
import com.qwibbler.salarm.settings.custompreference.CustomSliderPreference;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.uicomponents.toast.CustomToast;
import com.qwibbler.salarm.utils.AlarmUtils;
import com.qwibbler.salarm.utils.FileUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.SdkUtils;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.Calendar;
import java.util.List;

public class NamazAlarmDisplayCustomizationFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_PENDING_DIALOG_PREF_KEY = "pending_dialog_pref_key";

    private String mPendingDialogPrefKey = null;

    private AlarmUpdateHandler mAlarmUpdateHandler;

    String[] mAlarmClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mAlarmClockStylePref;
    ListPreference mAlarmClockDialPref;
    ListPreference mAlarmClockDialMaterialPref;
    CustomSliderPreference mAnalogClockSizePref;
    ListPreference mAlarmClockSecondHandPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mSwipeActionPref;
    SwitchPreferenceCompat mDisplaySnoozeSelectorPref;
    ColorPickerPreference mBackgroundColorPref;
    ColorPickerPreference mBackgroundAmoledColorPref;
    ColorPickerPreference mAlarmClockColorPref;
    ColorPickerPreference mAlarmSecondHandColorPref;
    ColorPickerPreference mAlarmTitleColorPref;
    ColorPickerPreference mSlideZoneColorPref;
    ColorPickerPreference mAlarmButtonColorPref;
    ColorPickerPreference mSnoozeTitleColorPref;
    ColorPickerPreference mSnoozeButtonColorPref;
    ColorPickerPreference mDismissTitleColorPref;
    ColorPickerPreference mDismissButtonColorPref;
    ColorPickerPreference mSnoozeZoneColorPref;
    ColorPickerPreference mSnoozeMinusButtonColorPref;
    ColorPickerPreference mSnoozePlusButtonColorPref;
    ColorPickerPreference mSnoozeSelectorTextColorPref;
    ColorPickerPreference mSnoozeMinusSymbolColorPref;
    ColorPickerPreference mSnoozePlusSymbolColorPref;
    CustomSliderPreference mAlarmDigitalClockFontSizePref;
    CustomSliderPreference mAlarmTitleFontSizePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    ColorPickerPreference mShadowColorPref;
    CustomSliderPreference mShadowOffsetPref;
    SwitchPreferenceCompat mDisplayAlarmActionMessagePref;
    SwitchPreferenceCompat mDisplayAlarmTitleOnSingleLinePref;
    SwitchPreferenceCompat mDisplayRingtoneTitlePref;
    ColorPickerPreference mRingtoneTitleColorPref;
    Preference mAlarmBackgroundImagePref;
    CustomSliderPreference mAlarmBlurIntensityPref;
    SwitchPreferenceCompat mEnablePerAlarmBackgroundImagePref;
    SwitchPreferenceCompat mDisplayStyleOverridePref;
    SwitchPreferenceCompat mColorsOverridePref;
    SwitchPreferenceCompat mFontOverridePref;
    SwitchPreferenceCompat mBackgroundImageOverridePref;
    Preference mAlarmFontPref;
    Preference mAlarmPreviewPref;

    Preference mStyleCategory;
    Preference mColorsCategory;
    Preference mFontCategory;
    Preference mBackgroundImageCategory;
    Preference mMiscCategory;

    private final ActivityResultLauncher<Intent> fontPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK) {
                return;
            }

            Intent intent = result.getData();
            final Uri sourceUri = intent == null ? null : intent.getData();
            if (sourceUri == null) {
                return;
            }

            final Context appContext = requireContext().getApplicationContext();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(FILE_ALARM_FONT + "_namaz");
            String oldFontPath = mPrefs.getString(KEY_NAMAZ_ALARM_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                FileUtils.clearFile(oldFontPath);

                // Clear the font cache
                ThemeUtils.removeFontFromCache(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_NAMAZ_ALARM_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing font");
                    }

                    if (!isAdded() || mAlarmFontPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mAlarmFontPref.setTitle(getString(R.string.custom_font_title_variant));
                    } else {
                        mAlarmFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });
            });
        });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK) {
                return;
            }

            Intent intent = result.getData();
            final Uri sourceUri = intent == null ? null : intent.getData();
            if (sourceUri == null) {
                return;
            }

            final Context appContext = requireContext().getApplicationContext();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(FILE_ALARM_BACKGROUND + "_namaz");
            String oldImagePath = mPrefs.getString(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old image if it exists
                FileUtils.clearFile(oldImagePath);

                // Copy the new image to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.background_image_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing image");
                    }

                    if (!isAdded()
                        || mAlarmBackgroundImagePref == null
                        || mAlarmBlurIntensityPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
                        mAlarmBlurIntensityPref.setVisible(SdkUtils.isAtLeastAndroid12());
                    } else {
                        mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
                        updateBlurPreferenceVisibility();
                    }
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.namaz_alarm_display_settings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, null);

        addPreferencesFromResource(R.xml.settings_namaz_alarm_display);

        mAlarmClockStylePref = findPreference(KEY_NAMAZ_ALARM_CLOCK_STYLE);
        mAlarmClockDialPref = findPreference(KEY_NAMAZ_ALARM_CLOCK_DIAL);
        mAlarmClockDialMaterialPref = findPreference(KEY_NAMAZ_ALARM_CLOCK_DIAL_MATERIAL);
        mAnalogClockSizePref = findPreference(KEY_NAMAZ_ALARM_ANALOG_CLOCK_SIZE);
        mDisplaySecondsPref = findPreference(KEY_NAMAZ_DISPLAY_ALARM_SECOND_HAND);
        mAlarmClockSecondHandPref = findPreference(KEY_NAMAZ_ALARM_CLOCK_SECOND_HAND);
        mSwipeActionPref = findPreference(KEY_NAMAZ_SWIPE_ACTION);
        mDisplaySnoozeSelectorPref = findPreference(KEY_NAMAZ_DISPLAY_SNOOZE_SELECTOR);
        mBackgroundColorPref = findPreference(KEY_NAMAZ_ALARM_BACKGROUND_COLOR);
        mBackgroundAmoledColorPref = findPreference(KEY_NAMAZ_ALARM_BACKGROUND_AMOLED_COLOR);
        mAlarmClockColorPref = findPreference(KEY_NAMAZ_ALARM_CLOCK_COLOR);
        mAlarmSecondHandColorPref = findPreference(KEY_NAMAZ_ALARM_SECOND_HAND_COLOR);
        mAlarmTitleColorPref = findPreference(KEY_NAMAZ_ALARM_TITLE_COLOR);
        mSlideZoneColorPref = findPreference(KEY_NAMAZ_SLIDE_ZONE_COLOR);
        mAlarmButtonColorPref = findPreference(KEY_NAMAZ_ALARM_BUTTON_COLOR);
        mSnoozeTitleColorPref = findPreference(KEY_NAMAZ_SNOOZE_TITLE_COLOR);
        mSnoozeButtonColorPref = findPreference(KEY_NAMAZ_SNOOZE_BUTTON_COLOR);
        mDismissTitleColorPref = findPreference(KEY_NAMAZ_DISMISS_TITLE_COLOR);
        mDismissButtonColorPref = findPreference(KEY_NAMAZ_DISMISS_BUTTON_COLOR);
        mSnoozeZoneColorPref = findPreference(KEY_NAMAZ_SNOOZE_ZONE_COLOR);
        mSnoozeMinusButtonColorPref = findPreference(KEY_NAMAZ_SNOOZE_MINUS_BUTTON_COLOR);
        mSnoozePlusButtonColorPref = findPreference(KEY_NAMAZ_SNOOZE_PLUS_BUTTON_COLOR);
        mSnoozeSelectorTextColorPref = findPreference(KEY_NAMAZ_SNOOZE_SELECTOR_TEXT_COLOR);
        mSnoozeMinusSymbolColorPref = findPreference(KEY_NAMAZ_SNOOZE_MINUS_SYMBOL_COLOR);
        mSnoozePlusSymbolColorPref = findPreference(KEY_NAMAZ_SNOOZE_PLUS_SYMBOL_COLOR);
        mAlarmDigitalClockFontSizePref = findPreference(KEY_NAMAZ_ALARM_DIGITAL_CLOCK_FONT_SIZE);
        mAlarmTitleFontSizePref = findPreference(KEY_NAMAZ_ALARM_TITLE_FONT_SIZE_PREF);
        mDisplayTextShadowPref = findPreference(KEY_NAMAZ_ALARM_DISPLAY_TEXT_SHADOW);
        mShadowColorPref = findPreference(KEY_NAMAZ_ALARM_SHADOW_COLOR);
        mShadowOffsetPref = findPreference(KEY_NAMAZ_ALARM_SHADOW_OFFSET);
        mDisplayAlarmActionMessagePref = findPreference(KEY_NAMAZ_DISPLAY_ALARM_ACTION_MESSAGE);
        mDisplayAlarmTitleOnSingleLinePref = findPreference(KEY_NAMAZ_DISPLAY_ALARM_TITLE_ON_SINGLE_LINE);
        mDisplayRingtoneTitlePref = findPreference(KEY_NAMAZ_DISPLAY_RINGTONE_TITLE);
        mRingtoneTitleColorPref = findPreference(KEY_NAMAZ_RINGTONE_TITLE_COLOR);
        mAlarmFontPref = findPreference(KEY_NAMAZ_ALARM_FONT);
        mAlarmBackgroundImagePref = findPreference(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE);
        mAlarmBlurIntensityPref = findPreference(KEY_NAMAZ_ALARM_BLUR_INTENSITY);
        mEnablePerAlarmBackgroundImagePref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_BACKGROUND_IMAGE);
        mAlarmPreviewPref = findPreference(KEY_NAMAZ_ALARM_PREVIEW);

        mDisplayStyleOverridePref = findPreference(KEY_NAMAZ_ALARM_DISPLAY_STYLE_OVERRIDE);
        mColorsOverridePref = findPreference(KEY_NAMAZ_ALARM_COLORS_OVERRIDE);
        mFontOverridePref = findPreference(KEY_NAMAZ_ALARM_FONT_OVERRIDE);
        mBackgroundImageOverridePref = findPreference(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE_OVERRIDE);

        mStyleCategory = findPreference(KEY_NAMAZ_ALARM_STYLE_CATEGORY);
        mColorsCategory = findPreference(KEY_NAMAZ_ALARM_COLORS_CATEGORY);
        mFontCategory = findPreference(KEY_NAMAZ_ALARM_FONT_CATEGORY);
        mBackgroundImageCategory = findPreference(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE_CATEGORY);
        mMiscCategory = findPreference(KEY_NAMAZ_ALARM_MISCELLANEOUS_CATEGORY);

        mAlarmClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mAlarmClockStyleValues[0];
        mMaterialAnalogClock = mAlarmClockStyleValues[1];
        mDigitalClock = mAlarmClockStyleValues[2];

        if (savedInstanceState != null) {
            mPendingDialogPrefKey = savedInstanceState.getString(KEY_PENDING_DIALOG_PREF_KEY, null);
        }

        setupPreferences();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_PENDING_DIALOG_PREF_KEY, mPendingDialogPrefKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPendingDialogPrefKey != null && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            triggerDisableSettingDialog();
        }

        restoreCustomFileDialogIfNeeded(KEY_NAMAZ_ALARM_FONT, mAlarmFontPref, fontPickerLauncher, null);

        restoreCustomFileDialogIfNeeded(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE, mAlarmBackgroundImagePref, imagePickerLauncher, () ->
            mAlarmBlurIntensityPref.setVisible(false)
        );
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mAlarmClockStylePref, mAlarmClockDialPref, mAlarmClockDialMaterialPref, mAnalogClockSizePref,
            mAlarmClockSecondHandPref, mDisplaySecondsPref, mSwipeActionPref, mDisplaySnoozeSelectorPref, mBackgroundColorPref,
            mBackgroundAmoledColorPref, mAlarmClockColorPref, mAlarmSecondHandColorPref, mAlarmTitleColorPref, mSlideZoneColorPref, mAlarmButtonColorPref,
            mSnoozeTitleColorPref, mSnoozeButtonColorPref, mDismissTitleColorPref, mDismissButtonColorPref, mSnoozeZoneColorPref,
            mSnoozeMinusButtonColorPref, mSnoozePlusButtonColorPref, mSnoozeSelectorTextColorPref, mSnoozeMinusSymbolColorPref,
            mSnoozePlusSymbolColorPref, mAlarmDigitalClockFontSizePref, mAlarmTitleFontSizePref, mDisplayTextShadowPref, mShadowColorPref, mShadowOffsetPref,
            mDisplayAlarmActionMessagePref, mDisplayAlarmTitleOnSingleLinePref, mDisplayRingtoneTitlePref, mRingtoneTitleColorPref,
            mAlarmBackgroundImagePref, mAlarmBlurIntensityPref, mEnablePerAlarmBackgroundImagePref, mAlarmPreviewPref,
            mDisplayStyleOverridePref, mColorsOverridePref, mFontOverridePref, mBackgroundImageOverridePref, mAlarmFontPref
        );

        nullifyAllPrefs();

        mAlarmUpdateHandler = null;

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();

        switch (key) {
            case KEY_NAMAZ_ALARM_DISPLAY_STYLE_OVERRIDE, KEY_NAMAZ_ALARM_COLORS_OVERRIDE,
                 KEY_NAMAZ_ALARM_FONT_OVERRIDE, KEY_NAMAZ_ALARM_BACKGROUND_IMAGE_OVERRIDE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                updatePreferencesVisibility(key, newValue);
            }

            case KEY_NAMAZ_ALARM_CLOCK_STYLE -> {
                boolean isAnalogClock = newValue.equals(mAnalogClock);
                boolean isMaterialAnalogClock = newValue.equals(mMaterialAnalogClock);
                boolean isDigitalClock = newValue.equals(mDigitalClock);
                boolean isSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs, true);

                final int clockIndex = mAlarmClockStylePref.findIndexOfValue((String) newValue);
                mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntries()[clockIndex]);

                mAlarmClockDialPref.setVisible(isAnalogClock);
                mAlarmClockDialMaterialPref.setVisible(isMaterialAnalogClock);
                mAlarmClockColorPref.setVisible(!isMaterialAnalogClock);
                mAnalogClockSizePref.setVisible(!isDigitalClock);
                mAlarmDigitalClockFontSizePref.setVisible(isDigitalClock);
                mDisplaySecondsPref.setVisible(!isDigitalClock);
                mAlarmClockSecondHandPref.setVisible(isAnalogClock && isSecondHandDisplayed);
                mAlarmSecondHandColorPref.setVisible(isAnalogClock && isSecondHandDisplayed);
            }

            case KEY_NAMAZ_ALARM_CLOCK_DIAL, KEY_NAMAZ_ALARM_CLOCK_DIAL_MATERIAL, KEY_NAMAZ_ALARM_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_NAMAZ_DISPLAY_ALARM_SECOND_HAND -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isSecondHandDisplayed = (boolean) newValue;
                ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(mPrefs, true);

                mAlarmClockSecondHandPref.setVisible(isSecondHandDisplayed && alarmClockStyle == ClockStyle.ANALOG);
                mAlarmSecondHandColorPref.setVisible(isSecondHandDisplayed && alarmClockStyle != ClockStyle.ANALOG_MATERIAL);
            }

            case KEY_NAMAZ_SWIPE_ACTION -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isSwipeActionEnabled = (boolean) newValue;

                mSlideZoneColorPref.setVisible(isSwipeActionEnabled);
                mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);
                mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
                mDismissTitleColorPref.setVisible(isSwipeActionEnabled);
                mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
                mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);
            }

            case KEY_NAMAZ_DISPLAY_SNOOZE_SELECTOR -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isSnoozeSelectorDisplayed = (boolean) newValue;

                mSnoozeZoneColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozeMinusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozePlusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozeSelectorTextColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozeMinusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);
                mSnoozePlusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);
            }

            case KEY_NAMAZ_ALARM_DISPLAY_TEXT_SHADOW -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isTextShadowDisplayed = (boolean) newValue;
                mShadowColorPref.setVisible(isTextShadowDisplayed);
                mShadowOffsetPref.setVisible(isTextShadowDisplayed);
            }

            case KEY_NAMAZ_DISPLAY_RINGTONE_TITLE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                mRingtoneTitleColorPref.setVisible((boolean) newValue);
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_BACKGROUND_IMAGE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);

                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.blurIntensity = SettingsDAO.getAlarmBlurIntensity(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog();
                    return false;
                }
            }

            case KEY_NAMAZ_DISPLAY_ALARM_ACTION_MESSAGE, KEY_NAMAZ_DISPLAY_ALARM_TITLE_ON_SINGLE_LINE ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        switch (pref.getKey()) {
            case KEY_NAMAZ_ALARM_BACKGROUND_IMAGE -> selectCustomFile(mAlarmBackgroundImagePref, imagePickerLauncher,
                SettingsDAO.getAlarmBackgroundImage(mPrefs, true), KEY_NAMAZ_ALARM_BACKGROUND_IMAGE, false, () -> {
                // Actions to perform when deleting a background image

                if (!SettingsDAO.isPerAlarmBackgroundImageEnable(mPrefs, true)) {
                    mAlarmBlurIntensityPref.setVisible(false);

                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);

                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                if (!TextUtils.isEmpty(alarm.backgroundImage)
                                    && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                                    FileUtils.clearFile(alarm.backgroundImage);
                                }
                                alarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    updateBlurPreferenceVisibility();
                }
            });

            case KEY_NAMAZ_ALARM_FONT -> selectCustomFile(mAlarmFontPref, fontPickerLauncher, SettingsDAO.getAlarmFont(mPrefs, true), KEY_NAMAZ_ALARM_FONT,
                true, null);

            case KEY_NAMAZ_ALARM_PREVIEW -> {
                Intent previewIntent = new Intent(context, AlarmDisplayPreviewActivity.class);
                Calendar now = Calendar.getInstance();

                previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_HOUR, now.get(Calendar.HOUR_OF_DAY));
                previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_MINUTE, now.get(Calendar.MINUTE));
                previewIntent.putExtra("is_namaz", true);
                previewIntent.putExtra("force_override", true);

                ThemeUtils.startActivityWithTransition(context, previewIntent);
            }
        }

        return true;
    }

    private void setupPreferences() {
        final boolean isAnalogClock = mAlarmClockStylePref.getValue().equals(mAnalogClock);
        final boolean isMaterialAnalogClock = mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock);
        final boolean isDigitalClock = mAlarmClockStylePref.getValue().equals(mDigitalClock);
        final boolean isSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs, true);
        final boolean isSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(mPrefs, true);
        final boolean isSnoozeSelectorDisplayed = SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true);
        final boolean isTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(mPrefs, true);

        mAlarmClockStylePref.setSummary(mAlarmClockStylePref.getEntry());
        mAlarmClockStylePref.setOnPreferenceChangeListener(this);

        mAlarmClockDialPref.setVisible(isAnalogClock);
        mAlarmClockDialPref.setSummary(mAlarmClockDialPref.getEntry());
        mAlarmClockDialPref.setOnPreferenceChangeListener(this);

        mAlarmClockDialMaterialPref.setVisible(isMaterialAnalogClock);
        mAlarmClockDialMaterialPref.setSummary(mAlarmClockDialMaterialPref.getEntry());
        mAlarmClockDialMaterialPref.setOnPreferenceChangeListener(this);

        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE);
        mBackgroundAmoledColorPref.setVisible(isAmoledMode);

        mBackgroundColorPref.setVisible(!isAmoledMode);

        mAlarmClockColorPref.setVisible(!isMaterialAnalogClock);

        mAnalogClockSizePref.setVisible(!isDigitalClock);

        mDisplaySecondsPref.setVisible(!isDigitalClock);
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mAlarmClockSecondHandPref.setVisible(isAnalogClock && isSecondHandDisplayed);
        mAlarmClockSecondHandPref.setSummary(mAlarmClockSecondHandPref.getEntry());
        mAlarmClockSecondHandPref.setOnPreferenceChangeListener(this);

        mSwipeActionPref.setOnPreferenceChangeListener(this);

        mDisplaySnoozeSelectorPref.setOnPreferenceChangeListener(this);

        int color = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK);
        mAlarmSecondHandColorPref.setVisible(isAnalogClock && isSecondHandDisplayed);
        mAlarmSecondHandColorPref.setDefaultValue(color);

        mSlideZoneColorPref.setVisible(isSwipeActionEnabled);

        mAlarmButtonColorPref.setVisible(isSwipeActionEnabled);
        mAlarmButtonColorPref.setDefaultValue(color);

        mSnoozeTitleColorPref.setVisible(isSwipeActionEnabled);

        mSnoozeButtonColorPref.setVisible(!isSwipeActionEnabled);
        mSnoozeButtonColorPref.setDefaultValue(color);

        mDismissTitleColorPref.setVisible(isSwipeActionEnabled);

        mDismissButtonColorPref.setVisible(!isSwipeActionEnabled);
        mDismissButtonColorPref.setDefaultValue(color);

        mSnoozeZoneColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozeMinusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozePlusButtonColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozeSelectorTextColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozeMinusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);

        mSnoozePlusSymbolColorPref.setVisible(isSnoozeSelectorDisplayed);

        mAlarmDigitalClockFontSizePref.setVisible(isDigitalClock);

        mAlarmTitleFontSizePref.setVisible(true);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShadowColorPref.setVisible(isTextShadowDisplayed);

        mShadowOffsetPref.setVisible(isTextShadowDisplayed);

        mDisplayAlarmActionMessagePref.setOnPreferenceChangeListener(this);

        mDisplayAlarmTitleOnSingleLinePref.setOnPreferenceChangeListener(this);

        mDisplayRingtoneTitlePref.setOnPreferenceChangeListener(this);

        mRingtoneTitleColorPref.setVisible(SettingsDAO.isRingtoneTitleDisplayed(mPrefs, true));

        if (SettingsDAO.getAlarmBackgroundImage(mPrefs, true) == null) {
            mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title));
        } else {
            mAlarmBackgroundImagePref.setTitle(getString(R.string.background_image_title_variant));
        }

        mAlarmBackgroundImagePref.setOnPreferenceClickListener(this);

        if (mAlarmFontPref != null) {
            mAlarmFontPref.setTitle(getString(SettingsDAO.getAlarmFont(mPrefs, true) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
            mAlarmFontPref.setOnPreferenceClickListener(this);
        }

        mColorsOverridePref.setOnPreferenceChangeListener(this);
        mFontOverridePref.setOnPreferenceChangeListener(this);
        mBackgroundImageOverridePref.setOnPreferenceChangeListener(this);
        mDisplayStyleOverridePref.setOnPreferenceChangeListener(this);

        updateBlurPreferenceVisibility();

        mEnablePerAlarmBackgroundImagePref.setOnPreferenceChangeListener(this);

        mAlarmPreviewPref.setOnPreferenceClickListener(this);

        updatePreferencesVisibility(null, null);
    }

    private void updatePreferencesVisibility(@Nullable String changedKey, @Nullable Object newValue) {
        if (mStyleCategory == null || mDisplayStyleOverridePref == null || mColorsOverridePref == null || mFontOverridePref == null || mBackgroundImageOverridePref == null) {
            return;
        }

        boolean styleOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_DISPLAY_STYLE_OVERRIDE)) ? (boolean) newValue : mDisplayStyleOverridePref.isChecked();
        boolean colorsOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_COLORS_OVERRIDE)) ? (boolean) newValue : mColorsOverridePref.isChecked();
        boolean fontOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_FONT_OVERRIDE)) ? (boolean) newValue : mFontOverridePref.isChecked();
        boolean backgroundImageOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_BACKGROUND_IMAGE_OVERRIDE)) ? (boolean) newValue : mBackgroundImageOverridePref.isChecked();

        if (mStyleCategory != null) {
            mStyleCategory.setVisible(true);
            mAlarmClockStylePref.setVisible(styleOverride);
            mAlarmClockDialPref.setVisible(styleOverride && mAlarmClockStylePref.getValue().equals(mAnalogClock));
            mAlarmClockDialMaterialPref.setVisible(styleOverride && mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock));
            mAnalogClockSizePref.setVisible(styleOverride && !mAlarmClockStylePref.getValue().equals(mDigitalClock));
            mDisplaySecondsPref.setVisible(styleOverride && !mAlarmClockStylePref.getValue().equals(mDigitalClock));
            mAlarmClockSecondHandPref.setVisible(styleOverride && mDisplaySecondsPref.isChecked() && mAlarmClockStylePref.getValue().equals(mAnalogClock));
            mSwipeActionPref.setVisible(styleOverride);
            mDisplaySnoozeSelectorPref.setVisible(styleOverride);
        }

        if (mColorsCategory != null) {
            mColorsCategory.setVisible(true); // Category itself stays visible if user entered this screen
            mBackgroundColorPref.setVisible(colorsOverride);
            mBackgroundAmoledColorPref.setVisible(colorsOverride && ThemeUtils.isNight(getResources()) && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE));
            mAlarmClockColorPref.setVisible(colorsOverride && !mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock));
            mAlarmSecondHandColorPref.setVisible(colorsOverride && SettingsDAO.isAlarmSecondHandDisplayed(mPrefs, true) && !mAlarmClockStylePref.getValue().equals(mMaterialAnalogClock));
            mAlarmTitleColorPref.setVisible(colorsOverride);
            mSlideZoneColorPref.setVisible(colorsOverride && SettingsDAO.isSwipeActionEnabled(mPrefs, true));
            mAlarmButtonColorPref.setVisible(colorsOverride && SettingsDAO.isSwipeActionEnabled(mPrefs, true));
            mSnoozeTitleColorPref.setVisible(colorsOverride && SettingsDAO.isSwipeActionEnabled(mPrefs, true));
            mSnoozeButtonColorPref.setVisible(colorsOverride && !SettingsDAO.isSwipeActionEnabled(mPrefs, true));
            mDismissTitleColorPref.setVisible(colorsOverride && SettingsDAO.isSwipeActionEnabled(mPrefs, true));
            mDismissButtonColorPref.setVisible(colorsOverride && !SettingsDAO.isSwipeActionEnabled(mPrefs, true));
            mSnoozeZoneColorPref.setVisible(colorsOverride && SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true));
            mSnoozeMinusButtonColorPref.setVisible(colorsOverride && SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true));
            mSnoozePlusButtonColorPref.setVisible(colorsOverride && SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true));
            mSnoozeSelectorTextColorPref.setVisible(colorsOverride && SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true));
            mSnoozeMinusSymbolColorPref.setVisible(colorsOverride && SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true));
            mSnoozePlusSymbolColorPref.setVisible(colorsOverride && SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, true));
        }

        if (mFontCategory != null) {
            mFontCategory.setVisible(true);
            mAlarmFontPref.setVisible(fontOverride);
            mAlarmDigitalClockFontSizePref.setVisible(fontOverride && mAlarmClockStylePref.getValue().equals(mDigitalClock));
            mAlarmTitleFontSizePref.setVisible(fontOverride);
            mDisplayTextShadowPref.setVisible(fontOverride);
            mShadowColorPref.setVisible(fontOverride && SettingsDAO.isAlarmTextShadowDisplayed(mPrefs, true));
            mShadowOffsetPref.setVisible(fontOverride && SettingsDAO.isAlarmTextShadowDisplayed(mPrefs, true));
        }

        if (mBackgroundImageCategory != null) {
            mBackgroundImageCategory.setVisible(true);
            mAlarmBackgroundImagePref.setVisible(backgroundImageOverride);
            updateBlurPreferenceVisibility();
            mEnablePerAlarmBackgroundImagePref.setVisible(backgroundImageOverride);
        }

        if (mMiscCategory != null) {
            mMiscCategory.setVisible(true);
        }
    }

    private void updateCategoriesVisibility() {
        // Redundant now, integrated into updatePreferencesVisibility
    }

    private void triggerDisableSettingDialog() {
        final Context appContext = requireContext().getApplicationContext();

        AppExecutors.getDiskIO().execute(() -> {
            final CustomizationState state = getAlarmCustomizationsState(appContext);

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || isDetached()) {
                    return;
                }

                if (state.hasAny()) {
                    mPendingDialogPrefKey = KEY_NAMAZ_ENABLE_PER_ALARM_BACKGROUND_IMAGE;
                    showDisablePerAlarmSettingDialog(state);
                } else {
                    mPrefs.edit().putBoolean(KEY_NAMAZ_ENABLE_PER_ALARM_BACKGROUND_IMAGE, false).apply();
                    mEnablePerAlarmBackgroundImagePref.setChecked(false);
                }
            });
        });
    }

    private void showDisablePerAlarmSettingDialog(CustomizationState state) {
        String confirmAction = getString(R.string.confirm_action_prompt);
        String dialogMessage;

        if (state.hasSpecificImages) {
            String blurIntensityMessage = getString(R.string.blur_intensity_dialog_message_1);
            dialogMessage = getString(R.string.enable_per_alarm_background_image_dialog_message, blurIntensityMessage, confirmAction);
        } else {
            dialogMessage = getString(R.string.blur_intensity_dialog_message_2, confirmAction);
        }

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error),
            getString(R.string.warning),
            dialogMessage,
            null,
            getString(android.R.string.ok),
            (d, w) -> {
                final Context appContext = requireContext().getApplicationContext();

                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(appContext.getContentResolver(), null);

                    for (Alarm alarm : currentAlarms) {
                        if (alarm.relationalRuleId != -1) {
                            // Delete only specific background images
                            if (!TextUtils.isEmpty(alarm.backgroundImage) && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                                FileUtils.clearFile(alarm.backgroundImage);
                            }

                            alarm.backgroundImage = DEFAULT_SPECIFIC_ALARM_BACKGROUND_IMAGE;
                            alarm.blurIntensity = SettingsDAO.getAlarmBlurIntensity(mPrefs, true);
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                        }
                    }

                    AppExecutors.getMainThread().post(() -> {
                        CustomToast.show(appContext, R.string.background_image_toast_message_deleted);
                        updateBlurPreferenceVisibility();
                    });
                });

                mPrefs.edit().putBoolean(KEY_NAMAZ_ENABLE_PER_ALARM_BACKGROUND_IMAGE, false).apply();
                mEnablePerAlarmBackgroundImagePref.setChecked(false);
            },
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingDialogPrefKey = null)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void updateBlurPreferenceVisibility() {
        if (SdkUtils.isBeforeAndroid12()) {
            mAlarmBlurIntensityPref.setVisible(false);
            return;
        }

        String globalImagePath = SettingsDAO.getAlarmBackgroundImage(mPrefs, true);

        if (!TextUtils.isEmpty(globalImagePath)) {
            mAlarmBlurIntensityPref.setVisible(true);
            return;
        }

        final Context appContext = requireContext().getApplicationContext();

        AppExecutors.getDiskIO().execute(() -> {
            final CustomizationState state = getAlarmCustomizationsState(appContext);
            final boolean finalHasSpecificImages = state.hasSpecificImages;

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || isDetached() || mAlarmBlurIntensityPref == null) {
                    return;
                }

                mAlarmBlurIntensityPref.setVisible(finalHasSpecificImages);
            });
        });
    }

    private CustomizationState getAlarmCustomizationsState(Context context) {
        CustomizationState state = new CustomizationState();

        try {
            List<Alarm> currentAlarms = Alarm.getAlarms(context.getContentResolver(), null);
            int globalBlur = SettingsDAO.getAlarmBlurIntensity(mPrefs, true);

            for (Alarm alarm : currentAlarms) {
                if (alarm.relationalRuleId != -1) {
                    if (!state.hasSpecificImages
                        && !TextUtils.isEmpty(alarm.backgroundImage)
                        && alarm.backgroundImage.contains(FILE_SPECIFIC_ALARM_BACKGROUND)) {
                        state.hasSpecificImages = true;
                    }

                    if (!state.hasSpecificBlur && alarm.blurIntensity != globalBlur) {
                        state.hasSpecificBlur = true;
                    }

                    if (state.hasSpecificImages && state.hasSpecificBlur) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Error checking for specific alarm customizations", e);
        }

        return state;
    }

    private void nullifyAllPrefs() {
        mAlarmClockStylePref = null;
        mAlarmClockDialPref = null;
        mAlarmClockDialMaterialPref = null;
        mAnalogClockSizePref = null;
        mAlarmClockSecondHandPref = null;
        mDisplaySecondsPref = null;
        mSwipeActionPref = null;
        mDisplaySnoozeSelectorPref = null;
        mBackgroundColorPref = null;
        mBackgroundAmoledColorPref = null;
        mAlarmClockColorPref = null;
        mAlarmSecondHandColorPref = null;
        mAlarmTitleColorPref = null;
        mSlideZoneColorPref = null;
        mAlarmButtonColorPref = null;
        mSnoozeTitleColorPref = null;
        mSnoozeButtonColorPref = null;
        mDismissTitleColorPref = null;
        mDismissButtonColorPref = null;
        mSnoozeZoneColorPref = null;
        mSnoozeMinusButtonColorPref = null;
        mSnoozePlusButtonColorPref = null;
        mSnoozeSelectorTextColorPref = null;
        mSnoozeMinusSymbolColorPref = null;
        mSnoozePlusSymbolColorPref = null;
        mAlarmDigitalClockFontSizePref = null;
        mAlarmTitleFontSizePref = null;
        mDisplayTextShadowPref = null;
        mShadowColorPref = null;
        mShadowOffsetPref = null;
        mDisplayAlarmActionMessagePref = null;
        mDisplayAlarmTitleOnSingleLinePref = null;
        mDisplayRingtoneTitlePref = null;
        mRingtoneTitleColorPref = null;
        mAlarmBackgroundImagePref = null;
        mAlarmBlurIntensityPref = null;
        mEnablePerAlarmBackgroundImagePref = null;
        mAlarmPreviewPref = null;

        mColorsOverridePref = null;
        mFontOverridePref = null;
        mBackgroundImageOverridePref = null;
        mDisplayStyleOverridePref = null;
        mAlarmFontPref = null;

        mStyleCategory = null;
        mColorsCategory = null;
        mFontCategory = null;
        mBackgroundImageCategory = null;
        mMiscCategory = null;

        mAlarmClockStyleValues = null;
        mAnalogClock = null;
        mMaterialAnalogClock = null;
        mDigitalClock = null;
    }

    private static class CustomizationState {
        boolean hasSpecificImages = false;
        boolean hasSpecificBlur = false;

        boolean hasAny() {
            return hasSpecificImages || hasSpecificBlur;
        }
    }
}
