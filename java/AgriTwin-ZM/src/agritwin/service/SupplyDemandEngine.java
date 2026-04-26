package agritwin.service;

import agritwin.model.Tomato;

/**
 * MODULE: Supply &amp; Demand Engine
 * PURPOSE: Performs the core economic calculations of the digital twin —
 *          surplus/shortage, waste, revenue, dynamic price adjustment and the
 *          food-security indicator. This is the analytical heart of the
 *          simulation; ConsoleUI and GuiApp both delegate to it.
 * OOP CONCEPTS USED: Encapsulation; Single-Responsibility (only economics
 *                    lives here, no UI or persistence concerns).
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
     * Cold storage cuts the effective spoilage rate by 40% (per assignment
     * spec). The result is the absolute volume lost over the holding period.
     */
    public double calculateWaste(Tomato t, int daysBeforeSale) {
        if (t == null || daysBeforeSale <= 0) return 0.0;
        double effectiveRate = t.getSpoilageRate();
        if ("cold".equalsIgnoreCase(t.getStorageType())) {
            effectiveRate = effectiveRate * 0.6; // 40% reduction
        }
        // spoilage rate is % per day; cap fraction at 100%
        double fractionLost = Math.min(1.0, (effectiveRate / 100.0) * daysBeforeSale);
        return t.getProductionVolume() * fractionLost;
    }

    /**
     * Straightforward revenue formula: volume sold * unit price.
     * Caller passes a non-negative volume; we guard against negatives.
     */
    public double calculateRevenue(double volumeSold, double pricePerUnit) {
        if (volumeSold < 0) volumeSold = 0;
        if (pricePerUnit < 0) pricePerUnit = 0;
        return volumeSold * pricePerUnit;
    }

    /**
     * Linear price adjustment relative to surplus.
     * Formula (per spec): newPrice = currentPrice * (1 - (surplus / production) * 0.1)
     * If surplus > 0 the price falls; if shortage (surplus &lt; 0) the price rises.
     * `production` is required as a denominator — we guard against zero.
     */
    public double adjustPrice(double currentPrice, double surplus, double production) {
        if (production <= 0) return currentPrice;
        double adjusted = currentPrice * (1.0 - (surplus / production) * 0.1);
        return Math.max(0.0, adjusted); // prices cannot go negative
    }

    /**
     * Convenience overload (matches the brief signature). When `production`
     * is unknown we fall back to using the surplus as a self-normalised input.
     */
    public double adjustPrice(double currentPrice, double surplus) {
        // Without a production figure we assume |surplus| represents 100% of
        // the reference volume so the multiplier becomes (1 - sign*0.1).
        if (surplus == 0) return currentPrice;
        double sign = Math.signum(surplus);
        return Math.max(0.0, currentPrice * (1.0 - sign * 0.1));
    }

    /**
     * Food-security indicator: percentage of demand that production covers.
     * Returns 100% if production &gt;= demand. Returns 0 when demand is 0
     * to avoid division by zero.
     */
    public double calculateFoodSecurityIndicator(double production, double demand) {
        if (demand <= 0) return 0.0;
        double pct = (production / demand) * 100.0;
        return Math.min(100.0, pct);
    }
}
