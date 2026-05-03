package agritwin.model;

/**
 * MODULE: Maize Model
 * PURPOSE: Represents a single maize commodity record with all attributes
 * relevant to a Zambian supply-chain simulation (production volume,
 * storage capacity, moisture, price and monthly post-harvest loss).
 * OOP CONCEPTS USED: Encapsulation (private fields + public getters/setters);
 * Abstraction (hides representation behind a clean API);
 * Input validation enforced at every setter — negative
 * volumes, capacities and rates are illegal and throw
 * IllegalArgumentException to maintain object invariants.
 * DATA STRUCTURES: Plain Java Object (POJO) — used inside ArrayList<Maize>
 * collections elsewhere in the program.
 * ALGORITHMS: None directly; this class is a passive data carrier consumed
 * by SupplyDemandEngine and StorageLossSimulator.
 */
public class Maize {

    // --- Private attributes (encapsulation) ---
    private double productionVolume; // tons produced this season
    private double storageCapacity; // tons available for storage
    private double moistureLevel; // % moisture (affects spoilage)
    private double pricePerTon; // farm-gate price in ZMW per ton
    private double lossPercentagePerMonth; // % lost per month in storage

    /** Default constructor — used when fields are set later via setters. */
    public Maize() {
    }

    /**
     * Full constructor — delegates to validated setters so every attribute
     * check fires regardless of whether the constructor or a standalone setter
     * is used.
     */
    public Maize(double productionVolume,
            double storageCapacity,
            double moistureLevel,
            double pricePerTon,
            double lossPercentagePerMonth) {
        setProductionVolume(productionVolume);
        setStorageCapacity(storageCapacity);
        setMoistureLevel(moistureLevel);
        setPricePerTon(pricePerTon);
        setLossPercentagePerMonth(lossPercentagePerMonth);
    }

    // --- Getters: read-only access for service classes ---
    public double getProductionVolume() {
        return productionVolume;
    }

    public double getStorageCapacity() {
        return storageCapacity;
    }

    public double getMoistureLevel() {
        return moistureLevel;
    }

    public double getPricePerTon() {
        return pricePerTon;
    }

    public double getLossPercentagePerMonth() {
        return lossPercentagePerMonth;
    }

    // --- Setters with validation ---
    public void setProductionVolume(double productionVolume) {
        if (productionVolume < 0)
            throw new IllegalArgumentException("production volume must be >= 0");
        this.productionVolume = productionVolume;
    }

    public void setStorageCapacity(double storageCapacity) {
        if (storageCapacity < 0)
            throw new IllegalArgumentException("storage capacity must be >= 0");
        this.storageCapacity = storageCapacity;
    }

    public void setMoistureLevel(double moistureLevel) {
        if (moistureLevel < 0)
            throw new IllegalArgumentException("moisture level must be >= 0");
        this.moistureLevel = moistureLevel;
    }

    public void setPricePerTon(double pricePerTon) {
        if (pricePerTon < 0)
            throw new IllegalArgumentException("price per ton must be >= 0");
        this.pricePerTon = pricePerTon;
    }

    public void setLossPercentagePerMonth(double lossPercentagePerMonth) {
        if (lossPercentagePerMonth < 0)
            throw new IllegalArgumentException("loss percentage per month must be >= 0");
        this.lossPercentagePerMonth = lossPercentagePerMonth;
    }

    /** Human-readable single-line summary used by ReportGenerator. */
    @Override
    public String toString() {
        return String.format(
                "Maize[production=%.2ft, storage=%.2ft, moisture=%.2f%%, price=ZMW %.2f/t, monthlyLoss=%.2f%%]",
                productionVolume, storageCapacity, moistureLevel, pricePerTon, lossPercentagePerMonth);
    }
}
