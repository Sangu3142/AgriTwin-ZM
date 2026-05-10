package agritwin.ui;

import agritwin.model.SimulationRecord;
import agritwin.ui.model.ReportSummary;
import agritwin.util.FileHandler;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SimulationPage {

    private static final String ACCENT_CYAN = "#00B5D8";
    private static final String GREEN_PILL  = "#48BB78";
    private static final String CORAL       = "#F56565";

    private static final DateTimeFormatter FILENAME_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final ObservableList<ReportSummary> savedReports;
    private final FileHandler fileHandler = new FileHandler();

    private final VBox   saveSectionNode;
    private final Button saveBtn;
    private final Label  errorLabel;
    private       VBox   confirmBadge;

    private List<SimulationRecord> lastRecords;
    private String                 lastCommodity = "Sim";

    public SimulationPage(ObservableList<ReportSummary> savedReports) {
        this.savedReports = savedReports;

        saveBtn = buildSaveButton();

        errorLabel = new Label();
        errorLabel.setTextFill(Color.web(CORAL));
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        saveSectionNode = new VBox(8, saveBtn, errorLabel);
        saveSectionNode.setAlignment(Pos.CENTER_LEFT);
        saveSectionNode.setVisible(false);
        saveSectionNode.setManaged(false);
    }

    public VBox getSaveSectionNode() { return saveSectionNode; }

    public void onSimulationComplete(List<SimulationRecord> records, String commodity) {
        this.lastRecords   = records;
        this.lastCommodity = commodity == null ? "Sim" : commodity;

        if (confirmBadge != null && saveSectionNode.getChildren().contains(confirmBadge)) {
            saveSectionNode.getChildren().remove(confirmBadge);
            confirmBadge = null;
        }
        if (!saveSectionNode.getChildren().contains(saveBtn)) {
            saveSectionNode.getChildren().add(0, saveBtn);
        }
        saveBtn.setVisible(true);
        saveBtn.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        saveSectionNode.setVisible(true);
        saveSectionNode.setManaged(true);
    }

    public void resetSaveSection() {
        saveSectionNode.setVisible(false);
        saveSectionNode.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        lastRecords = null;
    }

    // ── Save Report button ────────────────────────────────────────────────────

    private Button buildSaveButton() {
        Button btn = new Button("💾  Save Report");
        btn.setTextFill(Color.web(ACCENT_CYAN));
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setBackground(bg("#1A2A6C", 10));
        btn.setBorder(new Border(new BorderStroke(Color.web(ACCENT_CYAN),
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5))));
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setPadding(new Insets(10, 22, 10, 22));

        DropShadow hoverGlow = new DropShadow();
        hoverGlow.setRadius(14);
        hoverGlow.setSpread(0);
        hoverGlow.setColor(Color.web(ACCENT_CYAN).deriveColor(0, 1, 1, 0.5));

        btn.setOnMouseEntered(e -> {
            btn.setBackground(bg("rgba(0,181,216,0.15)", 10));
            btn.setEffect(hoverGlow);
            scale(btn, 1.04);
        });
        btn.setOnMouseExited(e -> {
            btn.setBackground(bg("#1A2A6C", 10));
            btn.setEffect(null);
            scale(btn, 1.0);
        });
        btn.setOnMousePressed(e  -> scale(btn, 0.96));
        btn.setOnMouseReleased(e -> scale(btn, 1.04));
        btn.setOnAction(e -> handleSaveClicked());
        return btn;
    }

    private void handleSaveClicked() {
        if (lastRecords == null || lastRecords.isEmpty()) return;

        String defaultName = lastCommodity + "_Sim_"
                + LocalDate.now().format(FILENAME_DATE);

        TextInputDialog dialog = new TextInputDialog(defaultName);
        dialog.setTitle("Save Simulation Report");
        dialog.setHeaderText("Enter a name for this report");
        dialog.setContentText("Report name:");
        dialog.getDialogPane().setStyle(
            "-fx-background-color:#111C44;-fx-border-color:#1A2A6C;");
        dialog.getDialogPane().lookup(".content.label")
              .setStyle("-fx-text-fill:#A0AEC0;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) return;
            performSave(trimmed);
        });
    }

    private void performSave(String reportName) {
        new File("reports").mkdirs();
        String filename = "reports/" + reportName.replaceAll("\\s+", "_") + ".txt";
        try {
            fileHandler.saveSimulation(lastRecords, filename);
            showSavedBadge(reportName, filename);
        } catch (IOException e) {
            showSaveError();
        }
    }

    private void showSavedBadge(String reportName, String filePath) {
        saveBtn.setVisible(false);
        saveBtn.setManaged(false);

        Label badge = new Label("✓  Saved as " + reportName);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        badge.setTextFill(Color.web(GREEN_PILL));
        badge.setOpacity(0);

        confirmBadge = new VBox(badge);
        confirmBadge.setAlignment(Pos.CENTER_LEFT);
        saveSectionNode.getChildren().add(0, confirmBadge);

        FadeTransition ft = new FadeTransition(Duration.millis(400), badge);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        double totalRevenue = lastRecords.stream().mapToDouble(SimulationRecord::getRevenue).sum();
        double avgFsi = lastRecords.stream()
                .mapToDouble(SimulationRecord::getFoodSecurityIndicator)
                .average().orElse(0);
        String dateSaved = LocalDateTime.now().format(DISPLAY_DATE);
        ReportSummary summary = new ReportSummary(
            reportName, filePath, lastCommodity,
            dateSaved, totalRevenue, avgFsi, lastRecords.size());
        savedReports.add(summary);

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showSaveError() {
        errorLabel.setText("⚠  Save failed. Check file permissions.");
        errorLabel.setOpacity(0);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void scale(Button btn, double factor) {
        ScaleTransition st = new ScaleTransition(
                Duration.millis(factor == 0.96 ? 80 : 120), btn);
        st.setToX(factor);
        st.setToY(factor);
        st.play();
    }

    private Background bg(String hex, double radius) {
        if (hex.startsWith("rgba")) {
            return new Background(new BackgroundFill(
                Color.web("#00B5D8").deriveColor(0, 1, 1, 0.15),
                new CornerRadii(radius), Insets.EMPTY));
        }
        Color c = "transparent".equalsIgnoreCase(hex) ? Color.TRANSPARENT : Color.web(hex);
        return new Background(new BackgroundFill(c, new CornerRadii(radius), Insets.EMPTY));
    }
}
