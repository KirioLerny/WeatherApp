package org.cityclim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cityclim.model.RainData;
import org.cityclim.model.WeatherData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeatherDataParser {

    public static List<WeatherData> parseWeatherData(String json) {
        List<WeatherData> readings = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("body") || !root.get("body").isJsonArray()) {
                return readings;
            }

            JsonArray body = root.getAsJsonArray("body");

            for (JsonElement el : body) {
                JsonObject obj = el.getAsJsonObject();
                JsonObject data = obj.getAsJsonObject("data");

                if (data.has("temperature") && data.has("humidity")) {
                    readings.add(new WeatherData(
                            obj.get("measured_at").getAsString(),
                            data.get("temperature").getAsDouble(),
                            data.get("humidity").getAsDouble(),
                            data.has("pressure") ? data.get("pressure").getAsInt() : 0,
                            data.has("irradiation") ? data.get("irradiation").getAsInt() : 0
                    ));
                }
            }

            Collections.reverse(readings);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return readings;
    }

    public static List<RainData> parseRainData(String json) {
        List<RainData> readings = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("body") || !root.get("body").isJsonArray()) {
                return readings;
            }

            JsonArray body = root.getAsJsonArray("body");

            for (JsonElement el : body) {
                JsonObject obj = el.getAsJsonObject();
                JsonObject data = obj.getAsJsonObject("data");

                if (data.has("clicks")) {
                    readings.add(new RainData(
                            obj.get("measured_at").getAsString(),
                            data.get("clicks").getAsInt()
                    ));
                }
            }

            Collections.reverse(readings);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return readings;
    }
}
