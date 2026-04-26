package agritwin.service;

import agritwin.model.Maize;
import agritwin.model.Tomato;

/**
 * MODULE: Storage Loss Simulator
 * PURPOSE: Models post-harvest losses for both commodities. For maize it
 *          applies a compounded monthly loss rate. For tomatoes it applies a
 *          per-day spoilage rate that is reduced by 40% under cold storage.
 * OOP CONCEPTS USED: Encapsulation; Method overloading-style separation
 *                    of concerns (one method per commodity).
 * DATA STRUCTURES: Primitive doubles + model references.
 * ALGORITHMS: Compound decay  remaining = initial * (1 - r)^months;
 *             Linear daily spoilage capped at full volume.
 */
public class StorageLossSimulator {

    /**
     * Returns the maize tonnage remaining after `months` months of storage,
     * applying the per-month loss rate compounded each month. This mirrors
     * how grain storage losses are commonly modelled in PHL studies.
     *
     * Example: 10000 tons at 3%/month for 3 months
     *          = 10000 * 0.97^3 ~= 9126.73 tons.
     */
    public double simulateMaizeLoss(Maize m, int months) {
        if (m == null || months <= 0) return m == null ? 0 : m.getProductionVolume();
        double r = m.getLossPercentagePerMonth() / 100.0;
        double remaining = m.getProductionVolume() * Math.pow(1.0 - r, months);
        return Math.max(0.0, remaining);
    }

    /**
     * Returns the tons of tomato spoiled after `daysStored` days of storage.
     * Cold storage reduces the spoilage rate by 40% (per spec). Result is
     * capped at the full production volume.
     *
     * Example: 100 tons, 5%/day, 4 days, open  spoilage = 100 * 0.20 = 20 tons
     *          Same params under cold  spoilage = 100 * 0.12 = 12 tons.
     */
    public double simulateTomatoSpoilage(Tomato t, int daysStored) {
        if (t == null || daysStored <= 0) return 0.0;
        double effectiveRate = t.getSpoilageRate();
        if ("cold".equalsIgnoreCase(t.getStorageType())) {
            effectiveRate = effectiveRate * 0.6; // 40% reduction
        }
        double fraction = Math.min(1.0, (effectiveRate / 100.0) * daysStored);
        return t.getProductionVolume() * fraction;
    }
}
