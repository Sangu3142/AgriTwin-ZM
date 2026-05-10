package agritwin.ui;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.StorageType;
import agritwin.model.Tomato;
import agritwin.service.PolicyEngine;
import agritwin.service.ReportGenerator;
import agritwin.service.SimulationRunner;
import agritwin.service.StorageLossSimulator;
import agritwin.service.SupplyDemandEngine;
import agritwin.ui.components.AlertPanel;
import agritwin.ui.components.SidebarMenu;
import agritwin.ui.components.StatCard;

import javafx.animation.FadeTransition;
import agritwin.ui.model.ReportSummary;
import agritwin.util.FileHandler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.geometry.Side;
import javafx.scene.input.KeyCode;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MODULE: Dashboard Controller
 * PURPOSE: Assembles the full JavaFX dashboard scene and manages multi-page
 *          navigation. Six pages are built once at startup and swapped into a
 *          shared ScrollPane when the user clicks a sidebar nav item.
 *          Vision UI dark navy/glassmorphism aesthetic. Page transitions via
 *          FadeTransition (200 ms). CSS is written to a temp file and loaded
 *          into the Scene so chart internals and controls pick up dark colours.
 * OOP CONCEPTS USED: Composition; Encapsulation; Callback via Consumer/Runnable.
 * DATA STRUCTURES: VBox[] pages — 6-element array; StatCard[] for card rows.
 */
public class DashboardController {

    // ── Palette (Vision UI dark) ───────────────────────────────────────────────
    private static final String PAGE_BG      = "#0F1535";
    private static final String CARD_BG      = "#111C44";
    private static final String CARD_BORDER  = "#1A2A6C";
    private static final String ACCENT_CYAN  = "#00B5D8";
    private static final String ACCENT_TEAL  = "#00CFB4";
    private static final String GREEN_PILL   = "#48BB78";
    private static final String CORAL        = "#F56565";
    private static final String GOLD         = "#ECC94B";
    private static final String TEXT_PRI     = "#FFFFFF";
    private static final String TEXT_SEC     = "#A0AEC0";
    private static final String TEXT_MUTED   = "#718096";
    private static final String INPUT_BG     = "#1A2240";
    private static final String INPUT_BORDER = "#2D3748";

    // ── Page indices — must match SidebarMenu LABELS order ────────────────────
    private static final int PG_DASHBOARD  = 0;
    private static final int PG_SIMULATION = 1;
    private static final int PG_STORAGE    = 2;
    private static final int PG_ALERTS     = 3;
    private static final int PG_REPORTS    = 4;
    private static final int PG_SETTINGS   = 5;



    // ── Navigation ─────────────────────────────────────────────────────────────
    private SidebarMenu sidebar;
    private ScrollPane  contentScroll;
    private VBox[]      pages;

    // ── Dashboard page ─────────────────────────────────────────────────────────
    private StatCard[]     dashCards;
    private AlertPanel     alertPanel;
    private VBox           recentSimsListBox;
    private final List<String[]> recentSimHistory = new ArrayList<>();

    // ── Simulation page ────────────────────────────────────────────────────────
    private String    simCommodity = "Maize";
    private Label     simMaizeTab, simTomatoTab;
    private Region    simMaizeUL,  simTomatoUL;
    private Label     lblPrice;
    private TextField fldProd, fldDemand, fldPrice;
    private TextField fldMonths, fldLossPct, fldMoisture;
    private TextField fldSpoilage, fldShelf, fldDays;
    private ComboBox<String> fldStorage;
    private VBox  maizeExtra, tomatoExtra;
    private Label simErrorLabel;

    private VBox                   simResultsSection;
    private StatCard[]             simCards;
    private BarChart<String, Number>  barChart;
    private LineChart<Number, Number> lineChart;
    private PieChart               pieChart;
    private TextArea               simPolicyArea;

    // ── Storage page ───────────────────────────────────────────────────────────
    private StatCard[]             storageCards;
    private LineChart<Number, Number> storageChart;
    private Label                  storageDescLabel;
    private VBox                   storagePlaceholder;
    private VBox                   storageChartArea;
    private Tomato                 lastTomato;
    private int                    lastTomatoDays;

    // ── Top-bar search ─────────────────────────────────────────────────────────
    private TextField searchBar;

    // ── Alerts page ────────────────────────────────────────────────────────────
    private TextField alertFldStock, alertFldSurplus, alertFldProd,
                      alertFldMonths, alertFldDays, alertFldShelf;
    private TextArea  alertsResultArea;

    // ── Shared reports state ───────────────────────────────────────────────────
    private ObservableList<ReportSummary> savedReports;
    private SimulationPage                simPage;
    private ReportsPage                   reportsPageCtrl;

    // ── Services ───────────────────────────────────────────────────────────────
    private final SupplyDemandEngine   supplyEngine = new SupplyDemandEngine();
    private final StorageLossSimulator storageSim   = new StorageLossSimulator();
    private final PolicyEngine         policyEngine = new PolicyEngine();
    private final SimulationRunner     simRunner    = new SimulationRunner();
    private final ReportGenerator      reporter     = new ReportGenerator();

    // ── Embedded dark CSS (written to temp file at startup) ───────────────────
    private static final String DARK_CSS =
        ".root{-fx-font-family:'Segoe UI';}" +
        ".scroll-pane{-fx-background-color:transparent;-fx-background:transparent;" +
            "-fx-border-color:transparent;}" +
        ".scroll-pane>.viewport{-fx-background-color:transparent;}" +
        ".scroll-bar{-fx-background-color:#0B1437;}" +
        ".scroll-bar .thumb{-fx-background-color:#1A2A6C;-fx-background-radius:4;}" +
        ".scroll-bar .increment-button,.scroll-bar .decrement-button{" +
            "-fx-background-color:#0B1437;}" +
        ".chart{-fx-background-color:#111C44;-fx-padding:16;}" +
        ".chart-plot-background{-fx-background-color:#0F1535;}" +
        ".chart-title{-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;}" +
        ".axis{-fx-tick-label-fill:#A0AEC0;}" +
        ".axis-label{-fx-text-fill:#A0AEC0;-fx-font-size:11px;}" +
        ".chart-vertical-grid-lines{-fx-stroke:#1A2A6C;-fx-stroke-width:0.5;}" +
        ".chart-horizontal-grid-lines{-fx-stroke:#1A2A6C;-fx-stroke-width:0.5;}" +
        ".chart-legend{-fx-background-color:transparent;}" +
        ".chart-legend-item{-fx-text-fill:#A0AEC0;}" +
        ".default-color0.chart-series-line{-fx-stroke:#00B5D8;-fx-stroke-width:2;}" +
        ".default-color1.chart-series-line{-fx-stroke:#48BB78;-fx-stroke-width:2;}" +
        ".default-color2.chart-series-line{-fx-stroke:#F56565;-fx-stroke-width:2;}" +
        ".default-color3.chart-series-line{-fx-stroke:#ECC94B;-fx-stroke-width:2;}" +
        ".default-color0.chart-bar{-fx-bar-fill:#00B5D8;}" +
        ".default-color1.chart-bar{-fx-bar-fill:#48BB78;}" +
        ".default-color2.chart-bar{-fx-bar-fill:#F56565;}" +
        ".default-color0.chart-pie{-fx-pie-color:#F56565;}" +
        ".default-color1.chart-pie{-fx-pie-color:#ECC94B;}" +
        ".default-color2.chart-pie{-fx-pie-color:#48BB78;}" +
        ".default-color3.chart-pie{-fx-pie-color:#A0AEC0;}" +
        ".text-area{-fx-control-inner-background:#111C44;-fx-text-fill:#A0AEC0;" +
            "-fx-border-color:#1A2A6C;-fx-border-radius:8;-fx-background-radius:8;}" +
        ".text-area .content{-fx-background-color:#111C44;}" +
        ".text-area:focused{-fx-border-color:#00B5D8;}" +
        ".text-field{-fx-background-color:#1A2240;-fx-text-fill:white;" +
            "-fx-prompt-text-fill:#718096;-fx-border-color:#2D3748;" +
            "-fx-border-radius:8;-fx-background-radius:8;}" +
        ".text-field:focused{-fx-border-color:#00B5D8;}" +
        ".combo-box{-fx-background-color:#1A2240;-fx-mark-color:#A0AEC0;" +
            "-fx-border-color:#2D3748;-fx-border-radius:8;-fx-background-radius:8;}" +
        ".combo-box .list-cell{-fx-text-fill:white;-fx-background-color:#1A2240;}" +
        ".combo-box .arrow-button{-fx-background-color:#1A2240;}" +
        ".combo-box-popup .list-view{-fx-background-color:#111C44;-fx-border-color:#1A2A6C;}" +
        ".combo-box-popup .list-cell{-fx-text-fill:white;-fx-background-color:#111C44;}" +
        ".combo-box-popup .list-cell:hover{-fx-background-color:#1A2A6C;}" +
        ".progress-indicator{-fx-progress-color:#00B5D8;}" +
        ".toggle-button{-fx-background-color:#1A2A6C;-fx-text-fill:#A0AEC0;" +
            "-fx-background-radius:20;-fx-border-radius:20;}" +
        ".toggle-button:selected{-fx-background-color:#00CFB4;-fx-text-fill:white;}" +
        ".table-view{-fx-background-color:#111C44;-fx-control-inner-background:#111C44;" +
            "-fx-control-inner-background-alt:#0F1535;-fx-border-color:#1A2A6C;}" +
        ".table-view .column-header-background{-fx-background-color:#1A2A6C;}" +
        ".table-view .column-header{-fx-background-color:#1A2A6C;-fx-border-color:#111C44;}" +
        ".table-view .column-header .label{-fx-text-fill:#FFFFFF;-fx-font-weight:bold;" +
            "-fx-font-size:12px;}" +
        ".table-view .filler{-fx-background-color:#1A2A6C;}" +
        ".table-row-cell{-fx-background-color:-fx-control-inner-background;" +
            "-fx-cell-focus-color:#1A2A6C;}" +
        ".table-row-cell:odd{-fx-background-color:-fx-control-inner-background-alt;}" +
        ".table-row-cell:selected{-fx-background-color:#1A2A6C;}" +
        ".table-row-cell:selected .table-cell{-fx-text-fill:#FFFFFF;}" +
        ".table-row-cell:hover{-fx-background-color:#1A3050;}" +
        ".table-cell{-fx-text-fill:#A0AEC0;-fx-border-color:transparent;}" +
        ".context-menu{-fx-background-color:#111C44;-fx-border-color:#1A2A6C;" +
            "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;}" +
        ".menu-item{-fx-background-color:#111C44;}" +
        ".menu-item .label{-fx-text-fill:#FFFFFF;-fx-font-size:12px;}" +
        ".menu-item:focused{-fx-background-color:#1A2A6C;}" +
        ".menu-item:hover{-fx-background-color:#1A2A6C;}";

    // ══════════════════════════════════════════════════════════════════════════
    // Entry point
    // ══════════════════════════════════════════════════════════════════════════

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setBackground(bg(PAGE_BG, 0));
        sidebar = new SidebarMenu(this::handleNavSelect);
        root.setLeft(sidebar);
        root.setCenter(buildMainArea());
        Scene scene = new Scene(root, 1280, 720);
        scene.setFill(Color.web(PAGE_BG));
        try {
            File cssFile = File.createTempFile("agritwin_dark", ".css");
            cssFile.deleteOnExit();
            try (FileWriter fw = new FileWriter(cssFile)) {
                fw.write(DARK_CSS);
            }
            scene.getStylesheets().add(cssFile.toURI().toString());
        } catch (IOException e) {
            // CSS unavailable; inline styles provide visual fallback
        }
        return scene;
    }

    private VBox buildMainArea() {
        VBox mainArea = new VBox(0);
        mainArea.setBackground(bg(PAGE_BG, 0));
        mainArea.getChildren().add(buildTopBar());

        savedReports    = loadAllSavedReports();
        simPage         = new SimulationPage(savedReports);
        reportsPageCtrl = new ReportsPage(savedReports, () -> selectPage(PG_SIMULATION));

        pages = new VBox[] {
                buildDashboardPage(),
                buildSimulationPage(),
                buildStoragePage(),
                buildAlertsPage(),
                reportsPageCtrl.buildPage(),
                buildSettingsPage()
        };

        contentScroll = new ScrollPane(pages[PG_DASHBOARD]);
        contentScroll.setFitToWidth(true);
        contentScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);
        mainArea.getChildren().add(contentScroll);
        return mainArea;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Top bar (shared across pages)
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));
        bar.setBackground(bg(CARD_BG, 0));
        bar.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0))));

        DropShadow barShadow = new DropShadow();
        barShadow.setRadius(8);
        barShadow.setOffsetY(3);
        barShadow.setColor(Color.color(0, 0, 0, 0.30));
        bar.setEffect(barShadow);

        searchBar = new TextField();
        searchBar.setPromptText("Search — alerts, reports, storage…");
        searchBar.setPrefWidth(260);

        // Navigation search index: {display label, keywords, page index}
        Object[][] navItems = {
            {"Dashboard",     new String[]{"dashboard","overview","home","kpi","stats"},        PG_DASHBOARD},
            {"Simulation",    new String[]{"simulation","simulate","crop","maize","tomato"},    PG_SIMULATION},
            {"Storage",       new String[]{"storage","warehouse","silo","stock","capacity"},    PG_STORAGE},
            {"Policy Alerts", new String[]{"alert","policy","fra","reserve","export","check"},  PG_ALERTS},
            {"Reports",       new String[]{"report","reports","saved","history","analysis"},    PG_REPORTS},
            {"Information",   new String[]{"info","information","settings","about","system"},   PG_SETTINGS},
        };

        ContextMenu suggestions = new ContextMenu();

        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            suggestions.getItems().clear();
            String q = (newVal == null ? "" : newVal.trim().toLowerCase());
            if (q.isEmpty()) { suggestions.hide(); return; }
            for (Object[] nav : navItems) {
                String   navLabel = (String)   nav[0];
                String[] keywords = (String[]) nav[1];
                int      pgIdx    = (Integer)  nav[2];
                boolean  match    = navLabel.toLowerCase().contains(q);
                if (!match) for (String kw : keywords) if (kw.contains(q)) { match = true; break; }
                if (match) {
                    MenuItem mi = new MenuItem(navLabel);
                    mi.setOnAction(ev -> { searchBar.clear(); selectPage(pgIdx); });
                    suggestions.getItems().add(mi);
                }
            }
            if (!suggestions.getItems().isEmpty())
                suggestions.show(searchBar, Side.BOTTOM, 0, 4);
            else
                suggestions.hide();
        });

        searchBar.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                if (!suggestions.getItems().isEmpty()) suggestions.getItems().get(0).fire();
            } else if (ke.getCode() == KeyCode.ESCAPE) {
                searchBar.clear();
                suggestions.hide();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label gear = new Label("ℹ");
        gear.setFont(Font.font("Segoe UI", 20));
        gear.setTextFill(Color.web(TEXT_SEC));
        gear.setCursor(javafx.scene.Cursor.HAND);
        gear.setPadding(new Insets(0, 4, 0, 4));
        gear.setOnMouseClicked(e -> selectPage(PG_SETTINGS));

        Circle avatar = new Circle(18);
        avatar.setFill(Color.web(ACCENT_CYAN));
        Label initials = new Label("AT");
        initials.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        initials.setTextFill(Color.WHITE);
        StackPane avatarStack = new StackPane(avatar, initials);

        bar.getChildren().addAll(searchBar, spacer, gear, avatarStack);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DASHBOARD PAGE
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildDashboardPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 32, 28));
        page.setBackground(bg(PAGE_BG, 0));

        page.getChildren().add(buildDashGreeting());

        dashCards = new StatCard[] {
            new StatCard("—", "Production Volume", ACCENT_CYAN, true,  0),
            new StatCard("—", "Market Demand",     TEXT_PRI,    false, 0),
            new StatCard("—", "Net Surplus",       GREEN_PILL,  false, 0),
            new StatCard("—", "Est. Revenue",      CORAL,       true,  0),
            new StatCard("—", "Food Security",     ACCENT_TEAL, true,  0),
        };

        VBox cardsAndLower = new VBox(20, buildCardsRow(dashCards), buildLowerSection());
        VBox.setVgrow(cardsAndLower, Priority.ALWAYS);
        page.getChildren().add(cardsAndLower);
        return page;
    }

    // ── Dashboard greeting ─────────────────────────────────────────────────────

    private VBox buildDashGreeting() {
        Label title = new Label("Hello! AgriTwin-ZM");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(TEXT_PRI));

        Label subtitle = new Label("Simulating Zambian maize and tomato supply chains");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web(TEXT_SEC));

        Label hint = new Label("Run a simulation — results will appear here.");
        hint.setFont(Font.font("Segoe UI", 13));
        hint.setTextFill(Color.web(TEXT_MUTED));

        Label runBtn = new Label("▶  Run Simulation");
        runBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        runBtn.setTextFill(Color.WHITE);
        runBtn.setBackground(bg(GREEN_PILL, 20));
        runBtn.setPadding(new Insets(7, 18, 7, 18));
        runBtn.setCursor(javafx.scene.Cursor.HAND);
        runBtn.setOnMouseClicked(e -> selectPage(PG_SIMULATION));

        HBox row = new HBox(14, hint, runBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, title, subtitle, row);
    }


    // ── Dashboard lower section ────────────────────────────────────────────────

    private HBox buildLowerSection() {
        VBox recent = buildRecentSims();
        HBox.setHgrow(recent, Priority.ALWAYS);

        alertPanel = new AlertPanel("Run a simulation to see policy alerts.", () -> selectPage(PG_ALERTS));
        alertPanel.setPrefWidth(320);
        alertPanel.setMinWidth(260);
        return new HBox(18, recent, alertPanel);
    }

    private VBox buildRecentSims() {
        Label heading = new Label("Recent Simulations");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web(TEXT_PRI));

        Button clearRecentBtn = actionButton("✕  Clear", CORAL);
        clearRecentBtn.setPadding(new Insets(4, 12, 4, 12));
        clearRecentBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        clearRecentBtn.setOnAction(e -> {
            recentSimHistory.clear();
            refreshRecentSimsDisplay();
            dashCards[0].update("—", "Production Volume", 0);
            dashCards[1].update("—", "Market Demand",     0);
            dashCards[2].update("—", "Net Surplus",       0);
            dashCards[3].update("—", "Est. Revenue",      0);
            dashCards[4].update("—", "Food Security",     0);
            alertPanel.setBodyText("Run a simulation to see policy alerts.");
        });

        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);
        HBox headingRow = new HBox(8, heading, spacerH, clearRecentBtn);
        headingRow.setAlignment(Pos.CENTER_LEFT);
        headingRow.setPadding(new Insets(0, 0, 8, 0));

        recentSimsListBox = new VBox(10);
        refreshRecentSimsDisplay();
        return new VBox(0, headingRow, recentSimsListBox);
    }

    private void refreshRecentSimsDisplay() {
        recentSimsListBox.getChildren().clear();
        if (recentSimHistory.isEmpty()) {
            Label placeholder = new Label("Run a simulation to see results here.");
            placeholder.setFont(Font.font("Segoe UI", 13));
            placeholder.setTextFill(Color.web(TEXT_MUTED));
            placeholder.setPadding(new Insets(14, 0, 0, 0));
            recentSimsListBox.getChildren().add(placeholder);
            return;
        }
        for (String[] e : recentSimHistory) {
            recentSimsListBox.getChildren().add(simRow(e[0], e[1], e[2], e[3]));
        }
    }

    private HBox simRow(String icon, String iconColor, String titleText, String subText) {
        Circle dot = new Circle(18);
        dot.setFill(Color.web(iconColor));
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        iconLbl.setTextFill(Color.WHITE);
        StackPane iconStack = new StackPane(dot, iconLbl);

        Label titleLbl = new Label(titleText);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        titleLbl.setTextFill(Color.web(TEXT_PRI));
        Label subLbl = new Label(subText);
        subLbl.setFont(Font.font("Segoe UI", 12));
        subLbl.setTextFill(Color.web(TEXT_SEC));

        HBox row = new HBox(12, iconStack, new VBox(3, titleLbl, subLbl));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setBackground(bg(CARD_BG, 10));
        row.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(8);
        s.setOffsetY(2);
        s.setColor(Color.color(0, 0, 0, 0.25));
        row.setEffect(s);
        StatCard.addHoverLift(row);
        return row;
    }

    // ── Dashboard update from simulation results ───────────────────────────────

    private void updateDashboardFromSimulation(List<SimulationRecord> records, String commodity) {
        if (records == null || records.isEmpty()) return;

        double totalProd  = records.get(0).getProduction() + records.get(0).getWaste();
        double totalDem   = records.stream().mapToDouble(SimulationRecord::getDemand).sum();
        double netSurplus = records.stream().mapToDouble(SimulationRecord::getSurplusOrShortage).sum();
        double totalRev   = records.stream().mapToDouble(SimulationRecord::getRevenue).sum();
        double avgFsi     = records.stream().mapToDouble(SimulationRecord::getFoodSecurityIndicator).average().orElse(0);

        dashCards[0].update(fmt(totalProd) + "t", "Production Volume",
                Math.min(1, totalProd / 200_000));
        dashCards[1].update(fmt(totalDem) + "t", "Market Demand",
                Math.min(1, totalDem / 200_000));
        dashCards[2].update((netSurplus >= 0 ? "+" : "") + fmt(netSurplus) + "t", "Net Surplus",
                Math.min(1, Math.abs(netSurplus) / Math.max(1, totalProd)));
        dashCards[3].update(fmtRev(totalRev), "Est. Revenue",
                Math.min(1, totalRev / 500_000_000));
        dashCards[4].update(String.format("%.1f%%", avgFsi), "Food Security",
                avgFsi / 100.0);

        StringBuilder alertsText = new StringBuilder();
        boolean hasAlerts = false;
        for (SimulationRecord r : records) {
            String a = r.getPolicyAlerts();
            if (a != null && !a.trim().isEmpty()) {
                alertsText.append("Month ").append(r.getMonthNumber())
                          .append(": ").append(a.trim()).append("\n");
                hasAlerts = true;
            }
        }
        alertPanel.setBodyText(hasAlerts
                ? alertsText.toString().trim()
                : "No policy alerts triggered.");

        String icon      = "Maize".equals(commodity) ? "M" : "T";
        String iconColor = "Maize".equals(commodity) ? GREEN_PILL : CORAL;
        int    periods   = records.size();
        String title     = commodity + " · " + periods + (periods == 1 ? " period" : " months");
        String sub       = String.format("%.1f%% FSI  ·  %s", avgFsi, fmtRev(totalRev));
        recentSimHistory.add(0, new String[]{icon, iconColor, title, sub});
        if (recentSimHistory.size() > 3) recentSimHistory.remove(3);
        refreshRecentSimsDisplay();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SIMULATION PAGE
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildSimulationPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setBackground(bg(PAGE_BG, 0));

        Label title = new Label("Run Simulation");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_PRI));

        Label subtitle = new Label("Enter parameters to compute supply-chain outcomes for Zambia");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web(TEXT_SEC));

        HBox simTabs = buildSimCommodityTabs();

        VBox formCard = buildSimFormCard();

        simErrorLabel = new Label("");
        simErrorLabel.setTextFill(Color.web(CORAL));
        simErrorLabel.setFont(Font.font("Segoe UI", 12));
        simErrorLabel.setVisible(false);

        Button runBtn = actionButton("▶  Run Simulation", GREEN_PILL);
        runBtn.setPrefWidth(220);
        runBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        runBtn.setPadding(new Insets(12, 24, 12, 24));
        runBtn.setOnAction(e -> runSimulation());

        Button clearBtn = actionButton("✕  Clear Results", CORAL);
        clearBtn.setPadding(new Insets(12, 20, 12, 20));
        clearBtn.setOnAction(e -> clearSimResults());

        HBox btnRow = new HBox(12, runBtn, clearBtn, simErrorLabel);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        simResultsSection = buildSimResultsSection();
        simResultsSection.setVisible(false);
        simResultsSection.setManaged(false);

        page.getChildren().addAll(title, subtitle, simTabs, formCard, btnRow, simResultsSection);
        return page;
    }

    // ── Simulation commodity tabs ──────────────────────────────────────────────

    private HBox buildSimCommodityTabs() {
        simMaizeTab  = tabLabel("Maize",  true);
        simTomatoTab = tabLabel("Tomato", false);
        simMaizeUL   = tabUnderline(true);
        simTomatoUL  = tabUnderline(false);

        VBox maizeVB  = new VBox(0, simMaizeTab,  simMaizeUL);
        VBox tomatoVB = new VBox(0, simTomatoTab, simTomatoUL);
        maizeVB.setOnMouseClicked(e  -> selectSimTab("Maize"));
        tomatoVB.setOnMouseClicked(e -> selectSimTab("Tomato"));
        maizeVB.setCursor(javafx.scene.Cursor.HAND);
        tomatoVB.setCursor(javafx.scene.Cursor.HAND);
        return new HBox(8, maizeVB, tomatoVB);
    }

    private void selectSimTab(String tab) {
        simCommodity = tab;
        boolean m = "Maize".equals(tab);
        simMaizeTab.setFont(Font.font("Segoe UI",  m  ? FontWeight.BOLD : FontWeight.NORMAL, 14));
        simMaizeTab.setTextFill(Color.web(m  ? TEXT_PRI : TEXT_MUTED));
        simMaizeUL.setBackground(bg(m  ? ACCENT_CYAN : "transparent", 2));
        simTomatoTab.setFont(Font.font("Segoe UI", !m ? FontWeight.BOLD : FontWeight.NORMAL, 14));
        simTomatoTab.setTextFill(Color.web(!m ? TEXT_PRI : TEXT_MUTED));
        simTomatoUL.setBackground(bg(!m ? ACCENT_CYAN : "transparent", 2));

        if (m) {
            lblPrice.setText("Price per Ton (ZMW):");
            fldProd.setPromptText("e.g. 110,000");
            fldDemand.setPromptText("e.g. 78,500");
            fldPrice.setPromptText("e.g. 3,920");
        } else {
            lblPrice.setText("Price per Crate (ZMW):");
            fldProd.setPromptText("e.g. 500");
            fldDemand.setPromptText("e.g. 420");
            fldPrice.setPromptText("e.g. 110");
        }
        fldProd.setText("");
        fldDemand.setText("");
        fldPrice.setText("");
        maizeExtra.setVisible(m);
        maizeExtra.setManaged(m);
        tomatoExtra.setVisible(!m);
        tomatoExtra.setManaged(!m);
        clearSimResults();
    }

    // ── Simulation input form ──────────────────────────────────────────────────

    private VBox buildSimFormCard() {
        fldProd    = blankField("e.g. 110,000");
        fldDemand  = blankField("e.g. 78,500");
        fldPrice   = blankField("e.g. 3,920");

        fldMonths   = blankField("e.g. 6");
        fldLossPct  = blankField("e.g. 3.0");
        fldMoisture = blankField("e.g. 12.5");

        fldSpoilage = blankField("e.g. 5.0");
        fldShelf    = blankField("e.g. 14");
        fldDays     = blankField("e.g. 7");
        fldStorage  = new ComboBox<>(FXCollections.observableArrayList("COLD", "OPEN"));
        fldStorage.setValue("COLD");
        fldStorage.setPrefWidth(220);

        GridPane commonGrid = formGrid();
        lblPrice = formLabel("Price per Ton (ZMW):");
        addRow(commonGrid, "Production Volume (t):", fldProd,   0);
        addRow(commonGrid, "Market Demand (t):",     fldDemand, 1);
        commonGrid.add(lblPrice, 0, 2);
        commonGrid.add(fldPrice, 1, 2);

        GridPane maizeGrid = formGrid();
        addRow(maizeGrid, "Months to Simulate:",       fldMonths,   0);
        addRow(maizeGrid, "Monthly Loss % (storage):", fldLossPct,  1);
        addRow(maizeGrid, "Moisture Level %:",         fldMoisture, 2);
        maizeExtra = new VBox(0, gap(10), sectionDivider("Maize Parameters"), gap(10), maizeGrid);

        GridPane tomatoGrid = formGrid();
        addRow(tomatoGrid, "Spoilage Rate (%/day):", fldSpoilage, 0);
        addRow(tomatoGrid, "Shelf Life (days):",     fldShelf,    1);
        addRow(tomatoGrid, "Days Before Sale:",      fldDays,     2);
        tomatoGrid.add(formLabel("Storage Type:"), 0, 3);
        tomatoGrid.add(fldStorage, 1, 3);
        tomatoExtra = new VBox(0, gap(10), sectionDivider("Tomato Parameters"), gap(10), tomatoGrid);
        tomatoExtra.setVisible(false);
        tomatoExtra.setManaged(false);

        VBox formContent = new VBox(0, commonGrid, maizeExtra, tomatoExtra);
        formContent.setPadding(new Insets(24));

        VBox card = new VBox(0, formContent);
        card.setBackground(bg(CARD_BG, 12));
        card.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(12);
        s.setOffsetY(4);
        s.setColor(Color.color(0, 0, 0, 0.35));
        card.setEffect(s);
        return card;
    }

    // ── Simulation results section (hidden until sim runs) ────────────────────

    private VBox buildSimResultsSection() {
        simCards = new StatCard[] {
                new StatCard("—", "Surplus / Shortage", ACCENT_CYAN, true, 0),
                new StatCard("—", "Adjusted Price",     CORAL,       true, 0),
                new StatCard("—", "Food Security %",    GREEN_PILL,  false, 0),
                new StatCard("—", "Total Waste",        TEXT_PRI,    false, 0),
                new StatCard("—", "Est. Revenue (ZMW)", ACCENT_TEAL, true, 0),
        };
        HBox simCardsRow = new HBox(14);
        for (StatCard c : simCards) {
            HBox.setHgrow(c, Priority.ALWAYS);
            simCardsRow.getChildren().add(c);
        }

        CategoryAxis bx = new CategoryAxis();
        bx.setLabel("Month");
        NumberAxis by = new NumberAxis();
        by.setLabel("Tons");
        barChart = new BarChart<>(bx, by);
        barChart.setTitle("Monthly Production vs Demand");
        barChart.setAnimated(false);
        barChart.setPrefHeight(300);
        barChart.setStyle("-fx-background-color:#111C44;");
        HBox.setHgrow(barChart, Priority.ALWAYS);

        NumberAxis lx = new NumberAxis();
        lx.setLabel("Month");
        NumberAxis ly = new NumberAxis();
        ly.setLabel("Remaining (t)");
        lineChart = new LineChart<>(lx, ly);
        lineChart.setTitle("Stock Loss Schedule");
        lineChart.setAnimated(false);
        lineChart.setPrefHeight(300);
        lineChart.setStyle("-fx-background-color:#111C44;");
        HBox.setHgrow(lineChart, Priority.ALWAYS);

        HBox chartsRow = new HBox(16, barChart, lineChart);
        chartsRow.setAlignment(Pos.TOP_LEFT);

        pieChart = new PieChart();
        pieChart.setTitle("Policy Alert Distribution");
        pieChart.setAnimated(false);
        pieChart.setPrefHeight(260);
        pieChart.setPrefWidth(400);
        pieChart.setStyle("-fx-background-color:#111C44;");

        Label policyHeading = new Label("Policy Evaluation & Season Report");
        policyHeading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        policyHeading.setTextFill(Color.web(TEXT_PRI));

        simPolicyArea = new TextArea();
        simPolicyArea.setEditable(false);
        simPolicyArea.setPrefRowCount(8);

        Label resultHeading = new Label("Simulation Results");
        resultHeading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        resultHeading.setTextFill(Color.web(TEXT_PRI));

        Region div = new Region();
        div.setPrefHeight(1);
        div.setBackground(bg(CARD_BORDER, 0));
        div.setMaxWidth(Double.MAX_VALUE);

        VBox section = new VBox(16, resultHeading, div, simCardsRow,
                chartsRow, pieChart, policyHeading, simPolicyArea,
                simPage.getSaveSectionNode());
        section.setBackground(bg(PAGE_BG, 0));
        section.setPadding(new Insets(8, 0, 0, 0));
        return section;
    }

    // ── Run simulation ─────────────────────────────────────────────────────────

    private void runSimulation() {
        simErrorLabel.setVisible(false);
        try {
            double prod   = Double.parseDouble(fldProd.getText().trim().replace(",", ""));
            double demand = Double.parseDouble(fldDemand.getText().trim().replace(",", ""));
            double price  = Double.parseDouble(fldPrice.getText().trim().replace(",", ""));

            List<SimulationRecord> records;
            if ("Maize".equals(simCommodity)) {
                int    months   = Integer.parseInt(fldMonths.getText().trim().replace(",", ""));
                double lossPct  = Double.parseDouble(fldLossPct.getText().trim().replace(",", ""));
                double moisture = Double.parseDouble(fldMoisture.getText().trim().replace(",", ""));
                Maize maize = new Maize(prod, prod * 1.2, moisture, price, lossPct);
                records = simRunner.runMaizeSimulation(maize, demand, months);
                populateMaizeResults(maize, records, demand, months);
            } else {
                double spoilage = Double.parseDouble(fldSpoilage.getText().trim().replace(",", ""));
                int    shelf    = Integer.parseInt(fldShelf.getText().trim().replace(",", ""));
                int    days     = Integer.parseInt(fldDays.getText().trim().replace(",", ""));
                StorageType st  = StorageType.valueOf(fldStorage.getValue());
                Tomato tomato = new Tomato(prod, shelf, st, spoilage, price);
                lastTomato    = tomato;
                lastTomatoDays = days;
                records = simRunner.runTomatoSimulation(tomato, demand, days, price * 40.0);
                populateTomatoResults(tomato, records, demand, days);
            }

            updateAlertsResultArea(records);
            simPage.onSimulationComplete(records, simCommodity);
            updateDashboardFromSimulation(records, simCommodity);
            updateStoragePageFromSimulation(records, simCommodity);
            simResultsSection.setVisible(true);
            simResultsSection.setManaged(true);

        } catch (NumberFormatException ex) {
            showSimError("Invalid input: please enter numeric values in all fields.");
        } catch (IllegalArgumentException ex) {
            showSimError("Parameter error: " + ex.getMessage());
        }
    }

    private void showSimError(String msg) {
        simErrorLabel.setText(msg);
        simErrorLabel.setVisible(true);
    }

    private void clearSimResults() {
        simResultsSection.setVisible(false);
        simResultsSection.setManaged(false);
        simErrorLabel.setVisible(false);
        simPage.resetSaveSection();
        // Clear every parameter field — both common and commodity-specific
        if (fldProd    != null) fldProd.clear();
        if (fldDemand  != null) fldDemand.clear();
        if (fldPrice   != null) fldPrice.clear();
        if (fldMonths  != null) fldMonths.clear();
        if (fldLossPct != null) fldLossPct.clear();
        if (fldMoisture!= null) fldMoisture.clear();
        if (fldSpoilage!= null) fldSpoilage.clear();
        if (fldShelf   != null) fldShelf.clear();
        if (fldDays    != null) fldDays.clear();
        if (fldStorage != null) fldStorage.setValue("COLD");
    }

    // ── Populate Maize results ─────────────────────────────────────────────────

    private void populateMaizeResults(Maize maize, List<SimulationRecord> records,
            double demand, int months) {
        double totalWaste = 0, totalRevenue = 0;
        for (SimulationRecord r : records) {
            totalWaste   += r.getWaste();
            totalRevenue += r.getRevenue();
        }

        SimulationRecord last = records.isEmpty() ? null : records.get(records.size() - 1);
        double lastSurplus = last == null ? 0 : last.getSurplusOrShortage();
        double adjPrice = supplyEngine.adjustPrice(maize.getPricePerTon(), lastSurplus,
                maize.getProductionVolume());
        double fsi = last == null ? 0 : last.getFoodSecurityIndicator();

        simCards[0].update((lastSurplus >= 0 ? "+" : "") + fmt(lastSurplus) + "t",
                "Surplus / Shortage",
                Math.min(1, Math.abs(lastSurplus) / maize.getProductionVolume()));
        simCards[1].update("ZMW " + fmtI(adjPrice), "Adjusted Price / Ton",
                Math.min(1, adjPrice / 8_000));
        simCards[2].update(String.format("%.1f%%", fsi), "Food Security", fsi / 100.0);
        simCards[3].update(fmt(totalWaste) + "t", "Total Waste (" + months + "mo)",
                Math.min(1, totalWaste / maize.getProductionVolume()));
        simCards[4].update(fmtRev(totalRevenue), "Est. Revenue",
                Math.min(1, totalRevenue / (maize.getProductionVolume() * maize.getPricePerTon())));

        barChart.getData().clear();
        XYChart.Series<String, Number> prodSeries = new XYChart.Series<>();
        prodSeries.setName("Remaining Stock");
        XYChart.Series<String, Number> demandSeries = new XYChart.Series<>();
        demandSeries.setName("Monthly Demand");
        for (SimulationRecord r : records) {
            String lbl = "M" + r.getMonthNumber();
            prodSeries.getData().add(new XYChart.Data<>(lbl, r.getProduction()));
            demandSeries.getData().add(new XYChart.Data<>(lbl, r.getDemand()));
        }
        barChart.getData().add(prodSeries);
        barChart.getData().add(demandSeries);
        barChart.setTitle("Monthly Stock vs Demand (tons)");

        lineChart.getData().clear();
        XYChart.Series<Number, Number> lossSeries = new XYChart.Series<>();
        lossSeries.setName("Remaining stock (t)");
        List<Double> schedule = storageSim.getMaizeLossSchedule(maize, months);
        for (int i = 0; i < schedule.size(); i++) {
            lossSeries.getData().add(new XYChart.Data<>(i, schedule.get(i)));
        }
        lineChart.getData().add(lossSeries);
        lineChart.setTitle("Compounded Storage Loss Over " + months + " Months");

        populatePieChart(records);

        String policyStr = policyEngine.evaluateAll(
                maize.getProductionVolume(), lastSurplus, maize.getProductionVolume(),
                months, -1, -1);
        String seasonSummary = records.isEmpty() ? "" : reporter.generateSeasonSummary(records);
        simPolicyArea.setText("Policy checks:\n" + policyStr + "\n\n" + seasonSummary);
    }

    // ── Populate Tomato results ────────────────────────────────────────────────

    private void populateTomatoResults(Tomato tomato, List<SimulationRecord> records,
            double demand, int days) {
        SimulationRecord r0 = records.isEmpty() ? null : records.get(0);
        double waste   = r0 == null ? 0 : r0.getWaste();
        double revenue = r0 == null ? 0 : r0.getRevenue();
        double surplus = r0 == null ? 0 : r0.getSurplusOrShortage();
        double fsi     = r0 == null ? 0 : r0.getFoodSecurityIndicator();
        double adjPrice = supplyEngine.adjustPrice(tomato.getPricePerTon(), surplus,
                tomato.getProductionVolume()) / 40.0;

        simCards[0].update((surplus >= 0 ? "+" : "") + fmt(surplus) + "t",
                "Surplus / Shortage",
                Math.min(1, Math.abs(surplus) / tomato.getProductionVolume()));
        simCards[1].update("ZMW " + fmtI(adjPrice), "Adjusted Price / Crate",
                Math.min(1, adjPrice / 200));
        simCards[2].update(String.format("%.1f%%", fsi), "Food Security", fsi / 100.0);
        simCards[3].update(String.format("%.1f", waste) + "t", "Total Spoilage",
                Math.min(1, waste / tomato.getProductionVolume()));
        simCards[4].update(fmtRev(revenue), "Est. Revenue",
                Math.min(1, revenue / tomato.estimateTotalValue()));

        barChart.getData().clear();
        double sold = tomato.getProductionVolume() - waste;
        XYChart.Series<String, Number> bars = new XYChart.Series<>();
        bars.setName("Tomato Outcome");
        bars.getData().add(new XYChart.Data<>("Total Production", tomato.getProductionVolume()));
        bars.getData().add(new XYChart.Data<>("Spoilage", waste));
        bars.getData().add(new XYChart.Data<>("Net Saleable", Math.max(0, sold)));
        barChart.getData().add(bars);
        barChart.setTitle("Tomato Outcome — " + tomato.getStorageType()
                + " Storage (" + days + " days)");

        lineChart.getData().clear();
        XYChart.Series<Number, Number> spoilageCurve = new XYChart.Series<>();
        spoilageCurve.setName("Cumulative spoilage (t)");
        int maxDays = Math.max(tomato.getShelfLifeDays(), days) + 2;
        for (int d = 0; d <= maxDays; d++) {
            spoilageCurve.getData().add(new XYChart.Data<>(d,
                    storageSim.simulateTomatoSpoilage(tomato, d)));
        }
        lineChart.getData().add(spoilageCurve);
        lineChart.setTitle("Tomato Spoilage Curve (cumulative)");

        populatePieChart(records);

        String policyStr = "Export check   : "
                + policyEngine.checkExportRestriction(surplus, tomato.getProductionVolume())
                + "\nShelf-life     : "
                + policyEngine.checkTomatoStorageTime(days, tomato.getShelfLifeDays())
                + "\nSpoilage total : " + String.format("%.2f", waste) + " t";
        String seasonSummary = records.isEmpty() ? "" : reporter.generateSeasonSummary(records);
        simPolicyArea.setText("Policy checks (Tomato):\n" + policyStr + "\n\n" + seasonSummary);
    }

    private void populatePieChart(List<SimulationRecord> records) {
        pieChart.getData().clear();
        int critical = 0, warning = 0, info = 0;
        for (SimulationRecord r : records) {
            String s = r.getPolicyAlerts();
            if (s == null || s.isEmpty()) continue;
            for (String part : s.split(";")) {
                String a = part.trim();
                if (a.isEmpty()) continue;
                if (a.contains("ALERT") || a.contains("CRITICAL") || a.contains("RISK HIGH"))
                    critical++;
                else if (a.contains("WARNING") || a.contains("REQUIRED") || a.contains("BREACHED"))
                    warning++;
                else
                    info++;
            }
        }
        if (critical > 0)
            pieChart.getData().add(new PieChart.Data("CRITICAL (" + critical + ")", critical));
        if (warning > 0)
            pieChart.getData().add(new PieChart.Data("WARNING  (" + warning + ")", warning));
        if (info > 0)
            pieChart.getData().add(new PieChart.Data("INFO     (" + info + ")", info));
        if (critical == 0 && warning == 0 && info == 0)
            pieChart.getData().add(new PieChart.Data("No alerts", 1));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STORAGE PAGE
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildStoragePage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setBackground(bg(PAGE_BG, 0));

        Label title = new Label("Storage Loss Analysis");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_PRI));

        Button clearFiguresBtn = actionButton("✕  Clear Figures", CORAL);
        clearFiguresBtn.setPadding(new Insets(8, 18, 8, 18));
        clearFiguresBtn.setOnAction(e -> clearStorageFigures());

        Region storageSpacer = new Region();
        HBox.setHgrow(storageSpacer, Priority.ALWAYS);
        HBox storageTitleRow = new HBox(16, title, storageSpacer, clearFiguresBtn);
        storageTitleRow.setAlignment(Pos.CENTER_LEFT);

        storageDescLabel = new Label("Run a simulation to see storage analysis for your parameters.");
        storageDescLabel.setFont(Font.font("Segoe UI", 13));
        storageDescLabel.setTextFill(Color.web(TEXT_SEC));
        storageDescLabel.setWrapText(true);

        // Stat cards — blank until simulation runs
        storageCards = new StatCard[] {
            new StatCard("—", "Initial Stock",    ACCENT_CYAN, true, 0),
            new StatCard("—", "Total Loss",       CORAL,       true, 0),
            new StatCard("—", "Remaining Stock",  ACCENT_TEAL, true, 0),
        };
        for (StatCard c : storageCards) HBox.setHgrow(c, Priority.ALWAYS);
        HBox cardsRow = new HBox(14, storageCards[0], storageCards[1], storageCards[2]);

        // Placeholder
        Label phIcon = new Label("🗄");
        phIcon.setFont(Font.font("Segoe UI", 42));
        Label phText = new Label("No simulation run yet.");
        phText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        phText.setTextFill(Color.web(TEXT_PRI));
        Label phHint = new Label("Run a Maize or Tomato simulation — the storage loss chart will appear here.");
        phHint.setFont(Font.font("Segoe UI", 13));
        phHint.setTextFill(Color.web(TEXT_MUTED));
        storagePlaceholder = new VBox(12, phIcon, phText, phHint);
        storagePlaceholder.setAlignment(javafx.geometry.Pos.CENTER);
        storagePlaceholder.setPadding(new Insets(60, 0, 60, 0));

        // Chart (hidden until simulation runs)
        NumberAxis sx = new NumberAxis();
        sx.setLabel("Period");
        NumberAxis sy = new NumberAxis();
        sy.setLabel("Tons");
        storageChart = new LineChart<>(sx, sy);
        storageChart.setAnimated(false);
        storageChart.setPrefHeight(360);
        storageChart.setStyle("-fx-background-color:#111C44;");

        storageChartArea = new VBox(storageChart);
        storageChartArea.setVisible(false);
        storageChartArea.setManaged(false);

        page.getChildren().addAll(storageTitleRow, storageDescLabel, cardsRow, storagePlaceholder, storageChartArea);
        return page;
    }

    private void updateStoragePageFromSimulation(List<SimulationRecord> records, String commodity) {
        if (records == null || records.isEmpty()) return;

        double firstProd   = records.get(0).getProduction();
        double firstWaste  = records.get(0).getWaste();
        double initialEst  = firstProd + firstWaste;
        double totalWaste  = records.stream().mapToDouble(SimulationRecord::getWaste).sum();
        double finalStock  = records.get(records.size() - 1).getProduction();

        storageCards[0].update(fmt(initialEst) + "t", "Initial Stock",
                Math.min(1, initialEst / 200_000));
        storageCards[1].update(fmt(totalWaste) + "t", "Total Loss",
                Math.min(1, totalWaste / Math.max(1, initialEst)));
        storageCards[2].update(fmt(finalStock) + "t", "Remaining Stock",
                Math.min(1, finalStock / Math.max(1, initialEst)));

        String periodDesc = "Tomato".equals(commodity)
                ? lastTomatoDays + " day" + (lastTomatoDays != 1 ? "s" : "") + " before sale"
                : records.size() + " month" + (records.size() != 1 ? "s" : "");
        storageDescLabel.setText(commodity + " simulation  ·  " + periodDesc
                + "  ·  Total loss: " + fmt(totalWaste) + "t");

        storageChart.getData().clear();

        XYChart.Series<Number, Number> stockSeries = new XYChart.Series<>();
        stockSeries.setName("Remaining Stock (t)");
        XYChart.Series<Number, Number> lossSeries = new XYChart.Series<>();
        lossSeries.setName("Cumulative Loss (t)");

        NumberAxis storageXAxis = (NumberAxis) storageChart.getXAxis();

        if ("Tomato".equals(commodity) && lastTomato != null) {
            storageXAxis.setLabel("Day");
            storageChart.setTitle("Tomato — Daily Spoilage over " + lastTomatoDays
                    + " Days (" + lastTomato.getStorageType() + " Storage)");
            for (int d = 0; d <= lastTomatoDays; d++) {
                double spoilage  = storageSim.simulateTomatoSpoilage(lastTomato, d);
                double remaining = Math.max(0.0, lastTomato.getProductionVolume() - spoilage);
                stockSeries.getData().add(new XYChart.Data<>(d, remaining));
                lossSeries.getData().add(new XYChart.Data<>(d, spoilage));
            }
        } else {
            int months = records.size();
            storageXAxis.setLabel("Month");
            storageChart.setTitle("Maize — Monthly Stock & Loss over " + months
                    + " Month" + (months != 1 ? "s" : ""));
            // Month 0 = initial state before any loss
            stockSeries.getData().add(new XYChart.Data<>(0, initialEst));
            lossSeries.getData().add(new XYChart.Data<>(0, 0.0));
            double cumLoss = 0;
            for (SimulationRecord r : records) {
                stockSeries.getData().add(new XYChart.Data<>(r.getMonthNumber(), r.getProduction()));
                cumLoss += r.getWaste();
                lossSeries.getData().add(new XYChart.Data<>(r.getMonthNumber(), cumLoss));
            }
        }

        storageChart.getData().add(stockSeries);
        storageChart.getData().add(lossSeries);

        storagePlaceholder.setVisible(false);
        storagePlaceholder.setManaged(false);
        storageChartArea.setVisible(true);
        storageChartArea.setManaged(true);
    }

    private void clearStorageFigures() {
        storageCards[0].update("—", "Initial Stock",   0);
        storageCards[1].update("—", "Total Loss",      0);
        storageCards[2].update("—", "Remaining Stock", 0);
        storageDescLabel.setText("Run a simulation to see storage analysis for your parameters.");
        storageChart.getData().clear();
        storageChartArea.setVisible(false);
        storageChartArea.setManaged(false);
        storagePlaceholder.setVisible(true);
        storagePlaceholder.setManaged(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ALERTS PAGE
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildAlertsPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setBackground(bg(PAGE_BG, 0));

        Label title = new Label("Policy Alerts — Interactive Checker");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_PRI));

        Label desc = new Label(
                "Enter stock levels below to evaluate FRA, export, storage, and price-floor policies.");
        desc.setFont(Font.font("Segoe UI", 13));
        desc.setTextFill(Color.web(TEXT_SEC));

        alertFldStock   = blankField("e.g. 480,000");
        alertFldSurplus = blankField("e.g. 31,500");
        alertFldProd    = blankField("e.g. 110,000");
        alertFldMonths  = blankField("e.g. 6");
        alertFldDays    = blankField("e.g. 7  (or -1 to skip)");
        alertFldShelf   = blankField("e.g. 14  (or -1 to skip)");

        GridPane alertGrid = formGrid();
        addRow(alertGrid, "National Stock (t):",              alertFldStock,   0);
        addRow(alertGrid, "Surplus (t):",                     alertFldSurplus, 1);
        addRow(alertGrid, "Production (t):",                  alertFldProd,    2);
        addRow(alertGrid, "Months Stored:",                   alertFldMonths,  3);
        addRow(alertGrid, "Days Stored (tomato, -1 to skip):", alertFldDays,   4);
        addRow(alertGrid, "Shelf Life (days, -1 to skip):",   alertFldShelf,   5);

        VBox alertFormCard = new VBox(0, alertGrid);
        alertFormCard.setPadding(new Insets(24));
        alertFormCard.setBackground(bg(CARD_BG, 12));
        alertFormCard.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(12);
        s.setOffsetY(4);
        s.setColor(Color.color(0, 0, 0, 0.35));
        alertFormCard.setEffect(s);

        Button checkBtn = new Button("⚑  Check Policies");
        checkBtn.setTextFill(Color.web("#1A1A1A"));
        checkBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        checkBtn.setBackground(bg(GOLD, 8));
        checkBtn.setCursor(javafx.scene.Cursor.HAND);
        checkBtn.setPadding(new Insets(12, 20, 12, 20));
        checkBtn.setOnAction(e -> runAlertCheck());

        Button alertClearBtn = new Button("Clear");
        alertClearBtn.setTextFill(Color.web(TEXT_SEC));
        alertClearBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        alertClearBtn.setBackground(bg(INPUT_BG, 8));
        alertClearBtn.setBorder(new Border(new BorderStroke(Color.web(INPUT_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1))));
        alertClearBtn.setCursor(javafx.scene.Cursor.HAND);
        alertClearBtn.setPadding(new Insets(12, 20, 12, 20));
        alertClearBtn.setOnAction(e -> {
            alertFldStock.clear();
            alertFldSurplus.clear();
            alertFldProd.clear();
            alertFldMonths.clear();
            alertFldDays.clear();
            alertFldShelf.clear();
            alertsResultArea.setText("Run a simulation or click Check Policies to see results.");
        });

        HBox alertBtnRow = new HBox(12, checkBtn, alertClearBtn);
        alertBtnRow.setAlignment(Pos.CENTER_LEFT);

        alertsResultArea = new TextArea("Run a simulation or click Check Policies to see results.");
        alertsResultArea.setEditable(false);
        alertsResultArea.setPrefRowCount(12);

        page.getChildren().addAll(title, desc, alertFormCard, alertBtnRow, alertsResultArea);
        return page;
    }

    private void runAlertCheck() {
        try {
            double stock   = Double.parseDouble(alertFldStock.getText().trim().replace(",", ""));
            double surplus = Double.parseDouble(alertFldSurplus.getText().trim().replace(",", ""));
            double prod    = Double.parseDouble(alertFldProd.getText().trim().replace(",", ""));
            int    months  = Integer.parseInt(alertFldMonths.getText().trim().replace(",", ""));
            int    days    = Integer.parseInt(alertFldDays.getText().trim().replace(",", ""));
            int    shelf   = Integer.parseInt(alertFldShelf.getText().trim().replace(",", ""));

            List<PolicyEngine.Alert> alerts = policyEngine.evaluateAllWithSeverity(
                    stock, surplus, prod, months, days, shelf);

            StringBuilder sb = new StringBuilder("=== POLICY EVALUATION ===\n\n");
            for (PolicyEngine.Alert a : alerts) {
                sb.append(String.format("[%-8s] %s%n", a.level, a.message));
            }
            sb.append("\n--- Individual Checks ---\n");
            sb.append("FRA Reserve    : ").append(policyEngine.checkFraReserve(stock)).append("\n");
            sb.append("Export         : ").append(policyEngine.checkExportRestriction(surplus, prod)).append("\n");
            sb.append("Maize Storage  : ").append(policyEngine.checkMaizeStorageTime(months)).append("\n");
            if (days >= 0 && shelf > 0) {
                sb.append("Tomato Shelf   : ")
                  .append(policyEngine.checkTomatoStorageTime(days, shelf)).append("\n");
            }
            alertsResultArea.setText(sb.toString());
        } catch (NumberFormatException ex) {
            alertsResultArea.setText(
                    "Input error: please enter numeric values in all fields.\n" + ex.getMessage());
        }
    }

    private void updateAlertsResultArea(List<SimulationRecord> records) {
        StringBuilder sb = new StringBuilder("=== POLICY ALERTS FROM LAST SIMULATION ===\n\n");
        for (SimulationRecord r : records) {
            sb.append(String.format("Month %-2d : %s%n", r.getMonthNumber(), r.getPolicyAlerts()));
        }
        if (alertsResultArea != null)
            alertsResultArea.setText(sb.toString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REPORTS PAGE
    // ══════════════════════════════════════════════════════════════════════════

    private ObservableList<ReportSummary> loadAllSavedReports() {
        ObservableList<ReportSummary> list = FXCollections.observableArrayList();
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) return list;
        FileHandler fh = new FileHandler();
        File[] files = reportsDir.listFiles((d, n) -> n.endsWith(".txt"));
        if (files == null) return list;
        for (File f : files) {
            try {
                List<SimulationRecord> records = fh.loadSimulation(f.getPath());
                if (!records.isEmpty()) {
                    list.add(ReportsPage.buildSummary(f.getName(), f.getPath(), records));
                } else {
                    System.err.println("Skipping malformed report: " + f.getName());
                }
            } catch (IOException e) {
                System.err.println("Skipping malformed report: " + f.getName());
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SETTINGS PAGE
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildSettingsPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setBackground(bg(PAGE_BG, 0));

        // ── Page header ──────────────────────────────────────────────────────
        Label title = new Label("About AgriTwin-ZM");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.web(TEXT_PRI));

        Label subtitle = new Label(
                "Digital Twin Simulation Platform for Zambian Maize & Tomato Supply Chains");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web(TEXT_SEC));

        // ── Project overview card ─────────────────────────────────────────────
        Label overviewIcon = new Label("🌾");
        overviewIcon.setFont(Font.font("Segoe UI", 36));

        Label overviewTitle = new Label("What is AgriTwin-ZM?");
        overviewTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        overviewTitle.setTextFill(Color.web(ACCENT_CYAN));

        Label overviewBody = new Label(
            "AgriTwin-ZM is a Java-based digital twin that models the Zambian agricultural " +
            "supply chain for maize and tomatoes. It simulates production volumes, market " +
            "demand, storage losses, spoilage, food security indicators, and government " +
            "policy triggers in real time. Users enter their own parameters to explore " +
            "supply-chain outcomes and save simulation reports.");
        overviewBody.setFont(Font.font("Segoe UI", 13));
        overviewBody.setTextFill(Color.web(TEXT_SEC));
        overviewBody.setWrapText(true);

        HBox overviewInner = new HBox(18, overviewIcon, new VBox(8, overviewTitle, overviewBody));
        overviewInner.setAlignment(Pos.TOP_LEFT);
        VBox overviewCard = infoCard(overviewInner);

        // ── Two-column info row ────────────────────────────────────────────────
        VBox academicCard  = labelledCard("🎓  Academic Context", new String[][]{
            {"Course",       "BIT 213 / BSC 120"},
            {"Institution",  "University of Lusaka"},
            {"Presentation", "13 May 2026  —  Semester Pitch"},
            {"Version",      "AgriTwin-ZM v1.0"},
            {"Platform",     "Java 21 + JavaFX 25"},
        });

        VBox howToCard = labelledCard("📖  How to Use", new String[][]{
            {"1. Simulate",  "Go to Simulation, choose Maize or Tomato"},
            {"2. Enter",     "Type in your production, demand and price figures"},
            {"3. Run",       "Click ▶ Run Simulation to compute outcomes"},
            {"4. Review",    "Check the results cards, charts and policy report"},
            {"5. Save",      "Click 💾 Save Report to save simulation results"},
            {"6. Dashboard", "Return here — cards update with your latest run"},
        });

        HBox.setHgrow(academicCard, Priority.ALWAYS);
        HBox.setHgrow(howToCard,   Priority.ALWAYS);
        HBox twoCol = new HBox(18, academicCard, howToCard);

        // ── Model parameters row ───────────────────────────────────────────────
        VBox maizeCard = labelledCard("🌽  Maize Model", new String[][]{
            {"FRA Reserve Threshold",  "500,000 t  (national strategic reserve)"},
            {"Export Surplus Trigger", "20% of production  (export restriction)"},
            {"Max Maize Storage",      "6 months  (before critical quality loss)"},
            {"Moisture Threshold",     "14%  (above → accelerated aflatoxin risk)"},
            {"Max Monthly Loss Cap",   "20% / month  (compound storage decay)"},
            {"Price Elasticity",       "0.1 coefficient  (supply-demand adjustment)"},
        });

        VBox tomatoCard = labelledCard("🍅  Tomato Model", new String[][]{
            {"Cold Storage Factor",    "0.6×  (40% spoilage reduction vs open)"},
            {"Shelf-Life Warning",     "Triggered when days stored ≥ shelf life"},
            {"Spoilage Rate",          "% per day  (user-defined, cold-adjusted)"},
            {"Food Security Target",   "≥ 80% FSI  (green),  60–79%  (warning)"},
            {"Critical FSI",           "< 60%  (red alert — policy review needed)"},
            {"Revenue Calculation",    "Net saleable × price per crate"},
        });

        HBox.setHgrow(maizeCard,  Priority.ALWAYS);
        HBox.setHgrow(tomatoCard, Priority.ALWAYS);
        HBox modelsRow = new HBox(18, maizeCard, tomatoCard);

        page.getChildren().addAll(title, subtitle, overviewCard, twoCol, modelsRow);
        return page;
    }

    private VBox infoCard(javafx.scene.Node content) {
        VBox card = new VBox(0, content);
        card.setPadding(new Insets(22));
        card.setBackground(bg(CARD_BG, 12));
        card.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(10);
        s.setOffsetY(3);
        s.setColor(Color.color(0, 0, 0, 0.35));
        card.setEffect(s);
        return card;
    }

    private VBox labelledCard(String heading, String[][] rows) {
        Label headLbl = new Label(heading);
        headLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        headLbl.setTextFill(Color.web(ACCENT_CYAN));
        headLbl.setPadding(new Insets(0, 0, 10, 0));

        VBox content = new VBox(0, headLbl);
        for (String[] row : rows) {
            Label k = new Label(row[0] + ":");
            k.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            k.setTextFill(Color.web(TEXT_SEC));
            k.setMinWidth(190);

            Label v = new Label(row[1]);
            v.setFont(Font.font("Segoe UI", 12));
            v.setTextFill(Color.web(TEXT_PRI));
            v.setWrapText(true);

            HBox hrow = new HBox(12, k, v);
            hrow.setAlignment(Pos.TOP_LEFT);
            hrow.setPadding(new Insets(5, 0, 5, 0));
            hrow.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    new BorderWidths(0, 0, 1, 0))));
            content.getChildren().add(hrow);
        }

        VBox card = new VBox(0, content);
        card.setPadding(new Insets(20));
        card.setBackground(bg(CARD_BG, 12));
        card.setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        DropShadow s = new DropShadow();
        s.setRadius(10);
        s.setOffsetY(3);
        s.setColor(Color.color(0, 0, 0, 0.35));
        card.setEffect(s);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════════════════════

    private void handleNavSelect(String label) {
        switch (label) {
            case "Dashboard":    selectPage(PG_DASHBOARD);  break;
            case "Simulation":   selectPage(PG_SIMULATION); break;
            case "Storage":      selectPage(PG_STORAGE);    break;
            case "Policy Alerts":selectPage(PG_ALERTS);     break;
            case "Reports":      selectPage(PG_REPORTS);    break;
            case "Information":  selectPage(PG_SETTINGS);   break;
            default: break;
        }
    }

    private void selectPage(int idx) {
        if (pages == null || idx < 0 || idx >= pages.length) return;
        VBox incoming = pages[idx];
        incoming.setOpacity(0.0);
        contentScroll.setContent(incoming);
        sidebar.setSelected(idx);
        FadeTransition ft = new FadeTransition(Duration.millis(200), incoming);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shared tab helpers
    // ══════════════════════════════════════════════════════════════════════════

    private Label tabLabel(String text, boolean active) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", active ? FontWeight.BOLD : FontWeight.NORMAL, 14));
        lbl.setTextFill(Color.web(active ? TEXT_PRI : TEXT_MUTED));
        lbl.setPadding(new Insets(0, 12, 6, 12));
        return lbl;
    }

    private Region tabUnderline(boolean active) {
        Region line = new Region();
        line.setPrefHeight(3);
        line.setBackground(bg(active ? ACCENT_CYAN : "transparent", 2));
        return line;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shared card / form helpers
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildCardsRow(StatCard[] cardArr) {
        HBox row = new HBox(14);
        for (StatCard c : cardArr) {
            HBox.setHgrow(c, Priority.ALWAYS);
            row.getChildren().add(c);
        }
        return row;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(20);
        g.setVgap(14);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(200);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    private void addRow(GridPane g, String labelText,
                        javafx.scene.control.Control field, int row) {
        Label lbl = formLabel(labelText);
        GridPane.setHalignment(lbl, HPos.LEFT);
        g.add(lbl, 0, row);
        g.add(field, 1, row);
    }

    private Label formLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web(TEXT_SEC));
        return lbl;
    }

    private TextField blankField(String promptText) {
        TextField tf = new TextField();
        tf.setPromptText(promptText);
        tf.setPrefWidth(220);
        return tf;
    }

    private Button actionButton(String text, String bgHex) {
        Button btn = new Button(text);
        btn.setTextFill(Color.WHITE);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setBackground(bg(bgHex, 8));
        btn.setCursor(javafx.scene.Cursor.HAND);
        return btn;
    }

    private Label sectionDivider(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(ACCENT_CYAN));
        lbl.setPadding(new Insets(2, 0, 0, 0));
        return lbl;
    }

    private Region gap(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        return r;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Style / colour helpers
    // ══════════════════════════════════════════════════════════════════════════

    private Background bg(String hex, double radius) {
        Color c = "transparent".equalsIgnoreCase(hex) ? Color.TRANSPARENT : Color.web(hex);
        return new Background(new BackgroundFill(c, new CornerRadii(radius), Insets.EMPTY));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Formatting helpers
    // ══════════════════════════════════════════════════════════════════════════

    private String fmt(double v) {
        return v >= 1_000 ? String.format("%,.0f", v) : String.format("%.1f", v);
    }

    private String fmtI(double v) {
        return String.format("%,.0f", v);
    }

    private String fmtRev(double v) {
        if (v >= 1_000_000_000) return String.format("ZMW %.2fB", v / 1_000_000_000);
        if (v >= 1_000_000)     return String.format("ZMW %.1fM", v / 1_000_000);
        if (v >= 1_000)         return String.format("ZMW %.0fK", v / 1_000);
        return String.format("ZMW %.0f", v);
    }

}
