import io.qt.charts.*;
import io.qt.core.Qt;
import io.qt.gui.QPainter;
import io.qt.widgets.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WeatherApp {
    public static void main(String[] args) {
        QApplication.initialize(args);

        QWidget window = new QWidget();
        window.setWindowTitle("Wetterstation Citypark Karlsruhe");

        // Haupt-Layout
        QVBoxLayout layout = new QVBoxLayout();
        window.setLayout(layout);

        // Chart und ChartView erstellen
        QChart chart = new QChart();
        chart.setTitle("Wetterdaten Verlauf");
        QChartView chartView = new QChartView(chart);
        chartView.setRenderHint(QPainter.RenderHint.Antialiasing); // Macht die Linien glatter

        // Button und ChartView zum Layout hinzufügen
        QPushButton reloadButton = new QPushButton("Aktualisieren");
        layout.addWidget(reloadButton);
        layout.addWidget(chartView);

        // Aktion für den Button
        reloadButton.clicked.connect(() -> {
            try {
                // 1. Daten laden und parsen
                String json = WeatherApiClient.fetchWeatherData();
                List<WeatherDTO> data = WeatherDataParser.parse(json);

                // API liefert neueste Daten zuerst, für den Graph drehen wir die Reihenfolge um
                Collections.reverse(data);

                // 2. Bestehende Serien aus dem Chart entfernen, um sie neu zu zeichnen
                chart.removeAllSeries();
                for(var axis : chart.axes()) {
                    chart.removeAxis(axis);
                }

                // 3. Daten-Serien für den Graphen erstellen
                QLineSeries tempSeries = new QLineSeries();
                tempSeries.setName("Temperatur");

                QLineSeries humiditySeries = new QLineSeries();
                humiditySeries.setName("Feuchtigkeit");

                // Datenpunkte zu den Serien hinzufügen
                for (WeatherDTO r : data) {
                    // Zeitstempel von String in Millisekunden umwandeln
                    long timestamp = Instant.parse(r.timestamp).toEpochMilli();
                    tempSeries.append(timestamp, r.temperature);
                    humiditySeries.append(timestamp, r.humidity);
                }

                // 4. Serien zum Chart hinzufügen
                chart.addSeries(tempSeries);
                chart.addSeries(humiditySeries);

                // 5. Achsen konfigurieren
                // X-Achse (Zeit)
                QDateTimeAxis axisX = new QDateTimeAxis();
                axisX.setFormat("dd.MM HH:mm");
                axisX.setTitleText("Zeit");
                chart.addAxis(axisX, Qt.AlignmentFlag.AlignBottom);
                tempSeries.attachAxis(axisX);
                humiditySeries.attachAxis(axisX);

                // Y-Achse für Temperatur (links)
                QValueAxis axisYTemp = new QValueAxis();
                axisYTemp.setTitleText("Temperatur (°C)");
                chart.addAxis(axisYTemp, Qt.AlignmentFlag.AlignLeft);
                tempSeries.attachAxis(axisYTemp);

                // Y-Achse für Feuchtigkeit (rechts)
                QValueAxis axisYHumidity = new QValueAxis();
                axisYHumidity.setTitleText("Feuchtigkeit (%)");
                chart.addAxis(axisYHumidity, Qt.AlignmentFlag.AlignRight);
                humiditySeries.attachAxis(axisYHumidity);

                // Legende anzeigen
                Objects.requireNonNull(chart.legend()).setVisible(true);
                Objects.requireNonNull(chart.legend()).setAlignment(Qt.AlignmentFlag.AlignBottom);

            } catch (Exception e) {
                QMessageBox.critical(window, "Fehler", "Daten konnten nicht geladen werden:\n" + e.getMessage());
                e.printStackTrace();
            }
        });

        window.resize(800, 600);
        window.show();

        // Startet den Ladevorgang direkt beim Öffnen
        reloadButton.click();

        QApplication.exec();
    }
}