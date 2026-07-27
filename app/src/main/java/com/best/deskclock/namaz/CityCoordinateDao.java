package com.best.deskclock.namaz;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CityCoordinateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CityCoordinate coordinate);

    @Query("SELECT * FROM city_coordinates WHERE nameKey = :nameKey LIMIT 1")
    CityCoordinate findByKey(String nameKey);

    @Query("SELECT * FROM city_coordinates WHERE nameKey LIKE '%' || :query || '%'")
    List<CityCoordinate> searchByName(String query);

    @Query("DELETE FROM city_coordinates WHERE lastUsed < :timestamp")
    void deleteOlderThan(long timestamp);
}
