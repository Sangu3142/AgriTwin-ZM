package agritwin.service;

import agritwin.model.StorageType;
import agritwin.model.Tomato;

/**
 * MODULE: Supply &amp; Demand Engine
 * PURPOSE: Performs the core economic calculations of the digital twin —
 *          surplus/shortage, waste, revenue, dynamic price adjustment and the
 *          food-security indicator. SimulationRunner delegates to it for all
 *          period-level calculations.
 * OOP CONCEPTS USED: Encapsulation; Single-Responsibility (only economics
 *                    lives here, no UI or persistence concerns); references
 *                    StorageLossSimulator.COLD_STORAGE_SPOILAGE_FACTOR to
 *                    avoid duplicating the 0.6 magic number; StorageType enum
 *                    replaces fragile String comparison.
 * DATA STRUCTURES: Primitive doubles + Tomato model reference.
 * ALGORITHMS: Linear price-elasticity adjustment; capped ratio for the
 *             food-security indicator; per-day spoilage accumulation.
 */
public class SupplyDemandEngine {

    /**
     * Returns positive value for surplus, negative for shortage.
     * Net = production - demand (tons).
     */
    public double calculateSurplus(double production, double demand) {
        return production - demand;
    }

    /**
     * Computes tons of tomato wasted before sale based on storage type.
     * Cold storage cuts the effective spoilage rate by 40% via
     * StorageLossSimulator.COLD_STORAGE_SPOILAGE_FACTOR (shared constant,
     * no duplication). Result is the absolute volume lost over the period.
     */
    public double calculateWaste(Tomato t, int daysBeforeSale) {
        if (t == null || daysBeforeSale <= 0) return 0.0;
        double effectiveRate = t.getSpoilageRate();
        if (t.getStorageType() == StorageType.COLD) {
            effectiveRate = effectiveRate * StorageLossSimulator.COLD_STORAGE_SPOILAGE_FACTOR;
        }
        double fractionLost = Math.min(1.0, (effectiveRate / 100.0) * daysBeforeSale);
        return t.getProductionVolume() * fractionLost;
    }

    /**
     * Straightforward revenue formula: volume sold * unit price.
     * Guards against caller-supplied negatives.
     */
    public double calculateRevenue(double volumeSold, double pricePerUnit) {
        if (volumeSold < 0)    volumeSold = 0;
        if (pricePerUnit < 0)  pricePerUnit = 0;
        return volumeSold * pricePerUnit;
    }

    /**
     * Linear price adjustment relative to surplus.
     * Formula: newPrice = currentPrice * (1 - (surplus / production) * 0.1)
     * Surplus > 0 → price falls; shortage (surplus < 0) → price rises.
     * `production` is the denominator — guarded against zero.
     */
    public double adjustPrice(double currentPrice, double surplus, double production) {
        if (production <= 0) return currentPrice;
        double adjusted = currentPrice * (1.0 - (surplus / production) * 0.1);
        return Math.max(0.0, adjusted);
    }

    /**
     * Convenience overload — when `production` is unknown the sign of surplus
     * determines direction only (±10% step).
     */
    public double adjustPrice(double currentPrice, double surplus) {
        if (surplus == 0) return currentPrice;
        double sign = Math.signum(surplus);
        return Math.max(0.0, currentPrice * (1.0 - sign * 0.1));
    }

    /**
     * Food-security indicator: percentage of demand that production covers.
     * Returns 100% when production >= demand. Returns 0 when demand is 0
     * to avoid division by zero.
     */
    public double calculateFoodSecurityIndicator(double production, double demand) {
        if (demand <= 0) return 0.0;
        double pct = (production / demand) * 100.0;
        return Math.min(100.0, pct);
    }
}
