package agritwin.service;

import agritwin.model.Maize;
import agritwin.model.StorageType;
import agritwin.model.Tomato;

/**
 * MODULE: Storage Loss Simulator
 * PURPOSE: Models post-harvest losses for both commodities. For maize it
 *          applies a compounded monthly loss rate. For tomatoes it applies a
 *          per-day spoilage rate that is reduced under cold storage by
 *          COLD_STORAGE_SPOILAGE_FACTOR (0.6 = 40% reduction per spec).
 * OOP CONCEPTS USED: Encapsulation; Single-Responsibility; named constant
 *                    (COLD_STORAGE_SPOILAGE_FACTOR) eliminates the 0.6 magic
 *                    number that was previously duplicated in SupplyDemandEngine;
 *                    StorageType enum replaces fragile String comparison.
 * DATA STRUCTURES: Primitive doubles + model references.
 * ALGORITHMS: Compound decay — remaining = initial * (1 - r)^months;
 *             Linear daily spoilage capped at full production volume.
 */
public class StorageLossSimulator {

    /**
     * Cold storage reduces the effective tomato spoilage rate by 40% (per spec).
     * Declared public so SupplyDemandEngine.calculateWaste can reference the
     * same value without duplicating it.
     */
    public static final double COLD_STORAGE_SPOILAGE_FACTOR = 0.6;

    /**
     * Returns the maize tonnage remaining after `months` months of storage,
     * applying the per-month loss rate compounded each month. This mirrors
     * how grain storage losses are commonly modelled in post-harvest studies.
     *
     * Example: 10 000 tons at 3%/month for 3 months
     *          = 10 000 * 0.97^3 ≈ 9 126.73 tons.
     */
    public double simulateMaizeLoss(Maize m, int months) {
        if (m == null || months <= 0) return m == null ? 0 : m.getProductionVolume();
        double r = m.getLossPercentagePerMonth() / 100.0;
        double remaining = m.getProductionVolume() * Math.pow(1.0 - r, months);
        return Math.max(0.0, remaining);
    }

    /**
     * Returns the tons of tomato spoiled after `daysStored` days. Cold
     * storage applies COLD_STORAGE_SPOILAGE_FACTOR (0.6) to the rate, giving
     * a 40% reduction. Result is capped at the full production volume.
     *
     * Example: 100 tons, 5%/day, 4 days, OPEN  → 100 * 0.20 = 20 tons spoiled
     *          Same params, COLD                 → 100 * 0.12 = 12 tons spoiled.
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
}
