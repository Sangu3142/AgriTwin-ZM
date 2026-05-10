package agritwin.service;

import agritwin.model.StorageType;
import agritwin.model.Tomato;

/**
 * MODULE: Supply &amp; Demand Engine
 * PURPOSE: Performs the core economic calculations of the digital twin —
 *          surplus/shortage, waste, revenue, dynamic price adjustment, the
 *          food-security indicator, seasonal demand modelling, and break-even
 *          price analysis. SimulationRunner delegates to it for all
 *          period-level calculations.
 * OOP CONCEPTS USED: Encapsulation; Single-Responsibility (only economics
 *                    lives here, no UI or persistence concerns); named constants
 *                    (PRICE_ELASTICITY) eliminate magic numbers and make the
 *                    model's assumptions explicit and testable; references
 *                    StorageLossSimulator.COLD_STORAGE_SPOILAGE_FACTOR to
 *                    avoid duplicating the 0.6 magic number; StorageType enum
 *                    replaces fragile String comparison.
 * DATA STRUCTURES: Primitive doubles + Tomato model reference.
 * ALGORITHMS: Linear price-elasticity adjustment; capped ratio for the
 *             food-security indicator; sinusoidal seasonal demand model.
 */
public class SupplyDemandEngine {

    /**
     * Price-elasticity coefficient used in both adjustPrice overloads.
     * A value of 0.1 means a surplus equal to 100% of production moves the
     * price by 10%. Exposed as a constant so tests can reference it and the
     * model's assumption is self-documenting.
     */
    public static final double PRICE_ELASTICITY = 0.1;

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
     * Linear price adjustment relative to surplus, using PRICE_ELASTICITY.
     * Formula: newPrice = currentPrice * (1 - (surplus / production) * PRICE_ELASTICITY)
     * Surplus > 0 → price falls; shortage (surplus < 0) → price rises.
     * {@code production} is the denominator — guarded against zero.
     */
    public double adjustPrice(double currentPrice, double surplus, double production) {
        if (production <= 0) return currentPrice;
        double adjusted = currentPrice * (1.0 - (surplus / production) * PRICE_ELASTICITY);
        return Math.max(0.0, adjusted);
    }

    /**
     * Convenience overload — when {@code production} is unknown the sign of
     * surplus determines direction only (±PRICE_ELASTICITY step).
     */
    public double adjustPrice(double currentPrice, double surplus) {
        if (surplus == 0) return currentPrice;
        double sign = Math.signum(surplus);
        return Math.max(0.0, currentPrice * (1.0 - sign * PRICE_ELASTICITY));
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

    /**
     * Returns the seasonally-adjusted monthly demand for a given month index,
     * modelling Zambian maize consumption patterns. Demand is higher in the
     * dry season (April–September, months 4–9) when on-farm food stocks are
     * depleted and market purchases peak, and lower in the wet/harvest season.
     *
     * <p>Formula:
     * <pre>
     *   demand(month) = (annualDemand / 12) * (1 + 0.25 * sin(2π * (month - 4) / 12))
     * </pre>
     * The sinusoid peaks near month 7 (July, mid dry season) and troughs near
     * month 1 (January, harvest season) — a ±25% swing around the monthly mean.
     *
     * @param annualDemand total annual demand in tons
     * @param monthIndex   calendar month (1 = January … 12 = December)
     * @return seasonally-adjusted demand for that month, in tons
     */
    public double simulateSeasonalDemand(double annualDemand, int monthIndex) {
        return (annualDemand / 12.0)
                * (1.0 + 0.25 * Math.sin(2.0 * Math.PI * (monthIndex - 4) / 12.0));
    }

    /**
     * Returns the minimum price per ton at which total revenue covers total
     * production cost — the farmer break-even point. Used in reports to flag
     * scenarios where market prices may not recover costs.
     *
     * @param production     total production in tons (must be &gt; 0)
     * @param totalCostZmw   total cost of production in ZMW
     * @return break-even price in ZMW/ton, or 0 if production is non-positive
     */
    public double calculateBreakEvenPrice(double production, double totalCostZmw) {
        if (production <= 0) return 0.0;
        return totalCostZmw / production;
    }
}
