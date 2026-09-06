package com.qwibbler.salarm.salah;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MasjidDao {

    @Query("SELECT * FROM masjids")
    List<Masjid> getAll();

    @Query("SELECT * FROM masjids WHERE id = :id")
    Masjid getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Masjid masjid);

    @Update
    void update(Masjid masjid);

    @Delete
    void delete(Masjid masjid);

    @Query("DELETE FROM masjids WHERE id = :id")
    void deleteById(long id);
}
