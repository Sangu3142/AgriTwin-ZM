package agritwin.model;

/**
 * MODULE: Maize Model
 * PURPOSE: Represents a single maize commodity record with all attributes
 *          relevant to a Zambian supply-chain simulation (production volume,
 *          storage capacity, moisture, price and monthly post-harvest loss).
 * OOP CONCEPTS USED: Encapsulation (private fields + public getters/setters),
 *                    Abstraction (hides representation behind a clean API).
 * DATA STRUCTURES: Plain Java Object (POJO) — used inside ArrayList&lt;Maize&gt;
 *                  collections elsewhere in the program.
 * ALGORITHMS: None directly; this class is a passive data carrier consumed
 *             by SupplyDemandEngine and StorageLossSimulator.
 */
public class Maize {

    // --- Private attributes (encapsulation) ---
    private double productionVolume;       // tons produced this season
    private double storageCapacity;        // tons available for storage
    private double moistureLevel;          // % moisture (affects spoilage)
    private double pricePerTon;            // farm-gate price in ZMW per ton
    private double lossPercentagePerMonth; // % lost per month in storage

    /** Default constructor — used when fields are set later via setters. */
    public Maize() {
    }

    /** Full constructor — initialises every attribute at once. */
    public Maize(double productionVolume,
                 double storageCapacity,
                 double moistureLevel,
                 double pricePerTon,
                 double lossPercentagePerMonth) {
        this.productionVolume = productionVolume;
        this.storageCapacity = storageCapacity;
        this.moistureLevel = moistureLevel;
        this.pricePerTon = pricePerTon;
        this.lossPercentagePerMonth = lossPercentagePerMonth;
    }

    // --- Getters: read-only access for service classes ---
    public double getProductionVolume() { return productionVolume; }
    public double getStorageCapacity() { return storageCapacity; }
    public double getMoistureLevel() { return moistureLevel; }
    public double getPricePerTon() { return pricePerTon; }
    public double getLossPercentagePerMonth() { return lossPercentagePerMonth; }

    // --- Setters: allow mutation when the simulation updates state ---
    public void setProductionVolume(double productionVolume) { this.productionVolume = productionVolume; }
    public void setStorageCapacity(double storageCapacity) { this.storageCapacity = storageCapacity; }
    public void setMoistureLevel(double moistureLevel) { this.moistureLevel = moistureLevel; }
    public void setPricePerTon(double pricePerTon) { this.pricePerTon = pricePerTon; }
    public void setLossPercentagePerMonth(double lossPercentagePerMonth) { this.lossPercentagePerMonth = lossPercentagePerMonth; }

    /** Human-readable single-line summary used by ReportGenerator. */
    @Override
    public String toString() {
        return String.format(
            "Maize[production=%.2ft, storage=%.2ft, moisture=%.2f%%, price=ZMW %.2f/t, monthlyLoss=%.2f%%]",
            productionVolume, storageCapacity, moistureLevel, pricePerTon, lossPercentagePerMonth);
    }
}
