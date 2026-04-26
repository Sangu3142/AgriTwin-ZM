package agritwin.ui;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.Tomato;
import agritwin.service.PolicyEngine;
import agritwin.service.ReportGenerator;
import agritwin.service.StorageLossSimulator;
import agritwin.service.SupplyDemandEngine;
import agritwin.util.FileHandler;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

/**
 * MODULE: JavaFX GUI Application (optional)
 * PURPOSE: Provides a tabbed graphical front-end over the SAME service
 *          classes used by ConsoleUI. Tab 1 collects inputs, Tab 2 shows a
 *          live TableView of simulation records bound via ObservableList,
 *          and Tab 3 mirrors the textual report output.
 * OOP CONCEPTS USED: Inheritance (extends Application); Composition;
 *                    Property-based binding (JavaFX observables).
 * DATA STRUCTURES: ObservableList&lt;RecordRow&gt; bound to a TableView.
 * ALGORITHMS: Same simulation loop as ConsoleUI -- delegated to services.
 *
 * Compiled only when JavaFX is on the classpath; safe to omit if you only
 * intend to run the console version (Main.java will simply not load it).
 */
public class GuiApp extends Application {

    private final SupplyDemandEngine supply = new SupplyDemandEngine();
    private final StorageLossSimulator storage = new StorageLossSimulator();
    private final PolicyEngine policy = new PolicyEngine();
    private final ReportGenerator reporter = new ReportGenerator();
    private final FileHandler files = new FileHandler();

    private final ObservableList<RecordRow> rows = FXCollections.observableArrayList();
    private final TextArea reportArea = new TextArea();

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

        Scene scene = new Scene(tabs, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    // -----------------------------------------------------------------
    // Tab 1: Inputs
    // -----------------------------------------------------------------
    private Tab buildInputTab() {
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(16));

        ComboBox<String> commodity = new ComboBox<>();
        commodity.getItems().addAll("Maize", "Tomato");
        commodity.setValue("Maize");

        TextField production = new TextField("10000");
        TextField demand     = new TextField("8000");
        TextField price      = new TextField("7000");
        TextField periods    = new TextField("6");
        ComboBox<String> storageType = new ComboBox<>();
        storageType.getItems().addAll("cold", "open");
        storageType.setValue("open");
        TextField extra = new TextField("3"); // monthly loss % or shelf-life days

        int r = 0;
        grid.add(new Label("Commodity:"),       0, r); grid.add(commodity, 1, r++);
        grid.add(new Label("Production (tons):"),0, r); grid.add(production, 1, r++);
        grid.add(new Label("Demand (tons):"),    0, r); grid.add(demand, 1, r++);
        grid.add(new Label("Unit price (ZMW):"), 0, r); grid.add(price, 1, r++);
        grid.add(new Label("Months / Days:"),    0, r); grid.add(periods, 1, r++);
        grid.add(new Label("Storage type:"),     0, r); grid.add(storageType, 1, r++);
        grid.add(new Label("Loss% / shelfLifeDays:"), 0, r); grid.add(extra, 1, r++);

        Button run = new Button("Run Simulation");
        run.setOnAction(ev -> {
            try {
                rows.clear();
                reportArea.clear();
                if ("Maize".equals(commodity.getValue())) {
                    runMaize(parseD(production), parseD(demand), parseD(price),
                             parseI(periods), parseD(extra));
                } else {
                    runTomato(parseD(production), parseD(demand), parseD(price),
                              parseI(periods), storageType.getValue(), parseD(extra));
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

    // -----------------------------------------------------------------
    // Tab 2: Results table
    // -----------------------------------------------------------------
    private Tab buildResultsTab() {
        TableView<RecordRow> table = new TableView<>(rows);
        table.getColumns().add(col("Month", "monthNumber", 60));
        table.getColumns().add(col("Commodity", "commodityName", 100));
        table.getColumns().add(col("Production", "production", 110));
        table.getColumns().add(col("Demand", "demand", 110));
        table.getColumns().add(col("Surplus", "surplusOrShortage", 110));
        table.getColumns().add(col("Waste", "waste", 100));
        table.getColumns().add(col("Revenue (ZMW)", "revenue", 130));
        table.getColumns().add(col("FSI %", "foodSecurityIndicator", 80));
        table.getColumns().add(col("Alerts", "policyAlerts", 220));

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

    // -----------------------------------------------------------------
    // Tab 3: Reports text area
    // -----------------------------------------------------------------
    private Tab buildReportsTab() {
        reportArea.setEditable(false);
        VBox box = new VBox(reportArea);
        VBox.setVgrow(reportArea, javafx.scene.layout.Priority.ALWAYS);
        box.setPadding(new Insets(8));
        Tab tab = new Tab("Reports", box);
        tab.setClosable(false);
        return tab;
    }

    // -----------------------------------------------------------------
    // Simulation methods (delegate to services -- mirror ConsoleUI logic)
    // -----------------------------------------------------------------
    private void runMaize(double production, double demand, double price,
                          int months, double monthlyLoss) {
        Maize maize = new Maize(production, production * 1.2, 12.5, price, monthlyLoss);
        double stock = production;
        double currentPrice = price;
        double monthlyDemand = demand / Math.max(1, months);

        for (int m = 1; m <= months; m++) {
            double remaining = storage.simulateMaizeLoss(
                    new Maize(stock, maize.getStorageCapacity(), maize.getMoistureLevel(),
                              currentPrice, monthlyLoss), 1);
            double waste = stock - remaining;
            stock = remaining;
            double sold = Math.min(stock, monthlyDemand);
            double revenue = supply.calculateRevenue(sold, currentPrice);
            stock -= sold;
            double surplus = supply.calculateSurplus(remaining, monthlyDemand);
            currentPrice = supply.adjustPrice(currentPrice, surplus, remaining);
            double fsi = supply.calculateFoodSecurityIndicator(remaining, monthlyDemand);
            String alerts = policy.evaluateAll(stock, surplus, remaining, m, -1, -1);

            SimulationRecord rec = new SimulationRecord(m, "Maize", remaining, monthlyDemand,
                    surplus, waste, revenue, fsi, alerts);
            rows.add(RecordRow.fromRecord(rec));
            reportArea.appendText(reporter.generateMonthlyReport(rec) + "\n\n");
        }
    }

    private void runTomato(double production, double demand, double pricePerCrate,
                           int days, String storageType, double spoilageRate) {
        Tomato t = new Tomato(production, 14, storageType, spoilageRate, pricePerCrate);
        double waste = storage.simulateTomatoSpoilage(t, days);
        double saleable = Math.max(0, production - waste);
        double pricePerTon = pricePerCrate * 40.0;
        double sold = Math.min(saleable, demand);
        double revenue = supply.calculateRevenue(sold, pricePerTon);
        double surplus = supply.calculateSurplus(saleable, demand);
        double fsi = supply.calculateFoodSecurityIndicator(saleable, demand);
        String alerts = policy.evaluateAll(Double.POSITIVE_INFINITY, surplus, saleable,
                                           -1, days, t.getShelfLifeDays());

        SimulationRecord rec = new SimulationRecord(1, "Tomato", saleable, demand,
                surplus, waste, revenue, fsi, alerts);
        rows.add(RecordRow.fromRecord(rec));
        reportArea.appendText(reporter.generateMonthlyReport(rec) + "\n");
    }

    private double parseD(TextField f) { return Double.parseDouble(f.getText().trim()); }
    private int    parseI(TextField f) { return Integer.parseInt(f.getText().trim()); }

    /**
     * Property-based row class -- TableView's PropertyValueFactory needs
     * accessor methods named like the property (e.g. getMonthNumber()).
     */
    public static class RecordRow {
        private final SimpleIntegerProperty monthNumber = new SimpleIntegerProperty();
        private final SimpleStringProperty commodityName = new SimpleStringProperty();
        private final SimpleDoubleProperty production = new SimpleDoubleProperty();
        private final SimpleDoubleProperty demand = new SimpleDoubleProperty();
        private final SimpleDoubleProperty surplusOrShortage = new SimpleDoubleProperty();
        private final SimpleDoubleProperty waste = new SimpleDoubleProperty();
        private final SimpleDoubleProperty revenue = new SimpleDoubleProperty();
        private final SimpleDoubleProperty foodSecurityIndicator = new SimpleDoubleProperty();
        private final SimpleStringProperty policyAlerts = new SimpleStringProperty();

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

        public int    getMonthNumber()           { return monthNumber.get(); }
        public String getCommodityName()         { return commodityName.get(); }
        public double getProduction()            { return production.get(); }
        public double getDemand()                { return demand.get(); }
        public double getSurplusOrShortage()     { return surplusOrShortage.get(); }
        public double getWaste()                 { return waste.get(); }
        public double getRevenue()               { return revenue.get(); }
        public double getFoodSecurityIndicator() { return foodSecurityIndicator.get(); }
        public String getPolicyAlerts()          { return policyAlerts.get(); }
    }
}
