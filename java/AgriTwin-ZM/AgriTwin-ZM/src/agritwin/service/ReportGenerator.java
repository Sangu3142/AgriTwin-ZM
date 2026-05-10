package agritwin.service;

import agritwin.model.SimulationRecord;
import agritwin.util.FileHandler;
import agritwin.util.RecordStore;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE: Report Generator
 * PURPOSE: Formats SimulationRecord objects into human-readable reports for
 *          the console and exports a full simulation history to a .txt file
 *          or a RFC-4180-compliant .csv file. Uses the RecordStore interface
 *          (not the concrete FileHandler) for the structured CSV-style
 *          persistence path.
 * OOP CONCEPTS USED: Encapsulation; Composition (delegates to RecordStore);
 *                    Polymorphism — the injected RecordStore can be any
 *                    conforming implementation (FileHandler today, SQLite in v2);
 *                    Dependency Injection via constructor — the store is supplied
 *                    by the caller rather than instantiated internally.
 * DATA STRUCTURES: List<SimulationRecord>; Map<String,Integer> for alert
 *                  frequency counting in generateSeasonSummary.
 * ALGORITHMS: Iteration over records; String formatting; alert frequency sort.
 */
public class ReportGenerator {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RecordStore store;

    /** Default — uses the standard file-based store. */
    public ReportGenerator() {
        this.store = new FileHandler();
    }

    /**
     * Injection constructor — callers (tests, future DB-backed versions) can
     * supply any RecordStore implementation without touching this class.
     */
    public ReportGenerator(RecordStore store) {
        this.store = store;
    }

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
     * Builds an aggregated season summary from all records in the list.
     * Includes total production, total waste, total revenue, average food
     * security indicator, count of CRITICAL and WARNING alerts, and the top
     * three policy alert messages by frequency of occurrence.
     *
     * <p>Alert severity is inferred from message content: messages containing
     * "ALERT", "CRITICAL", or "RISK HIGH" are CRITICAL; messages containing
     * "WARNING", "REQUIRED", or "BREACHED" are WARNING.
     *
     * @param records list of simulation records to aggregate (may be empty)
     * @return formatted multi-line summary string
     */
    public String generateSeasonSummary(List<SimulationRecord> records) {
        if (records == null || records.isEmpty()) {
            return "\n(no records to summarise)";
        }

        double totalProduction = 0.0;
        double totalWaste      = 0.0;
        double totalRevenue    = 0.0;
        double fsiSum          = 0.0;
        int    criticalCount   = 0;
        int    warningCount    = 0;
        Map<String, Integer> alertFreq = new HashMap<>();

        for (SimulationRecord r : records) {
            totalProduction += r.getProduction();
            totalWaste      += r.getWaste();
            totalRevenue    += r.getRevenue();
            fsiSum          += r.getFoodSecurityIndicator();

            String alerts = r.getPolicyAlerts();
            if (alerts != null && !alerts.isEmpty()) {
                for (String raw : alerts.split(";")) {
                    String a = raw.trim();
                    if (a.isEmpty()) continue;
                    if (isCriticalAlert(a))       criticalCount++;
                    else if (isWarningAlert(a))   warningCount++;
                    alertFreq.put(a, alertFreq.getOrDefault(a, 0) + 1);
                }
            }
        }

        double avgFsi = fsiSum / records.size();

        // Sort alert entries by frequency descending, take top 3
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(alertFreq.entrySet());
        Collections.sort(sorted, (a, b) -> b.getValue() - a.getValue());
        StringBuilder top3 = new StringBuilder();
        int limit = Math.min(3, sorted.size());
        for (int i = 0; i < limit; i++) {
            if (top3.length() > 0) top3.append(", ");
            top3.append(sorted.get(i).getKey())
                .append(" (x").append(sorted.get(i).getValue()).append(")");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n============================================================\n");
        sb.append("  SEASON SUMMARY\n");
        sb.append("============================================================\n");
        sb.append(String.format("  Total Production          : %.2f tons%n",    totalProduction));
        sb.append(String.format("  Total Waste / Spoilage    : %.2f tons%n",    totalWaste));
        sb.append(String.format("  Total Revenue             : ZMW %,.2f%n",   totalRevenue));
        sb.append(String.format("  Average Food Security     : %.2f%%%n",       avgFsi));
        sb.append(String.format("  CRITICAL Alerts           : %d%n",           criticalCount));
        sb.append(String.format("  WARNING Alerts            : %d%n",           warningCount));
        sb.append(String.format("  Top Alerts (by frequency) : %s%n",
                top3.length() == 0 ? "(none)" : top3.toString()));
        sb.append("============================================================");
        return sb.toString();
    }

    /** True when the alert message is CRITICAL severity. */
    private boolean isCriticalAlert(String alert) {
        return alert.contains("ALERT") || alert.contains("CRITICAL") || alert.contains("RISK HIGH");
    }

    /** True when the alert message is WARNING severity. */
    private boolean isWarningAlert(String alert) {
        return alert.contains("WARNING") || alert.contains("REQUIRED") || alert.contains("BREACHED");
    }

    /**
     * Writes a full readable report (one block per month) plus the season
     * summary to a .txt file. Uses FileWriter directly per spec; structured
     * pipe-delimited export goes through exportStructured below.
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
            // Season summary appended before the END OF REPORT footer
            pw.println(generateSeasonSummary(records));
            pw.println();
            pw.println("============================================================");
            pw.println(" END OF REPORT");
            pw.println("============================================================");
        }
    }

    /**
     * Writes all records to a comma-separated CSV file. The policyAlerts
     * field is double-quoted (with internal double-quotes escaped as {@code ""})
     * to handle embedded commas per RFC 4180.
     *
     * <p>Header row: {@code monthNumber,commodityName,production,demand,
     * surplusOrShortage,waste,revenue,foodSecurityIndicator,policyAlerts}
     *
     * @param records  records to export
     * @param filename output file path (created or overwritten)
     * @throws IOException if the file cannot be written
     */
    public void exportToCsv(List<SimulationRecord> records, String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("monthNumber,commodityName,production,demand,"
                    + "surplusOrShortage,waste,revenue,foodSecurityIndicator,policyAlerts");
            for (SimulationRecord r : records) {
                String alerts = r.getPolicyAlerts() == null ? "" : r.getPolicyAlerts();
                // RFC 4180: double-quote the field and escape any embedded quotes
                String quotedAlerts = "\"" + alerts.replace("\"", "\"\"") + "\"";
                pw.printf("%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s%n",
                        r.getMonthNumber(),
                        r.getCommodityName(),
                        r.getProduction(),
                        r.getDemand(),
                        r.getSurplusOrShortage(),
                        r.getWaste(),
                        r.getRevenue(),
                        r.getFoodSecurityIndicator(),
                        quotedAlerts);
            }
        }
    }

    /**
     * Persists records via the injected RecordStore — pipe-delimited by
     * FileHandler by default, swappable to any RecordStore implementation.
     */
    public void exportStructured(List<SimulationRecord> records, String path) throws IOException {
        store.saveSimulation(records, path);
    }
}
