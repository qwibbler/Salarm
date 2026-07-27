// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.widgets;

import static com.qwibbler.salarm.utils.WidgetUtils.KEY_LAUNCHED_FROM_WIDGET;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.settings.AppWidgetAnalogSettingsFragment;
import com.qwibbler.salarm.settings.AppWidgetDigitalSettingsFragment;
import com.qwibbler.salarm.settings.AppWidgetNamazCountdownSettingsFragment;
import com.qwibbler.salarm.settings.AppWidgetNamazSettingsFragment;
import com.qwibbler.salarm.settings.AppWidgetNextAlarmSettingsFragment;
import com.qwibbler.salarm.settings.AppWidgetVerticalSettingsFragment;
import com.qwibbler.salarm.uicomponents.CollapsingToolbarBaseActivity;

/**
 * Class called when the user launches the widget configuration from the widget.
 */
public class WidgetConfiguration {

    public static class AnalogWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.analog_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AppWidgetAnalogSettingsFragment());
        }
    }

    public static class DigitalWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AppWidgetDigitalSettingsFragment());
        }
    }

    public static class VerticalWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AppWidgetVerticalSettingsFragment());
        }
    }

    public static class NextAlarmWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.digital_widget);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AppWidgetNextAlarmSettingsFragment());
        }
    }

    public static class NamazWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.namaz_timeline_widget_label);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AppWidgetNamazSettingsFragment());
        }
    }

    public static class NamazCountdownWidgetConfiguration extends CollapsingToolbarBaseActivity {

        @Override
        protected String getActivityTitle() {
            return getString(R.string.namaz_countdown_widget_label);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            showFragmentFromWidget(this, savedInstanceState, new AppWidgetNamazCountdownSettingsFragment());
        }
    }

    public static void showFragmentFromWidget(AppCompatActivity activity, Bundle savedInstanceState, Fragment fragment) {
        if (savedInstanceState == null) {
            Bundle args = fragment.getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putBoolean(KEY_LAUNCHED_FROM_WIDGET, true);
            fragment.setArguments(args);

            activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .disallowAddToBackStack()
                .commit();
        }
    }

}
