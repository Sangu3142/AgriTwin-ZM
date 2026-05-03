package agritwin.ui;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.StorageType;
import agritwin.model.Tomato;
import agritwin.service.ReportGenerator;
import agritwin.service.SimulationRunner;
import agritwin.util.FileHandler;
import agritwin.util.RecordStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * MODULE: Console User Interface
 * PURPOSE: Menu-driven entry point that lets the user run maize / tomato
 *          simulations, view monthly reports, save a simulation to disk,
 *          reload a previously saved one, and export a season summary as CSV.
 *          All simulation logic is delegated to SimulationRunner; this file
 *          handles only I/O and flow control.
 * OOP CONCEPTS USED: Composition (holds SimulationRunner, ReportGenerator and
 *                    RecordStore instances); Encapsulation; Polymorphism — files
 *                    is declared as RecordStore (interface), not the concrete
 *                    FileHandler, following Dependency Inversion; try-with-
 *                    resources guarantees the Scanner is always closed.
 * DATA STRUCTURES: ArrayList<SimulationRecord> for the in-memory history.
 * ALGORITHMS: Sequential menu loop; delegates month-by-month simulation state
 *             machine to SimulationRunner (no loop logic here).
 */
public class ConsoleUI {

    private Scanner scanner;
    private final SimulationRunner runner   = new SimulationRunner();
    private final ReportGenerator reporter  = new ReportGenerator();
    private final RecordStore files         = new FileHandler();

    /** History of every record produced in this session. */
    private final List<SimulationRecord> history = new ArrayList<>();

    /**
     * Entry point used by Main. The Scanner is declared inside try-with-resources
     * so it is closed automatically (even on exception) the moment start() returns.
     */
    public void start() {
        printBanner();
        try (Scanner sc = new Scanner(System.in)) {
            this.scanner = sc;
            boolean running = true;
            while (running) {
                printMenu();
                String choice = readLine("Choose an option: ").trim();
                switch (choice) {
                    case "1": runMaizeSimulation();        break;
                    case "2": runTomatoSimulation();       break;
                    case "3": viewReports();               break;
                    case "4": loadPreviousSimulation();    break;
                    case "5": saveCurrentSimulation();     break;
                    case "7": exportCsv();                 break;
                    case "0":
                    case "6":
                        running = false;
                        System.out.println("Goodbye -- thank you for using AgriTwin-ZM.");
                        break;
                    default:
                        System.out.println("Unknown option, please try again.");
                }
            }
        }
    }

    private void printBanner() {
        System.out.println("============================================================");
        System.out.println("  AgriTwin-ZM v1.0  --  Digital Twin for Zambian Agriculture");
        System.out.println("  Maize & Tomato Supply Chain Simulation");
        System.out.println("============================================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("---------------- MAIN MENU (options 0-7) -------------------");
        System.out.println("  1) Run Maize Simulation");
        System.out.println("  2) Run Tomato Simulation");
        System.out.println("  3) View Reports (this session)");
        System.out.println("  4) Load Previous Simulation (from file)");
        System.out.println("  5) Save Current Simulation (to file)");
        System.out.println("  6) Exit");
        System.out.println("  7) Export season summary (CSV)");
        System.out.println("------------------------------------------------------------");
    }

    // ---------------------------------------------------------------------
    // Simulation flows — collect inputs, delegate logic to SimulationRunner
    // ---------------------------------------------------------------------

    private void runMaizeSimulation() {
        System.out.println("\n>> Maize Simulation Setup");
        double production  = readDouble("Enter production volume (tons): ", 10000);
        double demand      = readDouble("Enter projected demand (tons): ", 8000);
        double storageCap  = readDouble("Enter storage capacity (tons): ", 12000);
        double moisture    = readDouble("Enter moisture level (%): ", 12.5);
        double price       = readDouble("Enter farm-gate price (ZMW/ton): ", 7000);
        double monthlyLoss = readDouble("Enter monthly loss rate (%): ", 3.0);
        int    months      = readInt   ("Number of months to simulate: ", 6);

        Maize maize = new Maize(production, storageCap, moisture, price, monthlyLoss);
        System.out.println("\nInitial: " + maize);

        List<SimulationRecord> records = runner.runMaizeSimulation(maize, demand, months);
        for (SimulationRecord rec : records) {
            history.add(rec);
            reporter.generateMonthlyReport(rec);
        }

        offerExport();
    }

    private void runTomatoSimulation() {
        System.out.println("\n>> Tomato Simulation Setup");
        double production  = readDouble("Enter production volume (tons): ", 500);
        double demand      = readDouble("Enter projected demand (tons): ", 480);
        int    shelfLife   = readInt   ("Enter shelf life (days): ", 14);
        String storageStr  = readChoice("Storage type [cold/open]: ", new String[]{"cold","open"}, "open");
        StorageType storageT = StorageType.valueOf(storageStr.trim().toUpperCase());
        double spoilage    = readDouble("Enter base spoilage rate (%/day): ", 5.0);
        double crateP      = readDouble("Enter market price (ZMW/crate): ", 120.0);
        int    days        = readInt   ("Days before sale: ", 7);

        // ZMW/ton derived from ZMW/crate at ~25 kg per crate (40 crates per ton)
        double pricePerTon = crateP * 40.0;
        Tomato tomato = new Tomato(production, shelfLife, storageT, spoilage, crateP);
        System.out.println("\nInitial: " + tomato);

        List<SimulationRecord> records = runner.runTomatoSimulation(tomato, demand, days, pricePerTon);
        for (SimulationRecord rec : records) {
            history.add(rec);
            reporter.generateMonthlyReport(rec);
        }

        offerExport();
    }

    // ---------------------------------------------------------------------
    // Reports / persistence
    // ---------------------------------------------------------------------

    private void viewReports() {
        if (history.isEmpty()) {
            System.out.println("(no records yet -- run a simulation first)");
            return;
        }
        System.out.println("\nAll records this session:");
        for (SimulationRecord r : history) reporter.generateMonthlyReport(r);
        System.out.println(reporter.generateSeasonSummary(history));
    }

    private void offerExport() {
        String yes = readLine("\nExport these records to a .txt report? (y/n): ").trim().toLowerCase();
        if (yes.startsWith("y")) {
            String path = readLine("Output filename [report.txt]: ").trim();
            if (path.isEmpty()) path = "report.txt";
            try {
                reporter.exportToFile(history, path);
                System.out.println("Report saved to " + path);
            } catch (IOException ex) {
                System.out.println("ERROR writing report: " + ex.getMessage());
            }
        }
    }

    private void saveCurrentSimulation() {
        if (history.isEmpty()) {
            System.out.println("(nothing to save)");
            return;
        }
        String path = readLine("Save path [simulation.dat]: ").trim();
        if (path.isEmpty()) path = "simulation.dat";
        try {
            files.saveSimulation(history, path);
            System.out.println("Saved " + history.size() + " records to " + path);
        } catch (IOException ex) {
            System.out.println("ERROR saving simulation: " + ex.getMessage());
        }
    }

    private void loadPreviousSimulation() {
        String path = readLine("Load path [simulation.dat]: ").trim();
        if (path.isEmpty()) path = "simulation.dat";
        try {
            List<SimulationRecord> loaded = files.loadSimulation(path);
            history.clear();
            history.addAll(loaded);
            System.out.println("Loaded " + loaded.size() + " records from " + path);
            for (SimulationRecord r : loaded) reporter.generateMonthlyReport(r);
        } catch (IOException ex) {
            System.out.println("ERROR loading simulation: " + ex.getMessage());
        }
    }

    private void exportCsv() {
        if (history.isEmpty()) {
            System.out.println("(no records yet -- run a simulation first)");
            return;
        }
        String path = readLine("CSV output filename [season_summary.csv]: ").trim();
        if (path.isEmpty()) path = "season_summary.csv";
        try {
            reporter.exportToCsv(history, path);
            System.out.println("CSV exported to " + path + " (" + history.size() + " records)");
        } catch (IOException ex) {
            System.out.println("ERROR writing CSV: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Small input helpers
    // ---------------------------------------------------------------------

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private double readDouble(String prompt, double def) {
        while (true) {
            String s = readLine(prompt + "[" + def + "] ");
            if (s.trim().isEmpty()) return def;
            try { return Double.parseDouble(s.trim()); }
            catch (NumberFormatException ex) { System.out.println("(please enter a number)"); }
        }
    }

    private int readInt(String prompt, int def) {
        while (true) {
            String s = readLine(prompt + "[" + def + "] ");
            if (s.trim().isEmpty()) return def;
            try { return Integer.parseInt(s.trim()); }
            catch (NumberFormatException ex) { System.out.println("(please enter an integer)"); }
        }
    }

    private String readChoice(String prompt, String[] valid, String def) {
        while (true) {
            String s = readLine(prompt + "[" + def + "] ").trim().toLowerCase();
            if (s.isEmpty()) return def;
            for (String v : valid) if (v.equalsIgnoreCase(s)) return s;
            System.out.println("(please choose one of: " + String.join(", ", valid) + ")");
        }
    }
}
