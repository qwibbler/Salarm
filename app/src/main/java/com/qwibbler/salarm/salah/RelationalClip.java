package com.qwibbler.salarm.salah;

import androidx.room.Embedded;
import androidx.room.Ignore;

import java.util.Objects;

/**
 * Shared clipping/capping logic.
 */
public class RelationalClip {
    @Embedded(prefix = "anchor_")
    public RelationalAnchor anchor;

    public int offsetMinutes;
    public RelationalRule.RoundingDirection roundingDirection;
    public int roundingIntervalMinutes;
    public RelationalRule.BoundaryType boundaryType;

    public RelationalClip() {
        this.anchor = new RelationalAnchor();
    }

    @Ignore
    public RelationalClip(RelationalRule.AnchorEvent clipEvent, int offsetMinutes, RelationalRule.RoundingDirection roundingDirection, int roundingIntervalMinutes, RelationalRule.BoundaryType boundaryType) {
        this.anchor = new RelationalAnchor(clipEvent);
        this.offsetMinutes = offsetMinutes;
        this.roundingDirection = roundingDirection;
        this.roundingIntervalMinutes = roundingIntervalMinutes;
        this.boundaryType = boundaryType;
    }

    @Ignore
    public RelationalClip(RelationalClip other) {
        if (other != null) {
            this.anchor = new RelationalAnchor(other.anchor);
            this.offsetMinutes = other.offsetMinutes;
            this.roundingDirection = other.roundingDirection;
            this.roundingIntervalMinutes = other.roundingIntervalMinutes;
            this.boundaryType = other.boundaryType;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationalClip that = (RelationalClip) o;
        return offsetMinutes == that.offsetMinutes &&
                roundingIntervalMinutes == that.roundingIntervalMinutes &&
                Objects.equals(anchor, that.anchor) &&
                roundingDirection == that.roundingDirection &&
                boundaryType == that.boundaryType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(anchor, offsetMinutes, roundingDirection, roundingIntervalMinutes, boundaryType);
    }
}
