package agritwin.model;

/**
 * MODULE: Simulation Record (snapshot)
 * PURPOSE: Holds one month's snapshot of a simulation run for any commodity:
 *          production, demand, surplus/shortage, waste, revenue, food-security
 *          indicator and any policy alerts triggered. Used both for reporting
 *          and for serialising results to disk.
 * OOP CONCEPTS USED: Encapsulation; acts as a Data Transfer Object (DTO)
 *                    between service classes and the UI / file layers.
 * DATA STRUCTURES: POJO; aggregated inside ArrayList&lt;SimulationRecord&gt;.
 * ALGORITHMS: None — values are computed elsewhere and stored here.
 */
public class SimulationRecord {

    // --- Snapshot fields ---
    private int monthNumber;              // sequential month index (1..N)
    private String commodityName;         // "Maize" or "Tomato"
    private double production;            // tons produced this period
    private double demand;                // tons demanded this period
    private double surplusOrShortage;     // + means surplus, - means shortage
    private double waste;                 // tons lost to spoilage / storage loss
    private double revenue;               // ZMW earned from sold volume
    private double foodSecurityIndicator; // % production / demand (capped 100)
    private String policyAlerts;          // concatenated policy-engine messages

    /** Default constructor — needed when fields are populated incrementally. */
    public SimulationRecord() {
    }

    /** Full constructor — used by the simulation engines to build a record. */
    public SimulationRecord(int monthNumber,
                            String commodityName,
                            double production,
                            double demand,
                            double surplusOrShortage,
                            double waste,
                            double revenue,
                            double foodSecurityIndicator,
                            String policyAlerts) {
        this.monthNumber = monthNumber;
        this.commodityName = commodityName;
        this.production = production;
        this.demand = demand;
        this.surplusOrShortage = surplusOrShortage;
        this.waste = waste;
        this.revenue = revenue;
        this.foodSecurityIndicator = foodSecurityIndicator;
        this.policyAlerts = policyAlerts;
    }

    // --- Getters ---
    public int getMonthNumber() { return monthNumber; }
    public String getCommodityName() { return commodityName; }
    public double getProduction() { return production; }
    public double getDemand() { return demand; }
    public double getSurplusOrShortage() { return surplusOrShortage; }
    public double getWaste() { return waste; }
    public double getRevenue() { return revenue; }
    public double getFoodSecurityIndicator() { return foodSecurityIndicator; }
    public String getPolicyAlerts() { return policyAlerts; }

    // --- Setters ---
    public void setMonthNumber(int monthNumber) { this.monthNumber = monthNumber; }
    public void setCommodityName(String commodityName) { this.commodityName = commodityName; }
    public void setProduction(double production) { this.production = production; }
    public void setDemand(double demand) { this.demand = demand; }
    public void setSurplusOrShortage(double surplusOrShortage) { this.surplusOrShortage = surplusOrShortage; }
    public void setWaste(double waste) { this.waste = waste; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
    public void setFoodSecurityIndicator(double foodSecurityIndicator) { this.foodSecurityIndicator = foodSecurityIndicator; }
    public void setPolicyAlerts(String policyAlerts) { this.policyAlerts = policyAlerts; }

    /**
     * CSV-style line used by FileHandler.saveSimulation.
     * Fields are pipe-delimited because alerts can themselves contain commas.
     */
    public String toCsvLine() {
        return monthNumber + "|" + commodityName + "|" + production + "|" + demand + "|"
                + surplusOrShortage + "|" + waste + "|" + revenue + "|"
                + foodSecurityIndicator + "|" + (policyAlerts == null ? "" : policyAlerts);
    }

    /** Reverse of toCsvLine — used by FileHandler.loadSimulation. */
    public static SimulationRecord fromCsvLine(String line) {
        String[] parts = line.split("\\|", -1); // -1 keeps trailing empties
        if (parts.length < 9) {
            throw new IllegalArgumentException("Malformed simulation line: " + line);
        }
        return new SimulationRecord(
            Integer.parseInt(parts[0].trim()),
            parts[1].trim(),
            Double.parseDouble(parts[2].trim()),
            Double.parseDouble(parts[3].trim()),
            Double.parseDouble(parts[4].trim()),
            Double.parseDouble(parts[5].trim()),
            Double.parseDouble(parts[6].trim()),
            Double.parseDouble(parts[7].trim()),
            parts[8]
        );
    }

    /** Friendly multi-line representation used by reports / GUI table cells. */
    @Override
    public String toString() {
        return String.format(
            "Month %d | %s | Prod=%.2f | Demand=%.2f | Net=%.2f | Waste=%.2f | Revenue=ZMW %.2f | FSI=%.2f%% | Alerts=[%s]",
            monthNumber, commodityName, production, demand, surplusOrShortage,
            waste, revenue, foodSecurityIndicator,
            policyAlerts == null ? "" : policyAlerts);
    }
}
