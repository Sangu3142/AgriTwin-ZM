package agritwin.model;

/**
 * MODULE: Tomato Model
 * PURPOSE: Represents a single tomato commodity record. Tomatoes are highly
 *          perishable, so this class also tracks shelf-life and storage type
 *          (cold vs open) which together drive the spoilage calculation.
 * OOP CONCEPTS USED: Encapsulation, Abstraction.
 * DATA STRUCTURES: POJO — placed inside ArrayList&lt;Tomato&gt; when needed.
 * ALGORITHMS: None directly; consumed by SupplyDemandEngine and
 *             StorageLossSimulator.
 */
public class Tomato {

    // --- Private attributes (encapsulation) ---
    private double productionVolume;     // tons produced this season
    private int shelfLifeDays;           // expected days before spoilage onset
    private String storageType;          // "cold" or "open"
    private double spoilageRate;         // % spoiled per day at base rate
    private double marketPricePerCrate;  // ZMW per crate (~25kg)

    /** Default constructor for use with later setter calls. */
    public Tomato() {
    }

    /** Full constructor — initialises every attribute. */
    public Tomato(double productionVolume,
                  int shelfLifeDays,
                  String storageType,
                  double spoilageRate,
                  double marketPricePerCrate) {
        this.productionVolume = productionVolume;
        this.shelfLifeDays = shelfLifeDays;
        this.storageType = storageType;
        this.spoilageRate = spoilageRate;
        this.marketPricePerCrate = marketPricePerCrate;
    }

    // --- Getters ---
    public double getProductionVolume() { return productionVolume; }
    public int getShelfLifeDays() { return shelfLifeDays; }
    public String getStorageType() { return storageType; }
    public double getSpoilageRate() { return spoilageRate; }
    public double getMarketPricePerCrate() { return marketPricePerCrate; }

    // --- Setters ---
    public void setProductionVolume(double productionVolume) { this.productionVolume = productionVolume; }
    public void setShelfLifeDays(int shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public void setSpoilageRate(double spoilageRate) { this.spoilageRate = spoilageRate; }
    public void setMarketPricePerCrate(double marketPricePerCrate) { this.marketPricePerCrate = marketPricePerCrate; }

    /** Human-readable summary used by ReportGenerator. */
    @Override
    public String toString() {
        return String.format(
            "Tomato[production=%.2ft, shelfLife=%dd, storage=%s, spoilage=%.2f%%/day, price=ZMW %.2f/crate]",
            productionVolume, shelfLifeDays, storageType, spoilageRate, marketPricePerCrate);
    }
}
