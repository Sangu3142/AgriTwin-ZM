package agritwin.ui.components;

import agritwin.ui.model.ReportSummary;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.OverrunStyle;
import javafx.util.Duration;

public class ReportCard extends VBox {

    private static final String CARD_BG     = "#111C44";
    private static final String CARD_BORDER = "#1A2A6C";
    private static final String ACCENT_CYAN = "#00B5D8";
    private static final String ACCENT_TEAL = "#00CFB4";
    private static final String TEXT_PRI    = "#FFFFFF";
    private static final String TEXT_SEC    = "#A0AEC0";
    private static final String TEXT_MUTED  = "#718096";
    private static final String CORAL       = "#F56565";
    private static final String GOLD        = "#ECC94B";
    private static final String GREEN_PILL  = "#48BB78";
    private static final String PROGRESS_BG = "#1E3A5F";

    private final ReportSummary summary;

    @SuppressWarnings("this-escape")
    public ReportCard(ReportSummary summary, Runnable onView, Runnable onDelete) {
        this.summary = summary;
        setPrefWidth(280);
        setPadding(new Insets(20));
        setSpacing(12);
        setBackground(bg(CARD_BG, 12));
        setBorder(new Border(new BorderStroke(Color.web(CARD_BORDER),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));

        DropShadow normalShadow = new DropShadow();
        normalShadow.setRadius(8);
        normalShadow.setOffsetY(2);
        normalShadow.setColor(Color.color(0, 0, 0, 0.3));
        setEffect(normalShadow);

        getChildren().addAll(buildTopRow(), buildStatsRow(), buildFraRow(), buildProgressRow());

        setupHoverEffect(normalShadow);
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) onView.run();
        });
        setupContextMenu(onDelete);
        setCursor(javafx.scene.Cursor.HAND);
    }

    private HBox buildTopRow() {
        String commodity  = summary.getCommodityType();
        String initial    = commodity.isEmpty() ? "?" : commodity.substring(0, 1).toUpperCase();
        String avatarColor = "Maize".equalsIgnoreCase(commodity) ? GREEN_PILL
                           : "Tomato".equalsIgnoreCase(commodity) ? CORAL
                           : ACCENT_CYAN;

        Circle circle = new Circle(20);
        circle.setFill(Color.web(avatarColor));
        Label initLbl = new Label(initial);
        initLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        initLbl.setTextFill(Color.WHITE);
        StackPane avatar = new StackPane(circle, initLbl);
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);

        Label nameLbl = new Label(summary.getReportName());
        nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLbl.setTextFill(Color.web(TEXT_PRI));
        nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLbl.setMaxWidth(190);

        Label dateLbl = new Label(summary.getDateSaved());
        dateLbl.setFont(Font.font("Segoe UI", 11));
        dateLbl.setTextFill(Color.web(TEXT_MUTED));

        VBox nameBox = new VBox(2, nameLbl, dateLbl);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(10, avatar, nameBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildStatsRow() {
        double fsi      = summary.getAvgFoodSecurity();
        String fsiColor = fsi >= 80 ? ACCENT_TEAL : fsi >= 60 ? GOLD : CORAL;

        HBox row = new HBox(12,
            miniStat(formatRevenue(summary.getTotalRevenue()), TEXT_SEC),
            miniStat(summary.getRecordCount() + " months",    TEXT_SEC),
            miniStat(String.format("%.1f%% FSI", fsi),        fsiColor));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildFraRow() {
        String status = summary.getFraStatus();
        boolean isAlert = status != null && status.contains("ALERT");
        String label = isAlert ? "⚠ FRA Reserve Alert" : "✓ FRA Reserve OK";
        String color = isAlert ? CORAL : ACCENT_TEAL;

        Label fraLbl = new Label(label);
        fraLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        fraLbl.setTextFill(Color.web(color));

        HBox row = new HBox(fraLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildProgressRow() {
        double frac = Math.min(1.0, summary.getAvgFoodSecurity() / 100.0);

        Region progressBg = new Region();
        progressBg.setPrefHeight(4);
        progressBg.setMaxWidth(Double.MAX_VALUE);
        progressBg.setBackground(bg(PROGRESS_BG, 2));

        Region progressFill = new Region();
        progressFill.setPrefHeight(4);
        progressFill.setBackground(bg(ACCENT_TEAL, 2));

        StackPane bar = new StackPane(progressBg, progressFill);
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);
        progressFill.prefWidthProperty().bind(bar.widthProperty().multiply(frac));

        Label viewLink = new Label("View Report →");
        viewLink.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        viewLink.setTextFill(Color.web(ACCENT_CYAN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox linkRow = new HBox(spacer, viewLink);
        linkRow.setAlignment(Pos.CENTER_RIGHT);

        return new VBox(8, bar, linkRow);
    }

    private void setupHoverEffect(DropShadow normalShadow) {
        DropShadow hoverShadow = new DropShadow();
        hoverShadow.setRadius(14);
        hoverShadow.setOffsetY(2);
        hoverShadow.setColor(Color.web("#00B5D8").deriveColor(0, 1, 1, 0.2));

        setOnMouseEntered(e -> {
            new Timeline(new KeyFrame(Duration.millis(150),
                new KeyValue(translateYProperty(), -4))).play();
            setEffect(hoverShadow);
        });
        setOnMouseExited(e -> {
            new Timeline(new KeyFrame(Duration.millis(150),
                new KeyValue(translateYProperty(), 0))).play();
            setEffect(normalShadow);
        });
    }

    private void setupContextMenu(Runnable onDelete) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color:#111C44;-fx-border-color:#1A2A6C;");

        MenuItem deleteItem = new MenuItem("🗑 Delete Report");
        deleteItem.setStyle("-fx-text-fill:white;");
        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete \"" + summary.getReportName() + "\"?\nThis cannot be undone.",
                    ButtonType.YES, ButtonType.CANCEL);
            confirm.setTitle("Delete Report");
            confirm.setHeaderText(null);
            confirm.getDialogPane().setStyle(
                "-fx-background-color:#111C44;-fx-border-color:#1A2A6C;");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) onDelete.run();
            });
        });

        MenuItem copyItem = new MenuItem("📋 Copy File Path");
        copyItem.setStyle("-fx-text-fill:white;");
        copyItem.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(summary.getFilePath());
            Clipboard.getSystemClipboard().setContent(cc);
        });

        menu.getItems().addAll(deleteItem, copyItem);
        setOnContextMenuRequested(e -> menu.show(this, e.getScreenX(), e.getScreenY()));
    }

    private Label miniStat(String text, String colorHex) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web(colorHex));
        return lbl;
    }

    private String formatRevenue(double v) {
        if (v >= 1_000_000_000) return String.format("ZMW %.1fB", v / 1_000_000_000);
        if (v >= 1_000_000)     return String.format("ZMW %.1fM", v / 1_000_000);
        if (v >= 1_000)         return String.format("ZMW %.0fK", v / 1_000);
        return String.format("ZMW %.0f", v);
    }

    private Background bg(String hex, double radius) {
        Color c = "transparent".equalsIgnoreCase(hex) ? Color.TRANSPARENT : Color.web(hex);
        return new Background(new BackgroundFill(c, new CornerRadii(radius), Insets.EMPTY));
    }
}
