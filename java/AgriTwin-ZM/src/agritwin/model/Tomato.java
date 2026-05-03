package agritwin.model;

/**
 * MODULE: Tomato Model
 * PURPOSE: Represents a single tomato commodity record. Tomatoes are highly
 *          perishable, so this class tracks shelf-life and storage type
 *          (cold vs open) as a type-safe enum, which drives the spoilage
 *          calculation in the service layer.
 * OOP CONCEPTS USED: Encapsulation; Abstraction; Type-safety via the
 *                    StorageType enum (eliminates mis-spelling bugs that a
 *                    plain String field cannot prevent at compile time);
 *                    Input validation in setters enforces non-negative values.
 * DATA STRUCTURES: POJO — placed inside ArrayList<Tomato> when needed.
 * ALGORITHMS: None directly; consumed by SupplyDemandEngine and
 *             StorageLossSimulator.
 */
public class Tomato {

    // --- Private attributes (encapsulation) ---
    private double productionVolume;     // tons produced this season
    private int shelfLifeDays;           // expected days before spoilage onset
    private StorageType storageType;     // COLD or OPEN (enum, not String)
    private double spoilageRate;         // % spoiled per day at base rate
    private double marketPricePerCrate;  // ZMW per crate (~25 kg)

    /** Default constructor for use with later setter calls. */
    public Tomato() {
    }

    /**
     * Full constructor — delegates to validated setters so every attribute
     * check fires regardless of whether the constructor or a standalone setter
     * is used.
     */
    public Tomato(double productionVolume,
                  int shelfLifeDays,
                  StorageType storageType,
                  double spoilageRate,
                  double marketPricePerCrate) {
        setProductionVolume(productionVolume);
        setShelfLifeDays(shelfLifeDays);
        setStorageType(storageType);
        setSpoilageRate(spoilageRate);
        setMarketPricePerCrate(marketPricePerCrate);
    }

    // --- Getters ---
    public double      getProductionVolume()   { return productionVolume; }
    public int         getShelfLifeDays()      { return shelfLifeDays; }
    public StorageType getStorageType()        { return storageType; }
    public double      getSpoilageRate()       { return spoilageRate; }
    public double      getMarketPricePerCrate(){ return marketPricePerCrate; }

    // --- Setters with validation ---
    public void setProductionVolume(double productionVolume) {
        if (productionVolume < 0)
            throw new IllegalArgumentException("production volume must be >= 0");
        this.productionVolume = productionVolume;
    }

    public void setShelfLifeDays(int shelfLifeDays) {
        if (shelfLifeDays < 0)
            throw new IllegalArgumentException("shelf life days must be >= 0");
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public void setSpoilageRate(double spoilageRate) {
        if (spoilageRate < 0)
            throw new IllegalArgumentException("spoilage rate must be >= 0");
        this.spoilageRate = spoilageRate;
    }

    public void setMarketPricePerCrate(double marketPricePerCrate) {
        if (marketPricePerCrate < 0)
            throw new IllegalArgumentException("market price per crate must be >= 0");
        this.marketPricePerCrate = marketPricePerCrate;
    }

    /** Human-readable summary used by ReportGenerator. */
    @Override
    public String toString() {
        return String.format(
            "Tomato[production=%.2ft, shelfLife=%dd, storage=%s, spoilage=%.2f%%/day, price=ZMW %.2f/crate]",
            productionVolume, shelfLifeDays, storageType, spoilageRate, marketPricePerCrate);
    }
}
