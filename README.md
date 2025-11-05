# Java Weather App (Console) — Assessment 1 & 2

- Assessment 1: current weather + 5-day forecast (OpenWeather; requires API key)
- Assessment 2: CRUD in SQLite + date-range temps (Open-Meteo; no key)

## Run
1) Install JDK 11+ and Maven.
2) Edit `config.properties` with your OpenWeather API key.
3) Demo video link; https://drive.google.com/file/d/1JwMiO_SUItogb2PhxI3egA7dim6N7Zi5/view?usp=sharing
4) Build & run:
```bash
mvn -q -DskipTests package
java -cp "target/java-weather-app-1.0.0.jar:~/.m2/repository/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar:~/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" com.assessment.app.WeatherApp
```
(Windows: replace `:` with `;` in classpath)
