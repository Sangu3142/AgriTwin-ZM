package agritwin.ui;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.Tomato;
import agritwin.service.PolicyEngine;
import agritwin.service.ReportGenerator;
import agritwin.service.StorageLossSimulator;
import agritwin.service.SupplyDemandEngine;
import agritwin.util.FileHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * MODULE: Console User Interface
 * PURPOSE: Menu-driven entry point that lets the user run maize / tomato
 *          simulations, view monthly reports, save a simulation to disk and
 *          reload a previously saved one. All business logic is delegated
 *          to the service classes — this file only handles I/O and flow.
 * OOP CONCEPTS USED: Composition (holds service instances); Encapsulation;
 *                    Polymorphism via service-class APIs.
 * DATA STRUCTURES: ArrayList&lt;SimulationRecord&gt; for the in-memory history.
 * ALGORITHMS: Sequential month loop driving compounded loss + economics.
 */
public class ConsoleUI {

    private final Scanner scanner = new Scanner(System.in);
    private final SupplyDemandEngine supply = new SupplyDemandEngine();
    private final StorageLossSimulator storage = new StorageLossSimulator();
    private final PolicyEngine policy = new PolicyEngine();
    private final ReportGenerator reporter = new ReportGenerator();
    private final FileHandler files = new FileHandler();

    /** History of every record produced in this session. */
    private final List<SimulationRecord> history = new ArrayList<>();

    /** Entry point used by Main. */
    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Choose an option: ").trim();
            switch (choice) {
                case "1": runMaizeSimulation(); break;
                case "2": runTomatoSimulation(); break;
                case "3": viewReports(); break;
                case "4": loadPreviousSimulation(); break;
                case "5": saveCurrentSimulation(); break;
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

    private void printBanner() {
        System.out.println("============================================================");
        System.out.println("  AgriTwin-ZM v1.0  --  Digital Twin for Zambian Agriculture");
        System.out.println("  Maize & Tomato Supply Chain Simulation");
        System.out.println("============================================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("---------------- MAIN MENU -----------------");
        System.out.println("  1) Run Maize Simulation");
        System.out.println("  2) Run Tomato Simulation");
        System.out.println("  3) View Reports (this session)");
        System.out.println("  4) Load Previous Simulation (from file)");
        System.out.println("  5) Save Current Simulation (to file)");
        System.out.println("  6) Exit");
        System.out.println("--------------------------------------------");
    }

    // ---------------------------------------------------------------------
    // Simulation flows
    // ---------------------------------------------------------------------

    private void runMaizeSimulation() {
        System.out.println("\n>> Maize Simulation Setup");
        double production = readDouble("Enter production volume (tons): ", 10000);
        double demand     = readDouble("Enter projected demand (tons): ", 8000);
        double storageCap = readDouble("Enter storage capacity (tons): ", 12000);
        double moisture   = readDouble("Enter moisture level (%): ", 12.5);
        double price      = readDouble("Enter farm-gate price (ZMW/ton): ", 7000); // ~ZMW 350/50kg
        double monthlyLoss= readDouble("Enter monthly loss rate (%): ", 3.0);
        int months        = readInt   ("Number of months to simulate: ", 6);

        Maize maize = new Maize(production, storageCap, moisture, price, monthlyLoss);
        System.out.println("\nInitial: " + maize);

        double currentStock = production;
        double currentPrice = price;
        double monthlyDemand = demand / months; // spread demand over horizon

        for (int month = 1; month <= months; month++) {
            // Apply this month's storage loss to remaining stock
            double remainingAfterLoss = storage.simulateMaizeLoss(
                    new Maize(currentStock, storageCap, moisture, currentPrice, monthlyLoss),
                    1);
            double waste = currentStock - remainingAfterLoss;
            currentStock = remainingAfterLoss;

            // Sell up to monthly demand from current stock
            double sold = Math.min(currentStock, monthlyDemand);
            double revenue = supply.calculateRevenue(sold, currentPrice);
            currentStock -= sold;

            double surplus = supply.calculateSurplus(remainingAfterLoss, monthlyDemand);
            currentPrice  = supply.adjustPrice(currentPrice, surplus, remainingAfterLoss);
            double fsi    = supply.calculateFoodSecurityIndicator(remainingAfterLoss, monthlyDemand);
            String alerts = policy.evaluateAll(currentStock, surplus, remainingAfterLoss,
                                               month, -1, -1);

            SimulationRecord rec = new SimulationRecord(
                    month, "Maize", remainingAfterLoss, monthlyDemand,
                    surplus, waste, revenue, fsi, alerts);
            history.add(rec);
            reporter.generateMonthlyReport(rec);
        }

        offerExport();
    }

    private void runTomatoSimulation() {
        System.out.println("\n>> Tomato Simulation Setup");
        double production = readDouble("Enter production volume (tons): ", 500);
        double demand     = readDouble("Enter projected demand (tons): ", 480);
        int    shelfLife  = readInt   ("Enter shelf life (days): ", 14);
        String storageT   = readChoice("Storage type [cold/open]: ", new String[]{"cold","open"}, "open");
        double spoilage   = readDouble("Enter base spoilage rate (%/day): ", 5.0);
        double crateP     = readDouble("Enter market price (ZMW/crate): ", 120.0);
        int    days       = readInt   ("Days before sale: ", 7);

        // ZMW/ton derived from ZMW/crate at ~25kg per crate (40 crates per ton)
        double pricePerTon = crateP * 40.0;

        Tomato tomato = new Tomato(production, shelfLife, storageT, spoilage, crateP);
        System.out.println("\nInitial: " + tomato);

        double waste = storage.simulateTomatoSpoilage(tomato, days);
        double saleable = Math.max(0.0, production - waste);

        double sold = Math.min(saleable, demand);
        double revenue = supply.calculateRevenue(sold, pricePerTon);
        double surplus = supply.calculateSurplus(saleable, demand);
        double newPrice = supply.adjustPrice(pricePerTon, surplus, Math.max(saleable,1));
        double fsi = supply.calculateFoodSecurityIndicator(saleable, demand);
        String alerts = policy.evaluateAll(/*maize stock irrelevant*/Double.POSITIVE_INFINITY,
                                           surplus, saleable, -1, days, shelfLife);

        SimulationRecord rec = new SimulationRecord(
                1, "Tomato", saleable, demand, surplus, waste, revenue, fsi, alerts);
        history.add(rec);
        System.out.printf("Adjusted ZMW/ton after market response: %.2f%n", newPrice);
        reporter.generateMonthlyReport(rec);

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
