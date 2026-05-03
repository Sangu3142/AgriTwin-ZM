package agritwin.ui;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.StorageType;
import agritwin.model.Tomato;
import agritwin.service.ReportGenerator;
import agritwin.service.SimulationRunner;
import agritwin.service.StorageLossSimulator;
import agritwin.util.FileHandler;
import agritwin.util.RecordStore;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * MODULE: JavaFX GUI Application (optional)
 * PURPOSE: Provides a tabbed graphical front-end over the SAME service
 *          classes used by ConsoleUI. Tab 1 collects inputs, Tab 2 shows a
 *          live TableView of simulation records bound via ObservableList,
 *          Tab 3 mirrors the textual report output, and Tab 4 shows three
 *          charts visualising the simulation results. Simulation logic is
 *          delegated entirely to SimulationRunner; this file handles only
 *          input collection and output display.
 * OOP CONCEPTS USED: Inheritance (extends Application); Composition;
 *                    Polymorphism — files declared as RecordStore interface;
 *                    Property-based binding (JavaFX observables) — TableView
 *                    rows use SimpleDoubleProperty / SimpleStringProperty
 *                    wrappers so the table auto-updates when the list changes;
 *                    the same ObservableList pattern drives the chart data via
 *                    ObservableList<XYChart.Data> for LineChart/BarChart and
 *                    ObservableList<PieChart.Data> for the PieChart.
 * DATA STRUCTURES: ObservableList<RecordRow> bound to TableView;
 *                  ObservableList<XYChart.Data<Number,Number>> for the maize
 *                  loss line chart; ObservableList<XYChart.Data<String,Number>>
 *                  for the outcomes bar chart; ObservableList<PieChart.Data>
 *                  for the alert distribution pie chart.
 * ALGORITHMS: None — simulation state machine lives in SimulationRunner.
 *
 * Compiled only when JavaFX is on the classpath; safe to omit in console
 * builds (Main.java loads it by reflection and falls back gracefully).
 */
public class GuiApp extends Application {

    private final SimulationRunner    runner     = new SimulationRunner();
    private final ReportGenerator     reporter   = new ReportGenerator();
    private final RecordStore         files      = new FileHandler();
    private final StorageLossSimulator storageSim = new StorageLossSimulator();

    private final ObservableList<RecordRow> rows = FXCollections.observableArrayList();
    private final TextArea reportArea = new TextArea();

    // -------------------------------------------------------------------------
    // Chart fields — axes initialised here so charts can be constructed inline.
    // Titles and axis labels are applied in buildChartsTab() after the JavaFX
    // scene graph is fully wired.
    // -------------------------------------------------------------------------

    private final NumberAxis  lossXAxis = new NumberAxis();
    private final NumberAxis  lossYAxis = new NumberAxis();
    private final LineChart<Number, Number> maizeLossChart =
            new LineChart<>(lossXAxis, lossYAxis);

    private final CategoryAxis outXAxis = new CategoryAxis();
    private final NumberAxis   outYAxis = new NumberAxis();
    private final BarChart<String, Number> outcomeChart =
            new BarChart<>(outXAxis, outYAxis);

    private final PieChart alertChart = new PieChart();

    public static void launchApp(String[] args) {
        Application.launch(GuiApp.class, args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("AgriTwin-ZM v1.0 -- Digital Twin");

        TabPane tabs = new TabPane();
        tabs.getTabs().add(buildInputTab());
        tabs.getTabs().add(buildResultsTab());
        tabs.getTabs().add(buildReportsTab());
        tabs.getTabs().add(buildChartsTab());

        Scene scene = new Scene(tabs, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    // -------------------------------------------------------------------------
    // Tab 1: Inputs
    // -------------------------------------------------------------------------
    private Tab buildInputTab() {
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(16));

        ComboBox<String> commodity = new ComboBox<>();
        commodity.getItems().addAll("Maize", "Tomato");
        commodity.setValue("Maize");

        TextField production  = new TextField("10000");
        TextField demand      = new TextField("8000");
        TextField price       = new TextField("7000");
        TextField periods     = new TextField("6");
        ComboBox<String> storageType = new ComboBox<>();
        storageType.getItems().addAll("cold", "open");
        storageType.setValue("open");
        TextField extra = new TextField("14"); // monthly loss % (Maize) or shelf-life days (Tomato)

        int r = 0;
        grid.add(new Label("Commodity:"),            0, r); grid.add(commodity,    1, r++);
        grid.add(new Label("Production (tons):"),    0, r); grid.add(production,   1, r++);
        grid.add(new Label("Demand (tons):"),        0, r); grid.add(demand,       1, r++);
        grid.add(new Label("Unit price (ZMW):"),     0, r); grid.add(price,        1, r++);
        grid.add(new Label("Months / Days:"),        0, r); grid.add(periods,      1, r++);
        grid.add(new Label("Storage type:"),         0, r); grid.add(storageType,  1, r++);
        grid.add(new Label("Loss% / shelfLifeDays:"),0, r); grid.add(extra,        1, r++);

        Button run = new Button("Run Simulation");
        run.setOnAction(ev -> {
            try {
                rows.clear();
                reportArea.clear();
                if ("Maize".equals(commodity.getValue())) {
                    double prod = parseD(production);
                    double dem  = parseD(demand);
                    double prc  = parseD(price);
                    int    mos  = parseI(periods);
                    double loss = parseD(extra);
                    runMaize(prod, dem, prc, mos, loss);
                    // Reconstruct same Maize used internally by runMaize for chart data
                    refreshCharts(new Maize(prod, prod * 1.2, 12.5, prc, loss), mos);
                } else {
                    StorageType st = StorageType.valueOf(
                            storageType.getValue().trim().toUpperCase());
                    runTomato(parseD(production), parseD(demand), parseD(price),
                              parseI(periods), st, parseI(extra));
                    refreshCharts(null, 0);
                }
            } catch (Exception ex) {
                reportArea.setText("Error: " + ex.getMessage());
            }
        });

        Button save = new Button("Save records...");
        save.setOnAction(ev -> {
            try {
                java.util.List<SimulationRecord> recs = new java.util.ArrayList<>();
                for (RecordRow rr : rows) recs.add(rr.toRecord());
                files.saveSimulation(recs, "simulation.dat");
                reportArea.appendText("\nSaved " + recs.size() + " records to simulation.dat\n");
            } catch (Exception ex) {
                reportArea.appendText("\nSave error: " + ex.getMessage() + "\n");
            }
        });

        grid.add(run, 0, r); grid.add(save, 1, r);
        Tab tab = new Tab("Inputs", grid);
        tab.setClosable(false);
        return tab;
    }

    // -------------------------------------------------------------------------
    // Tab 2: Results table
    // -------------------------------------------------------------------------
    private Tab buildResultsTab() {
        TableView<RecordRow> table = new TableView<>(rows);
        table.getColumns().add(col("Month",         "monthNumber",          60));
        table.getColumns().add(col("Commodity",     "commodityName",        100));
        table.getColumns().add(col("Production",    "production",           110));
        table.getColumns().add(col("Demand",        "demand",               110));
        table.getColumns().add(col("Surplus",       "surplusOrShortage",    110));
        table.getColumns().add(col("Waste",         "waste",                100));
        table.getColumns().add(col("Revenue (ZMW)", "revenue",              130));
        table.getColumns().add(col("FSI %",         "foodSecurityIndicator", 80));
        table.getColumns().add(col("Alerts",        "policyAlerts",         220));

        VBox box = new VBox(table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        box.setPadding(new Insets(8));
        Tab tab = new Tab("Results", box);
        tab.setClosable(false);
        return tab;
    }

    private <T> TableColumn<RecordRow, T> col(String title, String prop, double width) {
        TableColumn<RecordRow, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(width);
        return c;
    }

    // -------------------------------------------------------------------------
    // Tab 3: Reports text area
    // -------------------------------------------------------------------------
    private Tab buildReportsTab() {
        reportArea.setEditable(false);
        VBox box = new VBox(reportArea);
        VBox.setVgrow(reportArea, javafx.scene.layout.Priority.ALWAYS);
        box.setPadding(new Insets(8));
        Tab tab = new Tab("Reports", box);
        tab.setClosable(false);
        return tab;
    }

    // -------------------------------------------------------------------------
    // Tab 4: Charts
    // -------------------------------------------------------------------------

    /**
     * Builds the Charts tab containing three stacked visualisations inside a
     * ScrollPane. Each chart is a class field so refreshCharts() can repopulate
     * it without rebuilding the scene graph.
     */
    private Tab buildChartsTab() {
        // --- Chart 1: Maize Loss Schedule (LineChart) ---
        maizeLossChart.setTitle("Maize Stock — Cumulative Storage Loss");
        lossXAxis.setLabel("Months stored");
        lossYAxis.setLabel("Remaining tonnage");
        maizeLossChart.setPrefHeight(280);
        maizeLossChart.setAnimated(false);
        maizeLossChart.setLegendVisible(true);
        // Start hidden until a maize simulation runs
        maizeLossChart.setVisible(false);
        maizeLossChart.setManaged(false);

        // --- Chart 2: Monthly Outcomes (BarChart) ---
        outcomeChart.setTitle("Monthly Outcomes — Production vs Waste");
        outXAxis.setLabel("Month");
        outYAxis.setLabel("Tons");
        outcomeChart.setPrefHeight(280);
        outcomeChart.setAnimated(false);

        // --- Chart 3: Policy Alert Distribution (PieChart) ---
        alertChart.setTitle("Policy Alert Distribution (this season)");
        alertChart.setPrefHeight(280);
        alertChart.setAnimated(false);
        alertChart.setLegendVisible(true);

        VBox chartBox = new VBox(10, maizeLossChart, outcomeChart, alertChart);
        chartBox.setPadding(new Insets(8));

        ScrollPane scroll = new ScrollPane(chartBox);
        scroll.setFitToWidth(true);

        Tab tab = new Tab("Charts", scroll);
        tab.setClosable(false);
        return tab;
    }

    /**
     * Clears all three charts and repopulates them from the current state of
     * {@code rows}. Called at the end of every simulation run.
     *
     * <p>The maize loss schedule (Chart 1) is only meaningful for maize runs.
     * When {@code lastMaize} is null (tomato run), the line chart is hidden and
     * removed from layout so the bar and pie charts fill the visible area.
     *
     * @param lastMaize the Maize object used in this run, or null for tomato
     * @param months    number of months simulated (ignored when lastMaize is null)
     */
    private void refreshCharts(Maize lastMaize, int months) {
        // --- Chart 1: Maize Loss Schedule ---
        maizeLossChart.getData().clear();
        if (lastMaize != null) {
            maizeLossChart.setVisible(true);
            maizeLossChart.setManaged(true);
            XYChart.Series<Number, Number> lossSeries = new XYChart.Series<>();
            lossSeries.setName("Remaining stock (tons)");
            List<Double> schedule = storageSim.getMaizeLossSchedule(lastMaize, months);
            for (int i = 0; i < schedule.size(); i++) {
                lossSeries.getData().add(new XYChart.Data<>(i, schedule.get(i)));
            }
            maizeLossChart.getData().add(lossSeries);
        } else {
            maizeLossChart.setVisible(false);
            maizeLossChart.setManaged(false);
        }

        // --- Chart 2: Monthly Outcomes (Production vs Waste) ---
        outcomeChart.getData().clear();
        XYChart.Series<String, Number> prodSeries  = new XYChart.Series<>();
        XYChart.Series<String, Number> wasteSeries = new XYChart.Series<>();
        prodSeries.setName("Production");
        wasteSeries.setName("Waste");
        for (RecordRow rr : rows) {
            String monthLabel = "Month " + rr.getMonthNumber();
            prodSeries.getData().add(new XYChart.Data<>(monthLabel, rr.getProduction()));
            wasteSeries.getData().add(new XYChart.Data<>(monthLabel, rr.getWaste()));
        }
        outcomeChart.getData().addAll(prodSeries, wasteSeries);

        // --- Chart 3: Policy Alert Distribution ---
        alertChart.getData().clear();
        int critical = 0, warning = 0, info = 0;
        for (RecordRow rr : rows) {
            String alerts = rr.getPolicyAlerts();
            if (alerts == null || alerts.isEmpty()) continue;
            for (String raw : alerts.split(";")) {
                String a = raw.trim();
                if (a.isEmpty()) continue;
                if (a.contains("ALERT") || a.contains("CRITICAL") || a.contains("RISK HIGH")) {
                    critical++;
                } else if (a.contains("WARNING") || a.contains("REQUIRED") || a.contains("BREACHED")) {
                    warning++;
                } else {
                    info++;
                }
            }
        }
        // Omit zero-count slices so the legend stays clean
        if (critical > 0) alertChart.getData().add(new PieChart.Data("CRITICAL", critical));
        if (warning  > 0) alertChart.getData().add(new PieChart.Data("WARNING",  warning));
        if (info     > 0) alertChart.getData().add(new PieChart.Data("INFO",     info));
    }

    // -------------------------------------------------------------------------
    // Simulation methods — collect data, delegate to SimulationRunner
    // -------------------------------------------------------------------------
    private void runMaize(double production, double demand, double price,
                          int months, double monthlyLoss) {
        Maize maize = new Maize(production, production * 1.2, 12.5, price, monthlyLoss);
        java.util.List<SimulationRecord> records =
                runner.runMaizeSimulation(maize, demand, months);
        for (SimulationRecord rec : records) {
            rows.add(RecordRow.fromRecord(rec));
            reportArea.appendText(reporter.generateMonthlyReport(rec) + "\n\n");
        }
    }

    private void runTomato(double production, double demand, double pricePerCrate,
                           int days, StorageType storageType, int shelfLifeDays) {
        double pricePerTon = pricePerCrate * 40.0;
        Tomato t = new Tomato(production, shelfLifeDays, storageType, 5.0, pricePerCrate);
        java.util.List<SimulationRecord> records =
                runner.runTomatoSimulation(t, demand, days, pricePerTon);
        for (SimulationRecord rec : records) {
            rows.add(RecordRow.fromRecord(rec));
            reportArea.appendText(reporter.generateMonthlyReport(rec) + "\n");
        }
    }

    private double parseD(TextField f) { return Double.parseDouble(f.getText().trim()); }
    private int    parseI(TextField f) { return Integer.parseInt(f.getText().trim()); }

    /**
     * Property-based row class — TableView's PropertyValueFactory needs
     * accessor methods named like the property (e.g. getMonthNumber()).
     */
    public static class RecordRow {
        private final SimpleIntegerProperty monthNumber          = new SimpleIntegerProperty();
        private final SimpleStringProperty  commodityName        = new SimpleStringProperty();
        private final SimpleDoubleProperty  production           = new SimpleDoubleProperty();
        private final SimpleDoubleProperty  demand               = new SimpleDoubleProperty();
        private final SimpleDoubleProperty  surplusOrShortage    = new SimpleDoubleProperty();
        private final SimpleDoubleProperty  waste                = new SimpleDoubleProperty();
        private final SimpleDoubleProperty  revenue              = new SimpleDoubleProperty();
        private final SimpleDoubleProperty  foodSecurityIndicator= new SimpleDoubleProperty();
        private final SimpleStringProperty  policyAlerts         = new SimpleStringProperty();

        public static RecordRow fromRecord(SimulationRecord r) {
            RecordRow rr = new RecordRow();
            rr.monthNumber.set(r.getMonthNumber());
            rr.commodityName.set(r.getCommodityName());
            rr.production.set(r.getProduction());
            rr.demand.set(r.getDemand());
            rr.surplusOrShortage.set(r.getSurplusOrShortage());
            rr.waste.set(r.getWaste());
            rr.revenue.set(r.getRevenue());
            rr.foodSecurityIndicator.set(r.getFoodSecurityIndicator());
            rr.policyAlerts.set(r.getPolicyAlerts());
            return rr;
        }

        public SimulationRecord toRecord() {
            return new SimulationRecord(
                    monthNumber.get(), commodityName.get(), production.get(), demand.get(),
                    surplusOrShortage.get(), waste.get(), revenue.get(),
                    foodSecurityIndicator.get(), policyAlerts.get());
        }

        public int    getMonthNumber()            { return monthNumber.get(); }
        public String getCommodityName()          { return commodityName.get(); }
        public double getProduction()             { return production.get(); }
        public double getDemand()                 { return demand.get(); }
        public double getSurplusOrShortage()      { return surplusOrShortage.get(); }
        public double getWaste()                  { return waste.get(); }
        public double getRevenue()                { return revenue.get(); }
        public double getFoodSecurityIndicator()  { return foodSecurityIndicator.get(); }
        public String getPolicyAlerts()           { return policyAlerts.get(); }
    }
}
