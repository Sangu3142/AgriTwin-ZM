package agritwin;

import agritwin.model.Maize;
import agritwin.model.SimulationRecord;
import agritwin.model.StorageType;
import agritwin.model.Tomato;
import agritwin.service.PolicyEngine;
import agritwin.service.StorageLossSimulator;
import agritwin.service.SupplyDemandEngine;
import agritwin.util.FileHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MODULE: Test Runner (plain main, no JUnit)
 * PURPOSE: Exercises the twelve test cases that cover the core simulation,
 *          storage, pricing, policy and file-persistence logic. Each test
 *          prints PASS/FAIL with actual vs expected values so the report
 *          can quote the output verbatim.
 * OOP CONCEPTS USED: Composition (instantiates services); Encapsulation.
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
    // Test 11 -- infinite stock passes FRA reserve check (contains "FRA RESERVE OK")
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
    // Reporter helpers
    // -----------------------------------------------------------------
    private static void report(String name, Object expected, Object actual, boolean ok) {
        String tag = ok ? "PASS" : "FAIL";
        System.out.printf("  [%s] %-42s  expected=%s  actual=%s%n",
                tag, name, expected, actual);
        if (ok) passed++; else failed++;
    }
}
