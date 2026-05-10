package agritwin.util;

import agritwin.model.SimulationRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * MODULE: File Handler (utility)
 * PURPOSE: Persists simulation results to disk and reads them back. Uses a
 *          simple pipe-delimited text format so files are human-readable;
 *          SimulationRecord knows how to encode/decode its own line.
 * OOP CONCEPTS USED: Encapsulation; Single Responsibility (only I/O lives
 *                    here, no business logic); Interface implementation —
 *                    FileHandler implements RecordStore, enabling callers to
 *                    depend on the abstract type and swap implementations
 *                    (e.g. a SQLite store) without changing calling code.
 * DATA STRUCTURES: ArrayList<SimulationRecord>.
 * ALGORITHMS: Linear write / read of records; defensive header line so
 *             accidentally opened files explain themselves; malformed lines
 *             are skipped and logged rather than aborting the entire load.
 */
public class FileHandler implements RecordStore {

    private static final Logger LOG = Logger.getLogger(FileHandler.class.getName());

    private static final String HEADER =
            "# AgriTwin-ZM v1.0 simulation file (pipe-delimited)\n"
          + "# month|commodity|production|demand|surplusOrShortage|waste|revenue|foodSecurityIndicator|policyAlerts";

    /** Writes every record (one per line) to the given path. Overwrites. */
    @Override
    public void saveSimulation(List<SimulationRecord> records, String path) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println(HEADER);
            for (SimulationRecord r : records) {
                pw.println(r.toCsvLine());
            }
        }
    }

    /**
     * Reads a previously saved file and returns the list of records.
     * Lines starting with '#' are treated as comments and skipped, as are
     * blank lines. Malformed data lines are logged as warnings and skipped
     * rather than aborting the load.
     */
    @Override
    public List<SimulationRecord> loadSimulation(String path) throws IOException {
        List<SimulationRecord> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                try {
                    out.add(SimulationRecord.fromCsvLine(trimmed));
                } catch (RuntimeException ex) {
                    LOG.warning("Skipping malformed line: " + trimmed
                            + " (" + ex.getMessage() + ")");
                }
            }
        }
        return out;
    }
}
