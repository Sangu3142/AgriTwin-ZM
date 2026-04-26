package agritwin;

import agritwin.ui.ConsoleUI;

import java.lang.reflect.Method;

/**
 * MODULE: Application Entry Point
 * PURPOSE: Boots AgriTwin-ZM. Defaults to the console front-end so the
 *          program is always runnable with a vanilla JDK. If the user passes
 *          --gui as an argument, JavaFX's GuiApp is launched via reflection,
 *          keeping the JavaFX dependency optional rather than mandatory.
 * OOP CONCEPTS USED: Composition (delegates to ConsoleUI / GuiApp); Reflection
 *                    used purely to keep the GUI an optional dependency.
 * DATA STRUCTURES: String[] (command-line args).
 * ALGORITHMS: Simple argument scan.
 *
 * USAGE:
 *   java -cp out agritwin.Main          (console)
 *   java -cp out agritwin.Main --gui    (JavaFX, requires javafx libs)
 */
public final class Main {

    private Main() {} // utility class -- no instances

    public static void main(String[] args) {
        if (hasFlag(args, "--gui")) {
            launchGui(args);
        } else {
            new ConsoleUI().start();
        }
    }

    /** Returns true if the given flag is present in args. */
    private static boolean hasFlag(String[] args, String flag) {
        if (args == null) return false;
        for (String a : args) {
            if (flag.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    /**
     * Loads agritwin.ui.GuiApp by name and invokes its launchApp(String[])
     * static method. Using reflection means agritwin.Main itself never
     * imports a JavaFX class -- so the program still compiles and runs in
     * pure-console environments where javafx is unavailable.
     */
    private static void launchGui(String[] args) {
        try {
            Class<?> guiClass = Class.forName("agritwin.ui.GuiApp");
            Method launchApp = guiClass.getMethod("launchApp", String[].class);
            launchApp.invoke(null, (Object) args);
        } catch (ClassNotFoundException ex) {
            System.err.println("GUI requested but agritwin.ui.GuiApp is not on the classpath.");
            System.err.println("Falling back to ConsoleUI...");
            new ConsoleUI().start();
        } catch (NoClassDefFoundError ex) {
            System.err.println("JavaFX runtime not found on the module path / classpath: "
                    + ex.getMessage());
            System.err.println("Falling back to ConsoleUI...");
            new ConsoleUI().start();
        } catch (Exception ex) {
            System.err.println("Unable to start GUI: " + ex.getMessage());
            ex.printStackTrace();
            new ConsoleUI().start();
        }
    }
}
