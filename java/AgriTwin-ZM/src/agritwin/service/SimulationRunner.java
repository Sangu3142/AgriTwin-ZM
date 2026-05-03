package agritwin.service;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.Tomato;

import java.util.ArrayList;
import java.util.List;

/**
 * MODULE: Simulation Runner (service)
 * PURPOSE: Encapsulates the month-by-month simulation state machine for both
 *          maize and tomato runs. Neither ConsoleUI nor GuiApp carries any
 *          simulation loop logic — they collect inputs, delegate here, then
 *          display the returned SimulationRecord list. This is a key
 *          architectural improvement: previously the loops lived inside UI
 *          classes, violating Single Responsibility.
 * OOP CONCEPTS USED: Encapsulation; Single-Responsibility (the simulation
 *                    loop lives here only, nowhere in any UI class);
 *                    Composition (holds SupplyDemandEngine, StorageLossSimulator
 *                    and PolicyEngine as collaborators).
 * DATA STRUCTURES: ArrayList<SimulationRecord> returned per run.
 * ALGORITHMS: Compounded monthly loss loop for maize; single-period waste
 *             calculation for tomatoes — identical formulae to those previously
 *             inlined in ConsoleUI / GuiApp, now in a single authoritative place.
 */
public class SimulationRunner {

    private final SupplyDemandEngine supply = new SupplyDemandEngine();
    private final StorageLossSimulator storage = new StorageLossSimulator();
    private final PolicyEngine policy = new PolicyEngine();

    /**
     * Runs a multi-month maize simulation and returns one SimulationRecord
     * per month. The Maize object supplies initial stock, storage capacity,
     * moisture level, price and monthly loss rate; demand is the total
     * seasonal demand spread evenly across months.
     */
    public List<SimulationRecord> runMaizeSimulation(Maize m, double demand, int months) {
        List<SimulationRecord> records = new ArrayList<>();
        double currentStock  = m.getProductionVolume();
        double currentPrice  = m.getPricePerTon();
        double monthlyLoss   = m.getLossPercentagePerMonth();
        double storageCap    = m.getStorageCapacity();
        double moisture      = m.getMoistureLevel();
        double monthlyDemand = demand / Math.max(1, months);

        for (int month = 1; month <= months; month++) {
            // Apply one month of compounded storage loss to remaining stock
            double remainingAfterLoss = storage.simulateMaizeLoss(
                    new Maize(currentStock, storageCap, moisture, currentPrice, monthlyLoss), 1);
            double waste  = currentStock - remainingAfterLoss;
            currentStock  = remainingAfterLoss;

            // Sell up to monthly demand from current stock
            double sold    = Math.min(currentStock, monthlyDemand);
            double revenue = supply.calculateRevenue(sold, currentPrice);
            currentStock  -= sold;

            // Economics and policy for this period
            double surplus = supply.calculateSurplus(remainingAfterLoss, monthlyDemand);
            currentPrice   = supply.adjustPrice(currentPrice, surplus, remainingAfterLoss);
            double fsi     = supply.calculateFoodSecurityIndicator(remainingAfterLoss, monthlyDemand);
            String alerts  = policy.evaluateAll(currentStock, surplus, remainingAfterLoss,
                                                month, -1, -1);

            records.add(new SimulationRecord(month, "Maize", remainingAfterLoss, monthlyDemand,
                    surplus, waste, revenue, fsi, alerts));
        }
        return records;
    }

    /**
     * Runs a single-period tomato simulation and returns a one-element list.
     * The Tomato object carries all commodity attributes including the
     * StorageType enum. pricePerTon is derived by the caller from
     * marketPricePerCrate * 40 (40 crates per ton at ~25 kg/crate).
     */
    public List<SimulationRecord> runTomatoSimulation(Tomato t, double demand,
                                                       int daysBeforeSale,
                                                       double pricePerTon) {
        List<SimulationRecord> records = new ArrayList<>();
        double waste    = storage.simulateTomatoSpoilage(t, daysBeforeSale);
        double saleable = Math.max(0.0, t.getProductionVolume() - waste);
        double sold     = Math.min(saleable, demand);
        double revenue  = supply.calculateRevenue(sold, pricePerTon);
        double surplus  = supply.calculateSurplus(saleable, demand);
        double fsi      = supply.calculateFoodSecurityIndicator(saleable, demand);
        String alerts   = policy.evaluateAll(Double.POSITIVE_INFINITY, surplus, saleable,
                                              -1, daysBeforeSale, t.getShelfLifeDays());

        records.add(new SimulationRecord(1, "Tomato", saleable, demand,
                surplus, waste, revenue, fsi, alerts));
        return records;
    }
}
