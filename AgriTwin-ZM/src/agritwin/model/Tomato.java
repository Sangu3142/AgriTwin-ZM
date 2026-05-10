package agritwin.model;

/**
 * MODULE: Tomato Model
 * PURPOSE: Represents a single tomato commodity record. Tomatoes are highly
 *          perishable, so this class tracks shelf-life and storage type
 *          (cold vs open) as a type-safe enum, which drives the spoilage
 *          calculation in the service layer.
 * OOP CONCEPTS USED: Encapsulation; Abstraction; Inheritance — extends Commodity
 *                    to share productionVolume / price fields and their
 *                    validation, avoiding code duplication with Maize;
 *                    Polymorphism — getPricePerTon() is overridden to document
 *                    the crate-to-ton derivation (crate price × 40);
 *                    Type-safety via the StorageType enum (eliminates
 *                    mis-spelling bugs that a plain String field cannot prevent
 *                    at compile time).
 * DATA STRUCTURES: POJO — placed inside ArrayList<Tomato> (or ArrayList<Commodity>)
 * ALGORITHMS: None directly; consumed by SupplyDemandEngine and
 *             StorageLossSimulator.
 */
public class Tomato extends Commodity {

    // --- Tomato-specific attributes (productionVolume and price are in Commodity) ---
    private int shelfLifeDays;           // expected days before spoilage onset
    private StorageType storageType;     // COLD or OPEN (enum, not String)
    private double spoilageRate;         // % spoiled per day at base rate
    private double marketPricePerCrate;  // ZMW per crate (~25 kg)

    // price (inherited from Commodity) is kept equal to marketPricePerCrate * 40
    // via setMarketPricePerCrate, so getPricePerTon() always returns the correct value.

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
        if (productionVolume < 0)       throw new IllegalArgumentException("production volume must be >= 0");
        if (shelfLifeDays < 0)          throw new IllegalArgumentException("shelf life days must be >= 0");
        if (spoilageRate < 0)           throw new IllegalArgumentException("spoilage rate must be >= 0");
        if (marketPricePerCrate < 0)    throw new IllegalArgumentException("market price per crate must be >= 0");
        this.productionVolume       = productionVolume;
        this.shelfLifeDays          = shelfLifeDays;
        this.storageType            = storageType;
        this.spoilageRate           = spoilageRate;
        this.marketPricePerCrate    = marketPricePerCrate;
        this.price                  = marketPricePerCrate * 40.0;
    }

    // --- Getters ---
    // getProductionVolume() is public final, inherited from Commodity
    public int         getShelfLifeDays()       { return shelfLifeDays; }
    public StorageType getStorageType()         { return storageType; }
    public double      getSpoilageRate()        { return spoilageRate; }
    public double      getMarketPricePerCrate() { return marketPricePerCrate; }

    /**
     * Returns the per-ton price derived from the crate price.
     * One metric ton of tomatoes ≈ 40 crates at ~25 kg each.
     * Overrides Commodity.getPricePerTon() to document the derivation explicitly.
     */
    @Override
    public double getPricePerTon() {
        return marketPricePerCrate * 40.0;
    }

    // --- Setters with validation ---
    // setProductionVolume(double) is inherited (with validation) from Commodity

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
        this.price = marketPricePerCrate * 40.0; // keep inherited price field in sync
    }

    /** Total value of this harvest at the current per-ton price. */
    @Override
    public double estimateTotalValue() {
        return productionVolume * getPricePerTon();
    }

    /** Commodity label used in SimulationRecord and reports. */
    @Override
    public String getCommodityName() {
        return "Tomato";
    }

    /** Human-readable summary used by ReportGenerator. */
    @Override
    public String toString() {
        return String.format(
            "Tomato[production=%.2ft, shelfLife=%dd, storage=%s, spoilage=%.2f%%/day, price=ZMW %.2f/crate]",
            productionVolume, shelfLifeDays, storageType, spoilageRate, marketPricePerCrate);
    }
}
