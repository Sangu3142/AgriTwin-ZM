package agritwin.service;

import java.util.ArrayList;
import java.util.List;

/**
 * MODULE: Policy Engine
 * PURPOSE: Encodes the rule-based agricultural policies that govern the
 *          Zambian food system: the Food Reserve Agency (FRA) buffer
 *          threshold, export-review trigger, storage-time warnings for
 *          both maize and tomatoes, aflatoxin contamination risk, and the
 *          farmer price-floor check. Each rule returns a short status
 *          message; evaluateAllWithSeverity attaches a Severity level to
 *          each, and evaluateAll (kept for backward compatibility) joins
 *          the messages into a semicolon-delimited string.
 * OOP CONCEPTS USED: Encapsulation; Strategy-like grouping of related rules;
 *                    Inner static class (Alert) bundles a message with its
 *                    Severity so callers receive structured, type-safe output
 *                    rather than strings that must be re-parsed.
 * DATA STRUCTURES: List<Alert> for structured output; String for the
 *                  backward-compatible evaluateAll path.
 * ALGORITHMS: If-else rule cascade — deterministic, easily auditable.
 */
public class PolicyEngine {

    // -----------------------------------------------------------------------
    // Policy constants
    // -----------------------------------------------------------------------

    /** FRA strategic-reserve threshold in tons (per spec). */
    public static final double FRA_RESERVE_THRESHOLD    = 500_000.0;
    /** Export-review trigger: surplus &gt; 20% of production. */
    public static final double EXPORT_SURPLUS_FRACTION  = 0.20;
    /** Maize storage warning threshold in months. */
    public static final int    MAIZE_MAX_STORAGE_MONTHS = 6;

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    /**
     * A structured policy alert pairing a human-readable message with a
     * Severity level. Used by evaluateAllWithSeverity and the reporting layer.
     */
    public static class Alert {
        public final String   message;
        public final Severity level;

        public Alert(String message, Severity level) {
            this.message = message;
            this.level   = level;
        }

        @Override
        public String toString() {
            return "[" + level + "] " + message;
        }
    }

    // -----------------------------------------------------------------------
    // Individual rule methods
    // -----------------------------------------------------------------------

    /**
     * Triggers a "RESERVE ALERT" when national maize stock dips below the
     * FRA threshold; otherwise returns "FRA RESERVE OK".
     */
    public String checkFraReserve(double maizeStockTons) {
        if (maizeStockTons < FRA_RESERVE_THRESHOLD) {
            return "RESERVE ALERT (stock " + String.format("%.0f", maizeStockTons)
                    + "t below FRA threshold " + (long) FRA_RESERVE_THRESHOLD + "t)";
        }
        return "FRA RESERVE OK";
    }

    /**
     * Flags an export review when surplus exceeds 20% of production.
     * Returns "EXPORT OK" when the surplus is small or negative.
     */
    public String checkExportRestriction(double surplus, double production) {
        if (production <= 0) return "EXPORT N/A";
        if (surplus > production * EXPORT_SURPLUS_FRACTION) {
            return "EXPORT REVIEW REQUIRED (surplus "
                    + String.format("%.1f%%", (surplus / production) * 100.0)
                    + " of production)";
        }
        return "EXPORT OK";
    }

    /**
     * Maize storage time check — beyond 6 months we issue a STORAGE WARNING.
     */
    public String checkMaizeStorageTime(int monthsStored) {
        if (monthsStored > MAIZE_MAX_STORAGE_MONTHS) {
            return "STORAGE WARNING (maize stored " + monthsStored
                    + " months; max " + MAIZE_MAX_STORAGE_MONTHS + ")";
        }
        return "STORAGE OK";
    }

    /**
     * Tomato storage check — beyond shelf-life days we mark SPOILAGE CRITICAL.
     */
    public String checkTomatoStorageTime(int daysStored, int shelfLifeDays) {
        if (daysStored > shelfLifeDays) {
            return "SPOILAGE CRITICAL (held " + daysStored
                    + "d > shelf-life " + shelfLifeDays + "d)";
        }
        return "TOMATO SHELF-LIFE OK";
    }

    /**
     * Aflatoxin contamination risk check. Returns "AFLATOXIN RISK HIGH" when
     * both conditions are met — moisture above 14% AND grain stored for at
     * least 3 months. Aflatoxin-producing moulds thrive in warm, humid
     * conditions; sustained high moisture over multiple months is the primary
     * driver of contamination in Zambian stored maize.
     *
     * @param moistureLevel  grain moisture percentage
     * @param monthsStored   number of months the grain has been in storage
     * @return "AFLATOXIN RISK HIGH" or "AFLATOXIN RISK OK"
     */
    public String checkAflatoxinRisk(double moistureLevel, int monthsStored) {
        if (moistureLevel > 14.0 && monthsStored >= 3) {
            return "AFLATOXIN RISK HIGH";
        }
        return "AFLATOXIN RISK OK";
    }

    /**
     * Farmer price-floor check. Returns "FARMER PRICE FLOOR BREACHED" when the
     * current market price falls below the cost of production per ton. This
     * condition is the standard trigger for FRA price-support intervention in
     * Zambia: if farmers cannot recover costs, they reduce planting in the next
     * season, threatening future food security.
     *
     * @param currentPrice          current market price in ZMW/ton
     * @param productionCostPerTon  total cost of production in ZMW/ton
     * @return "FARMER PRICE FLOOR BREACHED" or "FARMER PRICE OK"
     */
    public String checkFarmerPriceFloor(double currentPrice, double productionCostPerTon) {
        if (currentPrice < productionCostPerTon) {
            return "FARMER PRICE FLOOR BREACHED";
        }
        return "FARMER PRICE OK";
    }

    // -----------------------------------------------------------------------
    // Aggregators
    // -----------------------------------------------------------------------

    /**
     * Runs every applicable rule and returns a list of {@link Alert} objects,
     * each carrying the rule message and its Severity level. Pass -1 for
     * fields that do not apply (e.g. shelfLifeDays when running a maize
     * simulation).
     *
     * <p>Severity assignment:
     * <ul>
     *   <li>FRA RESERVE ALERT → CRITICAL
     *   <li>EXPORT REVIEW REQUIRED → WARNING
     *   <li>STORAGE WARNING → WARNING
     *   <li>SPOILAGE CRITICAL → CRITICAL
     *   <li>all OK / N/A messages → INFO
     * </ul>
     */
    public List<Alert> evaluateAllWithSeverity(double currentStockTons,
                                               double surplus,
                                               double production,
                                               int monthsStored,
                                               int daysStored,
                                               int shelfLifeDays) {
        List<Alert> alerts = new ArrayList<>();

        String fra = checkFraReserve(currentStockTons);
        alerts.add(new Alert(fra,
                fra.contains("ALERT") ? Severity.CRITICAL : Severity.INFO));

        String exp = checkExportRestriction(surplus, production);
        alerts.add(new Alert(exp,
                exp.contains("REVIEW") ? Severity.WARNING : Severity.INFO));

        if (monthsStored >= 0) {
            String stor = checkMaizeStorageTime(monthsStored);
            alerts.add(new Alert(stor,
                    stor.contains("WARNING") ? Severity.WARNING : Severity.INFO));
        }

        if (daysStored >= 0 && shelfLifeDays > 0) {
            String tom = checkTomatoStorageTime(daysStored, shelfLifeDays);
            alerts.add(new Alert(tom,
                    tom.contains("CRITICAL") ? Severity.CRITICAL : Severity.INFO));
        }

        return alerts;
    }

    /**
     * Backward-compatible aggregator — runs every applicable rule and returns
     * a single semicolon-delimited summary suitable for inclusion in a
     * SimulationRecord. Delegates to {@link #evaluateAllWithSeverity} so rule
     * logic is never duplicated.
     */
    public String evaluateAll(double currentStockTons,
                              double surplus,
                              double production,
                              int monthsStored,
                              int daysStored,
                              int shelfLifeDays) {
        List<Alert> alerts = evaluateAllWithSeverity(
                currentStockTons, surplus, production, monthsStored, daysStored, shelfLifeDays);
        StringBuilder sb = new StringBuilder();
        for (Alert a : alerts) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(a.message);
        }
        return sb.toString();
    }
}
