import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherApiClient {
    private static final String API_URL = "https://iot.skd-ka.de/api/v1/devices/d0fa8ca0-6339-4e20-9d76-88e80c7af2c4/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26";

    public static String fetchWeatherData() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        client.close();
        return response.body();
    }
}
