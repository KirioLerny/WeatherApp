package org.cityclim;

public class WeatherDTO {
    public String timestamp;
    public double temperature;
    public double humidity;
    public int pressure;
    public int irradiation;

    public WeatherDTO(String timestamp, double temperature, double humidity, int pressure, int irradiation) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        this.irradiation = irradiation;
    }
}
