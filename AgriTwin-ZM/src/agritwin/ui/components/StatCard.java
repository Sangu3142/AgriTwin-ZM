package agritwin.ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * MODULE: Reusable Stat Card Component (Vision UI dark theme)
 * PURPOSE: Dark glassmorphism-style metric card with animated count-up value,
 *          teal progress bar (DoubleProperty-driven), drop shadow, and hover-lift.
 * OOP CONCEPTS: Encapsulation; Inheritance (extends StackPane); Property binding.
 */
public class StatCard extends StackPane {

    private static final String CARD_BG     = "#111C44";
    private static final String CARD_BORDER = "#1A2A6C";
    private static final String ACCENT_TEAL = "#00CFB4";
    private static final String PROGRESS_BG = "#1E3A5F";
    private static final String TEXT_SEC    = "#A0AEC0";
    private static final String TEXT_MUTED  = "#718096";

    private final Label  valueLabel    = new Label();
    private final Label  titleLabel    = new Label();
    private final Region progressFill  = new Region();
    private final HBox   progressTrack = new HBox();
    private final DoubleProperty progressFraction = new SimpleDoubleProperty(0.0);

    /**
     * @param value         display value string (e.g. "110,000t", "87.5%")
     * @param title         sub-label text
     * @param valueColorHex hex colour for the large value label
     * @param ignored       legacy param (was whiteText flag) — not used
     * @param progress      0.0–1.0 progress fraction
     */
    @SuppressWarnings("this-escape")
    public StatCard(String value, String title, String valueColorHex,
                    boolean ignored, double progress) {
        build(value, title, valueColorHex);
        animateProgress(progress);
        addHoverLift(this);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    private void build(String value, String title, String vcHex) {
        setMinWidth(140);
        setPrefWidth(200);

        setBackground(new Background(new BackgroundFill(
                Color.web(CARD_BG), new CornerRadii(16), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                Color.web(CARD_BORDER), BorderStrokeStyle.SOLID,
                new CornerRadii(16), new BorderWidths(1))));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(12);
        shadow.setOffsetY(4);
        shadow.setColor(Color.color(0, 0, 0, 0.40));
        setEffect(shadow);

        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(vcHex));
        valueLabel.setText(value);

        titleLabel.setFont(Font.font("Segoe UI", 12));
        titleLabel.setTextFill(Color.web(TEXT_SEC));
        titleLabel.setWrapText(true);
        titleLabel.setText(title);

        progressTrack.setPrefHeight(5);
        progressTrack.setMaxHeight(5);
        progressTrack.setBackground(new Background(new BackgroundFill(
                Color.web(PROGRESS_BG), new CornerRadii(3), Insets.EMPTY)));

        progressFill.setPrefHeight(5);
        progressFill.setMaxHeight(5);
        progressFill.setBackground(new Background(new BackgroundFill(
                Color.web(ACCENT_TEAL), new CornerRadii(3), Insets.EMPTY)));
        progressFill.prefWidthProperty().bind(
                progressTrack.widthProperty().multiply(progressFraction));
        progressTrack.getChildren().add(progressFill);

        Label caption = new Label("Finish 100%");
        caption.setFont(Font.font("Segoe UI", 9));
        caption.setTextFill(Color.web(TEXT_MUTED));

        VBox content = new VBox(8, valueLabel, titleLabel, progressTrack, caption);
        content.setPadding(new Insets(20));

        setAlignment(Pos.TOP_LEFT);
        getChildren().add(content);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Updates the card content with a count-up animation and re-triggers the progress bar. */
    public void update(String value, String title, double progress) {
        titleLabel.setText(title);
        animateCountUp(value);
        animateProgress(progress);
    }

    /** Changes the value label colour (e.g. TEAL for surplus, CORAL for shortage). */
    public void setValueColor(String hex) {
        valueLabel.setTextFill(Color.web(hex));
    }

    /**
     * Adds a small badge label at the given corner.
     * Used for "✓ Target met!" overlays on the Food-Security card.
     */
    public void addBadge(String text, Color textColor, Pos alignment, Insets margin) {
        Label badge = new Label(text);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        badge.setTextFill(textColor);
        badge.setBackground(new Background(new BackgroundFill(
                Color.web("#48BB78"), new CornerRadii(8), Insets.EMPTY)));
        badge.setPadding(new Insets(3, 7, 3, 7));
        StackPane.setAlignment(badge, alignment);
        StackPane.setMargin(badge, margin);
        getChildren().add(badge);
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private void animateCountUp(String rawVal) {
        String stripped = rawVal.replaceAll("[^0-9.]", "");
        if (stripped.isEmpty() || stripped.equals(".")) {
            valueLabel.setText(rawVal);
            return;
        }
        try {
            double target = Double.parseDouble(stripped);

            // locate first and last digit to split prefix / suffix
            int firstD = 0;
            for (int i = 0; i < rawVal.length(); i++) {
                if (Character.isDigit(rawVal.charAt(i))) { firstD = i; break; }
            }
            int lastD = rawVal.length() - 1;
            for (int i = rawVal.length() - 1; i >= 0; i--) {
                if (Character.isDigit(rawVal.charAt(i))) { lastD = i; break; }
            }
            String pre = rawVal.substring(0, firstD);
            String suf = rawVal.substring(lastD + 1);

            final int frames = 30;
            Timeline tl = new Timeline();
            for (int i = 0; i <= frames; i++) {
                final int fi = i;
                tl.getKeyFrames().add(new KeyFrame(Duration.millis(600.0 * i / frames), e -> {
                    double v = target * fi / frames;
                    String fmt = target >= 1_000
                            ? String.format("%,.0f", v)
                            : String.format("%.1f", v);
                    valueLabel.setText(pre + fmt + suf);
                }));
            }
            // final frame snaps to exact original string
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(620), e -> valueLabel.setText(rawVal)));
            tl.play();
        } catch (NumberFormatException ex) {
            valueLabel.setText(rawVal);
        }
    }

    private void animateProgress(double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(progressFraction, 0.0)),
                new KeyFrame(Duration.millis(800), new KeyValue(progressFraction, clamped)));
        tl.play();
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    /**
     * Attaches a smooth hover-lift (TranslateY 0 → -3 → 0) to any node.
     * Called from StatCard constructor and reused by DashboardController
     * for other .card containers.
     */
    public static void addHoverLift(Node node) {
        TranslateTransition enter = new TranslateTransition(Duration.millis(150), node);
        TranslateTransition exit  = new TranslateTransition(Duration.millis(150), node);
        node.setOnMouseEntered(e -> {
            enter.stop(); exit.stop();
            enter.setToY(-3);
            enter.play();
        });
        node.setOnMouseExited(e -> {
            enter.stop(); exit.stop();
            exit.setToY(0);
            exit.play();
        });
    }
}
