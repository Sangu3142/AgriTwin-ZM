package agritwin.model;

/**
 * MODULE: Commodity (abstract base)
 * PURPOSE: Common supertype for all simulated agricultural commodities.
 *          Eliminates code duplication between Maize and Tomato by centralising
 *          the shared productionVolume / price fields and their validation, so
 *          future commodities (cassava, groundnuts) extend this class and plug
 *          in without repeating the same boilerplate.
 * OOP CONCEPTS USED: Inheritance — Maize and Tomato extend this class, reusing
 *                    shared state and validation without duplication;
 *                    Abstraction — abstract methods force each subclass to
 *                    declare its name and value formula;
 *                    Encapsulation — protected fields accessible to subclasses
 *                    but hidden from unrelated code;
 *                    Polymorphism — SimulationRunner can hold Commodity
 *                    references and call estimateTotalValue() / getCommodityName()
 *                    without knowing the concrete type.
 * DATA STRUCTURES: None — acts as a typed root node in the commodity hierarchy.
 * ALGORITHMS: None — computation delegated to subclasses via abstract methods.
 */
public abstract class Commodity {

    /** Tons produced this season — shared by all commodity types. */
    protected double productionVolume;

    /**
     * Farm-gate or derived price in ZMW per ton. For Maize this equals the
     * farm-gate price directly; for Tomato it is kept in sync with
     * marketPricePerCrate * 40 via setMarketPricePerCrate.
     */
    protected double price;

    /**
     * Returns the production volume. Declared final so no subclass can
     * accidentally shadow the field with a differently-scaled getter.
     */
    public final double getProductionVolume() {
        return productionVolume;
    }

    /**
     * Validates and sets the production volume. Declared once here so the
     * single authoritative check replaces the identical guards that previously
     * lived in both Maize.setProductionVolume and Tomato.setProductionVolume.
     */
    public void setProductionVolume(double v) {
        if (v < 0)
            throw new IllegalArgumentException("production volume must be >= 0");
        this.productionVolume = v;
    }

    /**
     * Returns the price per ton in ZMW. Non-abstract — subclasses either
     * inherit it directly (Maize) or override it to document the derivation
     * (Tomato multiplies marketPricePerCrate by 40).
     */
    public double getPricePerTon() {
        return price;
    }

    /**
     * Total market value of the entire harvest at current prices.
     * Formula: productionVolume × price-per-ton. Abstract so each subclass
     * documents its own price derivation explicitly.
     *
     * @return ZMW value of full production at the current unit price
     */
    public abstract double estimateTotalValue();

    /**
     * Human-readable commodity name used in reports and SimulationRecord labels.
     *
     * @return "Maize", "Tomato", or any future commodity name
     */
    public abstract String getCommodityName();
}
