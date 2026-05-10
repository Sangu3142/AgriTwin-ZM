package agritwin.ui;

import agritwin.model.SimulationRecord;
import agritwin.service.ReportGenerator;
import agritwin.ui.model.ReportSummary;
import agritwin.util.FileHandler;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportDetailPage {

    private static final String PAGE_BG = "#0F1535";
    private static final String CARD_BG = "#111C44";
    private static final String CARD_BORDER = "#1A2A6C";
    private static final String ACCENT_CYAN = "#00B5D8";
    private static final String ACCENT_TEAL = "#00CFB4";
    private static final String CORAL = "#F56565";
    private static final String GOLD = "#ECC94B";
    private static final String TEXT_PRI = "#FFFFFF";
    private static final String TEXT_SEC = "#A0AEC0";
    private static final String TEXT_MUTED = "#718096";

    private final FileHandler fileHandler = new FileHandler();
    private final ReportGenerator reporter = new ReportGenerator();

    public VBox buildView(ReportSummary summary, Runnable onBack) {
        List<SimulationRecord> records = new ArrayList<>();
        try {
            records = fileHandler.loadSimulation(summary.getFilePath());
        } catch (IOException e) {
            System.err.println("Failed to load report: " + summary.getFilePath());
        }

        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setBackground(bg(PAGE_BG, 0));

        page.getChildren().add(buildHeader(summary, records, onBack));

        HBox chartsTop = buildChartsTop(records);
        VBox chart3Wrapper = buildFsiChartCard(records);

        page.getChildren().add(chartsTop);
        page.getChildren().add(chart3Wrapper);
        page.getChildren().add(buildDataTableCard(records));

        animateCharts(chartsTop, chart3Wrapper);
        return page;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader(ReportSummary summary, List<SimulationRecord> records, Runnable onBack) {
        Button backBtn = outlineButton("← Reports");
        backBtn.setOnAction(e -> onBack.run());

        Label title = new Label(summary.getReportName());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_PRI));

        String sub = summary.getCommodityType() + " · Saved " + summary.getDateSaved()
                + " · " + summary.getRecordCount() + " months of data";
        Label subtitle = new Label(sub);
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web(TEXT_MUTED));

        VBox titleBox = new VBox(4, title, subtitle);

        HBox headerRow = new HBox(12, backBtn, titleBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        return headerRow;
    }

    // ── Chart row 1: Surplus + Revenue ────────────────────────────────────────

    private HBox buildChartsTop(List<SimulationRecord> records) {
        VBox surplusCard = buildSurplusChartCard(records);
        VBox revenueCard = buildRevenueChartCard(records);
        HBox.setHgrow(surplusCard, Priority.ALWAYS);
        HBox.setHgrow(revenueCard, Priority.ALWAYS);
        HBox row = new HBox(16, surplusCard, revenueCard);
        row.setOpacity(0);
        return row;
    }

    private VBox buildSurplusChartCard(List<SimulationRecord> records) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Tons");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Surplus / Shortage Trend (t)");
        chart.setAnimated(false);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color:#111C44;");
        HBox.setHgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> surplusSeries = new XYChart.Series<>();
        surplusSeries.setName("Surplus");
        XYChart.Series<String, Number> shortageSeries = new XYChart.Series<>();
        shortageSeries.setName("Shortage");
        XYChart.Series<String, Number> zeroSeries = new XYChart.Series<>();
        zeroSeries.setName("");

        if (records.size() >= 1) {
            for (SimulationRecord r : records) {
                String lbl = "M" + r.getMonthNumber();
                double val = r.getSurplusOrShortage();
                if (val >= 0) {
                    surplusSeries.getData().add(new XYChart.Data<>(lbl, val));
                    shortageSeries.getData().add(new XYChart.Data<>(lbl, 0));
                } else {
                    surplusSeries.getData().add(new XYChart.Data<>(lbl, 0));
                    shortageSeries.getData().add(new XYChart.Data<>(lbl, val));
                }
                zeroSeries.getData().add(new XYChart.Data<>(lbl, 0));
            }
        }

        chart.getData().add(surplusSeries);
        chart.getData().add(shortageSeries);
        chart.getData().add(zeroSeries);

        // Style series after layout
        Platform.runLater(() -> {
            applyLineStyle(chart, 0, "#00B5D8", "2", "");
            applyLineStyle(chart, 1, "#F56565", "2", "");
            applyLineStyle(chart, 2, "#718096", "1", "4 4");
            // Hide symbols on zero line
            for (XYChart.Data<String, Number> d : zeroSeries.getData()) {
                if (d.getNode() != null)
                    d.getNode().setVisible(false);
            }
        });

        VBox card = card(chart, "Surplus / Shortage Trend (t)");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildRevenueChartCard(List<SimulationRecord> records) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("ZMW");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Revenue per Month (ZMW)");
        chart.setAnimated(false);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color:#111C44;");
        HBox.setHgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (ZMW)");

        if (records.size() >= 1) {
            for (SimulationRecord r : records) {
                XYChart.Data<String, Number> bar = new XYChart.Data<>("M" + r.getMonthNumber(), r.getRevenue());
                series.getData().add(bar);
            }
        }
        chart.getData().add(series);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> bar : series.getData()) {
                if (bar.getNode() != null) {
                    bar.getNode().setStyle("-fx-bar-fill:#4A90D9;");
                    Tooltip.install(bar.getNode(),
                            new Tooltip("ZMW " + String.format("%,.0f", bar.getYValue().doubleValue())));
                }
            }
        });

        VBox card = card(chart, "Revenue per Month (ZMW)");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── Chart row 2: FSI AreaChart ─────────────────────────────────────────────

    private VBox buildFsiChartCard(List<SimulationRecord> records) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("FSI (%)");

        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("Food Security Indicator (%)");
        chart.setAnimated(false);
        chart.setPrefHeight(280);
        chart.setStyle("-fx-background-color:#111C44;");
        chart.setOpacity(0);

        XYChart.Series<String, Number> fsiSeries = new XYChart.Series<>();
        fsiSeries.setName("FSI (%)");
        XYChart.Series<String, Number> line80Series = new XYChart.Series<>();
        line80Series.setName("— 80% target");
        XYChart.Series<String, Number> line60Series = new XYChart.Series<>();
        line60Series.setName("— 60% critical");

        if (records.size() >= 1) {
            for (SimulationRecord r : records) {
                String lbl = "M" + r.getMonthNumber();
                fsiSeries.getData().add(new XYChart.Data<>(lbl, r.getFoodSecurityIndicator()));
                line80Series.getData().add(new XYChart.Data<>(lbl, 80.0));
                line60Series.getData().add(new XYChart.Data<>(lbl, 60.0));
            }
        }
        chart.getData().add(fsiSeries);
        chart.getData().add(line80Series);
        chart.getData().add(line60Series);

        // Style reference lines after layout: hide their area fills, colour strokes
        Platform.runLater(() -> {
            applyAreaLineStyle(chart, 1, "#ECC94B", "1.5", "6 4");
            applyAreaLineStyle(chart, 2, "#F56565", "1.5", "6 4");
            hideAreaFill(chart, 1);
            hideAreaFill(chart, 2);
            // Legend label colours for reference lines
            chart.lookupAll(".chart-legend-item-symbol").forEach(n -> n.setStyle(""));
        });

        // FSI chart fades in with delay 300ms (handled by animateCharts caller)
        VBox wrapper = card(chart, null);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    // ── Data table ─────────────────────────────────────────────────────────────

    private VBox buildDataTableCard(List<SimulationRecord> records) {
        Label heading = new Label("Full Simulation Data");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web(TEXT_PRI));

        TableView<SimulationRecord> table = new TableView<>(
                FXCollections.observableArrayList(records));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setStyle("-fx-background-color:#111C44;-fx-border-color:#1A2A6C;");

        table.getColumns().add(colText("Month", 180, r -> "Month " + r.getMonthNumber()));
        table.getColumns().add(colText("Commodity", 150, SimulationRecord::getCommodityName));
        table.getColumns().add(colNum("Production (t)", 170, r -> String.format("%,.1f", r.getProduction())));
        table.getColumns().add(colNum("Demand (t)", 150, r -> String.format("%,.1f", r.getDemand())));
        table.getColumns().add(colSurplus());
        table.getColumns().add(colNum("Waste (t)", 130, r -> String.format("%,.1f", r.getWaste())));
        table.getColumns().add(colNum("Revenue (ZMW)", 180, r -> "ZMW " + String.format("%,.0f", r.getRevenue())));
        table.getColumns().add(colFsi());
        table.getColumns().add(colAlert());

        VBox card = new VBox(12, heading, table);
        card.setPadding(new Insets(20));
        card.setBackground(bg(CARD_BG, 12));
        card.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(8);
        s.setOffsetY(2);
        s.setColor(Color.color(0, 0, 0, 0.3));
        card.setEffect(s);
        return card;
    }

    @FunctionalInterface
    private interface RecordText {
        String get(SimulationRecord r);
    }

    private TableColumn<SimulationRecord, String> colText(String title, double width, RecordText fn) {
        TableColumn<SimulationRecord, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellFactory(tc -> new TableCell<SimulationRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(fn.get(getTableRow().getItem()));
                    setTextFill(Color.web(TEXT_SEC));
                }
            }
        });
        return col;
    }

    private TableColumn<SimulationRecord, String> colNum(String title, double width, RecordText fn) {
        return colText(title, width, fn);
    }

    private TableColumn<SimulationRecord, String> colSurplus() {
        TableColumn<SimulationRecord, String> col = new TableColumn<>("Surplus (t)");
        col.setPrefWidth(150);
        col.setCellFactory(tc -> new TableCell<SimulationRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    SimulationRecord r = getTableRow().getItem();
                    double val = r.getSurplusOrShortage();
                    setText(String.format("%,.1f", val));
                    setTextFill(Color.web(val >= 0 ? ACCENT_TEAL : CORAL));
                }
            }
        });
        return col;
    }

    private TableColumn<SimulationRecord, String> colFsi() {
        TableColumn<SimulationRecord, String> col = new TableColumn<>("Food Security");
        col.setPrefWidth(140);
        col.setCellFactory(tc -> new TableCell<SimulationRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    SimulationRecord r = getTableRow().getItem();
                    double fsi = r.getFoodSecurityIndicator();
                    setText(String.format("%.1f%%", fsi));
                    setTextFill(Color.web(fsi >= 80 ? ACCENT_TEAL : fsi >= 60 ? GOLD : CORAL));
                }
            }
        });
        return col;
    }

    private TableColumn<SimulationRecord, String> colAlert() {
        TableColumn<SimulationRecord, String> col = new TableColumn<>("Policy Alert");
        col.setPrefWidth(220);
        col.setCellFactory(tc -> new TableCell<SimulationRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    SimulationRecord r = getTableRow().getItem();
                    String alert = r.getPolicyAlerts();
                    if (alert == null || alert.trim().isEmpty()) {
                        setText("✓ OK");
                        setTextFill(Color.web(ACCENT_TEAL));
                    } else {
                        setText(alert);
                        setTextFill(Color.web(GOLD));
                    }
                }
            }
        });
        return col;
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private void animateCharts(HBox chartsTop, VBox chart3Wrapper) {
        // Chart 1+2 (HBox): fade 0ms delay
        FadeTransition ft1 = new FadeTransition(Duration.millis(300), chartsTop);
        ft1.setFromValue(0);
        ft1.setToValue(1);

        // Chart 3: fade 300ms delay
        javafx.scene.Node chart3Node = chart3Wrapper.getChildren().stream()
                .filter(n -> n instanceof AreaChart).findFirst().orElse(chart3Wrapper);
        FadeTransition ft3 = new FadeTransition(Duration.millis(300), chart3Node);
        ft3.setFromValue(0);
        ft3.setToValue(1);

        PauseTransition delay3 = new PauseTransition(Duration.millis(300));
        delay3.setOnFinished(e -> ft3.play());

        ft1.play();
        delay3.play();
    }

    // ── Chart style helpers ────────────────────────────────────────────────────

    private void applyLineStyle(LineChart<?, ?> chart, int seriesIdx,
            String color, String width, String dashArray) {
        if (seriesIdx >= chart.getData().size())
            return;
        String selector = ".default-color" + seriesIdx + ".chart-series-line";
        chart.lookupAll(selector).forEach(n -> {
            String style = "-fx-stroke:" + color + ";-fx-stroke-width:" + width + ";";
            if (!dashArray.isEmpty())
                style += "-fx-stroke-dash-array:" + dashArray + ";";
            n.setStyle(style);
        });
    }

    private void applyAreaLineStyle(AreaChart<?, ?> chart, int seriesIdx,
            String color, String width, String dashArray) {
        if (seriesIdx >= chart.getData().size())
            return;
        String selector = ".default-color" + seriesIdx + ".chart-series-area-line";
        chart.lookupAll(selector).forEach(n -> {
            String style = "-fx-stroke:" + color + ";-fx-stroke-width:" + width + ";";
            if (!dashArray.isEmpty())
                style += "-fx-stroke-dash-array:" + dashArray + ";";
            n.setStyle(style);
        });
    }

    private void hideAreaFill(AreaChart<?, ?> chart, int seriesIdx) {
        if (seriesIdx >= chart.getData().size())
            return;
        String selector = ".default-color" + seriesIdx + ".chart-series-area-fill";
        chart.lookupAll(selector).forEach(n -> n.setStyle("-fx-fill:transparent;"));
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private VBox card(javafx.scene.Node content, String titleText) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setBackground(bg(CARD_BG, 12));
        card.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(8);
        s.setOffsetY(2);
        s.setColor(Color.color(0, 0, 0, 0.3));
        card.setEffect(s);
        if (titleText != null && !titleText.isEmpty()) {
            Label lbl = new Label(titleText);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lbl.setTextFill(Color.web(TEXT_PRI));
            card.getChildren().add(lbl);
        }
        card.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private Button outlineButton(String text) {
        Button btn = new Button(text);
        btn.setTextFill(Color.web(ACCENT_CYAN));
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setBackground(bg("transparent", 8));
        btn.setBorder(new Border(new BorderStroke(Color.web(ACCENT_CYAN),
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5))));
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setPadding(new Insets(8, 16, 8, 16));
        return btn;
    }

    private Background bg(String hex, double radius) {
        Color c = "transparent".equalsIgnoreCase(hex) ? Color.TRANSPARENT : Color.web(hex);
        return new Background(new BackgroundFill(c, new CornerRadii(radius), Insets.EMPTY));
    }
}
