package agritwin.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * MODULE: Policy Alert Panel (Vision UI dark theme)
 * PURPOSE: Right-column panel showing FRA policy status. Gold border signals
 *          potential alerts; body text is updatable from controller callbacks.
 * OOP CONCEPTS: Encapsulation; Composition; Callback via Runnable.
 */
public class AlertPanel extends VBox {

    private static final String CARD_BG  = "#111C44";
    private static final String GOLD     = "#ECC94B";
    private static final String GREEN    = "#48BB78";
    private static final String TEXT_SEC = "#A0AEC0";

    private final Label bodyLabel = new Label();

    @SuppressWarnings("this-escape")
    public AlertPanel(String bodyText, Runnable onViewReport) {
        setSpacing(12);
        setPadding(new Insets(20));

        setBackground(new Background(new BackgroundFill(
                Color.web(CARD_BG), new CornerRadii(16), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                Color.web(GOLD), BorderStrokeStyle.SOLID,
                new CornerRadii(16), new BorderWidths(2))));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(12);
        shadow.setOffsetY(4);
        shadow.setColor(Color.color(0, 0, 0, 0.35));
        setEffect(shadow);

        // Header: gold dot + title
        Label dot = new Label("●");
        dot.setTextFill(Color.web(GOLD));
        dot.setFont(Font.font("Segoe UI", 13));

        Label title = new Label("FRA Policy Status");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        title.setTextFill(Color.WHITE);

        HBox header = new HBox(8, dot, title);
        header.setAlignment(Pos.CENTER_LEFT);

        bodyLabel.setText(bodyText);
        bodyLabel.setFont(Font.font("Segoe UI", 13));
        bodyLabel.setTextFill(Color.web(TEXT_SEC));
        bodyLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button("View Report  →");
        viewBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        viewBtn.setTextFill(Color.WHITE);
        viewBtn.setBackground(new Background(new BackgroundFill(
                Color.web(GREEN), new CornerRadii(10), Insets.EMPTY)));
        viewBtn.setPadding(new Insets(8, 16, 8, 16));
        viewBtn.setCursor(javafx.scene.Cursor.HAND);
        viewBtn.setOnAction(e -> onViewReport.run());

        HBox btnRow = new HBox(viewBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, bodyLabel, spacer, btnRow);
    }

    /** Replaces the alert body text (called after each simulation or live-data refresh). */
    public void setBodyText(String text) {
        bodyLabel.setText(text);
    }
}
