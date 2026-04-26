package agritwin.service;

import agritwin.model.SimulationRecord;
import agritwin.util.FileHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MODULE: Report Generator
 * PURPOSE: Formats SimulationRecord objects into human-readable reports for
 *          the console and exports a full simulation history to a .txt file.
 *          Uses FileHandler for the CSV-style structured persistence path.
 * OOP CONCEPTS USED: Encapsulation; Composition (delegates to FileHandler).
 * DATA STRUCTURES: List&lt;SimulationRecord&gt;.
 * ALGORITHMS: Iteration over records; String formatting.
 */
public class ReportGenerator {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Builds (and prints) a single monthly report for one record. */
    public String generateMonthlyReport(SimulationRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format("  AgriTwin-ZM v1.0 -- Month %d Report (%s)\n",
                record.getMonthNumber(), record.getCommodityName()));
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format("  Production            : %.2f tons%n", record.getProduction()));
        sb.append(String.format("  Demand                : %.2f tons%n", record.getDemand()));
        sb.append(String.format("  Surplus / Shortage    : %.2f tons %s%n",
                record.getSurplusOrShortage(),
                record.getSurplusOrShortage() >= 0 ? "(surplus)" : "(shortage)"));
        sb.append(String.format("  Waste / Spoilage      : %.2f tons%n", record.getWaste()));
        sb.append(String.format("  Revenue               : ZMW %,.2f%n", record.getRevenue()));
        sb.append(String.format("  Food Security Indicator: %.2f%%%n", record.getFoodSecurityIndicator()));
        sb.append(String.format("  Policy Alerts         : %s%n",
                record.getPolicyAlerts() == null || record.getPolicyAlerts().isEmpty()
                        ? "(none)" : record.getPolicyAlerts()));
        sb.append("------------------------------------------------------------");
        String out = sb.toString();
        System.out.println(out);
        return out;
    }

    /**
     * Writes a full readable report (one block per month) to a .txt file.
     * Uses FileWriter directly per spec; structured CSV-style export goes
     * through FileHandler.saveSimulation.
     */
    public void exportToFile(List<SimulationRecord> records, String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("============================================================");
            pw.println(" AgriTwin-ZM v1.0 -- Simulation Report");
            pw.println(" Generated: " + LocalDateTime.now().format(TS_FMT));
            pw.println(" Records  : " + records.size());
            pw.println("============================================================");
            pw.println();
            for (SimulationRecord r : records) {
                pw.println("Month " + r.getMonthNumber() + " | " + r.getCommodityName());
                pw.println("------------------------------------------------------------");
                pw.printf("  Production            : %.2f tons%n", r.getProduction());
                pw.printf("  Demand                : %.2f tons%n", r.getDemand());
                pw.printf("  Surplus / Shortage    : %.2f tons%n", r.getSurplusOrShortage());
                pw.printf("  Waste / Spoilage      : %.2f tons%n", r.getWaste());
                pw.printf("  Revenue               : ZMW %,.2f%n", r.getRevenue());
                pw.printf("  Food Security Indicator: %.2f%%%n", r.getFoodSecurityIndicator());
                pw.println("  Policy Alerts         : " +
                        (r.getPolicyAlerts() == null || r.getPolicyAlerts().isEmpty()
                                ? "(none)" : r.getPolicyAlerts()));
                pw.println();
            }
            pw.println("============================================================");
            pw.println(" END OF REPORT");
            pw.println("============================================================");
        }
    }

    /**
     * Convenience pass-through to FileHandler for the structured CSV-style
     * export the assignment also asks for.
     */
    public void exportStructured(List<SimulationRecord> records, String path) throws IOException {
        new FileHandler().saveSimulation(records, path);
    }
}
