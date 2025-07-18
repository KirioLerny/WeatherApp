import io.qt.charts.*;
import io.qt.core.Qt;
import io.qt.gui.QPainter;
import io.qt.widgets.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WeatherApp {
    public static void main(String[] args) {
        QApplication.initialize(args);

        QWidget window = new QWidget();
        window.setWindowTitle("Wetterstation Citypark Karlsruhe");

        QVBoxLayout layout = new QVBoxLayout();
        window.setLayout(layout);

        QChart chart = new QChart();
        chart.setTitle("Wetterdaten Verlauf");
        QChartView chartView = new QChartView(chart);
        chartView.setRenderHint(QPainter.RenderHint.Antialiasing);

        QPushButton reloadButton = new QPushButton("Aktualisieren");
        layout.addWidget(reloadButton);
        layout.addWidget(chartView);

        reloadButton.clicked.connect(() -> {
            try {
                String json = WeatherApiClient.fetchWeatherData();
                List<WeatherDTO> data = WeatherDataParser.parse(json);

                Collections.reverse(data);

                chart.removeAllSeries();
                for(var axis : chart.axes()) {
                    chart.removeAxis(axis);
                }

                QLineSeries tempSeries = new QLineSeries();
                tempSeries.setName("Temperatur");

                QLineSeries humiditySeries = new QLineSeries();
                humiditySeries.setName("Feuchtigkeit");

                for (WeatherDTO r : data) {
                    long timestamp = Instant.parse(r.timestamp).toEpochMilli();
                    tempSeries.append(timestamp, r.temperature);
                    humiditySeries.append(timestamp, r.humidity);
                }

                chart.addSeries(tempSeries);
                chart.addSeries(humiditySeries);

                QDateTimeAxis axisX = new QDateTimeAxis();
                axisX.setFormat("dd.MM HH:mm");
                axisX.setTitleText("Zeit");
                chart.addAxis(axisX, Qt.AlignmentFlag.AlignBottom);
                tempSeries.attachAxis(axisX);
                humiditySeries.attachAxis(axisX);

                QValueAxis axisYTemp = new QValueAxis();
                axisYTemp.setTitleText("Temperatur (°C)");
                chart.addAxis(axisYTemp, Qt.AlignmentFlag.AlignLeft);
                tempSeries.attachAxis(axisYTemp);

                QValueAxis axisYHumidity = new QValueAxis();
                axisYHumidity.setTitleText("Feuchtigkeit (%)");
                chart.addAxis(axisYHumidity, Qt.AlignmentFlag.AlignRight);
                humiditySeries.attachAxis(axisYHumidity);

                Objects.requireNonNull(chart.legend()).setVisible(true);
                Objects.requireNonNull(chart.legend()).setAlignment(Qt.AlignmentFlag.AlignBottom);

            } catch (Exception e) {
                QMessageBox.critical(window, "Fehler", "Daten konnten nicht geladen werden:\n" + e.getMessage());
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        });

        window.resize(800, 600);
        window.show();

        reloadButton.click();

        QApplication.exec();
    }
}