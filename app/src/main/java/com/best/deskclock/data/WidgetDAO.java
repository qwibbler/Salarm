/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.SharedPreferences;

/**
 * This class encapsulates the transfer of data between widget objects and their permanent storage
 * in {@link SharedPreferences}.
 */
public final class WidgetDAO {

    // *******************
    // ** ANALOG WIDGET **
    // *******************

    /**
     * @return the dial applied to the clock on the analog widget.
     */
    public static String getAnalogWidgetClockDial(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getString(KEY_ANALOG_WIDGET_CLOCK_DIAL, DEFAULT_ANALOG_WIDGET_CLOCK_DIAL);
    }

    /**
     * @return {@code true} if the second hand is displayed on the analog widget; {@code false} otherwise.
     */
    public static boolean isSecondHandDisplayedOnAnalogWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_WITH_SECOND_HAND, DEFAULT_ANALOG_WIDGET_WITH_SECOND_HAND);
    }

    /**
     * @return the clock second hand displayed on the analog widget.
     */
    public static String getAnalogWidgetClockSecondHand(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getString(KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND, DEFAULT_CLOCK_SECOND_HAND);
    }

    /**
     * @return {@code true} if the default color is applied to the dial on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultDialColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the dial color on the analog widget.
     */
    public static int getAnalogWidgetDialColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hour hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultHourHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the hour hand color on the analog widget.
     */
    public static int getAnalogWidgetHourHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minute hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultMinuteHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the minute hand color on the analog widget.
     */
    public static int getAnalogWidgetMinuteHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the second hand on the analog widget;
     * {@code false} otherwise.
     */
    public static boolean isAnalogWidgetDefaultSecondHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getBoolean(KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the second hand color on the analog widget.
     */
    public static int getAnalogWidgetSecondHandColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_analog_widget.xml
        return prefs.getInt(KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    // ********************
    // ** DIGITAL WIDGET **
    // ********************

    /**
     * @return {@code true} if the text is displayed in uppercase on the digital widget; {@code false} otherwise.
     */
    public static boolean isTextUppercaseDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    /**
     * @return {@code true} if the text shadow is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isTextShadowDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    /**
     * @return {@code true} if the seconds are displayed on the digital widget;
     * {@code false} otherwise.
     */
    public static boolean areSecondsDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED, DEFAULT_DIGITAL_WIDGET_DISPLAY_SECONDS);
    }

    /**
     * @return {@code true} if the AM/PM part is hidden on the digital widget; {@code false} otherwise.
     */
    public static boolean isAmPmHiddenOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_HIDE_AM_PM, DEFAULT_DIGITAL_WIDGET_HIDE_AM_PM);
    }

    /**
     * @return {@code true} if the background is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the background corner radius is customizable for the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetBackgroundCornerRadiusCustomizable(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return a value indicating the digital widget background corner radius.
     */
    public static int getDigitalWidgetBackgroundCornerRadius(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the date is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the date is displayed above the time on the digital widget; {@code false} otherwise.
     */
    public static boolean isTopDateDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_TOP_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM, DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if the title of the next alarm is displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmTitleDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE, DEFAULT_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE);
    }

    /**
     * @return {@code true} if the cities are displayed on the digital widget; {@code false} otherwise.
     */
    public static boolean areWorldCitiesDisplayedOnDigitalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the digital widget. {@code false} otherwise.
     */
    public static boolean isDigitalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the digital widget.
     */
    public static int getDigitalWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the digital clock on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the clock on the digital widget.
     */
    public static int getDigitalWidgetCustomClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the digital widget.
     */
    public static int getDigitalWidgetCustomDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the digital widget.
     */
    public static int getDigitalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm title on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultNextAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm title on the digital widget.
     */
    public static int getDigitalWidgetCustomNextAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city clock on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city clock on the digital widget.
     */
    public static int getDigitalWidgetCustomCityClockColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city name on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityNameColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city name on the digital widget.
     */
    public static int getDigitalWidgetCustomCityNameColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the city note on the digital widget; {@code false} otherwise.
     */
    public static boolean isDigitalWidgetDefaultCityNoteColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the city note on the digital widget.
     */
    public static int getDigitalWidgetCustomCityNoteColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NOTE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the clock on the digital widget.
     */
    public static int getDigitalWidgetMaxClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // *********************
    // ** VERTICAL WIDGET **
    // *********************

    /**
     * @return {@code true} if the text is displayed in uppercase on the vertical widget; {@code false} otherwise.
     */
    public static boolean isTextUppercaseDisplayedOnVerticalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    /**
     * @return {@code true} if the text shadow is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isTextShadowDisplayedOnVerticalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    /**
     * @return {@code true} if the background is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnVerticalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_BACKGROUND,
            DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the background corner radius is customizable for the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetBackgroundCornerRadiusCustomizable(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return a value indicating the vertical widget background corner radius.
     */
    public static int getVerticalWidgetBackgroundCornerRadius(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the date is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isDateDisplayedOnVerticalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_DATE, DEFAULT_VERTICAL_WIDGET_DISPLAY_DATE);
    }

    /**
     * @return {@code true} if the next alarm is displayed on the vertical widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmDisplayedOnVerticalWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM, DEFAULT_VERTICAL_WIDGET_DISPLAY_NEXT_ALARM);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the vertical widget. {@code false} otherwise.
     */
    public static boolean isVerticalWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the vertical widget.
     */
    public static int getVerticalWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the hours on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultHoursColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the hours on the vertical widget.
     */
    public static int getVerticalWidgetCustomHoursColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_HOURS_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the minutes on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultMinutesColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the minutes on the vertical widget.
     */
    public static int getVerticalWidgetCustomMinutesColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_MINUTES_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the date on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the date on the vertical widget.
     */
    public static int getVerticalWidgetCustomDateColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_DATE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the next alarm on the vertical widget; {@code false} otherwise.
     */
    public static boolean isVerticalWidgetDefaultNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getBoolean(KEY_VERTICAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the next alarm on the vertical widget.
     */
    public static int getVerticalWidgetCustomNextAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the hours on the vertical widget.
     */
    public static int getVerticalWidgetMaxClockFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_vertical_widget.xml
        return prefs.getInt(KEY_VERTICAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ***********************
    // ** NEXT ALARM WIDGET **
    // ***********************

    /**
     * @return {@code true} if the text is displayed in uppercase on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isTextUppercaseDisplayedOnNextAlarmWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    /**
     * @return {@code true} if the text shadow is displayed on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isTextShadowDisplayedOnNextAlarmWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    /**
     * @return {@code true} if the background is displayed on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isBackgroundDisplayedOnNextAlarmWidget(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
    }

    /**
     * @return {@code true} if the background corner radius is customizable for the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetBackgroundCornerRadiusCustomizable(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS,
            DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return a value indicating the Next alarm widget background corner radius.
     */
    public static int getNextAlarmWidgetBackgroundCornerRadius(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_digital_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if horizontal padding should be applied to the Next alarm widget. {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_WIDGETS_APPLY_HORIZONTAL_PADDING);
    }

    /**
     * @return {@code true} if the default color is applied to the background on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the background color on the Next alarm widget.
     */
    public static int getNextAlarmWidgetBackgroundColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the title on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the title on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm title on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm title on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmTitleColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return {@code true} if the default color is applied to the alarm on the Next alarm widget; {@code false} otherwise.
     */
    public static boolean isNextAlarmWidgetDefaultAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getBoolean(KEY_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
    }

    /**
     * @return a value indicating the color of the alarm on the Next alarm widget.
     */
    public static int getNextAlarmWidgetCustomAlarmColor(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    /**
     * @return the font size applied to the Next alarm widget.
     */
    public static int getNextAlarmWidgetMaxFontSize(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_customize_next_alarm_widget.xml
        return prefs.getInt(KEY_NEXT_ALARM_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    // ******************
    // ** NAMAZ WIDGET **
    // ******************

    public static boolean isTextUppercaseDisplayedOnNamazWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_NAMAZ_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    public static boolean isTextShadowDisplayedOnNamazWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_NAMAZ_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    public static boolean isBackgroundDisplayedOnNamazWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DISPLAY_BACKGROUND, DEFAULT_NAMAZ_WIDGET_DISPLAY_BACKGROUND);
    }

    public static boolean isNamazWidgetBackgroundCornerRadiusCustomizable(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    public static int getNamazWidgetBackgroundCornerRadius(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    public static boolean isNamazWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_NAMAZ_WIDGET_APPLY_HORIZONTAL_PADDING);
    }

    public static boolean isNamazWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    public static boolean isNamazWidgetDefaultCountdownTimeColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_COUNTDOWN_TIME_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetCustomCountdownTimeColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_COUNTDOWN_TIME_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazWidgetDefaultCountdownLabelColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_COUNTDOWN_LABEL_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetCustomCountdownLabelColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_COUNTDOWN_LABEL_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazWidgetDefaultTimelineActiveColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetCustomTimelineActiveColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_TIMELINE_ACTIVE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazWidgetDefaultTimelineNextColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetCustomTimelineNextColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_TIMELINE_NEXT_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazWidgetDefaultTimelineStandardColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetCustomTimelineStandardColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_TIMELINE_STANDARD_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isItemHighlightDisplayedOnNamazWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DISPLAY_ITEM_HIGHLIGHT, DEFAULT_NAMAZ_WIDGET_ITEM_HIGHLIGHT_DISPLAYED);
    }

    public static boolean isNamazWidgetItemActiveHighlightDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetItemActiveHighlightColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_ITEM_ACTIVE_HIGHLIGHT_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazWidgetItemNextHighlightDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR, DEFAULT_NAMAZ_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazWidgetItemNextHighlightColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_CUSTOM_ITEM_NEXT_HIGHLIGHT_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static int getNamazWidgetMaxFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    public static boolean isNamazWidgetShowBothAsrs(SharedPreferences prefs) {
        if (!prefs.contains(KEY_NAMAZ_WIDGET_SHOW_BOTH_ASRS)) {
            return prefs.getBoolean(KEY_NAMAZ_SHOW_BOTH_ASR, DEFAULT_NAMAZ_SHOW_BOTH_ASR);
        }
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_BOTH_ASRS, DEFAULT_NAMAZ_SHOW_BOTH_ASR);
    }

    public static boolean isNamazWidgetFajrVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_FAJR, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetSunriseVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_SUNRISE, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetDhuhrVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_DHUHR, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetAsrVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_ASR, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetAsrHanafiVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_ASR_HANAFI, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetAsrShafiVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_ASR_SHAFI, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetMaghribVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_MAGHRIB, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazWidgetIshaVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_WIDGET_SHOW_ISHA, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    // **************************
    // ** NAMAZ COMPACT WIDGET **
    // **************************

    public static boolean isTextUppercaseDisplayedOnNamazCompactWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_TEXT_UPPERCASE, DEFAULT_WIDGET_TEXT_UPPERCASE_DISPLAYED);
    }

    public static boolean isTextShadowDisplayedOnNamazCompactWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_TEXT_SHADOW, DEFAULT_WIDGET_TEXT_SHADOW_DISPLAYED);
    }

    public static boolean isBackgroundDisplayedOnNamazCompactWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_BACKGROUND, DEFAULT_NAMAZ_COMPACT_WIDGET_DISPLAY_BACKGROUND);
    }

    public static boolean isNamazCompactWidgetBackgroundCornerRadiusCustomizable(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGETS_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
    }

    public static int getNamazCompactWidgetBackgroundCornerRadius(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_BACKGROUND_CORNER_RADIUS, DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    public static boolean isNamazCompactWidgetHorizontalPaddingApplied(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_APPLY_HORIZONTAL_PADDING, DEFAULT_NAMAZ_COMPACT_WIDGET_APPLY_HORIZONTAL_PADDING);
    }

    public static boolean isNamazCompactWidgetDefaultBackgroundColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_BACKGROUND_COLOR, DEFAULT_NAMAZ_COMPACT_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazCompactWidgetBackgroundColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_BACKGROUND_COLOR, DEFAULT_WIDGETS_BACKGROUND_COLOR);
    }

    public static boolean isNamazCompactWidgetDefaultTimelineActiveColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR, DEFAULT_NAMAZ_COMPACT_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazCompactWidgetCustomTimelineActiveColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_TIMELINE_ACTIVE_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazCompactWidgetDefaultTimelineNextColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR, DEFAULT_NAMAZ_COMPACT_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazCompactWidgetCustomTimelineNextColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_TIMELINE_NEXT_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazCompactWidgetDefaultTimelineStandardColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR, DEFAULT_NAMAZ_COMPACT_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazCompactWidgetCustomTimelineStandardColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_TIMELINE_STANDARD_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isItemHighlightDisplayedOnNamazCompactWidget(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_ITEM_HIGHLIGHT, DEFAULT_NAMAZ_COMPACT_WIDGET_ITEM_HIGHLIGHT_DISPLAYED);
    }

    public static boolean isNamazCompactWidgetItemActiveHighlightDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR, DEFAULT_NAMAZ_COMPACT_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazCompactWidgetItemActiveHighlightColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_ITEM_ACTIVE_HIGHLIGHT_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static boolean isNamazCompactWidgetItemNextHighlightDefaultColor(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR, DEFAULT_NAMAZ_COMPACT_WIDGET_DEFAULT_COLOR);
    }

    public static int getNamazCompactWidgetItemNextHighlightColor(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_ITEM_NEXT_HIGHLIGHT_COLOR, DEFAULT_WIDGETS_CUSTOM_COLOR);
    }

    public static int getNamazCompactWidgetMaxFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_NAMAZ_COMPACT_WIDGET_MAXIMUM_FONT_SIZE, DEFAULT_WIDGETS_FONT_SIZE);
    }

    public static boolean isNamazCompactWidgetShowBothAsrs(SharedPreferences prefs) {
        if (!prefs.contains(KEY_NAMAZ_COMPACT_WIDGET_SHOW_BOTH_ASRS)) {
            return prefs.getBoolean(KEY_NAMAZ_SHOW_BOTH_ASR, DEFAULT_NAMAZ_SHOW_BOTH_ASR);
        }
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_BOTH_ASRS, DEFAULT_NAMAZ_SHOW_BOTH_ASR);
    }

    public static boolean isNamazCompactWidgetFajrVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_FAJR, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetSunriseVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_SUNRISE, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetDhuhrVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_DHUHR, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetAsrVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetAsrHanafiVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR_HANAFI, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetAsrShafiVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR_SHAFI, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetMaghribVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_MAGHRIB, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

    public static boolean isNamazCompactWidgetIshaVisible(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ISHA, DEFAULT_NAMAZ_WIDGET_SHOW_PRAYER_EVENT);
    }

}
