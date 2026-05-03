package agritwin.model;

/**
 * MODULE: Maize Model
 * PURPOSE: Represents a single maize commodity record with all attributes
 *          relevant to a Zambian supply-chain simulation (production volume,
 *          storage capacity, moisture, price and monthly post-harvest loss).
 * OOP CONCEPTS USED: Encapsulation (private fields + public getters/setters);
 *                    Abstraction (hides representation behind a clean API);
 *                    Inheritance — extends Commodity to share the
 *                    productionVolume / price fields and their validation,
 *                    avoiding code duplication with Tomato.  Input validation
 *                    for production volume is inherited from Commodity; all
 *                    other setters throw IllegalArgumentException to maintain
 *                    object invariants.
 * DATA STRUCTURES: POJO — used inside ArrayList<Maize> (or ArrayList<Commodity>)
 *                  collections elsewhere in the program.
 * ALGORITHMS: None directly; consumed by SupplyDemandEngine and
 *             StorageLossSimulator.
 */
public class Maize extends Commodity {

    // --- Maize-specific attributes (productionVolume and price are in Commodity) ---
    private double storageCapacity;        // tons available for storage
    private double moistureLevel;          // % moisture (affects spoilage)
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
        setProductionVolume(productionVolume); // inherited validation from Commodity
        setStorageCapacity(storageCapacity);
        setMoistureLevel(moistureLevel);
        setPricePerTon(pricePerTon);
        setLossPercentagePerMonth(lossPercentagePerMonth);
    }

    // --- Getters ---
    // getProductionVolume() is public final, inherited from Commodity
    public double getStorageCapacity()        { return storageCapacity; }
    public double getMoistureLevel()          { return moistureLevel; }
    // getPricePerTon() is inherited from Commodity (returns this.price)
    public double getLossPercentagePerMonth() { return lossPercentagePerMonth; }

    // --- Setters with validation ---
    // setProductionVolume(double) is inherited (with validation) from Commodity

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
        this.price = pricePerTon; // write to the inherited protected field
    }

    public void setLossPercentagePerMonth(double lossPercentagePerMonth) {
        if (lossPercentagePerMonth < 0)
            throw new IllegalArgumentException("loss percentage per month must be >= 0");
        this.lossPercentagePerMonth = lossPercentagePerMonth;
    }

    /** Total value of this harvest at the current farm-gate price. */
    @Override
    public double estimateTotalValue() {
        return productionVolume * price;
    }

    /** Commodity label used in SimulationRecord and reports. */
    @Override
    public String getCommodityName() {
        return "Maize";
    }

    /** Human-readable single-line summary used by ReportGenerator. */
    @Override
    public String toString() {
        return String.format(
                "Maize[production=%.2ft, storage=%.2ft, moisture=%.2f%%, price=ZMW %.2f/t, monthlyLoss=%.2f%%]",
                productionVolume, storageCapacity, moistureLevel, price, lossPercentagePerMonth);
    }
}
