/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.data;

import androidx.annotation.NonNull;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A read-only domain object representing a city of the world and associated time information. It
 * also contains static comparators that can be instantiated to order cities in common sort orders.
 */
public final class City {

    /**
     * A unique identifier for the city.
     */
    private final String mId;

    /**
     * An optional numeric index used to order cities for display; -1 if no such index exists.
     */
    private final int mIndex;

    /**
     * An index string used to order cities for display.
     */
    private final String mIndexString;

    /**
     * The display name of the city.
     */
    private final String mName;

    /**
     * The English city name.
     */
    private final String mEnglishName;

    /**
     * The phonetic name of the city used to order cities for display.
     */
    private final String mPhoneticName;

    /**
     * The TimeZone corresponding to the city.
     */
    private final TimeZone mTimeZone;

    /**
     * The country name of the city.
     */
    private final String mCountry;

    /**
     * Alternative names for the city.
     */
    private final List<String> mAlternativeNames;

    /**
     * A cached upper case form of the {@link #mName} used in case-insensitive name comparisons.
     */
    private String mNameUpperCase;

    /**
     * A cached upper case form of the {@link #mName} used in case-insensitive name comparisons
     * which ignore {@link #removeSpecialCharacters(String)} special characters.
     */
    private String mNameUpperCaseNoSpecialCharacters;

    /**
     * A cached upper case form of the {@link #mEnglishName} used in case-insensitive name comparisons.
     */
    private String mEnglishNameUpperCase;

    /**
     * A cached upper case form of the {@link #mEnglishName} used in case-insensitive name comparisons
     * which ignore {@link #removeSpecialCharacters(String)} special characters.
     */
    private String mEnglishNameUpperCaseNoSpecialCharacters;

    /**
     * A cached upper case form of the {@link #mCountry} used in case-insensitive name comparisons
     * which ignore {@link #removeSpecialCharacters(String)} special characters.
     */
    private String mCountryUpperCaseNoSpecialCharacters;

    /**
     * A cached list of upper case forms of {@link #mAlternativeNames} used in case-insensitive
     * name comparisons which ignore {@link #removeSpecialCharacters(String)} special characters.
     */
    private List<String> mAlternativeNamesUpperCaseNoSpecialCharacters;

    City(String id, int index, String indexString, String name, String englishName, String phoneticName, TimeZone tz, String country, List<String> alternativeNames) {
        mId = id;
        mIndex = index;
        mIndexString = indexString;
        mName = name;
        mEnglishName = englishName;
        mPhoneticName = phoneticName;
        mTimeZone = tz;
        mCountry = country;
        mAlternativeNames = alternativeNames != null ? Collections.unmodifiableList(alternativeNames) : Collections.emptyList();
    }

    /**
     * Strips out any characters considered optional for matching purposes. These include spaces,
     * dashes, periods and apostrophes.
     *
     * @param token a city name or search term
     * @return the given {@code token} without any characters considered optional when matching
     */
    public static String removeSpecialCharacters(String token) {
        return token.replaceAll("[ -.']", "");
    }

    public String getId() {
        return mId;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getName() {
        return mName;
    }

    public TimeZone getTimeZone() {
        return mTimeZone;
    }

    public String getIndexString() {
        return mIndexString;
    }

    public String getPhoneticName() {
        return mPhoneticName;
    }

    public String getCountry() {
        return mCountry;
    }

    public List<String> getAlternativeNames() {
        return mAlternativeNames;
    }

    /**
     * @return the city name converted to upper case
     */
    public String getNameUpperCase() {
        if (mNameUpperCase == null) {
            mNameUpperCase = mName.toUpperCase(Locale.ROOT);
        }
        return mNameUpperCase;
    }

    /**
     * @return the city name converted to upper case with all special characters removed
     */
    private String getNameUpperCaseNoSpecialCharacters() {
        if (mNameUpperCaseNoSpecialCharacters == null) {
            mNameUpperCaseNoSpecialCharacters = removeSpecialCharacters(getNameUpperCase());
        }
        return mNameUpperCaseNoSpecialCharacters;
    }

    /**
     * @return the English city name converted to upper case
     */
    public String getEnglishNameUpperCase() {
        if (mEnglishNameUpperCase == null) {
            mEnglishNameUpperCase = mEnglishName.toUpperCase(Locale.ROOT);
        }
        return mEnglishNameUpperCase;
    }

    /**
     * @return the English city name converted to upper case with all special characters removed
     */
    private String getEnglishNameUpperCaseNoSpecialCharacters() {
        if (mEnglishNameUpperCaseNoSpecialCharacters == null) {
            mEnglishNameUpperCaseNoSpecialCharacters = removeSpecialCharacters(getEnglishNameUpperCase());
        }
        return mEnglishNameUpperCaseNoSpecialCharacters;
    }

    /**
     * @return the country name converted to upper case with all special characters removed
     */
    private String getCountryUpperCaseNoSpecialCharacters() {
        if (mCountryUpperCaseNoSpecialCharacters == null && mCountry != null) {
            mCountryUpperCaseNoSpecialCharacters = removeSpecialCharacters(mCountry.toUpperCase(Locale.ROOT));
        }
        return mCountryUpperCaseNoSpecialCharacters;
    }

    /**
     * @return a list of alternative names converted to upper case with all special characters removed
     */
    private List<String> getAlternativeNamesUpperCaseNoSpecialCharacters() {
        if (mAlternativeNamesUpperCaseNoSpecialCharacters == null) {
            mAlternativeNamesUpperCaseNoSpecialCharacters = new ArrayList<>(mAlternativeNames.size());
            for (String alt : mAlternativeNames) {
                mAlternativeNamesUpperCaseNoSpecialCharacters.add(removeSpecialCharacters(alt.toUpperCase(Locale.ROOT)));
            }
        }
        return mAlternativeNamesUpperCaseNoSpecialCharacters;
    }

    /**
     * @param upperCaseQueryNoSpecialCharacters search term with all special characters removed
     *                                          to match against the upper case city name
     * @return {@code true} if the name of this city starts with the given query
     */
    public boolean matches(String upperCaseQueryNoSpecialCharacters) {
        // By removing all special characters, prefix matching becomes more liberal, and it is easier
        // to locate the desired city. e.g. "St. Lucia" is matched by "StL", "St.L", "St L", "St. L"
        if (getNameUpperCaseNoSpecialCharacters().startsWith(upperCaseQueryNoSpecialCharacters)
            || getEnglishNameUpperCaseNoSpecialCharacters().startsWith(upperCaseQueryNoSpecialCharacters)) {
            return true;
        }

        final String countryMatch = getCountryUpperCaseNoSpecialCharacters();
        if (countryMatch != null && countryMatch.contains(upperCaseQueryNoSpecialCharacters)) {
            return true;
        }

        for (String altMatch : getAlternativeNamesUpperCaseNoSpecialCharacters()) {
            if (altMatch.contains(upperCaseQueryNoSpecialCharacters)) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US,
            "City {id=%s, index=%d, indexString=%s, name=%s, englishName=%s, phonetic=%s, tz=%s, country=%s, alt=%s}",
            mId, mIndex, mIndexString, mName, mEnglishName, mPhoneticName, mTimeZone.getID(), mCountry, mAlternativeNames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof City city)) {
            return false;
        }

        return mIndex == city.mIndex
            && Objects.equals(mId, city.mId)
            && Objects.equals(mIndexString, city.mIndexString)
            && Objects.equals(mName, city.mName)
            && Objects.equals(mEnglishName, city.mEnglishName)
            && Objects.equals(mPhoneticName, city.mPhoneticName)
            && Objects.equals(mTimeZone, city.mTimeZone)
            && Objects.equals(mCountry, city.mCountry)
            && Objects.equals(mAlternativeNames, city.mAlternativeNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mIndex, mIndexString, mName, mEnglishName, mPhoneticName, mTimeZone, mCountry, mAlternativeNames);
    }


    /**
     * Orders by:
     *
     * <ol>
     *     <li>UTC offset of {@link #getTimeZone() timezone}</li>
     *     <li>{@link #getIndex() numeric index}</li>
     *     <li>{@link #getIndexString()} alphabetic index}</li>
     *     <li>{@link #getPhoneticName() phonetic name}</li>
     * </ol>
     */
    public static final class UtcOffsetComparator implements Comparator<City> {

        private final Comparator<City> mDelegate1 = new UtcOffsetIndexComparator();

        private final Comparator<City> mDelegate2 = new NameComparator();

        public int compare(City c1, City c2) {
            int result = mDelegate1.compare(c1, c2);

            if (result == 0) {
                result = mDelegate2.compare(c1, c2);
            }

            return result;
        }
    }

    /**
     * Orders by:
     *
     * <ol>
     *     <li>UTC offset of {@link #getTimeZone() timezone}</li>
     * </ol>
     */
    public static final class UtcOffsetIndexComparator implements Comparator<City> {

        public int compare(City c1, City c2) {
            final long now = System.currentTimeMillis();
            final int utcOffset1 = c1.getTimeZone().getOffset(now);
            final int utcOffset2 = c2.getTimeZone().getOffset(now);
            return Integer.compare(utcOffset1, utcOffset2);
        }
    }

    /**
     * This comparator sorts using the city fields that influence natural name sort order:
     *
     * <ol>
     *     <li>{@link #getIndex() numeric index}</li>
     *     <li>{@link #getIndexString()} alphabetic index}</li>
     *     <li>{@link #getPhoneticName() phonetic name}</li>
     * </ol>
     */
    public static final class NameComparator implements Comparator<City> {

        private final Comparator<City> mDelegate = new NameIndexComparator();

        // Locale-sensitive comparator for phonetic names.
        private final Collator mNameCollator = Collator.getInstance();

        @Override
        public int compare(City c1, City c2) {
            int result = mDelegate.compare(c1, c2);

            if (result == 0) {
                result = mNameCollator.compare(c1.getPhoneticName(), c2.getPhoneticName());
            }

            return result;
        }
    }

    /**
     * Orders by:
     *
     * <ol>
     *     <li>{@link #getIndex() numeric index}</li>
     *     <li>{@link #getIndexString()} alphabetic index}</li>
     * </ol>
     */
    public static final class NameIndexComparator implements Comparator<City> {

        // Locale-sensitive comparator for index strings.
        private final Collator mNameCollator = Collator.getInstance();

        @Override
        public int compare(City c1, City c2) {
            int result = Integer.compare(c1.getIndex(), c2.getIndex());

            if (result == 0) {
                result = mNameCollator.compare(c1.getIndexString(), c2.getIndexString());
            }

            return result;
        }
    }
}
