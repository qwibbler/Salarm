/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEBUG_LANGUAGE_CODE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_SYSTEM_LANGUAGE_CODE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.VIBRATION_PATTERN_ESCALATING;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.VIBRATION_PATTERN_HEARTBEAT;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.VIBRATION_PATTERN_SOFT;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.VIBRATION_PATTERN_STRONG;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.VIBRATION_PATTERN_TICK_TOCK;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.os.LocaleListCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Function;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.qwibbler.salarm.BuildConfig;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.uicomponents.CustomDialog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the service
     * @param intent  an Intent describing the service to be started
     * @return a PendingIntent that will start a service
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent) {
        return PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Creates and returns a {@link PendingIntent} to start a service, using a specific
     * {@code requestCode} to distinguish intents.
     *
     * @param context     the Context in which the PendingIntent should start the service
     * @param intent      an Intent describing the service to be started
     * @param requestCode a unique identifier to differentiate between multiple PendingIntents
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent, int requestCode) {
        return PendingIntent.getService(context, requestCode, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the activity
     * @param intent  an Intent describing the activity to be started
     * @return a PendingIntent that will start an activity
     */
    public static PendingIntent pendingActivityIntent(Context context, Intent intent) {
        // explicitly set the flag here, as getActivity() documentation states we must do so
        return PendingIntent.getActivity(context, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Convenience method to stop a service.
     *
     * @param context The context required to stop the service.
     */
    public static void stopService(Context context, Class<?> cls) {
        Intent serviceIntent = new Intent(context, cls);
        context.stopService(serviceIntent);
    }

    /**
     * Displays a {@link DialogFragment} only if it is not already shown.
     *
     * <p>This method checks whether a fragment with the given tag is already
     * present in the {@link FragmentManager}. If so, the call is ignored to prevent
     * opening the same dialog multiple times (e.g., due to fast repeated clicks).</p>
     *
     * @param manager  the FragmentManager used to display the dialog
     * @param fragment the DialogFragment instance to show
     * @param tag      the unique tag identifying this dialog in the FragmentManager
     */
    public static void showDialogFragment(FragmentManager manager, DialogFragment fragment, String tag) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final Fragment existing = manager.findFragmentByTag(tag);
        // Prevents the same dialog from being opened twice.
        if (existing != null) {
            return;
        }

        final FragmentTransaction tx = manager.beginTransaction();

        tx.addToBackStack(null);
        fragment.show(tx, tag);
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    /**
     * @return The specific string resource ID corresponding to the active build type.
     */
    @StringRes
    public static int getStringResByBuildType(@StringRes int resId, @StringRes int resDebugId, @StringRes int resNightlyId) {
        if (BuildConfig.IS_DEBUG_BUILD) {
            return resDebugId;
        } else if (BuildConfig.IS_NIGHTLY_BUILD) {
            return resNightlyId;
        } else {
            return resId;
        }
    }

    /**
     * Creates and returns a {@link Context} configured with the user's preferred Locale.
     *
     * <p>If the user selected the system default language, the system Locale is used.
     * Otherwise, a Locale is built from the stored custom language code.</p>
     *
     * @param context The base context used to read preferences and resources.
     * @return A new Context whose configuration applies the selected Locale.
     */
    @SuppressLint("AppBundleLocaleChanges")
    public static Context getLocalizedContext(Context context) {
        Locale locale = null;

        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();

        if (appLocales.isEmpty()) {
            String customLanguageCode = SettingsDAO.getLanguageCode(getDefaultSharedPreferences(context));
            if (customLanguageCode != null
                && !DEFAULT_SYSTEM_LANGUAGE_CODE.equals(customLanguageCode)
                && !customLanguageCode.isEmpty()) {
                locale = Locale.forLanguageTag(customLanguageCode);
            }
        } else {
            locale = appLocales.get(0);
        }

        if (locale == null) {
            return context;
        }

        // Normalize legacy JVM language codes to modern ISO standards
        String langOnly = locale.getLanguage();
        String normalizedLang = switch (langOnly) {
            case "iw" -> "he";
            case "in" -> "id";
            case "ji" -> "yi";
            default -> langOnly;
        };

        // Rebuild the Locale object only if the language code was mutated by the JVM
        if (!normalizedLang.equals(langOnly)) {
            locale = new Locale.Builder()
                .setLocale(locale)
                .setLanguage(normalizedLang)
                .build();
        }

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Applies the application's language settings during an app reset or backup restore.
     *
     * <p>If the app is being reset, it applies the default language (or a specific language for debug/nightly builds).
     * If the app is being restored, it reads the saved language from shared preferences and applies it.</p>
     *
     * @param context        the context used to access shared preferences
     * @param isResettingApp true if the app is resetting to default settings, false if restoring from a backup
     */
    public static void applyAppLanguage(Context context, boolean isResettingApp) {
        if (isResettingApp) {
            if (BuildConfig.IS_DEBUG_BUILD || BuildConfig.IS_NIGHTLY_BUILD) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEBUG_LANGUAGE_CODE));
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            }
        } else {
            SharedPreferences prefs = getDefaultSharedPreferences(context);
            String customLanguageCode = SettingsDAO.getLanguageCode(prefs);

            if (customLanguageCode.equals(DEFAULT_SYSTEM_LANGUAGE_CODE)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(customLanguageCode));
            }
        }
    }

    /**
     * Returns a context that points to device-protected storage on Android 7+,
     * or the regular context on older versions.
     *
     * @param context the base context
     * @return a context suitable for accessing device-protected storage
     */
    public static Context getSafeStorageContext(Context context) {
        return SdkUtils.isAtLeastAndroid7()
            ? context.createDeviceProtectedStorageContext()
            : context;
    }

    /**
     * Set the vibration duration if the device is equipped with a vibrator and if vibration is enabled in the settings.
     *
     * @param context      to define whether the device is equipped with a vibrator.
     * @param milliseconds Hours to display (if any)
     */
    public static void setVibrationTime(Context context, long milliseconds) {
        final boolean isVibrationsEnabled = SettingsDAO.isVibrationsEnabled(getDefaultSharedPreferences(context));
        final Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (isVibrationsEnabled) {
            if (SdkUtils.isAtLeastAndroid8()) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //noinspection deprecation
                vibrator.vibrate(milliseconds);
            }
        }
    }

    /**
     * Triggers haptic feedback if system or app settings allow it.
     *
     * @param view             The view from which the action is triggered.
     * @param feedbackConstant The constant of type {@link android.view.HapticFeedbackConstants}.
     */
    public static void performHapticFeedback(View view, int feedbackConstant) {
        if (view != null && SettingsDAO.isVibrationsEnabled(getDefaultSharedPreferences(view.getContext()))) {
            view.performHapticFeedback(feedbackConstant);
        }
    }

    /**
     * Hides the soft keyboard and clears focus from the given view.
     */
    public static void hideKeyboardAndClearFocus(View view) {
        if (view == null) {
            return;
        }

        // Move focus to the root view to avoid it bouncing back to an EditText
        View root = view.getRootView();
        if (root != null) {
            root.setFocusableInTouchMode(true);
            root.requestFocus();
        }

        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Set up a window callback to hide the soft keyboard and clear focus when tapping
     * outside of an EditText or starting to scroll.
     *
     * @param window The window to set up the callback for.
     */
    public static void setupTapToDismissKeyboard(@NonNull final Window window) {
        final Window.Callback original = window.getCallback();
        if (original == null) return;

        // Avoid wrapping multiple times
        if (Proxy.isProxyClass(original.getClass()) && Proxy.getInvocationHandler(original) instanceof TouchDismissHandler) {
            return;
        }

        // Make decor view focusable so it can take focus away from EditTexts
        View decorView = window.getDecorView();
        decorView.setFocusableInTouchMode(true);

        window.setCallback((Window.Callback) Proxy.newProxyInstance(
            Window.Callback.class.getClassLoader(),
            new Class[]{Window.Callback.class},
            new TouchDismissHandler(window, original)
        ));
    }

    /**
     * Set up a window callback to hide the soft keyboard and clear focus when tapping
     * outside of an EditText or starting to scroll.
     *
     * @param activity The activity whose window to set up the callback for.
     */
    public static void setupTapToDismissKeyboard(@NonNull Activity activity) {
        setupTapToDismissKeyboard(activity.getWindow());
    }

    private static class TouchDismissHandler implements InvocationHandler {
        private final Window mWindow;
        private final Window.Callback mOriginal;
        private final View mDecorView;
        private ViewTreeObserver.OnScrollChangedListener mScrollListener;

        TouchDismissHandler(Window window, Window.Callback original) {
            mWindow = window;
            mOriginal = original;
            mDecorView = window.getDecorView();

            // Handle scroll dismissal globally for this window
            mScrollListener = () -> {
                View v = mWindow.getCurrentFocus();
                if (v instanceof EditText) {
                    hideKeyboardAndClearFocus(v);
                }
            };
            mDecorView.getViewTreeObserver().addOnScrollChangedListener(mScrollListener);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("dispatchTouchEvent") && args != null && args.length > 0 && args[0] instanceof MotionEvent event) {
                handleTouchEvent(event);
            } else if (methodName.equals("onDetachedFromWindow")) {
                cleanup();
            }

            try {
                return method.invoke(mOriginal, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause != null) throw cause;
                throw e;
            }
        }

        private void cleanup() {
            if (mScrollListener != null) {
                if (mDecorView.getViewTreeObserver().isAlive()) {
                    mDecorView.getViewTreeObserver().removeOnScrollChangedListener(mScrollListener);
                }
                mScrollListener = null;
            }
        }

        private void handleTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = mWindow.getCurrentFocus();
                if (v instanceof EditText) {
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    float x = event.getRawX();
                    float y = event.getRawY();

                    if (x < location[0] || x > location[0] + v.getWidth() ||
                        y < location[1] || y > location[1] + v.getHeight()) {

                        // Check if the tap is going to another EditText to avoid flicker
                        View target = findViewAt(mWindow.getDecorView(), (int) x, (int) y);
                        if (!(target instanceof EditText)) {
                            hideKeyboardAndClearFocus(v);
                        }
                    }
                }
            }
        }

        private View findViewAt(View root, int x, int y) {
            if (root instanceof ViewGroup group) {
                for (int i = group.getChildCount() - 1; i >= 0; i--) {
                    View child = group.getChildAt(i);
                    if (child.getVisibility() != View.VISIBLE) continue;

                    int[] location = new int[2];
                    child.getLocationOnScreen(location);
                    if (x >= location[0] && x <= location[0] + child.getWidth() &&
                        y >= location[1] && y <= location[1] + child.getHeight()) {
                        View descendant = findViewAt(child, x, y);
                        return descendant != null ? descendant : child;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Returns the vibration pattern associated with the given pattern key.
     *
     * @param patternKey the key identifying the vibration pattern
     * @return a long array representing the vibration pattern durations in milliseconds;
     * if the pattern key is unknown, returns a default vibration pattern
     */
    public static long[] getVibrationPatternForKey(String patternKey) {
        return switch (patternKey) {
            case VIBRATION_PATTERN_SOFT -> new long[]{500, 200, 500};
            case VIBRATION_PATTERN_STRONG -> new long[]{500, 1000};
            case VIBRATION_PATTERN_HEARTBEAT -> new long[]{100, 100, 300, 100, 600};
            case VIBRATION_PATTERN_ESCALATING -> new long[]{500, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
            case VIBRATION_PATTERN_TICK_TOCK -> new long[]{300, 150, 300, 150};
            default -> new long[]{500, 500};
        };
    }

    /**
     * Initializes a cache map holding the current values of the given preferences.
     * <p>
     * This cache is used to compare old and new values during preference changes,
     * preventing unnecessary actions if the value has not actually changed.</p>
     *
     * @param keys   List of preference keys to retrieve and cache.
     * @param getter Function that returns the value for a given key.
     * @return A map of preference keys to their current values.
     */
    public static Map<String, Object> initCachedValues(List<String> keys, Function<String, Object> getter) {
        Map<String, Object> cached = new HashMap<>();
        for (String key : keys) {
            cached.put(key, getter.apply(key));
        }
        return cached;
    }

    /**
     * @return a dialog related to Google's announcement about app development.
     *
     * <p>Note: Clicking the "OK" button will no longer display this dialog box.</p>
     */
    public static AlertDialog displayKeepAndroidOpenDialog(Context context, SharedPreferences prefs, boolean isCancelable) {
        Spanned message = HtmlCompat.fromHtml(context.getString(R.string.keep_android_open_message_italic)
                + context.getString(R.string.keep_android_open_message), HtmlCompat.FROM_HTML_MODE_LEGACY);

        AlertDialog dialog = CustomDialog.create(
            context,
            null,
            AppCompatResources.getDrawable(context, R.drawable.ic_about_article),
            context.getString(R.string.keep_android_open_title),
            message,
            null,
            context.getString(android.R.string.ok),
            (d, w) -> {
                if (prefs.getBoolean(KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG, true)) {
                    prefs.edit().putBoolean(KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG, false).apply();
                }

                d.dismiss();
            },
            context.getString(R.string.keep_android_open_more_info_button),
            null,
            context.getString(R.string.keep_android_open_solution_button),
            null,
            alertDialog -> {
                Button moreInfoButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (moreInfoButton != null) {
                    moreInfoButton.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://keepandroidopen.org")))
                    );
                }

                Button solutionButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (solutionButton != null) {
                    solutionButton.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions")))
                    );
                }
            },
            CustomDialog.SoftInputMode.NONE);

        dialog.setCancelable(isCancelable);

        return dialog;
    }

    /**
     * Builds the default label for a timer based on its duration.
     *
     * @param context the context to access resources.
     * @param millis  the timer duration in milliseconds.
     * @return the timer's default label.
     */
    @SuppressWarnings("SizeReplaceableByIsEmpty")
    public static String buildDefaultTimerLabel(Context context, long millis) {
        long seconds = (millis / DateUtils.SECOND_IN_MILLIS) % 60;
        long minutes = (millis / DateUtils.MINUTE_IN_MILLIS) % 60;
        long hours = millis / DateUtils.HOUR_IN_MILLIS;

        StringBuilder timeBuilder = new StringBuilder();

        if (hours > 0) {
            timeBuilder.append(hours).append(context.getString(R.string.hours_label));
        }

        if (minutes > 0) {
            if (timeBuilder.length() > 0) {
                timeBuilder.append(" ");
            }

            timeBuilder.append(minutes).append(context.getString(R.string.minutes_label));
        }

        if (seconds > 0) {
            if (timeBuilder.length() > 0) {
                timeBuilder.append(" ");
            }

            timeBuilder.append(seconds).append(context.getString(R.string.seconds_label));
        }

        if (timeBuilder.length() == 0) {
            timeBuilder.append("0").append(context.getString(R.string.seconds_label));
        }

        return context.getString(R.string.timer_label_default, timeBuilder.toString());
    }

    /**
     * Checks if the user is pressing inside the timer circle or the stopwatch circle.
     */
    public static final class CircleTouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int actionMasked = event.getActionMasked();
            if (actionMasked != MotionEvent.ACTION_DOWN) {
                return false;
            }
            final float rX = view.getWidth() / 2f;
            final float rY = (view.getHeight() - view.getPaddingBottom()) / 2f;
            final float r = Math.min(rX, rY);

            final float x = event.getX() - rX;
            final float y = event.getY() - rY;

            final boolean inCircle = Math.pow(x / r, 2.0) + Math.pow(y / r, 2.0) <= 1.0;

            // Consume the event if it is outside the circle
            return !inCircle;
        }
    }

}
