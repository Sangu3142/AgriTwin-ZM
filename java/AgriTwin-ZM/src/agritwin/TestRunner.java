package agritwin;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.StorageType;
import agritwin.model.Tomato;
import agritwin.service.PolicyEngine;
import agritwin.service.ReportGenerator;
import agritwin.service.StorageLossSimulator;
import agritwin.service.SupplyDemandEngine;
import agritwin.util.FileHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MODULE: Test Runner (plain main, no JUnit)
 * PURPOSE: Exercises the eighteen test cases that cover the core simulation,
 *          storage, pricing, policy, seasonal demand, moisture-adjusted loss,
 *          loss schedules, aflatoxin detection, season summary aggregation,
 *          and CSV export logic. Each test prints PASS/FAIL with actual vs
 *          expected values so the report can quote the output verbatim.
 * OOP CONCEPTS USED: Composition (instantiates services); Encapsulation;
 *                    Inheritance (tests interact with Commodity subclasses).
 * DATA STRUCTURES: ArrayList<SimulationRecord> for the file-roundtrip test.
 * ALGORITHMS: Direct numerical assertions with a small epsilon tolerance.
 *
 * RUN: java -cp out agritwin.TestRunner
 */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static final double EPS = 0.5; // half-ton tolerance for floating-point math

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println(" AgriTwin-ZM v1.0 -- Test Runner");
        System.out.println("============================================================");

        test1_surplus();
        test2_priceAdjustmentOversupply();
        test3_maizeStorageLoss();
        test4_tomatoSpoilageColdVsOpen();
        test5_foodSecurityShortfall();
        test6_fraReserveAlert();
        test7_fileSaveAndReload();
        test8_zeroDaysReturnsZeroWaste();
        test9_negativeProductionThrows();
        test10_loadMissingFileThrows();
        test11_infiniteStockNoFraAlert();
        test12_zeroSurplusKeepsPrice();
        test13_seasonalDemandDryHigher();
        test14_moistureIncreasesMaizeLoss();
        test15_lossScheduleSizeMatchesMonths();
        test16_aflatoxinTriggers();
        test17_seasonSummaryAggregates();
        test18_csvExportContainsHeader();

        System.out.println("------------------------------------------------------------");
        System.out.printf(" Results: %d passed, %d failed%n", passed, failed);
        System.out.println("============================================================");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    // Test 1 -- surplus calculation
    // -----------------------------------------------------------------
    private static void test1_surplus() {
        SupplyDemandEngine eng = new SupplyDemandEngine();
        double actual = eng.calculateSurplus(10000, 8000);
        double expected = 2000.0;
        report("Test 1 (Surplus 10000-8000)", expected, actual, Math.abs(actual - expected) < EPS);
    }

    // -----------------------------------------------------------------
    // Test 2 -- price falls when there is surplus (oversupply)
    // -----------------------------------------------------------------
    private static void test2_priceAdjustmentOversupply() {
        SupplyDemandEngine eng = new SupplyDemandEngine();
        double currentPrice = 7000.0;
        double surplus      = 2000.0;
        double production   = 10000.0;
        double newPrice = eng.adjustPrice(currentPrice, surplus, production);
        boolean ok = newPrice < currentPrice;
        report("Test 2 (oversupply lowers price)", "<7000", String.format("%.2f", newPrice), ok);
    }

    // -----------------------------------------------------------------
    // Test 3 -- compounded storage loss: 10000 t * 0.97^3 ~= 9126.73
    // -----------------------------------------------------------------
    private static void test3_maizeStorageLoss() {
        StorageLossSimulator sim = new StorageLossSimulator();
        Maize m = new Maize(10000, 12000, 12.5, 7000, 3.0);
        double remaining = sim.simulateMaizeLoss(m, 3);
        double expected = 10000.0 * Math.pow(0.97, 3); // 9126.73
        boolean ok = Math.abs(remaining - expected) < 1.0;
        report("Test 3 (Maize 3% loss x3 months)", expected, remaining, ok);
    }

    // -----------------------------------------------------------------
    // Test 4 -- cold storage spoils less than open (now uses StorageType enum)
    // -----------------------------------------------------------------
    private static void test4_tomatoSpoilageColdVsOpen() {
        StorageLossSimulator sim = new StorageLossSimulator();
        Tomato cold = new Tomato(100, 14, StorageType.COLD, 5.0, 120);
        Tomato open = new Tomato(100, 14, StorageType.OPEN, 5.0, 120);
        double coldLoss = sim.simulateTomatoSpoilage(cold, 4);
        double openLoss = sim.simulateTomatoSpoilage(open, 4);
        boolean ok = coldLoss < openLoss;
        report("Test 4 (cold < open spoilage)",
                "cold<open", String.format("cold=%.2f open=%.2f", coldLoss, openLoss), ok);
    }

    // -----------------------------------------------------------------
    // Test 5 -- food security indicator falls below 100% when prod < demand
    // -----------------------------------------------------------------
    private static void test5_foodSecurityShortfall() {
        SupplyDemandEngine eng = new SupplyDemandEngine();
        double fsi = eng.calculateFoodSecurityIndicator(8000, 10000);
        boolean ok = fsi < 100.0 && fsi > 0;
        report("Test 5 (FSI when prod<demand)", "<100%", String.format("%.2f%%", fsi), ok);
    }

    // -----------------------------------------------------------------
    // Test 6 -- maize stock 400k triggers FRA reserve alert
    // -----------------------------------------------------------------
    private static void test6_fraReserveAlert() {
        PolicyEngine pol = new PolicyEngine();
        String msg = pol.checkFraReserve(400_000.0);
        boolean ok = msg.contains("RESERVE ALERT");
        report("Test 6 (FRA alert at 400k)", "RESERVE ALERT", msg, ok);
    }

    // -----------------------------------------------------------------
    // Test 7 -- save 2 records, reload, verify count and field values
    // -----------------------------------------------------------------
    private static void test7_fileSaveAndReload() {
        FileHandler fh = new FileHandler();
        List<SimulationRecord> in = new ArrayList<>();
        in.add(new SimulationRecord(1, "Maize",  10000, 8000, 2000, 300, 14_000_000, 100, "OK"));
        in.add(new SimulationRecord(2, "Tomato",   500,  480,   20,  25,     24_000, 100, "OK"));

        String tmpName = "test_simulation_" + System.currentTimeMillis() + ".dat";
        try {
            fh.saveSimulation(in, tmpName);
            List<SimulationRecord> out = fh.loadSimulation(tmpName);

            boolean countOk  = out.size() == in.size();
            boolean fieldsOk = countOk
                    && out.get(0).getCommodityName().equals("Maize")
                    && Math.abs(out.get(0).getProduction() - 10000) < EPS
                    && out.get(1).getCommodityName().equals("Tomato")
                    && Math.abs(out.get(1).getRevenue() - 24000) < EPS;
            report("Test 7 (save + reload roundtrip)",
                    "2 records, fields equal",
                    "loaded=" + out.size() + " fieldsOk=" + fieldsOk,
                    countOk && fieldsOk);
        } catch (Exception ex) {
            report("Test 7 (save + reload roundtrip)",
                    "no exception", "exception: " + ex.getMessage(), false);
        } finally {
            File f = new File(tmpName);
            if (f.exists() && !f.delete()) {
                System.err.println("(could not delete temp test file " + tmpName + ")");
            }
        }
    }

    // -----------------------------------------------------------------
    // Test 8 -- calculateWaste with zero days returns exactly 0.0
    // -----------------------------------------------------------------
    private static void test8_zeroDaysReturnsZeroWaste() {
        SupplyDemandEngine eng = new SupplyDemandEngine();
        Tomato t = new Tomato(100, 14, StorageType.OPEN, 5.0, 120);
        double waste = eng.calculateWaste(t, 0);
        report("Test 8 (zero days -> zero waste)", 0.0, waste, waste == 0.0);
    }

    // -----------------------------------------------------------------
    // Test 9 -- Maize.setProductionVolume(-1) throws IllegalArgumentException
    // -----------------------------------------------------------------
    private static void test9_negativeProductionThrows() {
        try {
            Maize m = new Maize();
            m.setProductionVolume(-1);
            report("Test 9 (negative production throws)",
                    "IllegalArgumentException", "no exception thrown", false);
        } catch (IllegalArgumentException ex) {
            report("Test 9 (negative production throws)",
                    "IllegalArgumentException", ex.getClass().getSimpleName(), true);
        }
    }

    // -----------------------------------------------------------------
    // Test 10 -- loadSimulation on a missing path throws IOException
    // -----------------------------------------------------------------
    private static void test10_loadMissingFileThrows() {
        FileHandler fh = new FileHandler();
        try {
            fh.loadSimulation("/nonexistent/path/agritwin_missing.dat");
            report("Test 10 (load missing file throws)",
                    "IOException", "no exception thrown", false);
        } catch (IOException ex) {
            report("Test 10 (load missing file throws)",
                    "IOException", ex.getClass().getSimpleName(), true);
        } catch (Exception ex) {
            report("Test 10 (load missing file throws)",
                    "IOException", ex.getClass().getName(), false);
        }
    }

    // -----------------------------------------------------------------
    // Test 11 -- infinite stock passes FRA reserve check
    // -----------------------------------------------------------------
    private static void test11_infiniteStockNoFraAlert() {
        PolicyEngine pol = new PolicyEngine();
        String result = pol.evaluateAll(Double.POSITIVE_INFINITY, 0, 1000, 3, -1, -1);
        boolean ok = result.contains("FRA RESERVE OK");
        report("Test 11 (infinite stock -> FRA RESERVE OK)",
                "contains 'FRA RESERVE OK'", result, ok);
    }

    // -----------------------------------------------------------------
    // Test 12 -- adjustPrice with zero surplus leaves price unchanged
    // -----------------------------------------------------------------
    private static void test12_zeroSurplusKeepsPrice() {
        SupplyDemandEngine eng = new SupplyDemandEngine();
        double result = eng.adjustPrice(7000, 0, 10000);
        report("Test 12 (zero surplus keeps price)", 7000.0, result, result == 7000.0);
    }

    // -----------------------------------------------------------------
    // Test 13 -- dry-season month returns higher demand than wet-season month
    // -----------------------------------------------------------------
    private static void test13_seasonalDemandDryHigher() {
        SupplyDemandEngine eng = new SupplyDemandEngine();
        double annualDemand = 120_000.0;
        // July (month 7) = peak dry season; January (month 1) = wet / harvest season
        double drySeason = eng.simulateSeasonalDemand(annualDemand, 7);
        double wetSeason = eng.simulateSeasonalDemand(annualDemand, 1);
        boolean ok = drySeason > wetSeason;
        report("Test 13 (dry-season demand > wet-season demand)",
                "dry>wet",
                String.format("dry=%.2f wet=%.2f", drySeason, wetSeason), ok);
    }

    // -----------------------------------------------------------------
    // Test 14 -- moisture > 14% causes more maize loss than moisture < 14%
    // -----------------------------------------------------------------
    private static void test14_moistureIncreasesMaizeLoss() {
        StorageLossSimulator sim = new StorageLossSimulator();
        // Same base loss rate 3%/month, only moisture differs
        Maize highMoisture = new Maize(10000, 12000, 18.0, 7000, 3.0);
        Maize lowMoisture  = new Maize(10000, 12000, 12.0, 7000, 3.0);
        double remainHigh = sim.simulateMaizeLoss(highMoisture, 1);
        double remainLow  = sim.simulateMaizeLoss(lowMoisture,  1);
        // High moisture raises effective rate → more loss → less remaining
        boolean ok = remainHigh < remainLow;
        report("Test 14 (moisture 18% leaves less than 12%)",
                "remainHigh < remainLow",
                String.format("high=%.2f low=%.2f", remainHigh, remainLow), ok);
    }

    // -----------------------------------------------------------------
    // Test 15 -- getMaizeLossSchedule(m, 6) returns list of size 7
    // -----------------------------------------------------------------
    private static void test15_lossScheduleSizeMatchesMonths() {
        StorageLossSimulator sim = new StorageLossSimulator();
        Maize m = new Maize(10000, 12000, 12.5, 7000, 3.0);
        List<Double> schedule = sim.getMaizeLossSchedule(m, 6);
        boolean ok = schedule.size() == 7; // months 0..6
        report("Test 15 (loss schedule size = 7 for 6 months)",
                7, schedule.size(), ok);
    }

    // -----------------------------------------------------------------
    // Test 16 -- aflatoxin triggers at moisture 16%, 3 months stored
    // -----------------------------------------------------------------
    private static void test16_aflatoxinTriggers() {
        PolicyEngine pol = new PolicyEngine();
        String result = pol.checkAflatoxinRisk(16.0, 3);
        boolean ok = result.contains("AFLATOXIN RISK HIGH");
        report("Test 16 (aflatoxin HIGH at moisture=16, months=3)",
                "AFLATOXIN RISK HIGH", result, ok);
    }

    // -----------------------------------------------------------------
    // Test 17 -- generateSeasonSummary total production = sum of both records
    // -----------------------------------------------------------------
    private static void test17_seasonSummaryAggregates() {
        ReportGenerator rg = new ReportGenerator();
        List<SimulationRecord> recs = new ArrayList<>();
        recs.add(new SimulationRecord(1, "Maize", 10000, 8000, 2000, 300, 70_000_000, 100, "FRA RESERVE OK"));
        recs.add(new SimulationRecord(2, "Maize",  8000, 7000, 1000, 200, 56_000_000, 100, "FRA RESERVE OK"));
        String summary = rg.generateSeasonSummary(recs);
        // Total production = 18 000.00 — check for both plain and comma-formatted variants
        boolean ok = summary.contains("18000") || summary.contains("18,000");
        report("Test 17 (season summary total production = 18000)",
                "contains 18000 or 18,000",
                summary.substring(0, Math.min(120, summary.length())), ok);
    }

    // -----------------------------------------------------------------
    // Test 18 -- exportToCsv writes a file whose first line is the header
    // -----------------------------------------------------------------
    private static void test18_csvExportContainsHeader() {
        ReportGenerator rg = new ReportGenerator();
        List<SimulationRecord> recs = new ArrayList<>();
        recs.add(new SimulationRecord(1, "Maize", 10000, 8000, 2000, 300, 70_000_000, 100, "OK"));
        String tmpName = "test_csv_" + System.currentTimeMillis() + ".csv";
        BufferedReader br = null;
        try {
            rg.exportToCsv(recs, tmpName);
            br = new BufferedReader(new FileReader(tmpName));
            String firstLine = br.readLine();
            boolean ok = firstLine != null && firstLine.startsWith("monthNumber,commodityName");
            report("Test 18 (CSV header starts with monthNumber,commodityName)",
                    "monthNumber,commodityName,...", firstLine, ok);
        } catch (Exception ex) {
            report("Test 18 (CSV header)",
                    "no exception", "exception: " + ex.getMessage(), false);
        } finally {
            if (br != null) try { br.close(); } catch (IOException ignored) {}
            File f = new File(tmpName);
            if (f.exists() && !f.delete()) {
                System.err.println("(could not delete temp CSV test file " + tmpName + ")");
            }
        }
    }

    // -----------------------------------------------------------------
    // Reporter helpers
    // -----------------------------------------------------------------
    private static void report(String name, Object expected, Object actual, boolean ok) {
        String tag = ok ? "PASS" : "FAIL";
        System.out.printf("  [%s] %-44s  expected=%s  actual=%s%n",
                tag, name, expected, actual);
        if (ok) passed++; else failed++;
    }
}
