package agritwin.service;

/**
 * MODULE: Severity Enumeration
 * PURPOSE: Type-safe severity levels for policy alerts produced by PolicyEngine.
 *          Distinguishes routine status messages (INFO) from advisories that
 *          require attention (WARNING) and conditions requiring immediate action
 *          (CRITICAL), enabling the reporting layer to filter and count alerts
 *          by urgency without parsing message text.
 * OOP CONCEPTS USED: Enumeration — a Java-specific OOP construct that provides
 *                    a fixed, named set of constants (a restricted form of class).
 *                    Demonstrates type-safety and Abstraction.
 * DATA STRUCTURES: Enum constants — referenced directly as Severity.INFO,
 *                  Severity.WARNING, or Severity.CRITICAL in PolicyEngine.Alert.
 * ALGORITHMS: None — this is a type definition, not a computation.
 */
public enum Severity {
    INFO,
    WARNING,
    CRITICAL
}
