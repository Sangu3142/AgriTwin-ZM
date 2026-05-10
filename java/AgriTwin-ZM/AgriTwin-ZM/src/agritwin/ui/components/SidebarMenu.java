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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * MODULE: Sidebar Navigation Menu (Vision UI dark theme)
 * PURPOSE: Dark-navy left panel. Active item highlighted with a cyan left-accent
 *          bar and SIDEBAR_ACTIVE background. Non-active items show a
 *          semi-transparent overlay on hover via FadeTransition.
 * OOP CONCEPTS: Encapsulation; Composition; Callback via Consumer<String>.
 */
public class SidebarMenu extends VBox {

    private static final String SIDEBAR_BG     = "#0B1437";
    private static final String SIDEBAR_ACTIVE = "#1A2A6C";
    private static final String ACCENT_CYAN    = "#00B5D8";
    private static final String TEXT_SEC       = "#A0AEC0";
    private static final String TEXT_MUTED     = "#718096";

    private static final String[] LABELS = {
        "Dashboard", "Simulation", "Storage", "Policy Alerts", "Reports", "Information"
    };
    private static final String[] ICONS = {
        "🏠", "▶", "🗄", "🔔", "📋", "ℹ"
    };

    // Each row is a StackPane so a semi-transparent hover overlay can be layered.
    private final StackPane[] rows = new StackPane[LABELS.length];
    private int selectedIndex = 0;

    @SuppressWarnings("this-escape")
    public SidebarMenu(Consumer<String> onSelect) {
        setPrefWidth(200);
        setMinWidth(180);
        setSpacing(0);
        setBackground(new Background(new BackgroundFill(
                Color.web(SIDEBAR_BG), CornerRadii.EMPTY, Insets.EMPTY)));

        getChildren().add(buildLogoArea());

        for (int i = 0; i < LABELS.length; i++) {
            final int idx = i;
            StackPane row = buildNavRow(ICONS[i], LABELS[i], i == selectedIndex);
            rows[i] = row;

            // Hover overlay: index-0 child of StackPane is the overlay Region
            Region overlay = (Region) row.getChildren().get(0);

            row.setOnMouseClicked(e -> {
                setSelected(idx);
                onSelect.accept(LABELS[idx]);
            });
            row.setOnMouseEntered(e -> {
                if (idx != selectedIndex) {
                    FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
                    ft.setToValue(1.0);
                    ft.play();
                }
            });
            row.setOnMouseExited(e -> {
                if (idx != selectedIndex) {
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
        Label icon = new Label("🌾"); // 🌾
        icon.setFont(Font.font("Segoe UI", 28));

        Label appName = new Label("AgriTwin-ZM");
        appName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        appName.setTextFill(Color.WHITE);

        Label subtitle = new Label("Digital Twin");
        subtitle.setFont(Font.font("Segoe UI", 11));
        subtitle.setTextFill(Color.web(TEXT_MUTED));

        VBox logo = new VBox(4, icon, appName, subtitle);
        logo.setPadding(new Insets(24, 20, 20, 20));
        logo.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.20), CornerRadii.EMPTY, Insets.EMPTY)));
        return logo;
    }

    // ── Nav row ───────────────────────────────────────────────────────────────

    private StackPane buildNavRow(String icon, String label, boolean active) {
        // Layer 0: hover overlay (invisible by default)
        Region hoverOverlay = new Region();
        hoverOverlay.setOpacity(0.0);
        hoverOverlay.setBackground(new Background(new BackgroundFill(
                Color.web(SIDEBAR_ACTIVE), CornerRadii.EMPTY, Insets.EMPTY)));
        hoverOverlay.setMaxWidth(Double.MAX_VALUE);
        hoverOverlay.setMaxHeight(Double.MAX_VALUE);

        // Layer 1: cyan accent bar
        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setMinWidth(4);
        accentBar.setBackground(new Background(new BackgroundFill(
                active ? Color.web(ACCENT_CYAN) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font("Segoe UI", 17));
        iconLbl.setTextFill(active ? Color.web(ACCENT_CYAN) : Color.color(1, 1, 1, 0.65));
        iconLbl.setMinWidth(28);

        Label textLbl = new Label(label);
        textLbl.setFont(Font.font("Segoe UI",
                active ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        textLbl.setTextFill(active ? Color.WHITE : Color.web(TEXT_SEC));

        HBox content = new HBox(12, accentBar, iconLbl, textLbl);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(14, 16, 14, 0));

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

    public void setSelected(int index) {
        if (index < 0 || index >= rows.length) return;
        refreshRow(selectedIndex, false);
        selectedIndex = index;
        refreshRow(selectedIndex, true);
    }

    private void refreshRow(int idx, boolean active) {
        StackPane row = rows[idx];
        // children: [hoverOverlay, contentHBox]
        HBox content    = (HBox) row.getChildren().get(1);
        Region accentBar = (Region) content.getChildren().get(0);
        Label iconLbl    = (Label)  content.getChildren().get(1);
        Label textLbl    = (Label)  content.getChildren().get(2);

        accentBar.setBackground(new Background(new BackgroundFill(
                active ? Color.web(ACCENT_CYAN) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));
        iconLbl.setTextFill(active ? Color.web(ACCENT_CYAN) : Color.color(1, 1, 1, 0.65));
        textLbl.setFont(Font.font("Segoe UI",
                active ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        textLbl.setTextFill(active ? Color.WHITE : Color.web(TEXT_SEC));
        row.setBackground(new Background(new BackgroundFill(
                active ? Color.web(SIDEBAR_ACTIVE) : Color.TRANSPARENT,
                CornerRadii.EMPTY, Insets.EMPTY)));

        // ensure hover overlay is reset when an item is de-selected
        if (active) {
            Region overlay = (Region) row.getChildren().get(0);
            overlay.setOpacity(0.0);
        }
    }
}
