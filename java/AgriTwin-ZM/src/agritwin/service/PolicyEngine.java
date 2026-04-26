package agritwin.service;

/**
 * MODULE: Policy Engine
 * PURPOSE: Encodes the rule-based agricultural policies that govern the
 *          Zambian food system: the Food Reserve Agency (FRA) buffer
 *          threshold, export-review trigger, and storage-time warnings for
 *          both maize and tomatoes. Each rule returns a short status
 *          message that the report layer concatenates into "policy alerts".
 * OOP CONCEPTS USED: Encapsulation; Strategy-like grouping of related rules.
 * DATA STRUCTURES: Primitive doubles / ints; String return values.
 * ALGORITHMS: If-else rule cascade — deterministic, easily auditable.
 */
public class PolicyEngine {

    // FRA strategic-reserve threshold in tons (per spec).
    public static final double FRA_RESERVE_THRESHOLD = 500_000.0;
    // Export-review trigger: surplus &gt; 20% of production.
    public static final double EXPORT_SURPLUS_FRACTION = 0.20;
    // Maize storage warning threshold in months.
    public static final int MAIZE_MAX_STORAGE_MONTHS = 6;

    /**
     * Triggers a "RESERVE ALERT" when national maize stock dips below the
     * FRA threshold; otherwise returns "OK".
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
     * Returns "OK" when the surplus is small / negative.
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
     * Convenience aggregator — runs every applicable rule and returns a
     * single semicolon-delimited summary suitable for inclusion in a
     * SimulationRecord. Pass -1 for fields that do not apply (e.g.
     * shelfLifeDays when running a maize simulation).
     */
    public String evaluateAll(double currentStockTons,
                              double surplus,
                              double production,
                              int monthsStored,
                              int daysStored,
                              int shelfLifeDays) {
        StringBuilder sb = new StringBuilder();
        sb.append(checkFraReserve(currentStockTons)).append("; ");
        sb.append(checkExportRestriction(surplus, production)).append("; ");
        if (monthsStored >= 0) sb.append(checkMaizeStorageTime(monthsStored)).append("; ");
        if (daysStored >= 0 && shelfLifeDays > 0) sb.append(checkTomatoStorageTime(daysStored, shelfLifeDays));
        return sb.toString().trim();
    }
}
