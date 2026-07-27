package com.qwibbler.salarm.namaz;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Cache for resolved city coordinates to enable offline use.
 */
@Entity(tableName = "city_coordinates",
        indices = {@Index(value = {"nameKey"}, unique = true)})
public class CityCoordinate {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Normalized key: "name|province|country"
     */
    public String nameKey;

    public String cityName;
    public String province;
    public String country;

    public double latitude;
    public double longitude;

    public String countryCode;
    public String timeZoneId;

    public long lastUsed;

    public CityCoordinate() {
    }

    public CityCoordinate(String nameKey, String city, String province, String country, double lat, double lon, String countryCode, String tzId) {
        this.nameKey = nameKey;
        this.cityName = city;
        this.province = province;
        this.country = country;
        this.latitude = lat;
        this.longitude = lon;
        this.countryCode = countryCode;
        this.timeZoneId = tzId;
        this.lastUsed = System.currentTimeMillis();
    }
}
