package agritwin.ui;

import agritwin.model.SimulationRecord;
import agritwin.ui.components.ReportCard;
import agritwin.ui.model.ReportSummary;
import agritwin.util.FileHandler;

import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

public class ReportsPage {

    private static final String PAGE_BG     = "#0a0f1e";
    private static final String CARD_BG     = "#111827";
    private static final String CARD_BORDER = "#1f2937";
    private static final String ACCENT_CYAN = "#00d4b4";
    private static final String ACCENT_TEAL = "#00d4b4";
    private static final String GREEN_PILL  = "#10b981";
    private static final String TEXT_PRI    = "#f9fafb";
    private static final String TEXT_SEC    = "#9ca3af";
    private static final String TEXT_MUTED  = "#4b5563";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final ObservableList<ReportSummary> savedReports;
    private final Runnable goToSimulation;
    private final Supplier<String> reportContentSupplier;

    // Root node held across grid↔detail transitions
    private final VBox root;

    // Grid view components
    private ScrollPane gridScroll;
    private FlowPane   flowPane;
    private double     savedScrollVvalue = 0.0;

    private final FileHandler fileHandler = new FileHandler();
    private final ReportDetailPage detailPageBuilder = new ReportDetailPage();

    public ReportsPage(ObservableList<ReportSummary> savedReports,
                       Runnable goToSimulation,
                       Supplier<String> reportContentSupplier) {
        this.savedReports          = savedReports;
        this.goToSimulation        = goToSimulation;
        this.reportContentSupplier = reportContentSupplier;
        root = new VBox(0);
        root.setBackground(bg(PAGE_BG, 0));
    }

    public VBox buildPage() {
        root.getChildren().add(buildTopBar());

        flowPane = new FlowPane();
        flowPane.setHgap(20);
        flowPane.setVgap(20);
        flowPane.setPadding(new Insets(24));
        flowPane.setBackground(bg(PAGE_BG, 0));

        gridScroll = new ScrollPane(flowPane);
        gridScroll.setFitToWidth(true);
        gridScroll.setStyle(
            "-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        populateGrid();

        savedReports.addListener((ListChangeListener<ReportSummary>) c -> populateGrid());

        showGridView(false);
        return root;
    }

    // ── Top bar ──────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        Label title = new Label("Simulation Reports");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_PRI));

        Label subtitle = new Label("Saved simulation runs — click any card to view full report");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web(TEXT_MUTED));

        VBox titleBox = new VBox(4, title, subtitle);

        Button goSimBtn = primaryButton("▶ Go to Simulation");
        goSimBtn.setOnAction(e -> goToSimulation.run());

        Button refreshBtn = outlineButton("🔄");
        refreshBtn.setTooltip(new javafx.scene.control.Tooltip("Re-scan reports folder"));
        refreshBtn.setOnAction(e -> refreshFromDisk());

        Button saveReportBtn = outlineButton("💾 Save Report");
        saveReportBtn.setOnAction(e -> handleExportReport());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(14, titleBox, spacer, goSimBtn, refreshBtn, saveReportBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(20, 28, 16, 28));
        bar.setBackground(bg(PAGE_BG, 0));
        return bar;
    }

    // ── Grid population ──────────────────────────────────────────────────────

    private void populateGrid() {
        flowPane.getChildren().clear();

        if (savedReports.isEmpty()) {
            showEmptyState();
            return;
        }

        for (ReportSummary summary : savedReports) {
            ReportCard card = new ReportCard(
                summary,
                () -> navigateToDetail(summary),
                () -> deleteReport(summary)
            );
            card.setUserData(summary);
            flowPane.getChildren().add(card);
        }
    }

    private void showEmptyState() {
        Label icon = new Label("📋");
        icon.setFont(Font.font("Segoe UI", 48));

        Label noReports = new Label("No saved reports yet.");
        noReports.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        noReports.setTextFill(Color.web(TEXT_PRI));

        Label hint = new Label("Run a simulation and click Save Report to save it here.");
        hint.setFont(Font.font("Segoe UI", 13));
        hint.setTextFill(Color.web(TEXT_MUTED));

        Button goBtn = primaryButton("▶ Go to Simulation");
        goBtn.setOnAction(e -> goToSimulation.run());

        VBox emptyBox = new VBox(16, icon, noReports, hint, goBtn);
        emptyBox.setAlignment(Pos.CENTER);

        StackPane emptyState = new StackPane(emptyBox);
        emptyState.setPrefSize(600, 400);
        StackPane.setAlignment(emptyBox, Pos.CENTER);

        flowPane.getChildren().add(emptyState);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void navigateToDetail(ReportSummary summary) {
        savedScrollVvalue = gridScroll.getVvalue();

        VBox detailView = detailPageBuilder.buildView(summary, this::navigateBackToGrid);

        FadeTransition ft = new FadeTransition(Duration.millis(200), detailView);
        ft.setFromValue(0);
        ft.setToValue(1);

        // Swap to a ScrollPane wrapping the detail view
        ScrollPane detailScroll = new ScrollPane(detailView);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle(
            "-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        VBox.setVgrow(detailScroll, Priority.ALWAYS);

        if (root.getChildren().size() > 1) root.getChildren().remove(root.getChildren().size() - 1);
        root.getChildren().add(detailScroll);
        ft.play();
    }

    private void navigateBackToGrid() {
        showGridView(true);
    }

    private void showGridView(boolean animate) {
        if (root.getChildren().size() > 1) root.getChildren().remove(root.getChildren().size() - 1);
        root.getChildren().add(gridScroll);

        if (animate) {
            FadeTransition ft = new FadeTransition(Duration.millis(200), gridScroll);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setOnFinished(e -> gridScroll.setVvalue(savedScrollVvalue));
            ft.play();
        } else {
            gridScroll.setOpacity(1);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void deleteReport(ReportSummary summary) {
        flowPane.getChildren().stream()
            .filter(n -> n instanceof ReportCard && n.getUserData() == summary)
            .findFirst()
            .ifPresentOrElse(card -> {
                FadeTransition ft = new FadeTransition(Duration.millis(250), card);
                ft.setFromValue(1);
                ft.setToValue(0);
                ft.setOnFinished(e -> {
                    File f = new File(summary.getFilePath());
                    if (f.exists()) f.delete();
                    savedReports.remove(summary);
                });
                ft.play();
            }, () -> {
                // Card not visible (e.g. navigated away) — still delete
                File f = new File(summary.getFilePath());
                if (f.exists()) f.delete();
                savedReports.remove(summary);
            });
    }

    // ── Disk refresh ──────────────────────────────────────────────────────────

    private void refreshFromDisk() {
        savedReports.clear();
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) return;
        File[] files = reportsDir.listFiles((d, n) -> n.endsWith(".txt"));
        if (files == null) return;
        for (File f : files) {
            try {
                List<SimulationRecord> records = fileHandler.loadSimulation(f.getPath());
                if (!records.isEmpty()) {
                    savedReports.add(buildSummary(f.getName(), f.getPath(), records));
                } else {
                    System.err.println("Skipping malformed report: " + f.getName());
                }
            } catch (IOException e) {
                System.err.println("Skipping malformed report: " + f.getName());
            }
        }
    }

    // ── Export report ─────────────────────────────────────────────────────────

    /** Opens a FileChooser and writes the full simulation report to a .txt file. */
    private void handleExportReport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Simulation Report");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        fc.setInitialFileName("AgriTwin_Report_" + LocalDate.now().format(df) + ".txt");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));
        File file = fc.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (file == null) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(reportContentSupplier.get());
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Report saved successfully to:\n" + file.getAbsolutePath(),
                    ButtonType.OK);
            alert.setTitle("Report Saved");
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Failed to save report:\n" + ex.getMessage(),
                    ButtonType.OK);
            alert.setTitle("Save Failed");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    // ── Summary builder ───────────────────────────────────────────────────────

    public static ReportSummary buildSummary(String fileName, String filePath,
                                              List<SimulationRecord> records) {
        String reportName = fileName.endsWith(".txt")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;

        String commodity = "Unknown";
        String upper = fileName.toUpperCase();
        if (upper.contains("MAIZE"))  commodity = "Maize";
        else if (upper.contains("TOMATO")) commodity = "Tomato";
        else if (!records.isEmpty())  commodity = records.get(0).getCommodityName();

        String dateSaved = "Unknown";
        try {
            BasicFileAttributes attrs = Files.readAttributes(
                new File(filePath).toPath(), BasicFileAttributes.class);
            dateSaved = attrs.lastModifiedTime()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FMT);
        } catch (IOException ignored) {}

        double totalRevenue = 0, totalFsi = 0;
        for (SimulationRecord r : records) {
            totalRevenue += r.getRevenue();
            totalFsi     += r.getFoodSecurityIndicator();
        }
        double avgFsi = records.isEmpty() ? 0 : totalFsi / records.size();

        String fraStatus = "FRA RESERVE OK";
        for (SimulationRecord r : records) {
            String alerts = r.getPolicyAlerts();
            if (alerts != null && alerts.contains("RESERVE ALERT")) {
                fraStatus = "RESERVE ALERT";
                break;
            }
        }

        return new ReportSummary(reportName, filePath, commodity,
                                 dateSaved, totalRevenue, avgFsi, records.size(), fraStatus);
    }

    // ── Button helpers ────────────────────────────────────────────────────────

    private Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.setTextFill(Color.WHITE);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setBackground(bg(GREEN_PILL, 8));
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setPadding(new Insets(8, 16, 8, 16));
        return btn;
    }

    private Button outlineButton(String text) {
        Button btn = new Button(text);
        btn.setTextFill(Color.web(ACCENT_CYAN));
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setBackground(bg("transparent", 8));
        btn.setBorder(new Border(new BorderStroke(Color.web(ACCENT_CYAN),
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5))));
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setPadding(new Insets(8, 14, 8, 14));
        return btn;
    }

    private Background bg(String hex, double radius) {
        Color c = "transparent".equalsIgnoreCase(hex) ? Color.TRANSPARENT : Color.web(hex);
        return new Background(new BackgroundFill(c, new CornerRadii(radius), Insets.EMPTY));
    }
}
