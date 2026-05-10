package agritwin.model;

/**
 * MODULE: Storage Type Enumeration
 * PURPOSE: Type-safe representation of tomato storage options. Replaces the
 *          error-prone free-text String that previously held this value;
 *          mis-spellings (e.g. "Cold", "COLD", "refrigerated") are now caught
 *          at compile time rather than silently falling through to open-storage
 *          behaviour at runtime.
 * OOP CONCEPTS USED: Enumeration — a Java-specific OOP construct that provides
 *                    a fixed, named set of constants (a restricted form of class).
 *                    Demonstrates type-safety and Abstraction.
 * DATA STRUCTURES: Enum constants — referenced directly as StorageType.COLD
 *                  or StorageType.OPEN throughout the simulation layer.
 * ALGORITHMS: None — this is a type definition, not a computation.
 */
public enum StorageType {
    COLD, OPEN
}
