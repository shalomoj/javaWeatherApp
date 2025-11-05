package com.assessment.app;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WeatherAPI {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static JsonObject getCurrentOpenWeather(double lat, double lon, String apiKey) throws IOException, InterruptedException {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s", lat, lon, apiKey);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("OpenWeather current failed: " + resp.statusCode());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    public static JsonObject get5DayForecast(double lat, double lon, String apiKey) throws IOException, InterruptedException {
        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s", lat, lon, apiKey);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("OpenWeather forecast failed: " + resp.statusCode());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    public static JsonObject getDailyRangeOpenMeteo(double lat, double lon, String startDate, String endDate) throws IOException, InterruptedException {
        String url = String.format(
                "https://archive-api.open-meteo.com/v1/era5?latitude=%f&longitude=%f&start_date=%s&end_date=%s&daily=temperature_2m_mean&timezone=auto",
                lat, lon, startDate, endDate);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("Open-Meteo archive failed: " + resp.statusCode());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    public static Map<String, Object> normalizeCurrent(JsonObject ow) {
        Map<String, Object> m = new HashMap<>();
        JsonObject main = ow.has("main") ? ow.getAsJsonObject("main") : new JsonObject();
        JsonArray weather = ow.has("weather") ? ow.getAsJsonArray("weather") : new JsonArray();
        JsonObject w = weather.size() > 0 ? weather.get(0).getAsJsonObject() : new JsonObject();
        JsonObject wind = ow.has("wind") ? ow.getAsJsonObject("wind") : new JsonObject();
        Double kelvin = main.has("temp") ? main.get("temp").getAsDouble() : null;
        Double c = kelvin != null ? kelvin - 273.15 : null;
        m.put("temp_c", c);
        m.put("humidity", main.has("humidity") ? main.get("humidity").getAsInt() : null);
        m.put("condition", w.has("main") ? w.get("main").getAsString() : null);
        m.put("description", w.has("description") ? w.get("description").getAsString() : null);
        m.put("wind_speed", wind.has("speed") ? wind.get("speed").getAsDouble() : null);
        m.put("timestamp", java.time.Instant.now().toString());
        return m;
    }

    public static boolean validDateRange(String s, String e) {
        try {
            LocalDate ss = LocalDate.parse(s);
            LocalDate ee = LocalDate.parse(e);
            return !ee.isBefore(ss);
        } catch (Exception ex) { return false; }
    }
}
