package agritwin.ui.model;

public class ReportSummary {
    private final String reportName;
    private final String filePath;
    private final String commodityType;
    private final String dateSaved;
    private final double totalRevenue;
    private final double avgFoodSecurity;
    private final int    recordCount;

    public ReportSummary(String reportName, String filePath, String commodityType,
                         String dateSaved, double totalRevenue, double avgFoodSecurity,
                         int recordCount) {
        this.reportName      = reportName;
        this.filePath        = filePath;
        this.commodityType   = commodityType;
        this.dateSaved       = dateSaved;
        this.totalRevenue    = totalRevenue;
        this.avgFoodSecurity = avgFoodSecurity;
        this.recordCount     = recordCount;
    }

    public String getReportName()      { return reportName; }
    public String getFilePath()        { return filePath; }
    public String getCommodityType()   { return commodityType; }
    public String getDateSaved()       { return dateSaved; }
    public double getTotalRevenue()    { return totalRevenue; }
    public double getAvgFoodSecurity() { return avgFoodSecurity; }
    public int    getRecordCount()     { return recordCount; }
}
