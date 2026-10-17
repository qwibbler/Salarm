package com.qwibbler.salarm.salah;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Model class representing a recently viewed city, custom location, or timezone in Salah times.
 */
public class RecentLocation {
    private String cityName;
    private String province;
    private String country;
    private String countryCode;
    double latitude;
    double longitude;
    private String timeZoneId;
    private boolean isCustom;
    private long timestamp;

    public RecentLocation(String cityName, String province, String country, String countryCode,
                          double latitude, double longitude, String timeZoneId, boolean isCustom, long timestamp) {
        this.cityName = cityName != null ? cityName : "";
        this.province = province != null ? province : "";
        this.country = country != null ? country : "";
        this.countryCode = countryCode != null ? countryCode : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZoneId = timeZoneId != null ? timeZoneId : "";
        this.isCustom = isCustom;
        this.timestamp = timestamp;
    }

    public String getCityName() {
        return cityName;
    }

    public String getProvince() {
        return province;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        if (!province.isEmpty()) sb.append(province);
        if (!country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("cityName", cityName);
        obj.put("province", province);
        obj.put("country", country);
        obj.put("countryCode", countryCode);
        obj.put("latitude", latitude);
        obj.put("longitude", longitude);
        obj.put("timeZoneId", timeZoneId);
        obj.put("isCustom", isCustom);
        obj.put("timestamp", timestamp);
        return obj;
    }

    public String toJsonString() {
        try {
            JSONObject obj = toJsonObject();
            String s = obj.toString();
            if (s != null && s.startsWith("{") && s.contains("cityName")) {
                return s;
            }
        } catch (Throwable ignored) {
        }
        return cityName + "|" + province + "|" + country + "|" + countryCode + "|" + latitude + "|" + longitude + "|" + timeZoneId + "|" + isCustom + "|" + timestamp;
    }

    public static RecentLocation fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        String cityName = obj.optString("cityName", "");
        String province = obj.optString("province", "");
        String country = obj.optString("country", "");
        String countryCode = obj.optString("countryCode", "");
        double latitude = obj.optDouble("latitude", 0.0);
        double longitude = obj.optDouble("longitude", 0.0);
        String timeZoneId = obj.optString("timeZoneId", "");
        boolean isCustom = obj.optBoolean("isCustom", false);
        long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
        return new RecentLocation(cityName, province, country, countryCode, latitude, longitude, timeZoneId, isCustom, timestamp);
    }

    public static RecentLocation fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return null;
        if (jsonStr.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                String cityName = obj.optString("cityName", "");
                if (!cityName.isEmpty()) {
                    return fromJsonObject(obj);
                }
            } catch (Throwable ignored) {
            }
        }
        String[] parts = jsonStr.split("\\|", -1);
        if (parts.length >= 9) {
            try {
                return new RecentLocation(
                        parts[0], parts[1], parts[2], parts[3],
                        Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                        parts[6], Boolean.parseBoolean(parts[7]), Long.parseLong(parts[8])
                );
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecentLocation that = (RecentLocation) o;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                isCustom == that.isCustom &&
                Objects.equals(cityName, that.cityName) &&
                Objects.equals(country, that.country) &&
                Objects.equals(timeZoneId, that.timeZoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityName, country, latitude, longitude, timeZoneId, isCustom);
    }

    @NonNull
    @Override
    public String toString() {
        return cityName + " (" + latitude + ", " + longitude + ", tz=" + timeZoneId + ")";
    }
}
