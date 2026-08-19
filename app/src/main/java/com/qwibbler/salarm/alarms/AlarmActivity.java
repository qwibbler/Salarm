/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.alarms;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_BLUR_INTENSITY;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_NAMAZ_SHOW_BOTH_ASR_IN_ALARM_EVENTS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.BaseActivity;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.DataModel.HeadphonesButtonBehavior;
import com.qwibbler.salarm.data.DataModel.PowerButtonBehavior;
import com.qwibbler.salarm.data.DataModel.VolumeButtonBehavior;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.AlarmActivityBinding;
import com.qwibbler.salarm.events.Events;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.provider.AlarmInstance;
import com.qwibbler.salarm.uicomponents.PillView;
import com.qwibbler.salarm.utils.AlarmUtils;
import com.qwibbler.salarm.utils.AnimatorUtils;
import com.qwibbler.salarm.utils.ClockUtils;
import com.qwibbler.salarm.utils.FormattedTextUtils;
import com.qwibbler.salarm.utils.InsetsUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.RingtoneUtils;
import com.qwibbler.salarm.utils.SdkUtils;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class AlarmActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmActivity");

    private static final long POWER_BUTTON_ACTIVATION_DELAY = 1500;
    private static final float TEXT_FADE_START_THRESHOLD = 0.5f;
    private static final int TRANSLATION_DURATION_START_DELAY = 1000;
    private static final int TRANSLATION_DURATION_DELAY = 1000;
    private static final int TRANSLATION_DURATION_MILLIS = 1000;
    private static final int ALPHA_DURATION_MILLIS = 400;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2500;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.i("Finished binding to AlarmService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGGER.i("Disconnected from AlarmService");
        }
    };

    private AlarmActivityBinding mBinding;
    private SharedPreferences mPrefs;
    private Typeface mGeneralBoldTypeface;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Alarm mAlarm;
    private AlarmInstance mAlarmInstance;
    private boolean mAlarmHandled;
    private VolumeButtonBehavior mVolumeBehavior;
    private PowerButtonBehavior mPowerBehavior;
    private HeadphonesButtonBehavior mHeadphonesButtonBehavior;
    private MediaSession mMediaSession;
    private long mActivityStartTime;
    private float mAlarmTitleFontSize;
    private int mAlarmTitleColor;
    private int mAlarmButtonColor;
    private boolean mIsTextShadowDisplayed;
    private int mShadowColor;
    private int mShadowOffset;
    private float mShadowRadius;
    private boolean mIsSwipeActionEnabled;
    private boolean mReceiverRegistered = false;
    private boolean mPowerBtnReceiverRegistered = false;
    private boolean mServiceBound;
    private boolean mIsSnoozeSelectorDisplayed;
    private String[] mSnoozeSelectorEntries;
    private int[] mSnoozeSelectorValues;
    private int mSnoozeSelectorIndex = 0;
    private int mSnoozeMinusButtonColor;
    private int mSnoozePlusButtonColor;
    private int mSnoozeMinusSymbolColor;
    private int mSnoozePlusSymbolColor;
    private int mDefaultSnoozeMinutes;
    private boolean mIsNamaz;
    private String mShowNextEventInfo;

    private Animator mTranslationAnimator;
    private int mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;
    private float mInitialTouchX = 0;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LOGGER.v("Received broadcast: %s", action);

            if (!mAlarmHandled) {
                if (action != null) {
                    switch (action) {
                        case AlarmService.ALARM_SNOOZE_ACTION -> snooze();
                        case AlarmService.ALARM_DISMISS_ACTION -> dismiss();
                        case AlarmService.ALARM_DONE_ACTION -> finish();
                        default -> LOGGER.i("Unknown broadcast: %s", action);
                    }
                }
            } else {
                LOGGER.v("Ignored broadcast: %s", action);
            }
        }
    };

    private final BroadcastReceiver mPowerBtnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LOGGER.v("Received broadcast: %s", action);

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // Ignore immediate screen-off events to prevent the proximity sensor from instantly dismissing
                // the alarm if the device wakes up in a pocket or face down.
                if (SystemClock.elapsedRealtime() - mActivityStartTime < POWER_BUTTON_ACTIVATION_DELAY) {
                    LOGGER.v("Ignored ACTION_SCREEN_OFF due to grace period.");
                    return;
                }

                // Power keys dismiss the alarm.
                if (!mAlarmHandled && !isFinishing()) {
                    if (mPowerBehavior == PowerButtonBehavior.SNOOZE) {
                        snooze();
                    } else if (mPowerBehavior == PowerButtonBehavior.DISMISS) {
                        dismiss();
                    }
                }
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityStartTime = SystemClock.elapsedRealtime();

        mBinding = AlarmActivityBinding.inflate(getLayoutInflater());

        Context storageContext = Utils.getSafeStorageContext(this);

        mPrefs = getDefaultSharedPreferences(storageContext);
        mGeneralBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs));

        // Register Power button (screen off) intent receiver
        if (mPowerBehavior != PowerButtonBehavior.NOTHING) {
            IntentFilter powerFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            if (SdkUtils.isAtLeastAndroid13()) {
                registerReceiver(mPowerBtnReceiver, powerFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mPowerBtnReceiver, powerFilter);
            }

            mPowerBtnReceiverRegistered = true;
        }

        setVolumeControlStream(AudioManager.STREAM_ALARM);

        if (!initAlarmAndInstance()) {
            return;
        }

        initDefaultSnoozeValue();

        initHeadphonesButton();

        LOGGER.i("Displaying alarm for instance: %s", mAlarmInstance);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        if (SdkUtils.isAtLeastAndroid81()) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        } else {
            //noinspection deprecation
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // Honor rotation on tablets; fix the orientation on phones.
        if (ThemeUtils.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        setContentView(mBinding.getRoot());

        mIsNamaz = mAlarm != null && mAlarm.relationalRuleId != -1;

        mVolumeBehavior = SettingsDAO.getAlarmVolumeButtonBehavior(mPrefs, mIsNamaz);
        mPowerBehavior = SettingsDAO.getAlarmPowerButtonBehavior(mPrefs, mIsNamaz);
        mHeadphonesButtonBehavior = SettingsDAO.getHeadphonesButtonBehavior(mPrefs, mIsNamaz);
        mShowNextEventInfo = SettingsDAO.getNamazShowNextEventInfo(mPrefs);

        initAlarmBackground();

        mIsSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(mPrefs, mIsNamaz);
        mIsSnoozeSelectorDisplayed = SettingsDAO.isSnoozeSelectorDisplayed(mPrefs, mIsNamaz);
        mAlarmTitleFontSize = SettingsDAO.getAlarmTitleFontSize(mPrefs, mIsNamaz);
        mAlarmTitleColor = SettingsDAO.getAlarmTitleColor(mPrefs, mIsNamaz);
        mAlarmButtonColor = SettingsDAO.getAlarmButtonColor(mPrefs, this, mIsNamaz);
        mSnoozeMinusButtonColor = SettingsDAO.getSnoozeMinusButtonColor(mPrefs, mIsNamaz);
        mSnoozePlusButtonColor = SettingsDAO.getSnoozePlusButtonColor(mPrefs, mIsNamaz);
        mSnoozeMinusSymbolColor = SettingsDAO.getSnoozeMinusSymbolColor(mPrefs, mIsNamaz);
        mSnoozePlusSymbolColor = SettingsDAO.getSnoozePlusSymbolColor(mPrefs, mIsNamaz);
        mIsTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(mPrefs, mIsNamaz);
        mShadowColor = SettingsDAO.getAlarmShadowColor(mPrefs, mIsNamaz);
        mShadowOffset = SettingsDAO.getAlarmShadowOffset(mPrefs, mIsNamaz);
        mShadowRadius = mShadowOffset * 0.5f;

        initAlarmClock();

        initAlarmTitle();

        if (mIsSwipeActionEnabled) {
            initSlideModeUI();
        } else {
            initButtonModeUI();
        }

        if (mIsSnoozeSelectorDisplayed) {
            initSnoozeSelector();
        } else {
            mBinding.snoozeSelectorLayout.setVisibility(GONE);
            mAlarmInstance.mSnoozeDuration = mDefaultSnoozeMinutes;
            mAlarmInstance.updateInstance(getContentResolver());
        }

        updateSnoozeTexts();

        initRingtoneTitle();

        initNextEventInfo();

        applyWindowInsets();

        ThemeUtils.hideSystemBars(getWindow(), getWindow().getDecorView());
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        // Re-query for AlarmInstance in case the state has changed externally
        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);

        if (mAlarmInstance == null) {
            LOGGER.i("No alarm instance for instanceId: %d", instanceId);
            finish();
            return;
        }

        // Verify that the alarm is still firing before showing the activity
        if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LOGGER.i("Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        if (!mReceiverRegistered) {
            // Register to get the alarm done/snooze/dismiss intent.
            final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
            filter.addAction(AlarmService.ALARM_SNOOZE_ACTION);
            filter.addAction(AlarmService.ALARM_DISMISS_ACTION);

            if (SdkUtils.isAtLeastAndroid13()) {
                registerReceiver(mReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mReceiver, filter);
            }

            mReceiverRegistered = true;
        }

        bindAlarmService();

        resetAnimations();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindAlarmService();

        // Skip if register didn't happen to avoid IllegalArgumentException
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    @Override
    protected void onDestroy() {
        if (mPowerBtnReceiverRegistered) {
            unregisterReceiver(mPowerBtnReceiver);
            mPowerBtnReceiverRegistered = false;
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession.release();
            mMediaSession = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) {
        // Do this in dispatch to intercept a few of the system keys.
        LOGGER.v("dispatchKeyEvent: %s", keyEvent);

        final int keyCode = keyEvent.getKeyCode();
        switch (keyCode) {
            // Volume keys and camera keys dismiss the alarm.
            case KeyEvent.KEYCODE_VOLUME_UP,
                 KeyEvent.KEYCODE_VOLUME_DOWN,
                 KeyEvent.KEYCODE_VOLUME_MUTE,
                 KeyEvent.KEYCODE_CAMERA,
                 KeyEvent.KEYCODE_FOCUS -> {
                if (!mAlarmHandled) {
                    switch (mVolumeBehavior) {
                        case NOTHING -> {
                            return keyEvent.getAction() != KeyEvent.ACTION_UP;
                        }
                        case SNOOZE -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                snooze();
                            }
                            return true;
                        }
                        case DISMISS -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                dismiss();
                            }
                            return true;
                        }
                        case CHANGE_VOLUME -> { /* Do nothing */ }
                    }
                }
            }

            case KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (!mAlarmHandled) {
                    switch (mHeadphonesButtonBehavior) {
                        case NOTHING -> {
                            return keyEvent.getAction() != KeyEvent.ACTION_UP;
                        }
                        case SNOOZE -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                snooze();
                            }
                            return true;
                        }
                        case DISMISS -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                dismiss();
                            }
                            return true;
                        }
                    }
                }
            }
        }

        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public void onClick(View view) {
        if (mAlarmHandled) {
            LOGGER.v("onClick ignored: %s", view);
            return;
        }
        LOGGER.v("onClick: %s", view);

        // If alarm swiping is disabled in settings, allow snooze/dismiss by tapping on respective text.
        if (!mIsSwipeActionEnabled) {
            if (view == mBinding.snoozeButton) {
                snooze();
            } else if (view == mBinding.dismissButton) {
                dismiss();
            } else if (view == mBinding.dismissOnlyButton) {
                if (isSnoozeDisabledForAlarmInstance()) {
                    dismiss();
                } else {
                    snooze();
                }
            }
        }

        if (mIsSnoozeSelectorDisplayed) {
            if (view == mBinding.snoozeSelectorPlus) {
                if (mSnoozeSelectorIndex < mSnoozeSelectorEntries.length - 1) {
                    mSnoozeSelectorIndex++;
                    updateSnoozeTexts();
                    updateSnoozeButtonsState();
                }
            } else if (view == mBinding.snoozeSelectorMinus) {
                if (mSnoozeSelectorIndex > 0) {
                    mSnoozeSelectorIndex--;
                    updateSnoozeTexts();
                    updateSnoozeButtonsState();
                }
            }

            int selectedValue = mSnoozeSelectorValues[mSnoozeSelectorIndex];
            if (selectedValue == -1) {
                mAlarmInstance.mSnoozeDuration = mDefaultSnoozeMinutes;
            } else {
                mAlarmInstance.mSnoozeDuration = selectedValue;
            }

            mAlarmInstance.updateInstance(getContentResolver());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mAlarmHandled) {
            LOGGER.v("onTouch ignored: %s", event);
            return false;
        }

        final int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            LOGGER.v("onTouch started: %s", event);

            // Track the pointer that initiated the touch sequence.
            mInitialPointerIndex = event.getPointerId(event.getActionIndex());

            // Stop the translation
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }

            final int[] contentLocation = {0, 0};
            mBinding.contentView.getLocationOnScreen(contentLocation);

            mInitialTouchX = event.getRawX() - contentLocation[0];
        } else if (action == MotionEvent.ACTION_CANCEL) {
            LOGGER.v("onTouch canceled: %s", event);

            // Clear the pointer index.
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            // Reset everything.
            resetAnimations();
            return true;
        }

        final int actionIndex = event.getActionIndex();
        if (mInitialPointerIndex == MotionEvent.INVALID_POINTER_ID || mInitialPointerIndex != event.getPointerId(actionIndex)) {
            // Ignore any pointers other than the initial one, bail early.
            return true;
        }

        final int[] contentLocation = {0, 0};
        mBinding.contentView.getLocationOnScreen(contentLocation);

        final float x = event.getRawX() - contentLocation[0];

        float deltaX = x - mInitialTouchX;

        // Limit movement within the parent
        float maxDeltaX = (getAvailableSlideZoneWidth() - mBinding.alarmButton.getWidth()) / 2f;
        deltaX = Math.max(-maxDeltaX, Math.min(deltaX, maxDeltaX));
        mBinding.alarmButton.setTranslationX(deltaX);

        if (Math.abs(deltaX) >= maxDeltaX) {
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }
        }

        updateTextAlpha(deltaX);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            LOGGER.v("onTouch ended: %s", event);

            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            if (mBinding.contentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                if (deltaX <= -maxDeltaX) {
                    dismiss(); // Left = Dismiss in RTL
                } else if (deltaX >= maxDeltaX) {
                    snooze(); // Right = Snooze en RTL
                } else {
                    resetAnimations();
                }
            } else {
                if (deltaX >= maxDeltaX) {
                    dismiss(); // Right = Dismiss in RTL
                } else if (deltaX <= -maxDeltaX) {
                    snooze(); // Left = Snooze in LTR
                } else {
                    resetAnimations();
                }
            }
        }

        return true;
    }

    /**
     * This method adjusts the space occupied by the status bar, and adjust the display of the clock layout accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBinding.clockLayout, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.displayCutout());

            v.setPadding(0, bars.top, 0, 0);
        });
    }

    private boolean initAlarmAndInstance() {
        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (mAlarmInstance == null) {
            // The alarm was deleted before the activity got created, so just finish()
            LOGGER.e("Error displaying alarm for intent: %s", getIntent());
            finish();
            return false;
        } else if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LOGGER.i("Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return false;
        }

        if (mAlarmInstance.mAlarmId == null) {
            LOGGER.e("mAlarmId is null for instance: %s", mAlarmInstance);
            finish();
            return false;
        }

        mAlarm = Alarm.getAlarm(getContentResolver(), mAlarmInstance.mAlarmId);
        if (mAlarm == null) {
            LogUtils.wtf("Failed to retrieve alarm with ID: %d", mAlarmInstance.mAlarmId);
            finish();
            return false;
        }
        return true;
    }

    private void initDefaultSnoozeValue() {
        mDefaultSnoozeMinutes = mAlarm.snoozeDuration;
    }

    private void initHeadphonesButton() {
        boolean isAdvancedPlayback = SettingsDAO.isAdvancedAudioPlaybackEnabled(mPrefs, mIsNamaz);
        boolean isAutoRouting = SettingsDAO.isAutoRoutingToExternalAudioDevice(mPrefs, mIsNamaz);

        if (isAdvancedPlayback && isAutoRouting) {
            mMediaSession = RingtoneUtils.createMediaSession(this, "AlarmMediaSession", () -> {
                if (!mAlarmHandled) {
                    switch (mHeadphonesButtonBehavior) {
                        case SNOOZE -> snooze();
                        case DISMISS -> dismiss();
                        case NOTHING -> { /* Do nothing */ }
                    }
                }
            });
        }
    }

    /**
     * Initializes the background.
     */
    private void initAlarmBackground() {
        final String darkMode = SettingsDAO.getDarkMode(mPrefs);
        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && Objects.equals(darkMode, AMOLED_DARK_MODE);
        int alarmBackgroundColor = isAmoledMode
            ? SettingsDAO.getAlarmBackgroundAmoledColor(mPrefs)
            : SettingsDAO.getAlarmBackgroundColor(mPrefs, mIsNamaz);
        final boolean isPerAlarmBackgroundImageEnable = SettingsDAO.isPerAlarmBackgroundImageEnable(mPrefs, mIsNamaz);
        String imagePath = SettingsDAO.getAlarmBackgroundImage(mPrefs, mIsNamaz);

        if (isPerAlarmBackgroundImageEnable && !TextUtils.isEmpty(mAlarm.backgroundImage)) {
            imagePath = mAlarm.backgroundImage;
        }

        // Apply a background image and a blur effect.
        if (TextUtils.isEmpty(imagePath)) {
            mBinding.alarmBackgroundImage.setVisibility(GONE);
            getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
        } else {
            mBinding.alarmBackgroundImage.setVisibility(VISIBLE);

            File imageFile = new File(imagePath);

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    mBinding.alarmBackgroundImage.setImageBitmap(bitmap);

                    float blurIntensity = isPerAlarmBackgroundImageEnable
                        ? mAlarm.blurIntensity
                        : SettingsDAO.getAlarmBlurIntensity(mPrefs, mIsNamaz);

                    if (SdkUtils.isAtLeastAndroid12() && blurIntensity != DEFAULT_BLUR_INTENSITY) {
                        RenderEffect blur = RenderEffect.createBlurEffect(blurIntensity, blurIntensity, Shader.TileMode.CLAMP);
                        mBinding.alarmBackgroundImage.setRenderEffect(blur);
                    }
                } else {
                    LogUtils.e("Bitmap null for path: " + imagePath);
                    getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
                }
            } else {
                LogUtils.e("Image file not found: " + imagePath);
                getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
            }
        }
    }

    /**
     * Initializes the digital or analog clock.
     */
    private void initAlarmClock() {
        final DataModel.ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(mPrefs, mIsNamaz);
        final boolean isAlarmSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs, mIsNamaz);
        final int alarmClockColor = SettingsDAO.getAlarmClockColor(mPrefs, mIsNamaz);
        final float alarmDigitalClockFontSize = SettingsDAO.getAlarmDigitalClockFontSize(mPrefs, mIsNamaz);

        ClockUtils.setClockStyle(alarmClockStyle, mBinding.digitalClock, mBinding.analogClock);

        if (alarmClockStyle == DataModel.ClockStyle.DIGITAL) {
            ClockUtils.setDigitalClockFont(mBinding.digitalClock, SettingsDAO.getAlarmFont(mPrefs, mIsNamaz));
            ClockUtils.setDigitalClockTimeFormat(mBinding.digitalClock, 0.4f, false, true, false, false, false);
            mBinding.digitalClock.applyUserPreferredTextSizeSp(alarmDigitalClockFontSize);
            mBinding.digitalClock.setTextColor(alarmClockColor);

            // Display a shadow if enabled in the settings
            if (mIsTextShadowDisplayed) {
                mBinding.digitalClock.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            }
        } else {
            ClockUtils.adjustAnalogClockSize(mBinding.analogClock, SettingsDAO.getAlarmAnalogClockSize(mPrefs, mIsNamaz));
            ClockUtils.setAnalogClockSecondsEnabled(alarmClockStyle, mBinding.analogClock, isAlarmSecondHandDisplayed);
        }
    }

    /**
     * Initializes the alarm title.
     */
    private void initAlarmTitle() {
        mBinding.alarmTitle.setText(mAlarmInstance.getLabelOrDefault(this));
        mBinding.alarmTitle.setTypeface(mGeneralBoldTypeface);
        mBinding.alarmTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        mBinding.alarmTitle.setTextColor(mAlarmTitleColor);

        if (SettingsDAO.isAlarmTitleDisplayedOnSingleLine(mPrefs, mIsNamaz)) {
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mBinding.alarmTitle, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            mBinding.alarmTitle.setSingleLine(true);
            mBinding.alarmTitle.setSelected(true); // Allow text scrolling
            mBinding.alarmTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            mBinding.alarmTitle.setMarqueeRepeatLimit(-1);
            mBinding.alarmTitle.setHorizontallyScrolling(true);
        } else {
            mBinding.alarmTitle.setSingleLine(false);
            mBinding.alarmTitle.setSelected(false);
            mBinding.alarmTitle.setEllipsize(null);
            mBinding.alarmTitle.setHorizontallyScrolling(false);

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                mBinding.alarmTitle,
                12,
                (int) mAlarmTitleFontSize,
                2,
                TypedValue.COMPLEX_UNIT_SP
            );
        }

        // Display a shadow if enabled in the settings
        if (mIsTextShadowDisplayed) {
            mBinding.alarmTitle.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
        }
    }

    /**
     * Initializes the slide mode.
     */
    private void initSlideModeUI() {
        mBinding.slideZoneLayout.setVisibility(VISIBLE);
        mBinding.snoozeButton.setVisibility(GONE);
        mBinding.dismissButton.setVisibility(GONE);
        mBinding.dismissOnlyButton.setVisibility(GONE);

        initSlideColors();
        initSlideTexts();
        initSlideAnimations();
    }

    /**
     * Initializes the slide mode colors.
     */
    private void initSlideColors() {
        int slideZoneColor = SettingsDAO.getSlideZoneColor(mPrefs, mIsNamaz);

        Drawable background = AppCompatResources.getDrawable(this, R.drawable.bg_alarm_slide_zone);
        if (background != null) {
            DrawableCompat.setTint(background, slideZoneColor);
        }

        mBinding.slideZoneLayout.setBackground(background);
        mBinding.alarmButton.setBackgroundColor(mAlarmButtonColor);
    }

    /**
     * Initializes the slide mode texts.
     */
    private void initSlideTexts() {
        boolean disabled = isSnoozeDisabledForAlarmInstance();

        mBinding.alarmButton.setContentDescription(getString(disabled
            ? (isOccasionalAlarmDeletedAfterUse()
            ? R.string.description_direction_both_for_occasional_non_repeatable_alarm
            : R.string.description_direction_both_for_non_repeatable_alarm)
            : (isOccasionalAlarmDeletedAfterUse()
            ? R.string.description_direction_both_for_occasional_alarm
            : R.string.description_direction_both)
        ));

        mBinding.snoozeText.setTypeface(mGeneralBoldTypeface);
        mBinding.snoozeText.setTextColor(SettingsDAO.getSnoozeTitleColor(mPrefs, mIsNamaz));
        int snoozeTextRes = isSnoozeDisabledForAlarmInstance()
            ? (isOccasionalAlarmDeletedAfterUse()
            ? R.string.delete
            : R.string.button_action_dismiss)
            : R.string.button_action_snooze;

        mBinding.snoozeText.setText(getString(snoozeTextRes));

        mBinding.dismissText.setText(getString(isOccasionalAlarmDeletedAfterUse()
            ? R.string.delete
            : R.string.button_action_dismiss
        ));
        mBinding.dismissText.setTypeface(mGeneralBoldTypeface);
        mBinding.dismissText.setTextColor(SettingsDAO.getDismissTitleColor(mPrefs, mIsNamaz));
    }

    /**
     * Initializes the slide mode animations.
     */
    private void initSlideAnimations() {
        mBinding.alarmButton.setOnTouchListener(this);

        mBinding.pill.setFillColor(ColorUtils.setAlphaComponent(mAlarmButtonColor, 128));

        mBinding.pill.post(() -> {
            mBinding.pill.setPillHeight(mBinding.alarmButton.getHeight()
                - mBinding.alarmButton.getInsetTop()
                - mBinding.alarmButton.getInsetBottom()
            );
            final float pillStretchWidth = getAvailableSlideZoneWidth() / 2f;
            final int originalFillColor = mBinding.pill.getFillColor();

            // Move to left
            AnimatorSet toLeftAnimator = new AnimatorSet();
            toLeftAnimator.playTogether(translationAnimator(mBinding.pill, pillStretchWidth,
                mBinding.pill.getPillCenterX() - pillStretchWidth / 2), alphaAnimator(mBinding.pill, originalFillColor)
            );
            toLeftAnimator.setStartDelay(TRANSLATION_DURATION_START_DELAY);
            toLeftAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

            // Apply alpha
            Animator alphaLeft = alphaAnimator(mBinding.pill, ColorUtils.setAlphaComponent(originalFillColor, 0));
            alphaLeft.setDuration(ALPHA_DURATION_MILLIS);

            // Reset position and alpha
            AnimatorSet resetAndRestoreLeft = new AnimatorSet();
            resetAndRestoreLeft.playTogether(translationAnimator(mBinding.pill, 0, mBinding.pill.getPillCenterX()),
                alphaAnimator(mBinding.pill, originalFillColor)
            );
            resetAndRestoreLeft.setDuration(0);

            // Move to right
            Animator toRightAnimator = translationAnimator(
                mBinding.pill, pillStretchWidth, mBinding.pill.getPillCenterX() + pillStretchWidth / 2);
            toRightAnimator.setStartDelay(TRANSLATION_DURATION_DELAY);
            toRightAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

            // Apply alpha
            Animator alphaRight = alphaAnimator(mBinding.pill, ColorUtils.setAlphaComponent(originalFillColor, 0));
            alphaRight.setDuration(ALPHA_DURATION_MILLIS);

            // Reset position and alpha
            AnimatorSet resetAndRestoreRight = new AnimatorSet();
            resetAndRestoreRight.playTogether(translationAnimator(mBinding.pill, 0, mBinding.pill.getPillCenterX()),
                alphaAnimator(mBinding.pill, originalFillColor)
            );
            resetAndRestoreRight.setDuration(0);

            // Sequence
            AnimatorSet translationSequence = new AnimatorSet();
            translationSequence.playSequentially(toLeftAnimator, alphaLeft, resetAndRestoreLeft, toRightAnimator,
                alphaRight, resetAndRestoreRight);
            translationSequence.setInterpolator(new AccelerateDecelerateInterpolator());

            // Listener to repeat animation if needed
            translationSequence.addListener(new AnimatorListenerAdapter() {
                private boolean wasCancelled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    mBinding.pill.setFillColor(Color.TRANSPARENT);

                    wasCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!wasCancelled && mTranslationAnimator == animation) {
                        mTranslationAnimator.start();
                    }

                    wasCancelled = false;
                }
            });

            mTranslationAnimator = translationSequence;
            mTranslationAnimator.start();
        });
    }

    /**
     * Initializes the button mode.
     */
    private void initButtonModeUI() {
        mBinding.slideZoneLayout.setVisibility(INVISIBLE);

        if (isSnoozeDisabledForAlarm()) {
            initDismissOnlyButton();
        } else {
            initSnoozeAndDismissButtons();
        }
    }

    /**
     * Initializes the "Dismiss only" button.
     */
    private void initDismissOnlyButton() {
        mBinding.snoozeButton.setVisibility(GONE);
        mBinding.dismissButton.setVisibility(GONE);

        mBinding.dismissOnlyButton.setBackgroundColor(SettingsDAO.getDismissButtonColor(mPrefs, this, mIsNamaz));
        mBinding.dismissOnlyButton.setTypeface(mGeneralBoldTypeface);
        mBinding.dismissOnlyButton.setText(getString(isOccasionalAlarmDeletedAfterUse() ? R.string.delete : R.string.button_action_dismiss));
        mBinding.dismissOnlyButton.setContentDescription(getString(isOccasionalAlarmDeletedAfterUse()
            ? R.string.description_dismiss_button_for_occasional_alarm
            : R.string.description_dismiss_button
        ));

        mBinding.dismissOnlyButton.setVisibility(VISIBLE);
        mBinding.dismissOnlyButton.setOnClickListener(this);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mBinding.dismissOnlyButton.setSelected(true);
    }

    /**
     * Initializes the "Snooze" and "Dismiss" buttons.
     */
    private void initSnoozeAndDismissButtons() {
        mBinding.dismissOnlyButton.setVisibility(GONE);

        mBinding.snoozeButton.setBackgroundColor(SettingsDAO.getSnoozeButtonColor(mPrefs, this, mIsNamaz));
        mBinding.snoozeButton.setText(getString(R.string.button_action_snooze));
        mBinding.snoozeButton.setTypeface(mGeneralBoldTypeface);
        mBinding.snoozeButton.setContentDescription(getString(R.string.description_snooze_button));
        mBinding.snoozeButton.setVisibility(VISIBLE);
        mBinding.snoozeButton.setOnClickListener(this);

        mBinding.dismissButton.setBackgroundColor(SettingsDAO.getDismissButtonColor(mPrefs, this, mIsNamaz));
        mBinding.dismissButton.setText(getString(isOccasionalAlarmDeletedAfterUse() ? R.string.delete : R.string.button_action_dismiss));
        mBinding.dismissButton.setTypeface(mGeneralBoldTypeface);
        mBinding.dismissButton.setContentDescription(getString(isOccasionalAlarmDeletedAfterUse()
            ? R.string.description_dismiss_button_for_occasional_alarm
            : R.string.description_dismiss_button
        ));
        mBinding.dismissButton.setVisibility(VISIBLE);
        mBinding.dismissButton.setOnClickListener(this);

        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mBinding.snoozeButton.setSelected(true);
        mBinding.dismissButton.setSelected(true);
    }

    /**
     * Initializes the snooze selector.
     */
    private void initSnoozeSelector() {
        mSnoozeSelectorValues = getResources().getIntArray(R.array.alarm_snooze_selector_values);
        mSnoozeSelectorEntries = new String[mSnoozeSelectorValues.length];

        for (int i = 0; i < mSnoozeSelectorValues.length; i++) {
            int snoozeValue = mSnoozeSelectorValues[i];

            if (snoozeValue == -1) {
                if (mDefaultSnoozeMinutes == ALARM_SNOOZE_DURATION_DISABLED) {
                    // "Default (None)"
                    mSnoozeSelectorEntries[i] =
                        String.format("%s (%s)", getString(R.string.label_default), getString(R.string.snooze_duration_none));
                } else {
                    // "Default (X min)"
                    String defaultTimeStr = buildTimeString(mDefaultSnoozeMinutes);
                    mSnoozeSelectorEntries[i] = String.format("%s (%s)", getString(R.string.label_default), defaultTimeStr);
                }
            } else {
                // "Snooze X min"
                String timeStr = buildTimeString(snoozeValue);
                mSnoozeSelectorEntries[i] = getString(R.string.alarm_alert_snooze_text) + " " + timeStr;
            }
        }

        initSnoozeSelectorStyle();
        initSnoozeSelectorListeners();

        mSnoozeSelectorIndex = findIndexForInstanceSnooze(mAlarmInstance.mSnoozeDuration, mDefaultSnoozeMinutes);

        updateSnoozeButtonsState();

        mBinding.snoozeSelectorLayout.setVisibility(VISIBLE);
    }

    /**
     * Initializes the snooze selector style.
     */
    private void initSnoozeSelectorStyle() {
        int snoozeZoneColor = SettingsDAO.getSnoozeZoneColor(mPrefs, mIsNamaz);
        int snoozeTextColor = SettingsDAO.getSnoozeSelectorTextColor(mPrefs, mIsNamaz);

        mBinding.snoozeSelectorText.setBackground(ThemeUtils.pillRippleDrawable(this, snoozeZoneColor));
        mBinding.snoozeSelectorText.setTypeface(mGeneralBoldTypeface);
        mBinding.snoozeSelectorText.setTextColor(snoozeTextColor);

        styleSnoozeButton(mBinding.snoozeSelectorMinus, mSnoozeMinusButtonColor, mSnoozeMinusSymbolColor, true);

        styleSnoozeButton(mBinding.snoozeSelectorPlus, mSnoozePlusButtonColor, mSnoozePlusSymbolColor, true);
    }

    /**
     * Initializes the snooze selector listeners when the buttons are pressed and the selector area
     * is long-pressed.
     */
    private void initSnoozeSelectorListeners() {
        mBinding.snoozeSelectorText.setOnLongClickListener(v -> {
            snooze();
            return true;
        });

        mBinding.snoozeSelectorMinus.setOnClickListener(this);
        mBinding.snoozeSelectorPlus.setOnClickListener(this);
    }

    /**
     * Initializes the ringtone title.
     */
    private void initRingtoneTitle() {
        if (SettingsDAO.isRingtoneTitleDisplayed(mPrefs, mIsNamaz)) {
            mBinding.ringtoneLayout.setVisibility(VISIBLE);

            displayRingtoneTitle();
        } else {
            mBinding.ringtoneLayout.setVisibility(GONE);
        }
    }

    /**
     * Returns the selector index that matches the given snooze value.
     *
     * <p>Index 0 represents the "Default" option.</p>
     *
     * @param instanceValue the snooze value of the alarm instance
     * @param defaultValue  the default snooze value of the alarm
     * @return the matching index, or 0 if no match is found
     */
    private int findIndexForInstanceSnooze(int instanceValue, int defaultValue) {
        // If the instance value == the alarm's default value : "Default"
        if (instanceValue == defaultValue) {
            return 0;
        }

        // Otherwise, we look for an explicit match (ignoring the entry -1)
        for (int i = 1; i < mSnoozeSelectorValues.length; i++) {
            if (mSnoozeSelectorValues[i] == instanceValue) {
                return i;
            }
        }

        // If nothing is found, it falls back to the default
        return 0;
    }

    /**
     * Enables or disables the plus and minus buttons based on the current snooze selector index.
     */
    private void updateSnoozeButtonsState() {
        boolean minusEnabled = mSnoozeSelectorIndex > 0;
        boolean plusEnabled = mSnoozeSelectorIndex < mSnoozeSelectorEntries.length - 1;

        styleSnoozeButton(mBinding.snoozeSelectorMinus, mSnoozeMinusButtonColor, mSnoozeMinusSymbolColor, minusEnabled);

        styleSnoozeButton(mBinding.snoozeSelectorPlus, mSnoozePlusButtonColor, mSnoozePlusSymbolColor, plusEnabled);
    }

    /**
     * Applies visual styling to a snooze button, including background, symbol color, and
     * enabled/disabled state.
     *
     * @param imageView       the button to style
     * @param backgroundColor the background color when enabled
     * @param symbolColor     the symbol color when enabled
     * @param enabled         true to enable the button, false to disable it
     */
    private void styleSnoozeButton(ImageView imageView, int backgroundColor, int symbolColor,
                                   boolean enabled) {

        GradientDrawable circle = (GradientDrawable) ThemeUtils.circleDrawable();

        if (!enabled) {
            circle.setColor(Color.parseColor("#80808080"));
            imageView.setBackground(circle);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.colorDisabled));
            imageView.setClickable(false);
            return;
        }

        circle.setColor(backgroundColor);

        imageView.setBackground(ThemeUtils.rippleDrawable(this, circle));
        imageView.setColorFilter(symbolColor);
        imageView.setClickable(true);
    }

    private String buildTimeString(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;

        if (hour > 0 && minute > 0) {
            String hourString = FormattedTextUtils.getNumberFormattedQuantityString(this, R.plurals.hours_short, hour);
            String minuteString = FormattedTextUtils.getNumberFormattedQuantityString(this, R.plurals.minutes_short, minute);
            return String.format("%s %s", hourString, minuteString);
        } else if (hour > 0) {
            return FormattedTextUtils.getNumberFormattedQuantityString(this, R.plurals.hours_short, hour);
        } else {
            return FormattedTextUtils.getNumberFormattedQuantityString(this, R.plurals.minutes_short, minute);
        }
    }

    /**
     * Updates the displayed snooze text and the action button text according to the current
     * selector index and alarm settings.
     */
    private void updateSnoozeTexts() {
        if (!mIsSnoozeSelectorDisplayed) {
            return;
        }

        mBinding.snoozeSelectorText.setText(mSnoozeSelectorEntries[mSnoozeSelectorIndex]);

        if (!isSnoozeDisabledForAlarm()) {
            // Nothing to change if snooze is enabled
            return;
        }

        boolean isDefault = mSnoozeSelectorIndex == 0;
        int textRes = isDefault
            ? isOccasionalAlarmDeletedAfterUse()
            ? R.string.delete
            : R.string.button_action_dismiss
            : R.string.button_action_snooze;

        if (mIsSwipeActionEnabled) {
            mBinding.snoozeText.setText(getString(textRes));
        } else {
            mBinding.dismissOnlyButton.setText(getString(textRes));
        }
    }

    /**
     * Returns the width available for animations or interactions in the slide area,
     * excluding the left and right paddings of the layout.
     */
    private float getAvailableSlideZoneWidth() {
        return mBinding.slideZoneLayout.getWidth() - mBinding.slideZoneLayout.getPaddingStart() - mBinding.slideZoneLayout.getPaddingEnd();
    }

    /**
     * Helper method to create a translation animation.
     */
    private Animator translationAnimator(View view, float targetWidth, float targetCenterX) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
            PropertyValuesHolder.ofFloat(PillView.PILL_WIDTH, targetWidth),
            PropertyValuesHolder.ofFloat(PillView.PILL_CENTER_X, targetCenterX));
    }

    /**
     * Helper method to create an alpha color change animation.
     */
    private Animator alphaAnimator(View view, int alphaColor) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
            PropertyValuesHolder.ofObject(PillView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR, alphaColor));
    }

    /**
     * Apply transparency to "Snooze" and "Dismiss" texts based on movement direction.
     */
    private void updateTextAlpha(float deltaX) {
        final View parentView = (View) mBinding.alarmButton.getParent();
        int parentPaddingHorizontal = parentView.getPaddingStart() + parentView.getPaddingLeft();
        int parentWidth = parentView.getWidth() - (parentPaddingHorizontal);
        float maxDeltaX = parentWidth - mBinding.alarmButton.getWidth();
        maxDeltaX /= 2f; // since the displacement is centered

        float threshold = TEXT_FADE_START_THRESHOLD * maxDeltaX;
        float absDeltaX = Math.abs(deltaX);

        if (absDeltaX <= threshold) {
            mBinding.snoozeText.setAlpha(1.0f);
            mBinding.dismissText.setAlpha(1.0f);
            return;
        }

        float fadeFraction = (absDeltaX - threshold) / (maxDeltaX - threshold);
        fadeFraction = Math.min(fadeFraction, 1.0f);
        float alpha = 1.0f - fadeFraction;

        boolean isRTL = mBinding.contentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        if ((deltaX > 0 && !isRTL) || (deltaX < 0 && isRTL)) {
            // Swipe right (Dismiss right side in LTR, left in RTL)
            mBinding.dismissText.setAlpha(alpha);
            mBinding.snoozeText.setAlpha(1.0f);
        } else {
            // Swipe left (Snooze left in LTR, right in RTL)
            mBinding.snoozeText.setAlpha(alpha);
            mBinding.dismissText.setAlpha(1.0f);
        }
    }

    /**
     * Set animators to initial values, reset text transparency and restart translation on pill view.
     */
    private void resetAnimations() {
        mBinding.snoozeText.setAlpha(1.0f);
        mBinding.dismissText.setAlpha(1.0f);

        mBinding.alarmButton.animate()
            .translationX(0)
            .setDuration(200)
            .start();

        if (mTranslationAnimator != null && !mTranslationAnimator.isRunning()) {
            mTranslationAnimator.start();
        }
    }

    /**
     * Perform snooze animation and send dismiss intent if snooze duration has been set to "None";
     * otherwise, send snooze intent.
     */
    private void snooze() {
        mAlarmHandled = true;
        LOGGER.v("Snoozed: %s", mAlarmInstance);

        // If snooze duration has been set to "None", simply dismiss the alarm.
        if (isSnoozeDisabledForAlarmInstance()) {
            int titleResId;
            int action;

            if (isOccasionalAlarmDeletedAfterUse()) {
                titleResId = R.string.alarm_alert_off_and_deleted_text;
                action = R.string.action_delete_alarm_after_use;
            } else {
                titleResId = R.string.alarm_alert_off_text;
                action = R.string.action_dismiss;
            }

            displayAlarmActionMessage(titleResId, null, getString(titleResId));

            AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance, false);

            Events.sendAlarmEvent(action, R.string.label_salarm);
        } else {
            int snoozeDuration = mAlarmInstance.mSnoozeDuration;
            final String descriptionText = buildTimeString(snoozeDuration);
            final String accessibilityText = getResources().getQuantityString(
                R.plurals.alarm_alert_snooze_set, snoozeDuration, snoozeDuration);

            displayAlarmActionMessage(R.string.alarm_alert_snoozed_text, descriptionText, accessibilityText);

            AlarmStateManager.setSnoozeState(this, mAlarmInstance, false);

            Events.sendAlarmEvent(R.string.action_snooze, R.string.label_salarm);
        }

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * Perform dismiss animation and send dismiss intent.
     */
    private void dismiss() {
        mAlarmHandled = true;
        LOGGER.v("Dismissed: %s", mAlarmInstance);

        int titleResId;
        int action;

        if (isOccasionalAlarmDeletedAfterUse()) {
            titleResId = R.string.alarm_alert_off_and_deleted_text;
            action = R.string.action_delete_alarm_after_use;
        } else {
            titleResId = R.string.alarm_alert_off_text;
            action = R.string.action_dismiss;
        }

        displayAlarmActionMessage(titleResId, null, getString(titleResId));

        AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance, false);

        Events.sendAlarmEvent(action, R.string.label_salarm);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * @return {@code true} if snooze is disabled for the alarm. {@code false} otherwise.
     *
     * <p>Useful to know the default behavior of the alarm.</p>
     */
    private boolean isSnoozeDisabledForAlarm() {
        return mAlarm.snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED;
    }

    /**
     * @return {@code true} if snooze is disabled for this alarm instance. {@code false} otherwise.
     *
     * <p>Useful to know what the alarm should do when it sounds.</p>
     */
    private boolean isSnoozeDisabledForAlarmInstance() {
        if (mAlarmInstance.mSnoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
            return true;
        }

        if (mAlarmInstance.mDismissAfter < 0) {
            int maxSnoozes = Math.abs(mAlarmInstance.mDismissAfter);
            if (mAlarmInstance.mMissedAlarmCurrentCount >= maxSnoozes) {
                return true;
            }
        }

        Calendar deadline = mAlarmInstance.getDismissTimeout();
        if (deadline != null) {
            Calendar nextSnooze = Calendar.getInstance();
            nextSnooze.add(Calendar.MINUTE, mAlarmInstance.mSnoozeDuration);
            // If next snooze trigger would be at or after the deadline, don't allow it.
            if (!nextSnooze.before(deadline)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return {@code true} if the "Delete alarm once dismissed" button is ticked;
     * {@code false} otherwise.
     */
    private boolean isOccasionalAlarmDeletedAfterUse() {
        if (mAlarm.daysOfWeek.isRepeating()) {
            return false;
        }

        return mAlarm.deleteAfterUse;
    }

    /**
     * Bind AlarmService if not yet bound.
     */
    private void bindAlarmService() {
        if (!mServiceBound) {
            final Intent intent = new Intent(this, AlarmService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mServiceBound = true;
        }
    }

    /**
     * Unbind AlarmService if bound.
     */
    private void unbindAlarmService() {
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceBound = false;
        }
    }

    private void initNextEventInfo() {
        if (!mAlarmInstance.mIsNamaz || "nothing".equals(mShowNextEventInfo)) {
            if (mBinding.nextEventInfo != null) {
                mBinding.nextEventInfo.setVisibility(GONE);
            }
            return;
        }

        if (mBinding.nextEventInfo == null) return;

        mBinding.nextEventInfo.setTypeface(mGeneralBoldTypeface);
        if (mIsTextShadowDisplayed) {
            mBinding.nextEventInfo.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
        }

        mBinding.nextEventInfo.setVisibility(VISIBLE);
        updateNextEventInfo();
    }

    private void updateNextEventInfo() {
        if (isFinishing() || mAlarmHandled || mBinding.nextEventInfo == null || mBinding.nextEventInfo.getVisibility() == GONE) {
            return;
        }

        double[] coords = com.qwibbler.salarm.namaz.NamazUtils.getCoordinates(this);
        com.batoulapps.adhan.CalculationMethod method = com.qwibbler.salarm.namaz.NamazUtils.getCalculationMethod(this);
        com.batoulapps.adhan.Madhab madhab = com.qwibbler.salarm.namaz.NamazUtils.getMadhab(this);
        boolean showBothAsr = mPrefs.getBoolean(KEY_NAMAZ_SHOW_BOTH_ASR_IN_ALARM_EVENTS, false);
        long now = System.currentTimeMillis();

        Calendar cal = Calendar.getInstance();
        com.qwibbler.salarm.namaz.NamazCalculator.NamazTimes times =
            com.qwibbler.salarm.namaz.NamazCalculator.calculateAllTimes(cal.getTime(), coords[0], coords[1], method);

        java.util.List<com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent> events = new java.util.ArrayList<>();
        events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.FAJR);
        events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.SUNRISE);
        events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.DHUHR);

        if (showBothAsr) {
            events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.ASR_SHAFI);
            events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.ASR_HANAFI);
        } else {
            events.add(madhab == com.batoulapps.adhan.Madhab.HANAFI
                ? com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.ASR_HANAFI
                : com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.ASR_SHAFI);
        }

        events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.MAGHRIB);
        events.add(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent.ISHA);

        com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent nextEvent = null;
        java.util.Date nextEventTime = null;

        // Find next event today
        for (com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent event : events) {
            java.util.Date time = getEventTime(event, times);
            if (time != null && time.getTime() > now) {
                if (nextEventTime == null || time.before(nextEventTime)) {
                    nextEventTime = time;
                    nextEvent = event;
                }
            }
        }

        // If no more events today, find first event tomorrow
        if (nextEventTime == null) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            times = com.qwibbler.salarm.namaz.NamazCalculator.calculateAllTimes(cal.getTime(), coords[0], coords[1], method);
            for (com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent event : events) {
                java.util.Date time = getEventTime(event, times);
                if (time != null) {
                    if (nextEventTime == null || time.before(nextEventTime)) {
                        nextEventTime = time;
                        nextEvent = event;
                    }
                }
            }
        }

        if (nextEventTime == null) {
            if (mBinding.nextEventInfo != null) {
                mBinding.nextEventInfo.setVisibility(GONE);
            }
            return;
        }

        String eventName = com.qwibbler.salarm.namaz.RelationalRule.getEventName(this, nextEvent);

        long remainingMillis = nextEventTime.getTime() - now;

        if (mBinding.nextEventInfo == null) return;

        if ("time".equals(mShowNextEventInfo)) {
            String formattedTime = android.text.format.DateFormat.getTimeFormat(this).format(nextEventTime);
            mBinding.nextEventInfo.setText(String.format(Locale.getDefault(),
                getString(R.string.namaz_next_event_info_time_format), eventName, formattedTime));
        } else {
            // countdown
            long remainingSeconds = (remainingMillis + 999) / 1000;
            int hours = (int) (remainingSeconds / 3600);
            int minutes = (int) ((remainingSeconds % 3600) / 60);
            int seconds = (int) (remainingSeconds % 60);

            String countdown;
            if (hours > 0) {
                countdown = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                countdown = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            }

            mBinding.nextEventInfo.setText(String.format(Locale.getDefault(),
                getString(R.string.namaz_next_event_info_countdown_format), countdown, eventName));

            // Update again in a second
            mHandler.postDelayed(this::updateNextEventInfo, 1000);
        }
    }

    private java.util.Date getEventTime(com.qwibbler.salarm.namaz.RelationalRule.AnchorEvent event,
                                        com.qwibbler.salarm.namaz.NamazCalculator.NamazTimes times) {
        return switch (event) {
            case FAJR -> times.fajr;
            case SUNRISE -> times.sunrise;
            case DHUHR -> times.dhuhr;
            case ASR_SHAFI -> times.asrShafi;
            case ASR_HANAFI -> times.asrHanafi;
            case MAGHRIB -> times.maghrib;
            case ISHA -> times.isha;
        };
    }

    /**
     * Display ringtone title if enabled in <i>"Customize alarm display"</i> settings.
     */
    private void displayRingtoneTitle() {
        final boolean silent = RingtoneUtils.RINGTONE_SILENT.equals(mAlarmInstance.mRingtone);
        final String title = DataModel.getDataModel().getRingtoneTitle(mAlarmInstance.mRingtone);
        final Drawable musicIcon = silent
            ? AppCompatResources.getDrawable(this, R.drawable.ic_ringtone_silent)
            : AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        int ringtoneIconSize = (int) dpToPx(24, getResources().getDisplayMetrics());
        final int ringtoneTitleColor = SettingsDAO.getRingtoneTitleColor(mPrefs, mIsNamaz);

        if (musicIcon != null) {
            musicIcon.setTint(ringtoneTitleColor);

            if (mIsTextShadowDisplayed) {
                // Convert the drawable to a bitmap
                Bitmap iconBitmap = Bitmap.createBitmap(ringtoneIconSize, ringtoneIconSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);
                musicIcon.setBounds(0, 0, ringtoneIconSize, ringtoneIconSize);
                musicIcon.draw(iconCanvas);

                // Create the alpha mask for the shadow
                Bitmap shadowBitmap = iconBitmap.extractAlpha();
                Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shadowPaint.setColor(mShadowColor);
                shadowPaint.setMaskFilter(new BlurMaskFilter(mShadowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL));

                // Create the final bitmap with space for the shadow
                int finalWidth = ringtoneIconSize + mShadowOffset;
                int finalHeight = ringtoneIconSize + mShadowOffset;
                Bitmap finalBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
                Canvas finalCanvas = new Canvas(finalBitmap);

                // Draw the blurred shadow with an offset
                finalCanvas.drawBitmap(shadowBitmap, mShadowOffset, mShadowOffset, shadowPaint);

                // Draw the normal icon on top
                finalCanvas.drawBitmap(iconBitmap, 0, 0, null);

                // Apply the result to the ImageView
                mBinding.ringtoneIcon.setImageBitmap(finalBitmap);

                mBinding.ringtoneTitle.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            } else {
                mBinding.ringtoneIcon.setImageDrawable(musicIcon);
            }
        }

        mBinding.ringtoneTitle.setText(title);
        mBinding.ringtoneTitle.setTypeface(mGeneralBoldTypeface);
        mBinding.ringtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mBinding.ringtoneTitle.setSelected(true);
    }

    /**
     * Display a message after snoozing or dismissing the alarm.
     */
    private void displayAlarmActionMessage(final int titleResId, final String descriptionText, final String accessibilityText) {
        if (SettingsDAO.isAlarmActionMessageHidden(mPrefs, mIsNamaz)) {
            finish();
            return;
        }

        mBinding.contentView.setVisibility(GONE);

        mBinding.actionMessageView.setVisibility(VISIBLE);

        mBinding.actionTitle.setText(titleResId);
        mBinding.actionTitle.setTypeface(mGeneralBoldTypeface);
        mBinding.actionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        mBinding.actionTitle.setTextColor(mAlarmTitleColor);

        if (descriptionText != null) {
            mBinding.actionDescription.setVisibility(VISIBLE);
            mBinding.actionDescription.setTypeface(mGeneralBoldTypeface);
            mBinding.actionDescription.setText(descriptionText);
            mBinding.actionDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
            mBinding.actionDescription.setTextColor(mAlarmTitleColor);
        }

        mBinding.actionMessageView.setAlpha(0f);
        mBinding.actionMessageView.animate()
            .alpha(1f)
            .setDuration(ALERT_REVEAL_DURATION_MILLIS)
            .withEndAction(() -> {
                ViewCompat.setStateDescription(mBinding.actionMessageView, accessibilityText);
                mHandler.postDelayed(this::finish, ALERT_DISMISS_DELAY_MILLIS);
            })
            .start();
    }
}
