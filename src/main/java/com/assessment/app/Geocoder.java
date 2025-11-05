package com.assessment.app;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Geocoder {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static class GeoResult {
        public Double lat;
        public Double lon;
        public String displayName;
    }

    public static GeoResult resolve(String input) throws IOException, InterruptedException {
        if (input == null) return null;
        input = input.trim();
        if (input.contains(",")) {
            try {
                String[] parts = input.split(",", 2);
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                GeoResult r = new GeoResult();
                r.lat = lat; r.lon = lon; r.displayName = lat + "," + lon;
                return r;
            } catch (Exception ignored) { }
        }
        String q = URLEncoder.encode(input, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + q;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "java-weather-app/1.0 (assessment)")
                .GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
        if (arr.size() == 0) return null;
        JsonObject o = arr.get(0).getAsJsonObject();
        GeoResult r = new GeoResult();
        r.lat = o.has("lat") ? Double.valueOf(o.get("lat").getAsString()) : null;
        r.lon = o.has("lon") ? Double.valueOf(o.get("lon").getAsString()) : null;
        r.displayName = o.has("display_name") ? o.get("display_name").getAsString() : input;
        return r;
    }
}
