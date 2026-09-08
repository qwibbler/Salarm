package com.qwibbler.salarm.salah;

import androidx.room.Ignore;

import java.util.Objects;

/**
 * Defines what an event or alarm is relative to.
 */
public class RelationalAnchor {
    public RelationalRule.AnchorEvent basicEvent;
    public Long masjidEventId;
    public boolean isMax;

    public RelationalAnchor() {
    }

    @Ignore
    public RelationalAnchor(RelationalRule.AnchorEvent basicEvent) {
        this.basicEvent = basicEvent;
    }

    @Ignore
    public RelationalAnchor(RelationalAnchor other) {
        if (other != null) {
            this.basicEvent = other.basicEvent;
            this.masjidEventId = other.masjidEventId;
            this.isMax = other.isMax;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationalAnchor that = (RelationalAnchor) o;
        return isMax == that.isMax &&
                basicEvent == that.basicEvent &&
                Objects.equals(masjidEventId, that.masjidEventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicEvent, masjidEventId, isMax);
    }
}
