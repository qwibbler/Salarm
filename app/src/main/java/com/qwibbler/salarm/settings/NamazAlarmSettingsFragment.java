// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.settings;

import static android.app.Activity.RESULT_OK;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_VIBRATION_START_DELAY;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.IntentCompat;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.alarms.AlarmUpdateHandler;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.dialogfragment.AlarmNotificationReminderDialogFragment;
import com.qwibbler.salarm.dialogfragment.AlarmSnoozeDurationDialogFragment;
import com.qwibbler.salarm.dialogfragment.AutoSilenceDurationDialogFragment;
import com.qwibbler.salarm.dialogfragment.VibrationPatternDialogFragment;
import com.qwibbler.salarm.dialogfragment.VibrationStartDelayDialogFragment;
import com.qwibbler.salarm.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.ringtone.RingtonePickerActivity;
import com.qwibbler.salarm.settings.custompreference.AlarmNotificationReminderPreference;
import com.qwibbler.salarm.settings.custompreference.AlarmSnoozeDurationPreference;
import com.qwibbler.salarm.settings.custompreference.AlarmVolumePreference;
import com.qwibbler.salarm.settings.custompreference.AutoSilenceDurationPreference;
import com.qwibbler.salarm.settings.custompreference.CustomSliderPreference;
import com.qwibbler.salarm.settings.custompreference.VibrationPatternPreference;
import com.qwibbler.salarm.settings.custompreference.VibrationStartDelayPreference;
import com.qwibbler.salarm.settings.custompreference.VolumeCrescendoDurationPreference;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.uicomponents.toast.CustomToast;
import com.qwibbler.salarm.utils.AlarmUtils;
import com.qwibbler.salarm.utils.DeviceUtils;
import com.qwibbler.salarm.utils.FileUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.RingtoneUtils;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;

import java.util.Calendar;
import java.util.List;

public class NamazAlarmSettingsFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_PENDING_DIALOG_PREF_KEY = "pending_dialog_pref_key";

    private String mPendingDialogPrefKey = null;

    private AudioManager mAudioManager;
    private AudioDeviceCallback mAudioDeviceCallback;
    private AlarmUpdateHandler mAlarmUpdateHandler;

    private boolean mHasExternalAudioDeviceConnected;

    SwitchPreferenceCompat mAlarmShowRoundingPref;

    SwitchPreferenceCompat mStyleOverridePref;
    SwitchPreferenceCompat mRingtoneOverridePref;
    SwitchPreferenceCompat mAdvancedAudioOverridePref;
    SwitchPreferenceCompat mVibrationOverridePref;
    SwitchPreferenceCompat mActionsOverridePref;
    SwitchPreferenceCompat mMiscOverridePref;

    Preference mAlarmDisplayCustomizationPref;
    Preference mAlarmRingtonePref;
    AutoSilenceDurationPreference mAlarmAutoSilencePref;
    SwitchPreferenceCompat mEnablePerAlarmAutoSilencePref;
    AlarmSnoozeDurationPreference mAlarmSnoozeDurationPref;
    SwitchPreferenceCompat mEnablePerAlarmSnoozeDurationPref;
    ListPreference mRepeatMissedAlarmPref;
    SwitchPreferenceCompat mEnablePerAlarmMissedRepeatLimitPref;
    AlarmVolumePreference mAlarmVolumePref;
    SwitchPreferenceCompat mEnablePerAlarmVolumePref;
    VolumeCrescendoDurationPreference mAlarmVolumeCrescendoDurationPref;
    SwitchPreferenceCompat mEnablePerAlarmVolumeCrescendoDurationPref;
    SwitchPreferenceCompat mAdvancedAudioPlaybackPref;
    SwitchPreferenceCompat mAutoRoutingToExternalAudioDevicePref;
    SwitchPreferenceCompat mSystemMediaVolumePref;
    CustomSliderPreference mExternalAudioDeviceVolumePref;
    SwitchPreferenceCompat mEnableAlarmVibrationsByDefaultPref;
    VibrationPatternPreference mVibrationPatternPref;
    SwitchPreferenceCompat mEnablePerAlarmVibrationPatternPref;
    VibrationStartDelayPreference mVibrationStartDelayPref;
    SwitchPreferenceCompat mEnableSnoozedOrDismissedAlarmVibrationsPref;
    ListPreference mVolumeButtonsPref;
    ListPreference mPowerButtonPref;
    ListPreference mHeadphonesButtonPref;
    ListPreference mFlipActionPref;
    ListPreference mShakeActionPref;
    CustomSliderPreference mShakeIntensityPref;
    SwitchPreferenceCompat mDisplayDismissButtonPref;
    AlarmNotificationReminderPreference mAlarmNotificationReminderPref;
    SwitchPreferenceCompat mTurnOnBackFlashForTriggeredAlarmPref;
    SwitchPreferenceCompat mDisplayLowAlarmVolumeWarningPref;
    Preference mAlarmPreviewPref;

    private final ActivityResultLauncher<Intent> ringtonePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                return;
            }

            Uri uri = IntentCompat.getParcelableExtra(result.getData(), android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
            if (uri != null) {
                SettingsDAO.setNamazAlarmRingtoneUriFromSettings(mPrefs, uri);
                updateRingtonePreferences();
            }
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.namaz_alarms_settings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAudioManager = requireContext().getApplicationContext().getSystemService(AudioManager.class);
        mHasExternalAudioDeviceConnected = RingtoneUtils.hasExternalAudioDeviceConnected(requireContext(), mPrefs);
        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, null);

        addPreferencesFromResource(R.xml.settings_namaz_alarms);

        mAlarmShowRoundingPref = findPreference(KEY_NAMAZ_ALARM_SHOW_ROUNDING);

        mStyleOverridePref = findPreference(KEY_NAMAZ_ALARM_STYLE_OVERRIDE);
        mRingtoneOverridePref = findPreference(KEY_NAMAZ_ALARM_RINGTONE_OVERRIDE);
        mAdvancedAudioOverridePref = findPreference(KEY_NAMAZ_ALARM_ADVANCED_AUDIO_OVERRIDE);
        mVibrationOverridePref = findPreference(KEY_NAMAZ_ALARM_VIBRATION_OVERRIDE);
        mActionsOverridePref = findPreference(KEY_NAMAZ_ALARM_ACTIONS_OVERRIDE);
        mMiscOverridePref = findPreference(KEY_NAMAZ_ALARM_MISC_OVERRIDE);

        mAlarmDisplayCustomizationPref = findPreference(KEY_NAMAZ_ALARM_DISPLAY_CUSTOMIZATION);
        mAlarmRingtonePref = findPreference(KEY_NAMAZ_DEFAULT_ALARM_RINGTONE);
        mAlarmAutoSilencePref = findPreference(KEY_NAMAZ_AUTO_SILENCE_DURATION);
        mEnablePerAlarmAutoSilencePref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_AUTO_SILENCE);
        mAlarmSnoozeDurationPref = findPreference(KEY_NAMAZ_ALARM_SNOOZE_DURATION);
        mEnablePerAlarmSnoozeDurationPref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_SNOOZE_DURATION);
        mRepeatMissedAlarmPref = findPreference(KEY_NAMAZ_REPEAT_MISSED_ALARM);
        mEnablePerAlarmMissedRepeatLimitPref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT);
        mAlarmVolumePref = findPreference(KEY_NAMAZ_ALARM_VOLUME_SETTING);
        mEnablePerAlarmVolumePref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME);
        mAlarmVolumeCrescendoDurationPref = findPreference(KEY_NAMAZ_ALARM_VOLUME_CRESCENDO_DURATION);
        mEnablePerAlarmVolumeCrescendoDurationPref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION);
        mAdvancedAudioPlaybackPref = findPreference(KEY_NAMAZ_ADVANCED_AUDIO_PLAYBACK);
        mAutoRoutingToExternalAudioDevicePref = findPreference(KEY_NAMAZ_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE);
        mSystemMediaVolumePref = findPreference(KEY_NAMAZ_SYSTEM_MEDIA_VOLUME);
        mExternalAudioDeviceVolumePref = findPreference(KEY_NAMAZ_EXTERNAL_AUDIO_DEVICE_VOLUME);
        mEnableAlarmVibrationsByDefaultPref = findPreference(KEY_NAMAZ_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
        mVibrationPatternPref = findPreference(KEY_NAMAZ_VIBRATION_PATTERN);
        mEnablePerAlarmVibrationPatternPref = findPreference(KEY_NAMAZ_ENABLE_PER_ALARM_VIBRATION_PATTERN);
        mVibrationStartDelayPref = findPreference(KEY_NAMAZ_VIBRATION_START_DELAY);
        mEnableSnoozedOrDismissedAlarmVibrationsPref = findPreference(KEY_NAMAZ_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS);
        mVolumeButtonsPref = findPreference(KEY_NAMAZ_VOLUME_BUTTONS);
        mPowerButtonPref = findPreference(KEY_NAMAZ_POWER_BUTTON);
        mHeadphonesButtonPref = findPreference(KEY_NAMAZ_HEADPHONES_BUTTON);
        mFlipActionPref = findPreference(KEY_NAMAZ_FLIP_ACTION);
        mShakeActionPref = findPreference(KEY_NAMAZ_SHAKE_ACTION);
        mShakeIntensityPref = findPreference(KEY_NAMAZ_SHAKE_INTENSITY);
        mDisplayDismissButtonPref = findPreference(KEY_NAMAZ_DISPLAY_DISMISS_BUTTON);
        mAlarmNotificationReminderPref = findPreference(KEY_NAMAZ_ALARM_NOTIFICATION_REMINDER);
        mTurnOnBackFlashForTriggeredAlarmPref = findPreference(KEY_NAMAZ_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM);
        mDisplayLowAlarmVolumeWarningPref = findPreference(KEY_NAMAZ_DISPLAY_LOW_ALARM_VOLUME_WARNING);
        mAlarmPreviewPref = findPreference(KEY_NAMAZ_ALARM_PREVIEW);

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFragmentResultListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPendingDialogPrefKey != null && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            triggerDisableSettingDialog(mPendingDialogPrefKey);
        }

        updateRingtonePreferences();

        if (mHasExternalAudioDeviceConnected) {
            mAlarmVolumePref.setTitle(R.string.disconnect_external_audio_device_title);
            mExternalAudioDeviceVolumePref.setTitle(R.string.external_audio_device_volume_title);
        } else {
            mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
            mExternalAudioDeviceVolumePref.setTitle(R.string.connect_external_audio_device_title);
        }

        if (mAudioDeviceCallback == null) {
            initAudioDeviceCallback();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        stopRingtonePreview();

        if (mAudioDeviceCallback != null) {
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
            mAudioDeviceCallback = null;
        }
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mAlarmShowRoundingPref, mStyleOverridePref, mRingtoneOverridePref, mAdvancedAudioOverridePref,
            mVibrationOverridePref, mActionsOverridePref, mMiscOverridePref, mAlarmDisplayCustomizationPref,
            mAlarmRingtonePref, mAlarmAutoSilencePref, mEnablePerAlarmAutoSilencePref, mAlarmSnoozeDurationPref,
            mEnablePerAlarmSnoozeDurationPref, mRepeatMissedAlarmPref, mEnablePerAlarmMissedRepeatLimitPref, mAlarmVolumePref,
            mEnablePerAlarmVolumePref, mAlarmVolumeCrescendoDurationPref, mEnablePerAlarmVolumeCrescendoDurationPref,
            mAdvancedAudioPlaybackPref, mAutoRoutingToExternalAudioDevicePref, mSystemMediaVolumePref, mExternalAudioDeviceVolumePref,
            mEnableAlarmVibrationsByDefaultPref, mVibrationPatternPref, mEnablePerAlarmVibrationPatternPref,
            mVibrationStartDelayPref, mEnableSnoozedOrDismissedAlarmVibrationsPref, mVolumeButtonsPref, mPowerButtonPref,
            mHeadphonesButtonPref, mFlipActionPref, mShakeActionPref, mShakeIntensityPref, mDisplayDismissButtonPref,
            mAlarmNotificationReminderPref, mTurnOnBackFlashForTriggeredAlarmPref, mDisplayLowAlarmVolumeWarningPref
        );

        mAudioManager = null;
        mAlarmUpdateHandler = null;

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();

        switch (key) {
            case KEY_NAMAZ_ALARM_SHOW_ROUNDING, KEY_NAMAZ_ALARM_STYLE_OVERRIDE,
                 KEY_NAMAZ_ALARM_VIBRATION_OVERRIDE, KEY_NAMAZ_ALARM_ACTIONS_OVERRIDE,
                 KEY_NAMAZ_ALARM_MISC_OVERRIDE, KEY_NAMAZ_DISPLAY_LOW_ALARM_VOLUME_WARNING,
                 KEY_NAMAZ_DISPLAY_DISMISS_BUTTON, KEY_NAMAZ_ENABLE_SNOOZED_OR_DISMISSED_ALARM_VIBRATIONS -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                updatePreferencesVisibility(key, newValue);
            }

            case KEY_NAMAZ_ALARM_RINGTONE_OVERRIDE, KEY_NAMAZ_ALARM_ADVANCED_AUDIO_OVERRIDE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                stopRingtonePreview();
                updatePreferencesVisibility(key, newValue);
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_AUTO_SILENCE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.autoSilenceDuration = SettingsDAO.getAlarmTimeout(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_PER_ALARM_AUTO_SILENCE);
                    return false;
                }
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_SNOOZE_DURATION -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.snoozeDuration = SettingsDAO.getSnoozeLength(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_PER_ALARM_SNOOZE_DURATION);
                    return false;
                }
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT);
                    return false;
                }
            }

            case KEY_NAMAZ_REPEAT_MISSED_ALARM -> {
                final int index = mRepeatMissedAlarmPref.findIndexOfValue((String) newValue);
                mRepeatMissedAlarmPref.setSummary(mRepeatMissedAlarmPref.getEntries()[index]);

                if (!SettingsDAO.isPerAlarmMissedRepeatLimitEnabled(mPrefs, true)) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.missedAlarmRepeatLimit = Integer.parseInt((String) newValue);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                }
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME -> {
                stopRingtonePreview();
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.alarmVolume = SettingsDAO.getAlarmVolume(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME);
                    return false;
                }
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.crescendoDuration = SettingsDAO.getAlarmVolumeCrescendoDuration(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION);
                    return false;
                }
            }

            case KEY_NAMAZ_ENABLE_PER_ALARM_VIBRATION_PATTERN -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.vibrationPattern = SettingsDAO.getVibrationPattern(mPrefs, true);
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_PER_ALARM_VIBRATION_PATTERN);
                    return false;
                }
            }

            case KEY_NAMAZ_ADVANCED_AUDIO_PLAYBACK -> {
                stopRingtonePreview();
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isAdvancedAudioPlaybackEnabled = (boolean) newValue;
                mAutoRoutingToExternalAudioDevicePref.setVisible(isAdvancedAudioPlaybackEnabled);
                mSystemMediaVolumePref.setVisible(isAdvancedAudioPlaybackEnabled && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, true));
                mExternalAudioDeviceVolumePref.setVisible(isAdvancedAudioPlaybackEnabled
                    && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, true)
                    && SettingsDAO.shouldUseCustomMediaVolume(mPrefs, true));
                mHeadphonesButtonPref.setVisible(isAdvancedAudioPlaybackEnabled
                    && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, true));
            }

            case KEY_NAMAZ_AUTO_ROUTING_TO_EXTERNAL_AUDIO_DEVICE -> {
                stopRingtonePreview();
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isAutoRoutingToExternalAudioDevice = (boolean) newValue;
                mSystemMediaVolumePref.setVisible(isAutoRoutingToExternalAudioDevice);
                mExternalAudioDeviceVolumePref.setVisible(isAutoRoutingToExternalAudioDevice
                    && SettingsDAO.shouldUseCustomMediaVolume(mPrefs, true));
                mHeadphonesButtonPref.setVisible(isAutoRoutingToExternalAudioDevice);
            }

            case KEY_NAMAZ_SYSTEM_MEDIA_VOLUME -> {
                stopRingtonePreview();
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mExternalAudioDeviceVolumePref.setVisible(!(boolean) newValue);
            }

            case KEY_NAMAZ_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if ((boolean) newValue) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.vibrate = true;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                } else {
                    triggerDisableSettingDialog(KEY_NAMAZ_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT);
                    return false;
                }
            }

            case KEY_NAMAZ_VOLUME_BUTTONS, KEY_NAMAZ_POWER_BUTTON, KEY_NAMAZ_HEADPHONES_BUTTON, KEY_NAMAZ_FLIP_ACTION -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_NAMAZ_SHAKE_ACTION -> {
                final int index = mShakeActionPref.findIndexOfValue((String) newValue);
                mShakeActionPref.setSummary(mShakeActionPref.getEntries()[index]);
                mShakeIntensityPref.setVisible(index != 2);
            }

            case KEY_NAMAZ_TURN_ON_BACK_FLASH_FOR_TRIGGERED_ALARM -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                    for (Alarm alarm : currentAlarms) {
                        if (alarm.relationalRuleId != -1) {
                            alarm.flash = (boolean) newValue;
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                        }
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        final Context context = getActivity();
        if (context == null) {
            return false;
        }

        String key = pref.getKey();

        if (key.equals(KEY_NAMAZ_ALARM_DISPLAY_CUSTOMIZATION)) {
            animateAndShowFragment(new NamazAlarmDisplayCustomizationFragment());
        } else if (key.equals(KEY_NAMAZ_DEFAULT_ALARM_RINGTONE)) {
            Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(context);
            intent.putExtra(RingtonePickerActivity.EXTRA_RETURN_RESULT_ONLY, true);
            ringtonePickerLauncher.launch(intent);
        } else if (key.equals(KEY_NAMAZ_ALARM_PREVIEW)) {
            Intent previewIntent = new Intent(context, AlarmDisplayPreviewActivity.class);
            Calendar now = Calendar.getInstance();

            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_HOUR, now.get(Calendar.HOUR_OF_DAY));
            previewIntent.putExtra(AlarmUtils.EXTRA_PREVIEW_MINUTE, now.get(Calendar.MINUTE));
            previewIntent.putExtra("is_namaz", true);
            previewIntent.putExtra("force_override", true);

            ThemeUtils.startActivityWithTransition(context, previewIntent);
        }

        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference pref) {
        String key = pref.getKey();
        if (pref instanceof AutoSilenceDurationPreference autoSilenceDurationPreference) {
            int currentValue = autoSilenceDurationPreference.getAutoSilenceDuration();
            AutoSilenceDurationDialogFragment dialogFragment = AutoSilenceDurationDialogFragment.newInstance(key, currentValue);
            AutoSilenceDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmSnoozeDurationPreference alarmSnoozeDurationPreference) {
            int currentValue = alarmSnoozeDurationPreference.getSnoozeDuration();
            AlarmSnoozeDurationDialogFragment dialogFragment = AlarmSnoozeDurationDialogFragment.newInstance(key, currentValue);
            AlarmSnoozeDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VolumeCrescendoDurationPreference volumeCrescendoDurationPreference) {
            int currentValue = volumeCrescendoDurationPreference.getVolumeCrescendoDuration();
            VolumeCrescendoDurationDialogFragment dialogFragment =
                VolumeCrescendoDurationDialogFragment.newInstance(key, currentValue);
            VolumeCrescendoDurationDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationPatternPreference vibrationPatternPreference) {
            String currentValue = vibrationPatternPreference.getPattern();
            VibrationPatternDialogFragment dialogFragment = VibrationPatternDialogFragment.newInstance(key, currentValue);
            VibrationPatternDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof VibrationStartDelayPreference vibrationStartDelayPreference) {
            int currentValue = vibrationStartDelayPreference.getVibrationStartDelay();
            VibrationStartDelayDialogFragment dialogFragment = VibrationStartDelayDialogFragment.newInstance(key, currentValue,
                currentValue == DEFAULT_VIBRATION_START_DELAY);
            VibrationStartDelayDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else if (pref instanceof AlarmNotificationReminderPreference alarmNotificationReminderPreference) {
            int currentValue = alarmNotificationReminderPreference.getAlarmNotificationReminderTime();
            AlarmNotificationReminderDialogFragment dialogFragment =
                AlarmNotificationReminderDialogFragment.newInstance(key, currentValue);
            AlarmNotificationReminderDialogFragment.show(getParentFragmentManager(), dialogFragment);
        } else {
            super.onDisplayPreferenceDialog(pref);
        }
    }

    private void setupPreferences() {
        mAlarmShowRoundingPref.setOnPreferenceChangeListener(this);

        mStyleOverridePref = findPreference(KEY_NAMAZ_ALARM_STYLE_OVERRIDE);
        mRingtoneOverridePref = findPreference(KEY_NAMAZ_ALARM_RINGTONE_OVERRIDE);
        mAdvancedAudioOverridePref = findPreference(KEY_NAMAZ_ALARM_ADVANCED_AUDIO_OVERRIDE);
        mVibrationOverridePref = findPreference(KEY_NAMAZ_ALARM_VIBRATION_OVERRIDE);
        mActionsOverridePref = findPreference(KEY_NAMAZ_ALARM_ACTIONS_OVERRIDE);
        mMiscOverridePref = findPreference(KEY_NAMAZ_ALARM_MISC_OVERRIDE);

        mStyleOverridePref.setOnPreferenceChangeListener(this);
        mRingtoneOverridePref.setOnPreferenceChangeListener(this);
        mAdvancedAudioOverridePref.setOnPreferenceChangeListener(this);
        mVibrationOverridePref.setOnPreferenceChangeListener(this);
        mActionsOverridePref.setOnPreferenceChangeListener(this);
        mMiscOverridePref.setOnPreferenceChangeListener(this);

        mAlarmDisplayCustomizationPref.setOnPreferenceClickListener(this);

        mAlarmRingtonePref.setOnPreferenceClickListener(this);

        mEnablePerAlarmAutoSilencePref.setOnPreferenceChangeListener(this);
        mEnablePerAlarmSnoozeDurationPref.setOnPreferenceChangeListener(this);

        updateMissedAlarmPrefsVisibility(SettingsDAO.getAlarmTimeout(mPrefs, true), SettingsDAO.getSnoozeLength(mPrefs, true), null);

        mRepeatMissedAlarmPref.setOnPreferenceChangeListener(this);
        mRepeatMissedAlarmPref.setSummary(mRepeatMissedAlarmPref.getEntry());

        mEnablePerAlarmMissedRepeatLimitPref.setOnPreferenceChangeListener(this);

        mEnablePerAlarmVolumePref.setOnPreferenceChangeListener(this);
        mEnablePerAlarmVolumeCrescendoDurationPref.setOnPreferenceChangeListener(this);

        mAdvancedAudioPlaybackPref.setOnPreferenceChangeListener(this);

        mAutoRoutingToExternalAudioDevicePref.setOnPreferenceChangeListener(this);

        mSystemMediaVolumePref.setOnPreferenceChangeListener(this);

        mEnableAlarmVibrationsByDefaultPref.setOnPreferenceChangeListener(this);
        mEnablePerAlarmVibrationPatternPref.setOnPreferenceChangeListener(this);
        mEnableSnoozedOrDismissedAlarmVibrationsPref.setOnPreferenceChangeListener(this);

        mVolumeButtonsPref.setOnPreferenceChangeListener(this);
        mVolumeButtonsPref.setSummary(mVolumeButtonsPref.getEntry());

        mPowerButtonPref.setOnPreferenceChangeListener(this);
        mPowerButtonPref.setSummary(mPowerButtonPref.getEntry());

        mHeadphonesButtonPref.setOnPreferenceChangeListener(this);
        mHeadphonesButtonPref.setSummary(mHeadphonesButtonPref.getEntry());

        SensorManager sensorManager = requireContext().getApplicationContext().getSystemService(SensorManager.class);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            mFlipActionPref.setValue("0");
            mShakeActionPref.setValue("0");
            mFlipActionPref.setVisible(false);
            mShakeActionPref.setVisible(false);
        } else {
            mFlipActionPref.setSummary(mFlipActionPref.getEntry());
            mFlipActionPref.setOnPreferenceChangeListener(this);
            mShakeActionPref.setSummary(mShakeActionPref.getEntry());
            mShakeActionPref.setOnPreferenceChangeListener(this);

            final int shakeActionIndex = mShakeActionPref.findIndexOfValue(String.valueOf(SettingsDAO.getShakeAction(mPrefs, true)));
            mShakeIntensityPref.setVisible(shakeActionIndex != 2);
        }

        mDisplayDismissButtonPref.setOnPreferenceChangeListener(this);
        mTurnOnBackFlashForTriggeredAlarmPref.setVisible(DeviceUtils.hasBackFlash(requireContext()));
        mTurnOnBackFlashForTriggeredAlarmPref.setOnPreferenceChangeListener(this);
        mDisplayLowAlarmVolumeWarningPref.setOnPreferenceChangeListener(this);

        updatePreferencesVisibility(null, null);
    }

    private void updatePreferencesVisibility(@Nullable String changedKey, @Nullable Object newValue) {
        if (mStyleOverridePref == null || mRingtoneOverridePref == null ||
            mAdvancedAudioOverridePref == null || mVibrationOverridePref == null ||
            mActionsOverridePref == null || mMiscOverridePref == null) {
            return;
        }

        boolean styleOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_STYLE_OVERRIDE)) ? (boolean) newValue : mStyleOverridePref.isChecked();
        boolean ringtoneOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_RINGTONE_OVERRIDE)) ? (boolean) newValue : mRingtoneOverridePref.isChecked();
        boolean advancedAudioOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_ADVANCED_AUDIO_OVERRIDE)) ? (boolean) newValue : mAdvancedAudioOverridePref.isChecked();
        boolean vibrationOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_VIBRATION_OVERRIDE)) ? (boolean) newValue : mVibrationOverridePref.isChecked();
        boolean actionsOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_ACTIONS_OVERRIDE)) ? (boolean) newValue : mActionsOverridePref.isChecked();
        boolean miscOverride = (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_MISC_OVERRIDE)) ? (boolean) newValue : mMiscOverridePref.isChecked();

        if (mAlarmDisplayCustomizationPref != null) mAlarmDisplayCustomizationPref.setVisible(styleOverride);

        if (mAlarmRingtonePref != null) mAlarmRingtonePref.setVisible(ringtoneOverride);
        if (mAlarmAutoSilencePref != null) mAlarmAutoSilencePref.setVisible(ringtoneOverride);
        if (mEnablePerAlarmAutoSilencePref != null) mEnablePerAlarmAutoSilencePref.setVisible(ringtoneOverride);
        if (mAlarmSnoozeDurationPref != null) mAlarmSnoozeDurationPref.setVisible(ringtoneOverride);
        if (mEnablePerAlarmSnoozeDurationPref != null) mEnablePerAlarmSnoozeDurationPref.setVisible(ringtoneOverride);
        if (mRepeatMissedAlarmPref != null) mRepeatMissedAlarmPref.setVisible(ringtoneOverride);
        if (mEnablePerAlarmMissedRepeatLimitPref != null) mEnablePerAlarmMissedRepeatLimitPref.setVisible(ringtoneOverride);
        if (mAlarmVolumePref != null) mAlarmVolumePref.setVisible(ringtoneOverride);
        if (mEnablePerAlarmVolumePref != null) mEnablePerAlarmVolumePref.setVisible(ringtoneOverride);
        if (mAlarmVolumeCrescendoDurationPref != null) mAlarmVolumeCrescendoDurationPref.setVisible(ringtoneOverride);
        if (mEnablePerAlarmVolumeCrescendoDurationPref != null) mEnablePerAlarmVolumeCrescendoDurationPref.setVisible(ringtoneOverride);

        if (mAdvancedAudioPlaybackPref != null) mAdvancedAudioPlaybackPref.setVisible(advancedAudioOverride);
        if (mAutoRoutingToExternalAudioDevicePref != null) mAutoRoutingToExternalAudioDevicePref.setVisible(advancedAudioOverride && SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs, true));
        if (mSystemMediaVolumePref != null) mSystemMediaVolumePref.setVisible(advancedAudioOverride && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, true));
        if (mExternalAudioDeviceVolumePref != null) mExternalAudioDeviceVolumePref.setVisible(advancedAudioOverride
            && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, true)
            && SettingsDAO.shouldUseCustomMediaVolume(mPrefs, true));

        if (mEnableAlarmVibrationsByDefaultPref != null) mEnableAlarmVibrationsByDefaultPref.setVisible(vibrationOverride);
        if (mVibrationPatternPref != null) mVibrationPatternPref.setVisible(vibrationOverride);
        if (mEnablePerAlarmVibrationPatternPref != null) mEnablePerAlarmVibrationPatternPref.setVisible(vibrationOverride);
        if (mVibrationStartDelayPref != null) mVibrationStartDelayPref.setVisible(vibrationOverride);
        if (mEnableSnoozedOrDismissedAlarmVibrationsPref != null) mEnableSnoozedOrDismissedAlarmVibrationsPref.setVisible(vibrationOverride);

        if (mVolumeButtonsPref != null) mVolumeButtonsPref.setVisible(actionsOverride);
        if (mPowerButtonPref != null) mPowerButtonPref.setVisible(actionsOverride);
        if (mHeadphonesButtonPref != null) mHeadphonesButtonPref.setVisible(actionsOverride && SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, true));
        if (mFlipActionPref != null) mFlipActionPref.setVisible(actionsOverride);
        if (mShakeActionPref != null) mShakeActionPref.setVisible(actionsOverride);
        if (mShakeIntensityPref != null) mShakeIntensityPref.setVisible(actionsOverride && SettingsDAO.getShakeAction(mPrefs, true) != 2);

        if (mDisplayDismissButtonPref != null) mDisplayDismissButtonPref.setVisible(miscOverride);
        if (mAlarmNotificationReminderPref != null) mAlarmNotificationReminderPref.setVisible(miscOverride);
        if (mTurnOnBackFlashForTriggeredAlarmPref != null) mTurnOnBackFlashForTriggeredAlarmPref.setVisible(miscOverride && DeviceUtils.hasBackFlash(requireContext()));
        if (mDisplayLowAlarmVolumeWarningPref != null) mDisplayLowAlarmVolumeWarningPref.setVisible(miscOverride);

        updateMissedAlarmPrefsVisibility(SettingsDAO.getAlarmTimeout(mPrefs, true), SettingsDAO.getSnoozeLength(mPrefs, true), (changedKey != null && changedKey.equals(KEY_NAMAZ_ALARM_RINGTONE_OVERRIDE)) ? newValue : null);
    }

    private void setupFragmentResultListeners() {
        FragmentManager parentFragmentManager = getParentFragmentManager();
        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();

        parentFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AutoSilenceDurationDialogFragment.RESULT_PREF_KEY);
                if (key == null || !key.startsWith("key_namaz_")) return;
                int newValue = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);

                mAlarmAutoSilencePref.setAutoSilenceDuration(newValue);
                updateMissedAlarmPrefsVisibility(newValue, SettingsDAO.getSnoozeLength(mPrefs, true), null);

                if (!SettingsDAO.isPerAlarmAutoSilenceEnabled(mPrefs, true)) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.autoSilenceDuration = newValue;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                }
            });

        parentFragmentManager.setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AlarmSnoozeDurationDialogFragment.RESULT_PREF_KEY);
                if (key == null || !key.startsWith("key_namaz_")) return;
                int newValue = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);

                mAlarmSnoozeDurationPref.setSnoozeDuration(newValue);
                updateMissedAlarmPrefsVisibility(SettingsDAO.getAlarmTimeout(mPrefs, true), newValue, null);

                if (!SettingsDAO.isPerAlarmSnoozeDurationEnabled(mPrefs, true)) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.snoozeDuration = newValue;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                }
            });

        parentFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VolumeCrescendoDurationDialogFragment.RESULT_PREF_KEY);
                if (key == null || !key.startsWith("key_namaz_")) return;
                int newValue = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);

                mAlarmVolumeCrescendoDurationPref.setVolumeCrescendoDuration(newValue);

                if (!SettingsDAO.isPerAlarmCrescendoDurationEnabled(mPrefs, true)) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.crescendoDuration = newValue;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                }
            });

        parentFragmentManager.setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VibrationPatternDialogFragment.RESULT_PREF_KEY);
                if (key == null || !key.startsWith("key_namaz_")) return;
                String newValue = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);

                mVibrationPatternPref.setPattern(newValue);

                if (!SettingsDAO.isPerAlarmVibrationPatternEnabled(mPrefs, true)) {
                    AppExecutors.getDiskIO().execute(() -> {
                        List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                        for (Alarm alarm : currentAlarms) {
                            if (alarm.relationalRuleId != -1) {
                                alarm.vibrationPattern = newValue;
                                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                            }
                        }
                    });
                }
            });

        parentFragmentManager.setFragmentResultListener(VibrationStartDelayDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(VibrationStartDelayDialogFragment.RESULT_PREF_KEY);
                if (key == null || !key.startsWith("key_namaz_")) return;
                int newValue = bundle.getInt(VibrationStartDelayDialogFragment.VIBRATION_DELAY_VALUE);
                mVibrationStartDelayPref.setVibrationStartDelay(newValue);
            });

        parentFragmentManager.setFragmentResultListener(AlarmNotificationReminderDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                String key = bundle.getString(AlarmNotificationReminderDialogFragment.RESULT_PREF_KEY);
                if (key == null || !key.startsWith("key_namaz_")) return;
                int newValue = bundle.getInt(AlarmNotificationReminderDialogFragment.ALARM_NOTIFICATION_REMINDER_VALUE);

                mAlarmNotificationReminderPref.setAlarmNotificationReminderTime(newValue);

                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                    for (Alarm alarm : currentAlarms) {
                        if (alarm.relationalRuleId != -1 && alarm.enabled) {
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
                        }
                    }
                });
            });
    }

    private void updateMissedAlarmPrefsVisibility(int autoSilenceDuration, int snoozeDuration, @Nullable Object newValue) {
        boolean ringtoneOverride = (newValue != null) ? (boolean) newValue : (mRingtoneOverridePref != null && mRingtoneOverridePref.isChecked());
        boolean isVisible = ringtoneOverride && autoSilenceDuration != TIMEOUT_NEVER && snoozeDuration != ALARM_SNOOZE_DURATION_DISABLED;
        if (mEnablePerAlarmMissedRepeatLimitPref != null) mEnablePerAlarmMissedRepeatLimitPref.setVisible(isVisible);
        if (mRepeatMissedAlarmPref != null) mRepeatMissedAlarmPref.setVisible(isVisible);
    }

    private void triggerDisableSettingDialog(String prefKey) {
        AppExecutors.getDiskIO().execute(() -> {
            boolean hasNamazAlarms = false;
            try (Cursor cursor = requireContext().getContentResolver().query(
                Alarm.CONTENT_URI, new String[]{Alarm._ID}, Alarm.RELATIONAL_RULE_ID + " != -1", null, null)) {
                hasNamazAlarms = cursor != null && cursor.getCount() > 0;
            } catch (Exception e) {
                LogUtils.e("Error checking for namaz alarms", e);
            }

            final boolean finalHasAlarms = hasNamazAlarms;
            AppExecutors.getMainThread().post(() -> {
                if (!isAdded() || isDetached()) return;
                if (finalHasAlarms) {
                    mPendingDialogPrefKey = prefKey;
                    switch (prefKey) {
                        case KEY_NAMAZ_ENABLE_PER_ALARM_AUTO_SILENCE ->
                            showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_auto_silence_dialog_message,
                                KEY_NAMAZ_ENABLE_PER_ALARM_AUTO_SILENCE, mEnablePerAlarmAutoSilencePref,
                                mAlarmAutoSilencePref, alarm -> alarm.autoSilenceDuration = SettingsDAO.getAlarmTimeout(mPrefs, true));
                        case KEY_NAMAZ_ENABLE_PER_ALARM_SNOOZE_DURATION ->
                            showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_snooze_duration_dialog_message,
                                KEY_NAMAZ_ENABLE_PER_ALARM_SNOOZE_DURATION, mEnablePerAlarmSnoozeDurationPref, mAlarmSnoozeDurationPref,
                                alarm -> alarm.snoozeDuration = SettingsDAO.getSnoozeLength(mPrefs, true));
                        case KEY_NAMAZ_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT ->
                            showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_missed_repeat_limit_dialog_message,
                                KEY_NAMAZ_ENABLE_PER_ALARM_MISSED_REPEAT_LIMIT, mEnablePerAlarmMissedRepeatLimitPref, mRepeatMissedAlarmPref,
                                alarm -> alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(mPrefs, true));
                        case KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME ->
                            showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_volume_dialog_message,
                                KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME, mEnablePerAlarmVolumePref, mAlarmVolumePref,
                                alarm -> alarm.alarmVolume = DEFAULT_ALARM_VOLUME);
                        case KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION ->
                            showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_crescendo_duration_dialog_message,
                                KEY_NAMAZ_ENABLE_PER_ALARM_VOLUME_CRESCENDO_DURATION, mEnablePerAlarmVolumeCrescendoDurationPref,
                                mAlarmVolumeCrescendoDurationPref, alarm ->
                                    alarm.crescendoDuration = SettingsDAO.getAlarmVolumeCrescendoDuration(mPrefs, true));
                        case KEY_NAMAZ_ENABLE_PER_ALARM_VIBRATION_PATTERN ->
                            showDisablePerAlarmSettingDialog(R.string.enable_per_alarm_vibration_pattern_dialog_message,
                                KEY_NAMAZ_ENABLE_PER_ALARM_VIBRATION_PATTERN, mEnablePerAlarmVibrationPatternPref, mVibrationPatternPref,
                                alarm -> alarm.vibrationPattern = SettingsDAO.getVibrationPattern(mPrefs, true));
                        case KEY_NAMAZ_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT ->
                            showDisablePerAlarmSettingDialog(R.string.enable_alarm_vibrations_by_default_dialog_message,
                                KEY_NAMAZ_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, mEnableAlarmVibrationsByDefaultPref, null,
                                alarm -> alarm.vibrate = false);
                    }
                } else {
                    mPrefs.edit().putBoolean(prefKey, false).apply();
                    Preference pref = findPreference(prefKey);
                    if (pref instanceof SwitchPreferenceCompat sp) sp.setChecked(false);
                }
            });
        });
    }

    private void showDisablePerAlarmSettingDialog(@StringRes int messageResId, String prefKey, SwitchPreferenceCompat switchPref,
                                                  @Nullable Preference dependentPref, AlarmUpdater alarmUpdater) {
        String confirmAction = getString(R.string.confirm_action_prompt);
        mActiveDialog = CustomDialog.create(requireContext(), null, AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error),
            getString(R.string.warning), getString(messageResId, confirmAction), null, getString(android.R.string.ok),
            (d, w) -> {
                AppExecutors.getDiskIO().execute(() -> {
                    List<Alarm> currentAlarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
                    for (Alarm alarm : currentAlarms) {
                        if (alarm.relationalRuleId != -1) {
                            alarmUpdater.update(alarm);
                            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                        }
                    }
                });
                mPrefs.edit().putBoolean(prefKey, false).apply();
                switchPref.setChecked(false);
                if (dependentPref != null) dependentPref.setVisible(true);
            }, getString(android.R.string.cancel), null, null, null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingDialogPrefKey = null)), CustomDialog.SoftInputMode.NONE);
        mActiveDialog.show();
    }

    private void initAudioDeviceCallback() {
        if (mAudioDeviceCallback != null) return;
        mAudioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                mAlarmVolumePref.stopRingtonePreview();
                for (AudioDeviceInfo device : addedDevices) {
                    if (RingtoneUtils.isExternalAudioDevice(device)) {
                        mHasExternalAudioDeviceConnected = true;
                        mAlarmVolumePref.setEnabled(false);
                        mAlarmVolumePref.setTitle(R.string.disconnect_external_audio_device_title);
                        mExternalAudioDeviceVolumePref.setEnabled(true);
                        mExternalAudioDeviceVolumePref.setTitle(R.string.external_audio_device_volume_title);
                    }
                }
            }
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();
                for (AudioDeviceInfo device : removedDevices) {
                    if (RingtoneUtils.isExternalAudioDevice(device)) {
                        mHasExternalAudioDeviceConnected = false;
                        mAlarmVolumePref.setEnabled(true);
                        mAlarmVolumePref.setTitle(R.string.alarm_volume_title);
                        mExternalAudioDeviceVolumePref.setEnabled(false);
                        mExternalAudioDeviceVolumePref.setTitle(R.string.connect_external_audio_device_title);
                    }
                }
            }
        };
        mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, new Handler(Looper.getMainLooper()));
    }

    private void stopRingtonePreview() {
        if (mHasExternalAudioDeviceConnected) mExternalAudioDeviceVolumePref.stopRingtonePreviewForExternalAudioDevices();
        else mAlarmVolumePref.stopRingtonePreview();
    }

    private void updateRingtonePreferences() {
        mAlarmRingtonePref.setSummary(DataModel.getDataModel().getAlarmRingtoneTitle(true));
        mAlarmRingtonePref.setIntent(RingtonePickerActivity.createAlarmRingtonePickerIntentForSettings(requireContext()));
    }

    private void nullifyAllPrefs() {
        mAlarmShowRoundingPref = null;
        mStyleOverridePref = null; mRingtoneOverridePref = null; mAdvancedAudioOverridePref = null;
        mVibrationOverridePref = null; mActionsOverridePref = null; mMiscOverridePref = null;
        mAlarmDisplayCustomizationPref = null; mAlarmRingtonePref = null;
        mAlarmAutoSilencePref = null; mEnablePerAlarmAutoSilencePref = null; mAlarmSnoozeDurationPref = null;
        mEnablePerAlarmSnoozeDurationPref = null; mRepeatMissedAlarmPref = null; mEnablePerAlarmMissedRepeatLimitPref = null;
        mAlarmVolumePref = null; mEnablePerAlarmVolumePref = null; mAlarmVolumeCrescendoDurationPref = null;
        mEnablePerAlarmVolumeCrescendoDurationPref = null; mAdvancedAudioPlaybackPref = null;
        mAutoRoutingToExternalAudioDevicePref = null; mSystemMediaVolumePref = null; mExternalAudioDeviceVolumePref = null;
        mEnableAlarmVibrationsByDefaultPref = null; mVibrationPatternPref = null; mEnablePerAlarmVibrationPatternPref = null;
        mVibrationStartDelayPref = null; mEnableSnoozedOrDismissedAlarmVibrationsPref = null;
        mVolumeButtonsPref = null; mPowerButtonPref = null; mHeadphonesButtonPref = null;
        mFlipActionPref = null; mShakeActionPref = null; mShakeIntensityPref = null;
        mDisplayDismissButtonPref = null; mAlarmNotificationReminderPref = null;
        mTurnOnBackFlashForTriggeredAlarmPref = null; mDisplayLowAlarmVolumeWarningPref = null;
    }

    private interface AlarmUpdater {
        void update(Alarm alarm);
    }
}
