package agritwin.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MODULE: JavaFX GUI Application entry point (optional)
 * PURPOSE: Boots the JavaFX runtime and hands off scene construction to
 * DashboardController. Loaded by reflection from Main so that the
 * console build never needs javafx on its classpath.
 * OOP CONCEPTS USED: Inheritance (extends Application); Composition (delegates
 * scene building to DashboardController rather than doing it
 * inline, keeping this class to its single responsibility).
 */
public class GuiApp extends Application {

    /** Reflection entry point called by Main when --gui flag is present. */
    public static void launchApp(String[] args) {
        Application.launch(GuiApp.class, args);
    }

    @Override
    public void start(Stage stage) {
        DashboardController controller = new DashboardController();
        Scene scene = controller.buildScene();

        stage.setTitle("AgriTwin-ZM — Digital Twin Dashboard");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }
}
