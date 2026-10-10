/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;
import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_TAB_TITLE_VISIBILITY;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TAB_ANIMATION_CUBE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TAB_ANIMATION_DEPTH;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TAB_ANIMATION_FLIP;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TAB_ANIMATION_GATE;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TAB_ANIMATION_ZOOM_OUT;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.TAB_TITLE_VISIBILITY_NEVER;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;
import static com.qwibbler.salarm.utils.AnimatorUtils.getScaleAnimator;
import static com.qwibbler.salarm.utils.NotificationUtils.EXTRA_UPDATE_ALARM_NOTIFICATIONS;
import static com.qwibbler.salarm.utils.WidgetUtils.EXTRA_UPDATE_WIDGETS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.qwibbler.salarm.alarms.AlarmFragment;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.BaseActivity;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.base.KeepAliveService;
import com.qwibbler.salarm.base.RunnableFragment;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.DataModel.SilentSetting;
import com.qwibbler.salarm.data.OnSilentSettingsListener;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.SalarmActivityBinding;
import com.qwibbler.salarm.events.Events;
import com.qwibbler.salarm.salah.SalahLocationManager;
import com.qwibbler.salarm.settings.PermissionsManagementActivity;
import com.qwibbler.salarm.settings.SettingsActivity;
import com.qwibbler.salarm.setup.FirstLaunch;
import com.qwibbler.salarm.stopwatch.StopwatchFragment;
import com.qwibbler.salarm.stopwatch.StopwatchService;
import com.qwibbler.salarm.timer.TimerService;
import com.qwibbler.salarm.uicomponents.CustomTooltip;
import com.qwibbler.salarm.uicomponents.FabContainer;
import com.qwibbler.salarm.uicomponents.FragmentTabPagerAdapter;
import com.qwibbler.salarm.uicomponents.pagetransformers.CubePageTransformer;
import com.qwibbler.salarm.uicomponents.pagetransformers.DepthPageTransformer;
import com.qwibbler.salarm.uicomponents.pagetransformers.FlipPageTransformer;
import com.qwibbler.salarm.uicomponents.pagetransformers.GatePageTransformer;
import com.qwibbler.salarm.uicomponents.pagetransformers.ZoomOutPageTransformer;
import com.qwibbler.salarm.uicomponents.toast.SnackbarManager;
import com.qwibbler.salarm.uidata.TabListener;
import com.qwibbler.salarm.uidata.UiDataModel;
import com.qwibbler.salarm.utils.InsetsUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.NotificationUtils;
import com.qwibbler.salarm.utils.PermissionUtils;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;
import com.qwibbler.salarm.utils.WidgetUtils;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;

/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and stopwatch.
 */
public class SalarmActivity extends BaseActivity implements FabContainer {

    private SalarmActivityBinding mBinding;

    private SharedPreferences mPrefs;
    private Typeface mRegularTypeface;
    private String mFontPath;
    private boolean mIsToolBarDisplayed;
    private long mLastFabClickTime = 0;
    private static final long MIN_CLICK_INTERVAL = 500;

    private int mBottomNavPage;

    private AlertDialog mKeepAndroidOpenDialog = null;

    /**
     * Shrinks the FAB, left FAB and right FAB to nothing.
     */
    private final AnimatorSet mHideAnimation = new AnimatorSet();

    /**
     * Grows the FAB, left FAB and right FAB to natural sizes.
     */
    private final AnimatorSet mShowAnimation = new AnimatorSet();

    /**
     * Hides, updates, and shows only the FAB; the buttons are untouched.
     */
    private final AnimatorSet mUpdateFabOnlyAnimation = new AnimatorSet();

    /**
     * Hides, updates, and shows only the left FAB and right FAB.
     */
    private final AnimatorSet mUpdateButtonsOnlyAnimation = new AnimatorSet();

    /**
     * Automatically starts the {@link #mShowAnimation} after {@link #mHideAnimation} ends.
     */
    private final AnimatorListenerAdapter mAutoStartShowListener = new AutoStartShowListener();

    /**
     * Updates the user interface to reflect the selected tab from the backing model.
     */
    private final TabListener mTabChangeWatcher = new TabChangeWatcher();

    /**
     * Shows/hides a snackbar explaining which setting is suppressing alarms from firing.
     */
    private final OnSilentSettingsListener mSilentSettingChangeWatcher = new SilentSettingChangeWatcher();

    /**
     * Displays a snackbar explaining why alarms may not fire or may fire silently.
     */
    private Runnable mShowSilentSettingSnackbarRunnable;

    /**
     * The current display state of the FAB.
     */
    private FabState mFabState = FabState.SHOWING;

    /**
     * Generates the fragments that are displayed by the FragmentTabPager.
     */
    private FragmentTabPagerAdapter mFragmentTabPagerAdapter;

    /**
     * {@code true} when a settings change necessitates recreating this activity.
     */
    private boolean mShouldRecreate = false;

    /**
     * List of supported preference keys used to monitor UI and behavior changes within
     * {@link #registerPrefListener()}.
     *
     * <p>This ensures that only relevant keys are tracked to optimize performance and avoid
     * unnecessary activity recreation.</p>
     */
    private static final List<String> SUPPORTED_PREF_KEYS = List.of(
        // Interface
        KEY_TOOLBAR_TITLE, KEY_TAB_TITLE_VISIBILITY, KEY_TAB_INDICATOR, KEY_TAB_TO_DISPLAY, KEY_TAB_ANIMATION, KEY_VIBRATIONS,
        KEY_KEEP_SCREEN_ON, KEY_VISIBLE_TABS,
        // Permission
        KEY_ESSENTIAL_PERMISSIONS_GRANTED
    );

    /**
     * Listener registered to observe changes in relevant app settings.
     * Used to trigger activity recreation only when relevant values actually change.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    @Override
    public void onNewIntent(@NonNull Intent newIntent) {
        super.onNewIntent(newIntent);

        // Fragments may query the latest intent for information, so update the intent.
        setIntent(newIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra(EXTRA_UPDATE_WIDGETS, false)) {
                WidgetUtils.updateAllWidgets(this);
                intent.removeExtra(EXTRA_UPDATE_WIDGETS);
            }

            if (intent.getBooleanExtra(EXTRA_UPDATE_ALARM_NOTIFICATIONS, false)) {
                NotificationUtils.updateAlarmNotifications(this);
                intent.removeExtra(EXTRA_UPDATE_ALARM_NOTIFICATIONS);
            }
        }

        mBinding = SalarmActivityBinding.inflate(getLayoutInflater());

        mPrefs = getDefaultSharedPreferences(this);

        if (isFirstLaunch()) {
            return;
        }

        mFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mIsToolBarDisplayed = SettingsDAO.isToolbarTitleDisplayed(mPrefs);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ThemeUtils.allowDisplayCutout(getWindow());

        setContentView(mBinding.getRoot());

        applyWindowInsets();

        configureViewPager();

        configureFabAndButtons();

        UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        int tabIndex = UiDataModel.getUiDataModel().getTabIndex(selectedTab);
        mBottomNavPage = (tabIndex <= 3) ? 1 : 2;

        displayKeepAndroidOpenDialogIfUnread();

        registerPrefListener();

        AppExecutors.getDiskIO().execute(() -> {
            mRegularTypeface = ThemeUtils.loadFont(mFontPath);
            ThemeUtils.boldTypeface(mFontPath);
            ThemeUtils.loadFont(SettingsDAO.getDigitalClockFont(mPrefs));
            ThemeUtils.loadFont(SettingsDAO.getTimerDurationFont(mPrefs));
            ThemeUtils.boldTypeface(SettingsDAO.getAlarmFont(mPrefs));
            ThemeUtils.loadFont(SettingsDAO.getStopwatchFont(mPrefs));

            AppExecutors.getMainThread().post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                configureToolbar();

                configureBottomNavigationView();

                applyBottomNavTooltips();
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        DataModel.getDataModel().addSilentSettingsListener(mSilentSettingChangeWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Remember the current tab
        UiDataModel.Tab oldTab = UiDataModel.getUiDataModel().getSelectedTab();

        // Remove the listener to prevent the ViewPager from crashing when the tabs visibility has been changed
        UiDataModel.getUiDataModel().removeTabListener(mTabChangeWatcher);

        final boolean tabsChanged = UiDataModel.getUiDataModel().updateActiveTabs();

        // Clear the fragment cache and notify the ViewPager if the tabs visibility has been changed
        if (tabsChanged) {
            mFragmentTabPagerAdapter.clearCache();
            mFragmentTabPagerAdapter.notifyDataSetChanged();

            // Re-calculate the current page based on the new visible tabs count
            UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
            int tabIndex = UiDataModel.getUiDataModel().getTabIndex(selectedTab);
            mBottomNavPage = (tabIndex <= 3) ? 1 : 2;

            mBinding.salarmActivityPager.setPageTransformer(false, null);
            updatePageTransformer();
        }

        if (mShouldRecreate) {
            mShouldRecreate = false;
            recreate();
            return;
        }

        refreshTabsVisibility(oldTab, tabsChanged);

        showTabFromNotifications();

        updateKeepScreenOn();

        if (SettingsDAO.isForegroundServiceEnabled(mPrefs)) {
            ContextCompat.startForegroundService(this, new Intent(this, KeepAliveService.class));
        }

        if (SalahLocationManager.getLocationMode(this) == SalahLocationManager.LocationMode.GPS) {
            SalahLocationManager.requestGpsUpdate(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        UiDataModel.getUiDataModel().removeTabListener(mTabChangeWatcher);
    }

    @Override
    protected void onStop() {
        DataModel.getDataModel().removeSilentSettingsListener(mSilentSettingChangeWatcher);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterPrefListener();

        if (mKeepAndroidOpenDialog != null && mKeepAndroidOpenDialog.isShowing()) {
            mKeepAndroidOpenDialog.dismiss();
            mKeepAndroidOpenDialog = null;
        }

        super.onDestroy();
    }

    @SuppressLint("AlwaysShowAction")
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        menu.add(0, Menu.NONE, 1, R.string.settings).setIcon(R.drawable.ic_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (PermissionUtils.areEssentialPermissionsNotGranted(this)) {
            final Drawable warningIcon = AppCompatResources.getDrawable(this, R.drawable.ic_error);
            if (warningIcon != null) {
                DrawableCompat.setTint(warningIcon, ContextCompat.getColor(this, R.color.colorAlert));
            }
            menu.add(0, Menu.FIRST, 0, R.string.denied_permission_label).setIcon(warningIcon).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        mBinding.toolbar.post(() -> ThemeUtils.applyToolbarTooltips(mBinding.toolbar));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 0) {
            final Intent settingIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingIntent);
            return true;
        } else if (item.getItemId() == 1) {
            final Intent permissionManagementIntent = new Intent(this, PermissionsManagementActivity.class);
            startActivity(permissionManagementIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Listens for keyboard activity for the tab fragments to handle if necessary. A tab may want to
     * respond to key presses even if they are not currently focused.
     */
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        return getSelectedSalarmFragment().onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        return getSelectedSalarmFragment().onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public void updateFab(@UpdateFabFlag int updateType) {
        final SalarmFragment f = getSelectedSalarmFragment();

        if (!f.isAdded() || f.getContext() == null) {
            return;
        }

        mBinding.fab.setOnClickListener(view -> f.onFabClick());

        switch (updateType & FAB_ANIMATION_MASK) {
            case FAB_SHRINK_AND_EXPAND -> mUpdateFabOnlyAnimation.start();
            case FAB_IMMEDIATE -> f.onUpdateFab(mBinding.fab);
            case FAB_MORPH -> f.onMorphFab(mBinding.fab);
        }
        if ((updateType & FAB_REQUEST_FOCUS_MASK) == FAB_REQUEST_FOCUS) {
            mBinding.fab.requestFocus();
        }
        switch (updateType & BUTTONS_ANIMATION_MASK) {
            case BUTTONS_IMMEDIATE -> f.onUpdateFabButtons(mBinding.leftButton, mBinding.rightButton);
            case BUTTONS_SHRINK_AND_EXPAND -> mUpdateButtonsOnlyAnimation.start();
        }
        if ((updateType & BUTTONS_DISABLE_MASK) == BUTTONS_DISABLE) {
            mBinding.leftButton.setClickable(false);
            mBinding.rightButton.setClickable(false);
        }
        switch (updateType & FAB_AND_BUTTONS_SHRINK_EXPAND_MASK) {
            case FAB_AND_BUTTONS_SHRINK -> mHideAnimation.start();
            case FAB_AND_BUTTONS_EXPAND -> mShowAnimation.start();
        }
    }

    /**
     * Check if this is the first time the application has been launched.
     */
    private boolean isFirstLaunch() {
        final boolean isFirstRun = mPrefs.getBoolean(KEY_IS_FIRST_LAUNCH, true);
        if (isFirstRun) {
            startActivity(new Intent(this, FirstLaunch.class));
            finish();
            return true;
        }
        return false;
    }

    /**
     * Display dialog related to Google's announcement about app development if unread.
     *
     * <p>Note: Clicking the "OK" button will no longer display this dialog box.</p>
     */
    private void displayKeepAndroidOpenDialogIfUnread() {
        if (!mPrefs.getBoolean(KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG, true)) {
            return;
        }

        mKeepAndroidOpenDialog = Utils.displayKeepAndroidOpenDialog(this, mPrefs, false);

        mKeepAndroidOpenDialog.show();
    }

    /**
     * Registers a SharedPreferences listener to monitor relevant app settings.
     *
     * <p>When a supported preference changes and its value is different from the cached one,
     * the activity is recreated to reflect the updated setting.</p>
     */
    private void registerPrefListener() {
        // Important: we use a cached map of preference values to avoid unnecessary activity recreation
        // when initializing the preference screen.
        // Without this check, any preference change (even with the same value) triggers recreate(),
        // which causes significant slowdown when opening the settings screen,
        // especially on low-end devices due to how Preferences are initialized.
        Map<String, Object> cachedValues = Utils.initCachedValues(SUPPORTED_PREF_KEYS, this::getPreferenceValue);

        mPrefListener = (sharedPreferences, key) -> {
            if (key == null || !cachedValues.containsKey(key)) {
                return;
            }

            Object oldValue = cachedValues.get(key);
            Object newValue = getPreferenceValue(key);

            boolean changed = (newValue == null && oldValue != null) || (newValue != null && !newValue.equals(oldValue));

            if (!changed) {
                return;
            }

            cachedValues.put(key, newValue);

            switch (key) {
                case KEY_TOOLBAR_TITLE, KEY_TAB_TITLE_VISIBILITY, KEY_TAB_INDICATOR, KEY_TAB_TO_DISPLAY, KEY_TAB_ANIMATION, KEY_VIBRATIONS,
                     KEY_KEEP_SCREEN_ON, KEY_VISIBLE_TABS, KEY_ESSENTIAL_PERMISSIONS_GRANTED -> mShouldRecreate = true;

            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    /**
     * Unregisters the internal listener to avoid memory leaks.
     */
    private void unregisterPrefListener() {
        if (mPrefListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        }
    }

    /**
     * Retrieves the value of the preference associated with the given key from SharedPreferences,
     * returning a suitable default value based on the key.
     *
     * @param key The preference key to retrieve.
     */
    private Object getPreferenceValue(String key) {
        return switch (key) {
            // Interface
            case KEY_TOOLBAR_TITLE -> SettingsDAO.isToolbarTitleDisplayed(mPrefs);
            case KEY_TAB_TITLE_VISIBILITY -> SettingsDAO.getTabTitleVisibility(mPrefs);
            case KEY_TAB_INDICATOR -> SettingsDAO.isTabIndicatorDisplayed(mPrefs);
            case KEY_TAB_TO_DISPLAY -> SettingsDAO.getTabToDisplay(mPrefs);
            case KEY_TAB_ANIMATION -> SettingsDAO.getTabAnimation(mPrefs);
            case KEY_VIBRATIONS -> SettingsDAO.isVibrationsEnabled(mPrefs);
            case KEY_KEEP_SCREEN_ON -> SettingsDAO.shouldScreenRemainOn(mPrefs);
            // Permission
            case KEY_ESSENTIAL_PERMISSIONS_GRANTED -> mPrefs.getBoolean(key, false);

            default -> null;
        };
    }

    private final NavigationBarView.OnItemSelectedListener mNavigationListener = item -> {
        UiDataModel.Tab tab = null;
        int itemId = item.getItemId();
        if (itemId == R.id.page_alarm) {
            tab = UiDataModel.Tab.ALARMS;
        } else if (itemId == R.id.page_clock) {
            tab = UiDataModel.Tab.CLOCKS;
        } else if (itemId == R.id.page_timer) {
            tab = UiDataModel.Tab.TIMERS;
        } else if (itemId == R.id.page_stopwatch) {
            tab = UiDataModel.Tab.STOPWATCH;
        } else if (itemId == R.id.page_salah) {
            tab = UiDataModel.Tab.SALAH;
        } else if (itemId == R.id.page_qibla) {
            tab = UiDataModel.Tab.QIBLA;
        } else if (itemId == R.id.page_masjid) {
            tab = UiDataModel.Tab.MASJID;
        } else if (itemId == R.id.page_next) {
            setBottomNavPage(2);
            return false;
        } else if (itemId == R.id.page_back) {
            setBottomNavPage(1);
            return false;
        }

        if (tab != null) {
            UiDataModel.getUiDataModel().setSelectedTab(tab);
            return true;
        }

        return false;
    };

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBinding.salarmActivityRootView, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            mBinding.salarmActivityBottomMenu.setPadding(0, 0, 0, bars.bottom);
        });
    }

    public SalarmActivityBinding getSalarmActivityBinding() {
        return mBinding;
    }

    /**
     * Configures the Toolbar.
     */
    private void configureToolbar() {
        setSupportActionBar(mBinding.toolbar);

        if (mIsToolBarDisplayed) {
            ThemeUtils.applyTypeface(mBinding.toolbar, mRegularTypeface);
        }
    }

    /**
     * Configures the buttons shared by the tabs.
     */
    private void configureFabAndButtons() {
        // Configure the buttons shared by the tabs.
        mBinding.fab.setOnClickListener(view -> {
            final SalarmFragment currentFragment = getSelectedSalarmFragment();
            long currentTime = SystemClock.elapsedRealtime();
            final boolean isStopwatch = currentFragment instanceof StopwatchFragment;

            if (isStopwatch || (currentTime - mLastFabClickTime >= MIN_CLICK_INTERVAL)) {
                mLastFabClickTime = currentTime;
                currentFragment.onFabClick();
            } else {
                LogUtils.v("Rapid FAB click ignored for Alarm/Timer to prevent double actions.");
            }
        });

        mBinding.leftButton.setOnLongClickListener(v -> {
            CustomTooltip.showAbove(v, mBinding.leftButton.getContentDescription().toString(), true);
            return true;
        });

        mBinding.rightButton.setOnLongClickListener(v -> {
            CustomTooltip.showAbove(v, mBinding.rightButton.getContentDescription().toString(), true);
            return true;
        });

        final long duration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        final ValueAnimator hideFabAnimation = getScaleAnimator(mBinding.fab, 1f, 0f);
        final ValueAnimator showFabAnimation = getScaleAnimator(mBinding.fab, 0f, 1f);

        final ValueAnimator leftHideAnimation = getScaleAnimator(mBinding.leftButton, 1f, 0f);
        final ValueAnimator rightHideAnimation = getScaleAnimator(mBinding.rightButton, 1f, 0f);
        final ValueAnimator leftShowAnimation = getScaleAnimator(mBinding.leftButton, 0f, 1f);
        final ValueAnimator rightShowAnimation = getScaleAnimator(mBinding.rightButton, 0f, 1f);

        hideFabAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBinding.fab.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBinding.fab.setLayerType(View.LAYER_TYPE_NONE, null);

                getSelectedSalarmFragment().onUpdateFab(mBinding.fab);
            }
        });

        leftHideAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBinding.leftButton.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mBinding.rightButton.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBinding.leftButton.setLayerType(View.LAYER_TYPE_NONE, null);
                mBinding.rightButton.setLayerType(View.LAYER_TYPE_NONE, null);

                getSelectedSalarmFragment().onUpdateFabButtons(mBinding.leftButton, mBinding.rightButton);
            }
        });

        // Build the reusable animations that hide and show the fab and left/right buttons.
        // These may be used independently or be chained together.
        mHideAnimation
            .setDuration(duration)
            .play(hideFabAnimation)
            .with(leftHideAnimation)
            .with(rightHideAnimation);

        mShowAnimation
            .setDuration(duration)
            .play(showFabAnimation)
            .with(leftShowAnimation)
            .with(rightShowAnimation);

        // Build the reusable animation that hides and shows only the fab.
        mUpdateFabOnlyAnimation
            .setDuration(duration)
            .play(showFabAnimation)
            .after(hideFabAnimation);

        // Build the reusable animation that hides and shows only the buttons.
        mUpdateButtonsOnlyAnimation
            .setDuration(duration)
            .play(leftShowAnimation)
            .with(rightShowAnimation)
            .after(leftHideAnimation)
            .after(rightHideAnimation);
    }

    /**
     * Configures the ViewPager.
     */
    private void configureViewPager() {
        // Customize the view pager.
        mFragmentTabPagerAdapter = new FragmentTabPagerAdapter(this);
        mBinding.salarmActivityPager.setAdapter(mFragmentTabPagerAdapter);

        // Set the number of pages to keep in the ViewPager base on the number of tabs displayed.
        int visibleTabsCount = UiDataModel.getUiDataModel().getTabCount();
        int offscreenLimit = Math.max(1, visibleTabsCount - 1);
        if (mBinding.salarmActivityPager.getOffscreenPageLimit() != offscreenLimit) {
            mBinding.salarmActivityPager.setOffscreenPageLimit(offscreenLimit);
        }

        // Set Accessibility Delegate to null so view pager doesn't intercept movements and
        // prevent the fab from being selected.
        mBinding.salarmActivityPager.setAccessibilityDelegate(null);

        // Mirror changes made to the selected page of the view pager into UiDataModel.
        mBinding.salarmActivityPager.addOnPageChangeListener(new PageChangeWatcher());

        // Set the animation when switching tabs.
        updatePageTransformer();
    }

    /**
     * Update the animation when switching tabs.
     */
    private void updatePageTransformer() {
        mBinding.salarmActivityPager.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            String tabAnimation = SettingsDAO.getTabAnimation(mPrefs);
            ViewPager.PageTransformer transformer = switch (tabAnimation) {
                case TAB_ANIMATION_CUBE -> new CubePageTransformer();
                case TAB_ANIMATION_DEPTH -> new DepthPageTransformer();
                case TAB_ANIMATION_FLIP -> new FlipPageTransformer();
                case TAB_ANIMATION_GATE -> new GatePageTransformer();
                case TAB_ANIMATION_ZOOM_OUT -> new ZoomOutPageTransformer();
                default -> null;
            };

            mBinding.salarmActivityPager.setPageTransformer(true, transformer);
        });
    }

    /**
     * Configures the {@link BottomNavigationView}, such as the icon color, text color,
     * text visibility, font update, etc.
     */
    @SuppressLint("RestrictedApi")
    private void configureBottomNavigationView() {
        Menu menu = mBinding.salarmActivityBottomMenu.getMenu();
        menu.clear();

        int totalTabs = UiDataModel.getUiDataModel().getTabCount();
        boolean pagingNeeded = totalTabs > 4;

        if (pagingNeeded) {
            if (mBottomNavPage == 1) {
                for (int i = 0; i < 4; i++) {
                    addTabToMenu(menu, UiDataModel.getUiDataModel().getTabAt(i), i);
                }
                MenuItem nextItem = menu.add(Menu.NONE, R.id.page_next, 4, R.string.menu_next_page)
                        .setIcon(R.drawable.ic_arrow_right);
                nextItem.setCheckable(false);
            } else {
                MenuItem backItem = menu.add(Menu.NONE, R.id.page_back, 0, R.string.menu_previous_page)
                        .setIcon(R.drawable.ic_chevron_left);
                backItem.setCheckable(false);
                for (int i = 4; i < totalTabs; i++) {
                    addTabToMenu(menu, UiDataModel.getUiDataModel().getTabAt(i), i - 3);
                }
            }
        } else {
            for (int i = 0; i < totalTabs; i++) {
                addTabToMenu(menu, UiDataModel.getUiDataModel().getTabAt(i), i);
            }
        }

        mBinding.salarmActivityBottomMenu.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        final int primaryColor = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK);
        final int onBackgroundColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnBackground, Color.BLACK);

        String tabTitleVisibility = SettingsDAO.getTabTitleVisibility(mPrefs);
        final boolean shouldUpdateTypeface = !tabTitleVisibility.equals(TAB_TITLE_VISIBILITY_NEVER) && mFontPath != null;

        if (shouldUpdateTypeface) {
            updateBottomNavTypeface();
        }

        mBinding.salarmActivityBottomMenu.setOnItemSelectedListener(item -> {
            if (shouldUpdateTypeface) {
                mBinding.salarmActivityBottomMenu.post(this::updateBottomNavTypeface);
            }

            return mNavigationListener.onNavigationItemSelected(item);
        });

        mBinding.salarmActivityBottomMenu.setItemActiveIndicatorEnabled(SettingsDAO.isTabIndicatorDisplayed(mPrefs));

        if (tabTitleVisibility.equals(DEFAULT_TAB_TITLE_VISIBILITY)) {
            mBinding.salarmActivityBottomMenu.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        } else if (tabTitleVisibility.equals(TAB_TITLE_VISIBILITY_NEVER)) {
            mBinding.salarmActivityBottomMenu.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        } else {
            mBinding.salarmActivityBottomMenu.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_SELECTED);
        }

        mBinding.salarmActivityBottomMenu.setItemIconTintList(new ColorStateList(
            new int[][]{{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
            new int[]{primaryColor, primaryColor, onBackgroundColor}));

        if (ThemeUtils.isNight(getResources()) && SettingsDAO.getDarkMode(mPrefs).equals(AMOLED_DARK_MODE)) {
            mBinding.salarmActivityBottomMenu.setBackgroundColor(Color.BLACK);
            mBinding.salarmActivityBottomMenu.setItemTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
                new int[]{primaryColor, primaryColor, Color.WHITE}));
        } else {
            final boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(mPrefs);

            if (isCardBackgroundDisplayed) {
                mBinding.salarmActivityBottomMenu.setBackgroundColor(surfaceColor);
            } else {
                mBinding.salarmActivityBottomMenu.setBackgroundColor(Color.TRANSPARENT);
            }

            mBinding.salarmActivityBottomMenu.setItemTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
                new int[]{primaryColor, primaryColor, onBackgroundColor}));
        }

        mBinding.salarmActivityBottomMenu.post(() -> {
            if (isFinishing() || isDestroyed() || mBinding == null) {
                return;
            }

            final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX > 0) {
                            setBottomNavPage(1);
                        } else {
                            setBottomNavPage(2);
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                    return Math.abs(distanceX) > Math.abs(distanceY);
                }

                @Override
                public boolean onDown(@NonNull MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent e) {
                    return true;
                }
            });

            View menuView = mBinding.salarmActivityBottomMenu.getChildAt(0);
            if (menuView instanceof android.view.ViewGroup menuViewGroup) {
                for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                    View itemView = menuViewGroup.getChildAt(i);
                    if (itemView != null) {
                        itemView.setOnTouchListener((v, event) -> {
                            if (gestureDetector.onTouchEvent(event)) {
                                if (event.getAction() == MotionEvent.ACTION_UP) {
                                    v.performClick();
                                }
                                return true;
                            }
                            return false;
                        });
                    }
                }
            }

            if (menuView != null) {
                menuView.setOnTouchListener((v, event) -> {
                    if (gestureDetector.onTouchEvent(event)) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            v.performClick();
                        }
                        return true;
                    }
                    return false;
                });
            }

            applyBottomNavTooltips();
        });
    }

    private void addTabToMenu(Menu menu, UiDataModel.Tab tab, int order) {
        menu.add(Menu.NONE, tab.getPageResId(), order, tab.getLabelResId())
                .setIcon(getTabIcon(tab));
    }

    private int getTabIcon(UiDataModel.Tab tab) {
        return switch (tab) {
            case ALARMS -> R.drawable.ic_tab_alarm;
            case CLOCKS -> R.drawable.ic_tab_clock;
            case TIMERS -> R.drawable.ic_tab_timer;
            case STOPWATCH -> R.drawable.ic_tab_stopwatch;
            case SALAH -> R.drawable.ic_tab_salah;
            case QIBLA -> R.drawable.ic_tab_qibla;
            case MASJID -> R.drawable.ic_masjid_timings;
        };
    }

    private void setBottomNavPage(int page) {
        if (mBottomNavPage == page) {
            return;
        }
        mBottomNavPage = page;
        configureBottomNavigationView();

        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        MenuItem item = mBinding.salarmActivityBottomMenu.getMenu().findItem(selectedTab.getPageResId());
        if (item != null) {
            mBinding.salarmActivityBottomMenu.setSelectedItemId(selectedTab.getPageResId());
        }
    }

    /**
     * Updates the typeface of each item in the {@link BottomNavigationView}.
     *
     * <p>The selected item receives the bold typeface, while all other items
     * receive the regular typeface. TextView lookup is cached for performance.</p>
     */
    @SuppressLint("RestrictedApi")
    private void updateBottomNavTypeface() {
        if (mRegularTypeface == null) {
            return;
        }

        BottomNavigationMenuView menuView = (BottomNavigationMenuView) mBinding.salarmActivityBottomMenu.getChildAt(0);

        for (int i = 0; i < menuView.getChildCount(); i++) {
            View itemView = menuView.getChildAt(i);

            ThemeUtils.applyTypeface(itemView, mRegularTypeface);
        }
    }

    /**
     * Installs custom tooltips on all items of the {@link BottomNavigationView}.
     *
     * <p>The system tooltips are disabled and replaced with custom tooltips
     * displayed above each navigation icon when long‑pressed.</p>
     */
    @SuppressLint("RestrictedApi")
    private void applyBottomNavTooltips() {
        if (mBinding == null) {
            return;
        }

        for (int i = 0; i < mBinding.salarmActivityBottomMenu.getChildCount(); i++) {
            View child = mBinding.salarmActivityBottomMenu.getChildAt(i);

            if (child instanceof BottomNavigationMenuView menuView) {
                for (int j = 0; j < menuView.getChildCount(); j++) {
                    View itemView = menuView.getChildAt(j);
                    if (itemView == null) {
                        continue;
                    }

                    // Disable the system tooltip
                    ViewCompat.setTooltipText(itemView, null);

                    // Install the custom tooltip
                    itemView.setOnLongClickListener(v -> {
                        if (!(v instanceof BottomNavigationItemView)) {
                            return true;
                        }
                        MenuItem item = ((BottomNavigationItemView) v).getItemData();

                        CharSequence title;
                        if (item != null) {
                            title = item.getTitle();
                            if (title != null) {
                                CustomTooltip.showAbove(v, title.toString(), false);
                            }
                        }

                        return true;
                    });
                }
            }
        }
    }

    /**
     * Displays the right tab if the application has been closed and then reopened from the notification.
     */
    private void showTabFromNotifications() {
        final Intent intent = getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                int label = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
                switch (action) {
                    case TimerService.ACTION_SHOW_TIMER -> {
                        Events.sendTimerEvent(R.string.action_show, label);
                        if (UiDataModel.getUiDataModel().isTabVisible(UiDataModel.Tab.TIMERS)) {
                            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS);
                        }
                        // Consume the action to prevent it from being reused
                        intent.setAction(null);
                    }
                    case StopwatchService.ACTION_SHOW_STOPWATCH -> {
                        Events.sendStopwatchEvent(R.string.action_show, label);
                        if (UiDataModel.getUiDataModel().isTabVisible(UiDataModel.Tab.STOPWATCH)) {
                            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.STOPWATCH);
                        }
                        // Consume the action to prevent it from being reused
                        intent.setAction(null);
                    }
                }
            }
        }
    }

    /**
     * Refreshes the visibility of the tabs in the user interface based on current user preferences.
     *
     * <p>This method synchronizes the BottomNavigationView items and the ViewPager. If the currently active tab is hidden by the new
     * preferences, it automatically falls back to the first available tab.</p>
     */
    private void refreshTabsVisibility(UiDataModel.Tab oldTab, boolean tabsChanged) {
        if (tabsChanged) {
            configureBottomNavigationView();
        }

        // Indicate in the menu which tab is currently active to force the Material indicator to redraw
        // when the tabs visibility has been changed
        UiDataModel.Tab currentTab = UiDataModel.getUiDataModel().getSelectedTab();
        int currentItemId = switch (currentTab) {
            case ALARMS -> R.id.page_alarm;
            case CLOCKS -> R.id.page_clock;
            case TIMERS -> R.id.page_timer;
            case STOPWATCH -> R.id.page_stopwatch;
            case SALAH -> R.id.page_salah;
            case QIBLA -> R.id.page_qibla;
            case MASJID -> R.id.page_masjid;
        };

        MenuItem activeItem = mBinding.salarmActivityBottomMenu.getMenu().findItem(currentItemId);
        if (activeItem != null && !activeItem.isChecked()) {
            activeItem.setChecked(true);
        }

        if (!SettingsDAO.getTabTitleVisibility(mPrefs).equals(TAB_TITLE_VISIBILITY_NEVER) && mFontPath != null) {
            updateBottomNavTypeface();
        }

        applyBottomNavTooltips();

        // Re-enable the listener
        UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);

        // Check to see if the tab has been modified after being hidden
        UiDataModel.Tab newTab = UiDataModel.getUiDataModel().getSelectedTab();

        if (oldTab != newTab) {
            // The active tab has been hidden: select the first visible tab
            mTabChangeWatcher.selectedTabChanged(newTab);
        } else {
            // The active tab has not been hidden: update only the interface
            updateCurrentTab();

            if (!tabsChanged) {
                // If the ViewPager has not been destroyed, the fragment is properly attached. So, force the FAB to appear immediately,
                // for example, when the screen rotates.
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }
        }
    }

    /**
     * Configure the Bottom Navigation Menu to display UiDataModel's selected tab.
     */
    @SuppressLint("ResourceType")
    private void updateCurrentTab() {
        // Fetch the selected tab from the source of truth: UiDataModel.
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        // Update the selected tab in the mBottomNavigation if it does not agree with UiDataModel.
        if (mBinding.salarmActivityBottomMenu.getSelectedItemId() != selectedTab.getPageResId()) {
            MenuItem item = mBinding.salarmActivityBottomMenu.getMenu().findItem(selectedTab.getPageResId());
            if (item != null) {
                mBinding.salarmActivityBottomMenu.setSelectedItemId(selectedTab.getPageResId());
            }
        }

        // Update the selected fragment in the viewpager if it does not agree with UiDataModel.
        int targetIndex = -1;
        for (int i = 0; i < UiDataModel.getUiDataModel().getTabCount(); i++) {
            if (UiDataModel.getUiDataModel().getTabAt(i) == selectedTab) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex != -1 && mBinding.salarmActivityPager.getCurrentItem() != targetIndex) {
            // 'false' disables the scroll animation during recreation
            mBinding.salarmActivityPager.setCurrentItem(targetIndex, false);
        }

        if (mIsToolBarDisplayed) {
            mBinding.toolbar.setTitle(selectedTab.getLabelResId());
        } else {
            mBinding.toolbar.setTitle("");
        }
    }

    /**
     * Update screen state based on selected tab and user preferences.
     * <p>
     * If the active tab requires the screen to remain on (e.g. timer in progress, stopwatch active,
     * or user preference enabled), apply the {@code FLAG_KEEP_SCREEN_ON} flag.
     */
    public void updateKeepScreenOn() {
        boolean screenShouldStayOn = SettingsDAO.shouldScreenRemainOn(mPrefs);

        if (!screenShouldStayOn) {
            UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();

            switch (selectedTab) {
                case TIMERS -> screenShouldStayOn = DataModel.getDataModel().hasActiveTimer();
                case STOPWATCH -> screenShouldStayOn = DataModel.getDataModel().getStopwatch().isRunning();
            }
        }

        if (screenShouldStayOn) {
            getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * Updates the runnable state when switching between tabs.
     * <p>
     * This method stops the runnable associated with the previously selected tab
     * and starts the runnable for the newly selected tab, if applicable.
     * It ensures that only the active tab's fragment continues its periodic updates.
     *
     * @param oldTab the previously selected tab
     * @param newTab the newly selected tab
     */
    private void updateTabRunnable(UiDataModel.Tab oldTab, UiDataModel.Tab newTab) {
        // Stop the runnable from the previous tab (if it exists and is still visible)
        if (oldTab != null) {
            int oldIndex = UiDataModel.getUiDataModel().getTabIndex(oldTab);
            if (oldIndex != -1 && oldIndex < mFragmentTabPagerAdapter.getCount()) {
                SalarmFragment oldFragment = mFragmentTabPagerAdapter.getSalarmFragment(oldIndex);
                if (oldFragment instanceof RunnableFragment runnableOld) {
                    runnableOld.stopRunnable();
                }
            }
        }

        // Launch the new tab runnable
        if (newTab != null) {
            int newIndex = UiDataModel.getUiDataModel().getTabIndex(newTab);
            if (newIndex != -1 && newIndex < mFragmentTabPagerAdapter.getCount()) {
                SalarmFragment newFragment = mFragmentTabPagerAdapter.getSalarmFragment(newIndex);
                if (newFragment instanceof RunnableFragment runnableNew) {
                    runnableNew.startRunnable();
                }
            }
        }
    }

    /**
     * @return the SalarmFragment that is currently selected according to UiDataModel
     */
    private SalarmFragment getSelectedSalarmFragment() {
        for (int i = 0; i < mFragmentTabPagerAdapter.getCount(); i++) {
            final SalarmFragment fragment = mFragmentTabPagerAdapter.getSalarmFragment(i);
            if (fragment.isTabSelected()) {
                return fragment;
            }
        }
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        throw new IllegalStateException("Unable to locate selected fragment (" + selectedTab + ")");
    }

    /**
     * @return a Snackbar that displays the message with the given id for 5 seconds
     */
    private Snackbar createSnackbar(@StringRes int messageId) {
        return Snackbar.make(mBinding.contentView, messageId, 5000);
    }

    /**
     * Models the interesting state of display the FAB button may inhabit.
     */
    private enum FabState {SHOWING, HIDE_ARMED, HIDING}

    /**
     * As the view pager changes the selected page, update the model to record the new selected tab.
     */
    private final class PageChangeWatcher implements OnPageChangeListener {

        /**
         * The last reported page scroll state; used to detect exotic state changes.
         */
        private int mPriorState = SCROLL_STATE_IDLE;

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Only hide the fab when a non-zero drag distance is detected. This prevents
            // over-scrolling from needlessly hiding the fab.
            if (mFabState == FabState.HIDE_ARMED && positionOffsetPixels > 5) {
                mFabState = FabState.HIDING;
                mHideAnimation.start();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (mPriorState == SCROLL_STATE_IDLE && state == SCROLL_STATE_SETTLING) {
                // The user has tapped a tab button; play the hide and show animations linearly.
                mHideAnimation.removeListener(mAutoStartShowListener);
                mHideAnimation.addListener(mAutoStartShowListener);
                mHideAnimation.start();
                mFabState = FabState.HIDING;
            } else if (mPriorState == SCROLL_STATE_SETTLING && state == SCROLL_STATE_DRAGGING) {
                // The user has interrupted settling on a tab and the fab button must be re-hidden.
                if (mShowAnimation.isStarted()) {
                    mShowAnimation.cancel();
                }
                if (mHideAnimation.isStarted()) {
                    // Let the hide animation finish naturally; don't auto show when it ends.
                    mHideAnimation.removeListener(mAutoStartShowListener);
                } else {
                    // Start and immediately end the hide animation to jump to the hidden state.
                    mHideAnimation.start();
                    mHideAnimation.end();
                }
                mFabState = FabState.HIDING;

            } else if (state != SCROLL_STATE_DRAGGING && mFabState == FabState.HIDING) {
                // The user has lifted their finger; show the buttons now or after hide ends.
                if (mHideAnimation.isStarted()) {
                    // Finish the hide animation and then start the show animation.
                    mHideAnimation.removeListener(mAutoStartShowListener);
                    mHideAnimation.addListener(mAutoStartShowListener);
                } else {
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    mShowAnimation.start();

                    // The animation to show the fab has begun; update the state to showing.
                    mFabState = FabState.SHOWING;
                }
            } else if (state == SCROLL_STATE_DRAGGING) {
                // The user has started a drag so arm the hide animation.
                mFabState = FabState.HIDE_ARMED;
            }

            // Update the last known state.
            mPriorState = state;
        }

        @Override
        public void onPageSelected(int position) {
            SalarmFragment fragment = mFragmentTabPagerAdapter.getSalarmFragment(position);
            fragment.selectTab();

            if (fragment instanceof AlarmFragment alarmFragment) {
                alarmFragment.hideSideButtonsOnlyAnimated();
            }

            int totalTabs = UiDataModel.getUiDataModel().getTabCount();
            if (totalTabs > 4) {
                if (position <= 3) {
                    setBottomNavPage(1);
                } else {
                    setBottomNavPage(2);
                }
            } else {
                setBottomNavPage(1);
            }
        }
    }

    /**
     * If this listener is attached to {@link #mHideAnimation} when it ends, the corresponding
     * {@link #mShowAnimation} is automatically started.
     */
    private final class AutoStartShowListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(@NonNull Animator animation) {
            // Prepare the hide animation for its next use; by default do not auto-show after hide.
            mHideAnimation.removeListener(mAutoStartShowListener);

            // Update the buttons now that they are no longer visible.
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            // Automatically start the growth animation now that shrinking is complete.
            mShowAnimation.start();

            // The animation to show the fab has begun; update the state to showing.
            mFabState = FabState.SHOWING;
        }
    }

    /**
     * Shows/hides a snackbar as silencing settings are enabled/disabled.
     */
    private final class SilentSettingChangeWatcher implements OnSilentSettingsListener {
        @Override
        public void onSilentSettingsChange(SilentSetting after) {
            if (mShowSilentSettingSnackbarRunnable != null) {
                mBinding.contentView.removeCallbacks(mShowSilentSettingSnackbarRunnable);
                mShowSilentSettingSnackbarRunnable = null;
            }

            if (after == null) {
                SnackbarManager.dismiss();
            } else {
                mShowSilentSettingSnackbarRunnable = new ShowSilentSettingSnackbarRunnable(after);
                mBinding.contentView.postDelayed(mShowSilentSettingSnackbarRunnable, SECOND_IN_MILLIS);
            }
        }
    }

    /**
     * Displays a snackbar that indicates a system setting is currently silencing alarms.
     */
    private final class ShowSilentSettingSnackbarRunnable implements Runnable {

        private final SilentSetting mSilentSetting;

        private ShowSilentSettingSnackbarRunnable(SilentSetting silentSetting) {
            mSilentSetting = silentSetting;
        }

        public void run() {
            // Create a snackbar with a message explaining the setting that is silencing alarms.
            final Snackbar snackbar = createSnackbar(mSilentSetting.getLabelResId());

            // Set the associated corrective action if one exists.
            if (mSilentSetting.isActionEnabled()) {
                final int actionResId = mSilentSetting.getActionResId();
                snackbar.setAction(actionResId, v -> mSilentSetting.executeAction(v.getContext()));
            }

            SnackbarManager.show(snackbar);
        }
    }

    /**
     * As the model reports changes to the selected tab, update the user interface.
     */
    private final class TabChangeWatcher implements TabListener {

        private UiDataModel.Tab mPreviousTab = null;

        @Override
        public void selectedTabChanged(UiDataModel.Tab newSelectedTab) {
            if (mPreviousTab == null) {
                mPreviousTab = UiDataModel.getUiDataModel().getSelectedTab();
            }

            // Update the view pager and tab layout to agree with the model.
            updateCurrentTab();

            updateKeepScreenOn();

            updateTabRunnable(mPreviousTab, newSelectedTab);
            mPreviousTab = newSelectedTab;

            // Avoid sending events for the initial tab selection on launch and re-selecting a tab
            // after a configuration change.
            if (DataModel.getDataModel().isApplicationInForeground()) {
                switch (newSelectedTab) {
                    case ALARMS -> Events.sendAlarmEvent(R.string.action_show, R.string.label_salarm);
                    case CLOCKS -> Events.sendClockEvent(R.string.action_show, R.string.label_salarm);
                    case TIMERS -> Events.sendTimerEvent(R.string.action_show, R.string.label_salarm);
                    case STOPWATCH -> Events.sendStopwatchEvent(R.string.action_show, R.string.label_salarm);
                }
            }

            // If the hide animation has already completed, the buttons must be updated now when the
            // new tab is known; otherwise they are updated at the end of the hide animation.
            if (!mHideAnimation.isStarted()) {
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }
        }
    }
}
