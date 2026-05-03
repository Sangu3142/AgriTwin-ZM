package agritwin.util;

import agritwin.model.SimulationRecord;

import java.io.IOException;
import java.util.List;

/**
 * MODULE: RecordStore Interface (persistence seam)
 * PURPOSE: Defines the contract for saving and loading simulation records.
 *          Any class that implements this interface can substitute for
 *          FileHandler, making it straightforward to swap in a database-backed
 *          store (SQLite, PostgreSQL) in a future v2.0 without touching any
 *          UI or service code.
 * OOP CONCEPTS USED: Interface (Polymorphism) — callers (ConsoleUI, GuiApp,
 *                    ReportGenerator) declare their dependency as RecordStore,
 *                    not the concrete FileHandler. This is the
 *                    Dependency-Inversion Principle: high-level modules depend
 *                    on abstractions, not implementations.
 *                    Abstraction — only the contract is visible; the storage
 *                    medium is an implementation detail.
 * DATA STRUCTURES: List<SimulationRecord>.
 * ALGORITHMS: None — this is a pure contract definition with no logic.
 */
public interface RecordStore {

    /**
     * Persists all records to the given path (creates or overwrites).
     *
     * @param records non-null list of records to save
     * @param path    file path to write to
     * @throws IOException if the path cannot be written
     */
    void saveSimulation(List<SimulationRecord> records, String path) throws IOException;

    /**
     * Loads records previously written by saveSimulation.
     *
     * @param path file path to read from
     * @return ordered list of records; never null
     * @throws IOException if the path cannot be read
     */
    List<SimulationRecord> loadSimulation(String path) throws IOException;
}
