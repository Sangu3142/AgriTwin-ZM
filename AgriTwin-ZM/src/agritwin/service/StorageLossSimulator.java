package agritwin.service;

import agritwin.model.Maize;
import agritwin.model.StorageType;
import agritwin.model.Tomato;

import java.util.ArrayList;
import java.util.List;

/**
 * MODULE: Storage Loss Simulator
 * PURPOSE: Models post-harvest losses for both commodities. For maize it
 *          applies a compounded monthly loss rate adjusted upward when grain
 *          moisture exceeds 14% (high moisture accelerates fungal growth).
 *          For tomatoes it applies a per-day spoilage rate that is reduced
 *          under cold storage by COLD_STORAGE_SPOILAGE_FACTOR (0.6 = 40%
 *          reduction per spec).
 * OOP CONCEPTS USED: Encapsulation; Single-Responsibility; named constants
 *                    (COLD_STORAGE_SPOILAGE_FACTOR, MOISTURE_LOSS_THRESHOLD,
 *                    MAX_MAIZE_MONTHLY_LOSS_RATE) eliminate magic numbers;
 *                    StorageType enum replaces fragile String comparison.
 * DATA STRUCTURES: List<Double> for the per-month loss schedule.
 * ALGORITHMS: Compound decay — remaining = initial * (1 - r)^months;
 *             Linear daily spoilage capped at full production volume;
 *             Moisture-adjusted effective loss rate.
 */
public class StorageLossSimulator {

    /**
     * Cold storage reduces the effective tomato spoilage rate by 40% (per spec).
     * Declared public so SupplyDemandEngine.calculateWaste can reference the
     * same value without duplicating it.
     */
    public static final double COLD_STORAGE_SPOILAGE_FACTOR = 0.6;

    /** Moisture threshold above which maize loss rate is increased. */
    private static final double MOISTURE_LOSS_THRESHOLD = 14.0;

    /**
     * Maximum effective monthly maize loss rate (20%/month). Prevents the
     * moisture adjustment from producing unrealistically large losses.
     */
    private static final double MAX_MAIZE_MONTHLY_LOSS_RATE = 20.0;

    /**
     * Computes the effective monthly loss rate for maize, adjusted for
     * moisture level. High moisture (above 14%) accelerates fungal growth
     * in stored grain; each percentage point above 14% adds 0.5 pp to the
     * base rate, capped at MAX_MAIZE_MONTHLY_LOSS_RATE.
     */
    private double effectiveMaizeLossRate(Maize m) {
        double base     = m.getLossPercentagePerMonth();
        double moisture = m.getMoistureLevel();
        if (moisture > MOISTURE_LOSS_THRESHOLD) {
            double adjustment = (moisture - MOISTURE_LOSS_THRESHOLD) * 0.5;
            base = Math.min(MAX_MAIZE_MONTHLY_LOSS_RATE, base + adjustment);
        }
        return base;
    }

    /**
     * Returns the maize tonnage remaining after {@code months} months of
     * storage, applying the per-month loss rate compounded each month.
     *
     * <p>When grain moisture exceeds 14%, the effective monthly loss rate is
     * increased by {@code (moisture - 14) * 0.5} percentage points, capped
     * at 20%/month. High moisture accelerates fungal (aflatoxin) growth in
     * stored grain — a well-documented post-harvest risk in sub-Saharan Africa.
     *
     * <p>Example: 10 000 tons at 3%/month, moisture 12.5%, 3 months
     *             = 10 000 * 0.97^3 ≈ 9 126.73 tons.
     */
    public double simulateMaizeLoss(Maize m, int months) {
        if (m == null || months <= 0) return m == null ? 0 : m.getProductionVolume();
        double r         = effectiveMaizeLossRate(m) / 100.0;
        double remaining = m.getProductionVolume() * Math.pow(1.0 - r, months);
        return Math.max(0.0, remaining);
    }

    /**
     * Returns a schedule of remaining maize tonnage at the end of each
     * storage month. Index 0 = initial production volume, index k = tonnage
     * remaining after k months. The list size is always {@code months + 1}.
     *
     * <p>The moisture-adjusted loss rate from {@link #simulateMaizeLoss} is
     * applied consistently so this schedule matches the per-month values that
     * method would produce for each horizon. Useful for GUI charting and for
     * the report aggregator.
     *
     * @param m      maize object carrying production volume and loss parameters
     * @param months number of storage months to project (must be &gt;= 0)
     * @return list of size {@code months + 1} with remaining tonnage per month
     */
    public List<Double> getMaizeLossSchedule(Maize m, int months) {
        List<Double> schedule = new ArrayList<>();
        schedule.add(m.getProductionVolume()); // month 0: no loss yet
        if (months <= 0) return schedule;

        double r       = effectiveMaizeLossRate(m) / 100.0;
        double current = m.getProductionVolume();
        for (int i = 1; i <= months; i++) {
            current = Math.max(0.0, current * (1.0 - r));
            schedule.add(current);
        }
        return schedule;
    }

    /**
     * Returns the tons of tomato spoiled after {@code daysStored} days. Cold
     * storage applies COLD_STORAGE_SPOILAGE_FACTOR (0.6) to the rate, giving
     * a 40% reduction. Result is capped at the full production volume.
     *
     * <p>Example: 100 tons, 5%/day, 4 days, OPEN  → 100 * 0.20 = 20 tons spoiled
     *             Same params, COLD                 → 100 * 0.12 = 12 tons spoiled.
     */
    public double simulateTomatoSpoilage(Tomato t, int daysStored) {
        if (t == null || daysStored <= 0) return 0.0;
        double effectiveRate = t.getSpoilageRate();
        if (t.getStorageType() == StorageType.COLD) {
            effectiveRate = effectiveRate * COLD_STORAGE_SPOILAGE_FACTOR;
        }
        double fraction = Math.min(1.0, (effectiveRate / 100.0) * daysStored);
        return t.getProductionVolume() * fraction;
    }

    /**
     * Returns the number of days until 50% of the tomato harvest is spoiled
     * at the current effective spoilage rate (cold or open storage). The
     * result is useful as an "act-by date" recommendation in reports: farmers
     * should sell or process tomatoes before this point to recover half the
     * harvest value.
     *
     * <p>Derivation: fraction = (rate / 100) * days; at fraction = 0.5,
     * days = 50 / effectiveRate. Result is rounded up to the nearest integer.
     *
     * @param t tomato commodity carrying spoilage rate and storage type
     * @return integer days until 50% spoilage; Integer.MAX_VALUE if rate is 0
     */
    public int daysToCriticalSpoilage(Tomato t) {
        if (t == null) return 0;
        double rate = t.getSpoilageRate();
        if (t.getStorageType() == StorageType.COLD) {
            rate = rate * COLD_STORAGE_SPOILAGE_FACTOR;
        }
        if (rate <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(50.0 / rate);
    }
}
