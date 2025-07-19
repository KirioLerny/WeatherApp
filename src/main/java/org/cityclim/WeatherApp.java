package org.cityclim;

import io.qt.charts.*;
import io.qt.core.QMetaObject;
import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.widgets.*;
import org.cityclim.model.RainData;
import org.cityclim.model.Station;
import org.cityclim.model.StationType;
import org.cityclim.model.WeatherData;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class WeatherApp extends QMainWindow {

    private static final List<Station> STATIONS = List.of(
            new Station("Kaiserplatz", "https://iot.skd-ka.de/api/v1/devices/c055eef5-b6dc-406e-ad5a-65dec60db90e/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.WEATHER),
            new Station("Albtalbahnhof", "https://iot.skd-ka.de/api/v1/devices/7ceb0590-e2f0-4f9e-a3dc-5257a4729f57/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.WEATHER),
            new Station("Citypark", "https://iot.skd-ka.de/api/v1/devices/d0fa8ca0-6339-4e20-9d76-88e80c7af2c4/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.WEATHER),
            new Station("Sonnenbad", "https://iot.skd-ka.de/api/v1/devices/a5264163-f9df-4995-9b35-0649e4119c85/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.WEATHER),
            new Station("Gemeinschaftsgarten W", "https://iot.skd-ka.de/api/v1/devices/ae034f41-c5bd-4fb0-97c3-02608da734ab/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.WEATHER),

            new Station("Gemeinschaftsgarten R", "https://iot.skd-ka.de/api/v1/devices/510a5f94-38ef-4aae-9a07-416237d1fa35/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.RAIN),
            new Station("Grünwettersbach", "https://iot.skd-ka.de/api/v1/devices/4ac74c30-4378-4182-86cb-f1e552d8f8fb/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.RAIN),
            new Station("Umweltamt", "https://iot.skd-ka.de/api/v1/devices/60e8e501-d0ec-44e8-9dbf-6983a5f97935/readings?limit=100&sort=measured_at&sort_direction=desc&auth=F20B6E04DCB4C114543B9E1BBACE3C26", StationType.RAIN)
    );

    private final QStackedWidget stackedWidget;
    private QChartView overviewChartView;
    private QChartView detailChartView;
    private QLabel detailTitleLabel;

    public WeatherApp() {
        setWindowTitle("Wetterdaten Karlsruhe");
        stackedWidget = new QStackedWidget();
        setCentralWidget(stackedWidget);

        QWidget overviewPage = createOverviewPage();
        QWidget detailPage = createDetailPage();

        stackedWidget.addWidget(overviewPage);
        stackedWidget.addWidget(detailPage);

        addToolBar(Qt.ToolBarArea.BottomToolBarArea, createStationToolBar());
        resize(900, 700);
        show();
        loadOverviewData();
    }

    private QWidget createOverviewPage() {
        QWidget page = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(page);
        QLabel title = new QLabel("Aktuelle Übersicht der Wetter- und Regenstationen");
        title.setAlignment(Qt.AlignmentFlag.AlignCenter);
        title.setObjectName("pageTitle");
        overviewChartView = new QChartView();
        overviewChartView.setRenderHint(QPainter.RenderHint.Antialiasing);
        layout.addWidget(title);
        layout.addWidget(overviewChartView);
        this.setStyleSheet("#pageTitle { font-size: 18px; font-weight: bold; margin-bottom: 10px; }");
        return page;
    }

    private QWidget createDetailPage() {
        QWidget page = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(page);
        QHBoxLayout topBarLayout = new QHBoxLayout();
        QPushButton homeButton = new QPushButton(QIcon.fromTheme("go-home"), "Zurück zur Übersicht");
        homeButton.clicked.connect(this::showOverviewPage);
        detailTitleLabel = new QLabel("Detailansicht");
        detailTitleLabel.setAlignment(Qt.AlignmentFlag.AlignCenter);
        detailTitleLabel.setObjectName("pageTitle");
        topBarLayout.addWidget(homeButton);
        topBarLayout.addStretch(1);
        topBarLayout.addWidget(detailTitleLabel);
        topBarLayout.addStretch(1);
        QWidget placeholder = new QWidget();
        placeholder.setFixedWidth(homeButton.sizeHint().width());
        topBarLayout.addWidget(placeholder);
        detailChartView = new QChartView();
        detailChartView.setRenderHint(QPainter.RenderHint.Antialiasing);
        layout.addLayout(topBarLayout);
        layout.addWidget(detailChartView);
        return page;
    }

    private QToolBar createStationToolBar() {
        QToolBar toolBar = new QToolBar("Stationen");
        toolBar.setMovable(false);
        toolBar.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon);
        for (Station station : STATIONS) {
            QIcon icon = QIcon.fromTheme(station.type() == StationType.WEATHER ? "weather-clear" : "weather-showers-scattered");
            QPushButton button = new QPushButton(icon, station.name());
            button.clicked.connect(() -> showStationDetailPage(station));
            toolBar.addWidget(button);
        }
        return toolBar;
    }

    private void showOverviewPage() {
        stackedWidget.setCurrentIndex(0);
        loadOverviewData();
    }

    private void showStationDetailPage(Station station) {
        detailTitleLabel.setText(station.name());
        stackedWidget.setCurrentIndex(1);
        loadStationDetailData(station);
    }

    private void loadOverviewData() {
        setDisabled(true);

        List<CompletableFuture<WeatherData>> weatherFutures = STATIONS.stream()
                .filter(s -> s.type() == StationType.WEATHER)
                .map(s -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String json = WeatherApiClient.fetch(s.url());
                        List<WeatherData> data = WeatherDataParser.parseWeatherData(json);
                        return data.isEmpty() ? null : data.get(data.size() - 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }))
                .toList();

        List<CompletableFuture<RainData>> rainFutures = STATIONS.stream()
                .filter(s -> s.type() == StationType.RAIN)
                .map(s -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String json = WeatherApiClient.fetch(s.url());
                        List<RainData> data = WeatherDataParser.parseRainData(json);
                        return data.isEmpty() ? null : data.get(data.size() - 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }))
                .toList();

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                CompletableFuture.allOf(weatherFutures.toArray(new CompletableFuture[0])),
                CompletableFuture.allOf(rainFutures.toArray(new CompletableFuture[0]))
        );

        allDone.thenRun(() -> {
            double avgTemp = weatherFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .mapToDouble(WeatherData::temperature)
                    .average()
                    .orElse(0.0);

            double avgClicks = rainFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .mapToDouble(RainData::clicks)
                    .average()
                    .orElse(0.0);

            QMetaObject.invokeMethod(this, () -> {
                updateOverviewChart(avgTemp, avgClicks);
                setDisabled(false);
            }, Qt.ConnectionType.QueuedConnection);
        });
    }

    private void updateOverviewChart(double avgTemp, double avgClicks) {
        QChart chart = new QChart();
        chart.setTitle("Durchschnittliche aktuelle Werte");
        QBarSeries series = new QBarSeries();
        QBarSet tempSet = new QBarSet("Temperatur");
        tempSet.append(avgTemp);
        QBarSet rainSet = new QBarSet("Regen-Clicks (Indikator)");
        rainSet.append(avgClicks);
        series.append(tempSet);
        series.append(rainSet);
        chart.addSeries(series);
        QBarCategoryAxis axisX = new QBarCategoryAxis();
        axisX.append("Temperatur (°C)");
        axisX.append("Regen (Clicks)");
        chart.addAxis(axisX, Qt.AlignmentFlag.AlignBottom);
        series.attachAxis(axisX);
        QValueAxis axisY = new QValueAxis();
        chart.addAxis(axisY, Qt.AlignmentFlag.AlignLeft);
        series.attachAxis(axisY);
        chart.legend().setVisible(true);
        chart.legend().setAlignment(Qt.AlignmentFlag.AlignBottom);
        overviewChartView.setChart(chart);
    }

    private void loadStationDetailData(Station station) {
        setDisabled(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                String json = WeatherApiClient.fetch(station.url());
                System.out.println("[" + station.name() + "] Response:\n" + json);
                return WeatherApiClient.fetch(station.url());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAccept(json -> {
            QMetaObject.invokeMethod(this, () -> {
                if (json == null) {
                    QMessageBox.critical(this, "Fehler", "Daten konnten nicht geladen werden.");
                    setDisabled(false);
                    return;
                }

                QChart chart = new QChart();
                chart.setTitle("Datenverlauf für " + station.name());

                if (station.type() == StationType.WEATHER) {
                    List<WeatherData> data = WeatherDataParser.parseWeatherData(json);

                    QLineSeries tempSeries = new QLineSeries();
                    tempSeries.setName("Temperatur");

                    QLineSeries humiditySeries = new QLineSeries();
                    humiditySeries.setName("Feuchtigkeit");

                    for (WeatherData r : data) {
                        long timestamp = Instant.parse(r.timestamp()).toEpochMilli();
                        tempSeries.append(timestamp, r.temperature());
                        humiditySeries.append(timestamp, r.humidity());
                    }

                    chart.addSeries(tempSeries);
                    chart.addSeries(humiditySeries);
                    setupAxes(chart, tempSeries, humiditySeries);

                } else {
                    List<RainData> data = WeatherDataParser.parseRainData(json);

                    QLineSeries rainSeries = new QLineSeries();
                    rainSeries.setName("Regen (Clicks)");

                    for (RainData r : data) {
                        long timestamp = Instant.parse(r.timestamp()).toEpochMilli();
                        rainSeries.append(timestamp, r.clicks());
                    }

                    chart.addSeries(rainSeries);
                    setupAxes(chart, rainSeries);
                }

                chart.legend().setVisible(true);
                chart.legend().setAlignment(Qt.AlignmentFlag.AlignBottom);

                detailChartView.setChart(chart);
                setDisabled(false);
            }, Qt.ConnectionType.QueuedConnection);
        });
    }

    private QChart createDetailChart(Station station, String json) {
        QChart chart = new QChart();
        chart.setTitle("Datenverlauf für " + station.name());
        if (station.type() == StationType.WEATHER) {
            List<WeatherData> data = WeatherDataParser.parseWeatherData(json);
            QLineSeries tempSeries = new QLineSeries();
            tempSeries.setName("Temperatur");
            QLineSeries humiditySeries = new QLineSeries();
            humiditySeries.setName("Feuchtigkeit");
            for (WeatherData r : data) {
                long timestamp = Instant.parse(r.timestamp()).toEpochMilli();
                tempSeries.append(timestamp, r.temperature());
                humiditySeries.append(timestamp, r.humidity());
            }
            chart.addSeries(tempSeries);
            chart.addSeries(humiditySeries);
            setupAxes(chart, tempSeries, humiditySeries);
        } else {
            List<RainData> data = WeatherDataParser.parseRainData(json);
            QLineSeries rainSeries = new QLineSeries();
            rainSeries.setName("Regen (Clicks)");
            for (RainData r : data) {
                long timestamp = Instant.parse(r.timestamp()).toEpochMilli();
                rainSeries.append(timestamp, r.clicks());
            }
            chart.addSeries(rainSeries);
            setupAxes(chart, rainSeries);
        }
        chart.legend().setVisible(true);
        chart.legend().setAlignment(Qt.AlignmentFlag.AlignBottom);
        return chart;
    }

    private void setupAxes(QChart chart, QXYSeries series1, QXYSeries series2) {
        QDateTimeAxis axisX = new QDateTimeAxis();
        axisX.setFormat("dd.MM HH:mm");
        axisX.setTitleText("Zeit");
        chart.addAxis(axisX, Qt.AlignmentFlag.AlignBottom);
        series1.attachAxis(axisX);
        series2.attachAxis(axisX);
        QValueAxis axisY1 = new QValueAxis();
        axisY1.setTitleText("Temperatur (°C)");
        chart.addAxis(axisY1, Qt.AlignmentFlag.AlignLeft);
        series1.attachAxis(axisY1);
        QValueAxis axisY2 = new QValueAxis();
        axisY2.setTitleText("Feuchtigkeit (%)");
        chart.addAxis(axisY2, Qt.AlignmentFlag.AlignRight);
        series2.attachAxis(axisY2);
    }

    private void setupAxes(QChart chart, QXYSeries series) {
        QDateTimeAxis axisX = new QDateTimeAxis();
        axisX.setFormat("dd.MM HH:mm");
        axisX.setTitleText("Zeit");
        chart.addAxis(axisX, Qt.AlignmentFlag.AlignBottom);
        series.attachAxis(axisX);
        QValueAxis axisY = new QValueAxis();
        axisY.setTitleText("Regen (kumulierte Clicks)");
        chart.addAxis(axisY, Qt.AlignmentFlag.AlignLeft);
        series.attachAxis(axisY);
    }

    public static void main(String[] args) {
        QApplication.initialize(args);
        new WeatherApp();
        QApplication.exec();
    }
}