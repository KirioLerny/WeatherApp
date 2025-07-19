package org.cityclim.model;

public record WeatherData(String timestamp, double temperature, double humidity, int pressure, int irradiation) {
}