package com.qwibbler.salarm.salah;

import java.util.Locale;

/**
 * Defines the type of a Salah event.
 */
public enum SalahType {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
    JUMA;

    public static SalahType fromString(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        try {
            return SalahType.valueOf(name.trim().toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
