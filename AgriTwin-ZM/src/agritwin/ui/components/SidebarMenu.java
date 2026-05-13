package agritwin.ui.components;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
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
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MODULE: Sidebar Navigation Menu
 * PURPOSE: Grouped dark-panel left navigation with CORE / SUPPLY CHAIN / REPORTS
 *          sections, status indicator, badge overlays (Live / FRA alert), and
 *          cyan accent bar on the active item.
 * OOP CONCEPTS: Encapsulation; Composition; Callback via Consumer<String>.
 */
public class SidebarMenu extends VBox {

    private static final String SIDEBAR_BG     = "#0a0f1e";
    private static final String SIDEBAR_ACTIVE = "#1f2937";
    private static final String ACCENT         = "#00d4b4";
    private static final String SUCCESS        = "#10b981";
    private static final String AMBER          = "#f59e0b";
    private static final String DANGER         = "#ef4444";
    private static final String TEXT_PRI       = "#f9fafb";
    private static final String TEXT_SEC       = "#9ca3af";
    private static final String TEXT_MUTED     = "#4b5563";

    // {label, icon, group}
    private static final String[][] NAV_ITEMS = {
        {"Dashboard",     "🏠", "CORE"},
        {"Simulate",      "▶",  "CORE"},
        {"Analytics",     "📈", "CORE"},
        {"Storage",       "🗄",  "SUPPLY CHAIN"},
        {"FRA Reserves",  "🏛", "SUPPLY CHAIN"},
        {"Season Report", "📋", "REPORTS"},
        {"Export CSV",    "📤", "REPORTS"},
        {"About",         "ℹ",  "REPORTS"},
    };

    private final Map<String, StackPane> rowMap = new HashMap<>();
    private String selectedLabel = "Dashboard";

    private Circle statusDot;
    private Label  statusText;
    private Label  fraAlertBadge;

    @SuppressWarnings("this-escape")
    public SidebarMenu(Consumer<String> onSelect) {
        setPrefWidth(210);
        setMinWidth(190);
        setSpacing(0);
        setBackground(new Background(new BackgroundFill(
                Color.web(SIDEBAR_BG), CornerRadii.EMPTY, Insets.EMPTY)));

        getChildren().add(buildLogoArea());
        getChildren().add(buildStatusBar());

        String lastGroup = "";
        for (String[] item : NAV_ITEMS) {
            String label = item[0], icon = item[1], group = item[2];
            if (!group.equals(lastGroup)) {
                getChildren().add(buildGroupHeader(group));
                lastGroup = group;
            }
            StackPane row = buildNavRow(icon, label, label.equals(selectedLabel));
            rowMap.put(label, row);

            Region overlay = (Region) row.getChildren().get(0);
            row.setOnMouseClicked(e -> {
                if (!"Export CSV".equals(label)) setSelected(label);
                onSelect.accept(label);
            });
            row.setOnMouseEntered(e -> {
                if (!label.equals(selectedLabel)) {
                    FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
                    ft.setToValue(1.0);
                    ft.play();
                }
            });
            row.setOnMouseExited(e -> {
                if (!label.equals(selectedLabel)) {
                    FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
                    ft.setToValue(0.0);
                    ft.play();
                }
            });
            getChildren().add(row);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
        getChildren().add(buildBottomDecoration());
    }

    // ── Logo area ─────────────────────────────────────────────────────────────

    private VBox buildLogoArea() {
        Label icon = new Label("🌾");
        icon.setFont(Font.font("Segoe UI", 28));

        Label appName = new Label("AgriTwin-ZM");
        appName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        appName.setTextFill(Color.web(TEXT_PRI));

        Label subtitle = new Label("Digital Twin");
        subtitle.setFont(Font.font("Segoe UI", 11));
        subtitle.setTextFill(Color.web(TEXT_MUTED));

        VBox logo = new VBox(4, icon, appName, subtitle);
        logo.setPadding(new Insets(24, 20, 14, 20));
        logo.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.20), CornerRadii.EMPTY, Insets.EMPTY)));
        return logo;
    }

    // ── Status indicator ──────────────────────────────────────────────────────

    private HBox buildStatusBar() {
        statusDot = new Circle(5);
        statusDot.setFill(Color.web(SUCCESS));

        statusText = new Label("Ready");
        statusText.setFont(Font.font("Segoe UI", 10));
        statusText.setTextFill(Color.web(TEXT_MUTED));

        HBox bar = new HBox(7, statusDot, statusText);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(7, 20, 9, 20));
        bar.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.12), CornerRadii.EMPTY, Insets.EMPTY)));
        return bar;
    }

    // ── Group header ──────────────────────────────────────────────────────────

    private Label buildGroupHeader(String groupName) {
        Label lbl = new Label(groupName);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web(TEXT_MUTED));
        lbl.setPadding(new Insets(16, 20, 5, 20));
        return lbl;
    }

    // ── Nav row ───────────────────────────────────────────────────────────────

    private StackPane buildNavRow(String icon, String label, boolean active) {
        Region hoverOverlay = new Region();
        hoverOverlay.setOpacity(0.0);
        hoverOverlay.setBackground(new Background(new BackgroundFill(
                Color.web(SIDEBAR_ACTIVE), CornerRadii.EMPTY, Insets.EMPTY)));
        hoverOverlay.setMaxWidth(Double.MAX_VALUE);
        hoverOverlay.setMaxHeight(Double.MAX_VALUE);

        Region accentBar = new Region();
        accentBar.setPrefWidth(3);
        accentBar.setMinWidth(3);
        accentBar.setBackground(new Background(new BackgroundFill(
                active ? Color.web(ACCENT) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font("Segoe UI", 15));
        iconLbl.setTextFill(active ? Color.web(ACCENT) : Color.color(1, 1, 1, 0.65));
        iconLbl.setMinWidth(26);

        Label textLbl = new Label(label);
        textLbl.setFont(Font.font("Segoe UI",
                active ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        textLbl.setTextFill(active ? Color.web(TEXT_PRI) : Color.web(TEXT_SEC));
        HBox.setHgrow(textLbl, Priority.ALWAYS);

        HBox content = new HBox(10, accentBar, iconLbl, textLbl);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(11, 14, 11, 0));

        if ("Maize".equals(label)) {
            Label liveBadge = new Label("Live");
            liveBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            liveBadge.setTextFill(Color.WHITE);
            liveBadge.setBackground(new Background(new BackgroundFill(
                    Color.web(SUCCESS), new CornerRadii(8), Insets.EMPTY)));
            liveBadge.setPadding(new Insets(2, 5, 2, 5));
            content.getChildren().add(liveBadge);
        } else if ("FRA Reserves".equals(label)) {
            fraAlertBadge = new Label("!");
            fraAlertBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            fraAlertBadge.setTextFill(Color.WHITE);
            fraAlertBadge.setBackground(new Background(new BackgroundFill(
                    Color.web(DANGER), new CornerRadii(8), Insets.EMPTY)));
            fraAlertBadge.setPadding(new Insets(2, 5, 2, 5));
            fraAlertBadge.setVisible(false);
            content.getChildren().add(fraAlertBadge);
        }

        StackPane row = new StackPane(hoverOverlay, content);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setBackground(new Background(new BackgroundFill(
                active ? Color.web(SIDEBAR_ACTIVE) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));
        row.setCursor(javafx.scene.Cursor.HAND);
        return row;
    }

    // ── Bottom decoration ────────────────────────────────────────────────────

    private VBox buildBottomDecoration() {
        Label version = new Label("v1.0  •  Univ. of Lusaka");
        version.setFont(Font.font("Segoe UI", 9));
        version.setTextFill(Color.color(1, 1, 1, 0.35));

        Label tagline = new Label("Zambian Supply Chains");
        tagline.setFont(Font.font("Segoe UI", 9));
        tagline.setTextFill(Color.color(1, 1, 1, 0.25));

        VBox bottom = new VBox(4, version, tagline);
        bottom.setPadding(new Insets(16, 20, 20, 20));
        return bottom;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setSelected(String label) {
        if (!rowMap.containsKey(label)) return;
        if (!selectedLabel.equals(label)) refreshRow(selectedLabel, false);
        selectedLabel = label;
        refreshRow(label, true);
    }

    /** Backward-compatible int overload used by legacy selectPage(int) calls. */
    public void setSelected(int index) {
        String[] mapping = {
            "Dashboard", "Simulate", "Storage", "FRA Reserves",
            "Season Report", "About", "Analytics"
        };
        if (index >= 0 && index < mapping.length && mapping[index] != null) {
            setSelected(mapping[index]);
        }
    }

    public void setStatusReady() {
        if (statusDot  != null) statusDot.setFill(Color.web(SUCCESS));
        if (statusText != null) statusText.setText("Ready");
    }

    public void setStatusRunning() {
        if (statusDot  != null) statusDot.setFill(Color.web(AMBER));
        if (statusText != null) statusText.setText("Running…");
    }

    public void setStatusComplete(String time) {
        if (statusDot  != null) statusDot.setFill(Color.web(ACCENT));
        if (statusText != null) statusText.setText("Done  " + time);
    }

    public void setFraAlert(boolean active) {
        if (fraAlertBadge != null) fraAlertBadge.setVisible(active);
    }

    // ── Row refresh ───────────────────────────────────────────────────────────

    private void refreshRow(String label, boolean active) {
        StackPane row = rowMap.get(label);
        if (row == null) return;
        HBox content    = (HBox)   row.getChildren().get(1);
        Region accentBar = (Region) content.getChildren().get(0);
        Label iconLbl    = (Label)  content.getChildren().get(1);
        Label textLbl    = (Label)  content.getChildren().get(2);

        accentBar.setBackground(new Background(new BackgroundFill(
                active ? Color.web(ACCENT) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));
        iconLbl.setTextFill(active ? Color.web(ACCENT) : Color.color(1, 1, 1, 0.65));
        textLbl.setFont(Font.font("Segoe UI",
                active ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        textLbl.setTextFill(active ? Color.web(TEXT_PRI) : Color.web(TEXT_SEC));
        row.setBackground(new Background(new BackgroundFill(
                active ? Color.web(SIDEBAR_ACTIVE) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));

        if (active) {
            Region overlay = (Region) row.getChildren().get(0);
            overlay.setOpacity(0.0);
        }
    }
}
