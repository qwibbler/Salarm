package com.qwibbler.salarm.salah;

import androidx.room.Ignore;

import java.util.Objects;

/**
 * Shared calculation parameters for both Masjid events and user alarms.
 */
public class RelationalCalculation {
    public int offsetMinutes;
    public RelationalRule.RoundingDirection roundingDirection;
    public int roundingIntervalMinutes;
    public int adjustmentMinutes;

    public RelationalCalculation() {
    }

    @Ignore
    public RelationalCalculation(int offsetMinutes, RelationalRule.RoundingDirection roundingDirection, int roundingIntervalMinutes) {
        this.offsetMinutes = offsetMinutes;
        this.roundingDirection = roundingDirection;
        this.roundingIntervalMinutes = roundingIntervalMinutes;
    }

    @Ignore
    public RelationalCalculation(RelationalCalculation other) {
        if (other != null) {
            this.offsetMinutes = other.offsetMinutes;
            this.roundingDirection = other.roundingDirection;
            this.roundingIntervalMinutes = other.roundingIntervalMinutes;
            this.adjustmentMinutes = other.adjustmentMinutes;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationalCalculation that = (RelationalCalculation) o;
        return offsetMinutes == that.offsetMinutes &&
                roundingIntervalMinutes == that.roundingIntervalMinutes &&
                adjustmentMinutes == that.adjustmentMinutes &&
                roundingDirection == that.roundingDirection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offsetMinutes, roundingDirection, roundingIntervalMinutes, adjustmentMinutes);
    }
}
