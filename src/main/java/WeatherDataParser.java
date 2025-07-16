import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class WeatherDataParser {
    public static List<WeatherDTO> parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray body = root.getAsJsonArray("body");
        List<WeatherDTO> readings = new ArrayList<>();

        for (JsonElement el : body) {
            JsonObject obj = el.getAsJsonObject();
            JsonObject data = obj.getAsJsonObject("data");

            WeatherDTO reading = new WeatherDTO(
                    obj.get("measured_at").getAsString(),
                    data.get("temperature").getAsDouble(),
                    data.get("humidity").getAsDouble(),
                    data.get("pressure").getAsInt(),
                    data.get("irradiation").getAsInt()
            );
            readings.add(reading);
        }

        return readings;
    }
}
