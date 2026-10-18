package com.qwibbler.salarm.salah;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Defines how a Masjid prayer event (e.g. Masjid Fajr) is calculated.
 */
@Entity(tableName = "masjid_events")
public class MasjidEvent {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long masjidId;
    public String salahType; // e.g. "FAJR", "JUMA"

    public boolean isSelected;
    public boolean isManuallyDeselected;

    public boolean dndEnabled;
    public int dndBeforeMinutes = 0;
    public int dndAfterMinutes = 10;

    @Embedded
    public RelationalDefinition definition;

    public MasjidEvent() {
        this.definition = new RelationalDefinition();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MasjidEvent that = (MasjidEvent) o;
        return id == that.id &&
                masjidId == that.masjidId &&
                isSelected == that.isSelected &&
                isManuallyDeselected == that.isManuallyDeselected &&
                dndEnabled == that.dndEnabled &&
                dndBeforeMinutes == that.dndBeforeMinutes &&
                dndAfterMinutes == that.dndAfterMinutes &&
                Objects.equals(salahType, that.salahType) &&
                Objects.equals(definition, that.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, masjidId, salahType, isSelected, isManuallyDeselected, dndEnabled, dndBeforeMinutes, dndAfterMinutes, definition);
    }
}
