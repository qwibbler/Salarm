package com.qwibbler.salarm.salah;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MasjidEventDao {
    @Query("SELECT * FROM masjid_events")
    List<MasjidEvent> getAll();

    @Query("SELECT * FROM masjid_events WHERE id = :id")
    MasjidEvent getById(long id);

    @Query("SELECT * FROM masjid_events WHERE masjidId = :masjidId AND salahType = :type LIMIT 1")
    MasjidEvent getMasjidEvent(long masjidId, String type);

    @Query("SELECT * FROM masjid_events WHERE masjidId = :masjidId")
    List<MasjidEvent> getByMasjidId(long masjidId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MasjidEvent event);

    @Update
    void update(MasjidEvent event);

    @Delete
    void delete(MasjidEvent event);

    @Query("DELETE FROM masjid_events WHERE id = :id")
    void deleteById(long id);
}
