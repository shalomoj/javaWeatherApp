package com.assessment.app;

import com.google.gson.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

public class WeatherApp {

    private static final Scanner IN = new Scanner(System.in);
    private static String OPENWEATHER_API_KEY;
    private static String DB_PATH;
    private static WeatherDatabase DB;

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("⚠️ Missing config.properties. Using defaults.");
            props.setProperty("openweather.apiKey", "YOUR_OPENWEATHER_API_KEY_HERE");
            props.setProperty("database.path", "weather.db");
        }
        OPENWEATHER_API_KEY = props.getProperty("openweather.apiKey", "YOUR_OPENWEATHER_API_KEY_HERE");
        DB_PATH = props.getProperty("database.path", "weather.db");
        DB = new WeatherDatabase(DB_PATH);

        System.out.println("=== Java Weather App (Assessments 1 & 2) ===");
        loop();
    }

    private static void loop() {
        while (true) {
            System.out.println();
            System.out.println("1) Current weather (Assessment 1)");
            System.out.println("2) 5-day forecast (Assessment 1)");
            System.out.println("3) CREATE (store date-range temps) (Assessment 2)");
            System.out.println("4) READ all (Assessment 2)");
            System.out.println("5) UPDATE record (Assessment 2)");
            System.out.println("6) DELETE record (Assessment 2)");
            System.out.println("7) Export (JSON/CSV) (optional)");
            System.out.println("0) Exit");
            System.out.print("Select: ");
            String choice = IN.nextLine().trim();
            try {
                switch (choice) {
                    case "1": doCurrent(); break;
                    case "2": doForecast(); break;
                    case "3": doCreate(); break;
                    case "4": doRead(); break;
                    case "5": doUpdate(); break;
                    case "6": doDelete(); break;
                    case "7": doExport(); break;
                    case "0": System.out.println("Bye"); return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static void doCurrent() throws Exception {
        if (OPENWEATHER_API_KEY == null || OPENWEATHER_API_KEY.contains("YOUR_OPENWEATHER_API_KEY_HERE")) {
            System.out.println("⚠️ Set openweather.apiKey in config.properties first.");
            return;
        }
        System.out.print("Enter location (city/ZIP/landmark or 'lat,lon'): ");
        String input = IN.nextLine();
        Geocoder.GeoResult g = Geocoder.resolve(input);
        if (g == null || g.lat == null || g.lon == null) {
            System.out.println("Could not resolve that location.");
            return;
        }
        JsonObject ow = WeatherAPI.getCurrentOpenWeather(g.lat, g.lon, OPENWEATHER_API_KEY);
        Map<String, Object> norm = WeatherAPI.normalizeCurrent(ow);
        System.out.println("Resolved: " + g.displayName);
        System.out.printf("Temp: %s °C | Humidity: %s | Cond: %s (%s) | Wind: %s m/s%n",
                norm.get("temp_c"), norm.get("humidity"), norm.get("condition"), norm.get("description"), norm.get("wind_speed"));
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(ow));
    }

    private static void doForecast() throws Exception {
        if (OPENWEATHER_API_KEY == null || OPENWEATHER_API_KEY.contains("YOUR_OPENWEATHER_API_KEY_HERE")) {
            System.out.println("⚠️ Set openweather.apiKey in config.properties first.");
            return;
        }
        System.out.print("Enter location (city/ZIP/landmark or 'lat,lon'): ");
        String input = IN.nextLine();
        Geocoder.GeoResult g = Geocoder.resolve(input);
        if (g == null || g.lat == null || g.lon == null) {
            System.out.println("Could not resolve that location.");
            return;
        }
        JsonObject fc = WeatherAPI.get5DayForecast(g.lat, g.lon, OPENWEATHER_API_KEY);
        System.out.println("Resolved: " + g.displayName);
        JsonArray list = fc.getAsJsonArray("list");
        System.out.println("Showing first ~10 forecast entries:");
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            JsonObject it = list.get(i).getAsJsonObject();
            double tempK = it.getAsJsonObject("main").get("temp").getAsDouble();
            double tempC = tempK - 273.15;
            String desc = it.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
            String dtTxt = it.has("dt_txt") ? it.get("dt_txt").getAsString() : "n/a";
            System.out.printf(" - %s | %.2f °C | %s%n", dtTxt, tempC, desc);
        }
    }

    private static void doCreate() throws Exception {
        System.out.print("Enter location (city/ZIP/landmark or 'lat,lon'): ");
        String input = IN.nextLine();
        System.out.print("Start date (YYYY-MM-DD): ");
        String start = IN.nextLine().trim();
        System.out.print("End date (YYYY-MM-DD): ");
        String end = IN.nextLine().trim();

        if (!WeatherAPI.validDateRange(start, end)) {
            System.out.println("Invalid date range.");
            return;
        }
        Geocoder.GeoResult g = Geocoder.resolve(input);
        if (g == null || g.lat == null || g.lon == null) {
            System.out.println("Could not resolve that location.");
            return;
        }
        JsonObject payload = WeatherAPI.getDailyRangeOpenMeteo(g.lat, g.lon, start, end);

        WeatherRecord r = new WeatherRecord();
        r.createdAt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        r.locationInput = input;
        r.resolvedLocation = g.displayName;
        r.latitude = g.lat;
        r.longitude = g.lon;
        r.startDate = start;
        r.endDate = end;
        r.provider = "open-meteo";
        r.payloadJson = new Gson().toJson(payload);

        int id = DB.create(r);
        System.out.println("Stored record id=" + id + " for " + g.displayName);
    }

    private static void doRead() {
        List<WeatherRecord> rows = DB.readAll();
        if (rows.isEmpty()) { System.out.println("No records."); return; }
        rows.forEach(System.out::println);
    }

    private static void doUpdate() throws Exception {
        List<WeatherRecord> rows = DB.readAll();
        if (rows.isEmpty()) { System.out.println("No records."); return; }
        IntStream.range(0, rows.size()).forEach(i -> System.out.println((i+1) + ") " + rows.get(i)));
        System.out.print("Select record #: ");
        int idx = Integer.parseInt(IN.nextLine().trim()) - 1;
        if (idx < 0 || idx >= rows.size()) { System.out.println("Invalid selection."); return; }
        WeatherRecord sel = rows.get(idx);

        System.out.print("New location (blank to keep): ");
        String newLoc = IN.nextLine();
        System.out.print("New start date (YYYY-MM-DD, blank to keep): ");
        String newStart = IN.nextLine();
        System.out.print("New end date (YYYY-MM-DD, blank to keep): ");
        String newEnd = IN.nextLine();

        String updLoc = Utils.isBlank(newLoc) ? null : newLoc;
        String updStart = Utils.isBlank(newStart) ? null : newStart;
        String updEnd = Utils.isBlank(newEnd) ? null : newEnd;

        Geocoder.GeoResult g = null;
        Double newLat = null, newLon = null;
        String newResolved = null;
        if (updLoc != null) {
            g = Geocoder.resolve(updLoc);
            if (g == null || g.lat == null || g.lon == null) {
                System.out.println("Could not resolve new location.");
                return;
            }
            newLat = g.lat; newLon = g.lon; newResolved = g.displayName;
        }

        boolean needRefetch = (updLoc != null) || (updStart != null && updEnd != null);
        String newPayload = null;
        if (needRefetch) {
            double lat = newLat != null ? newLat : sel.latitude;
            double lon = newLon != null ? newLon : sel.longitude;
            String s = updStart != null ? updStart : sel.startDate;
            String e = updEnd != null ? updEnd : sel.endDate;
            if (!WeatherAPI.validDateRange(s, e)) {
                System.out.println("Invalid date range.");
                return;
            }
            JsonObject payload = WeatherAPI.getDailyRangeOpenMeteo(lat, lon, s, e);
            newPayload = new Gson().toJson(payload);
        }

        DB.updateBasic(sel.id, updLoc, updStart, updEnd, newResolved, newLat, newLon, newPayload);
        System.out.println("Record updated.");
    }

    private static void doDelete() {
        List<WeatherRecord> rows = DB.readAll();
        if (rows.isEmpty()) { System.out.println("No records."); return; }
        IntStream.range(0, rows.size()).forEach(i -> System.out.println((i+1) + ") " + rows.get(i)));
        System.out.print("Select record # to delete: ");
        int idx = Integer.parseInt(IN.nextLine().trim()) - 1;
        if (idx < 0 || idx >= rows.size()) { System.out.println("Invalid selection."); return; }
        DB.delete(rows.get(idx).id);
        System.out.println("Deleted.");
    }

    private static void doExport() {
        List<WeatherRecord> rows = DB.readAll();
        if (rows.isEmpty()) { System.out.println("No records."); return; }
        System.out.println("1) JSON to stdout");
        System.out.println("2) CSV to stdout");
        System.out.print("Choose: ");
        String ch = IN.nextLine().trim();
        if (ch.equals("1")) {
            System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(rows));
        } else if (ch.equals("2")) {
            System.out.println("id,created_at,location_input,resolved_location,lat,lon,start_date,end_date,provider");
            for (WeatherRecord r : rows) {
                System.out.printf("%d,%s,%s,%s,%.6f,%.6f,%s,%s,%s%n",
                        r.id, safe(r.createdAt), csv(r.locationInput), csv(r.resolvedLocation),
                        r.latitude, r.longitude, safe(r.startDate), safe(r.endDate), safe(r.provider));
            }
        } else {
            System.out.println("No export.");
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String csv(String s) {
        if (s == null) return "";
        String t = s.replace("", "");
        return "" + t + "";
    }
}
