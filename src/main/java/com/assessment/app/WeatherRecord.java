package com.assessment.app;

public class WeatherRecord {
    public int id;
    public String createdAt;
    public String locationInput;
    public String resolvedLocation;
    public double latitude;
    public double longitude;
    public String startDate;
    public String endDate;
    public String provider;
    public String payloadJson;

    @Override
    public String toString() {
        return String.format("#%d @%s | %s -> %s (%.4f, %.4f) [%s..%s] provider=%s",
                id, createdAt, locationInput, resolvedLocation, latitude, longitude,
                startDate, endDate, provider);
    }
}
