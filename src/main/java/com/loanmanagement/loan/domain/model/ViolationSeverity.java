package com.loanmanagement.loan.domain.model;

/**
 * Violation Severity Enumeration
 * Defines the severity levels for business rule violations
 */
public enum ViolationSeverity {
    
    /**
     * Error - Prevents loan approval
     */
    ERROR("Error", "Violation prevents loan approval", 1, true),
    
    /**
     * Warning - Requires attention but may not prevent approval
     */
    WARNING("Warning", "Violation requires attention but may not prevent approval", 2, false),
    
    /**
     * Info - Informational only, does not impact approval
     */
    INFO("Info", "Informational violation, does not impact approval", 3, false);

    private final String displayName;
    private final String description;
    private final int priority; // 1 = highest priority
    private final boolean blocking;

    ViolationSeverity(String displayName, String description, int priority, boolean blocking) {
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
        this.blocking = blocking;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Check if this severity is more severe than another
     */
    public boolean isMoreSevereThan(ViolationSeverity other) {
        return this.priority < other.priority;
    }

    /**
     * Check if this severity is less severe than another
     */
    public boolean isLessSevereThan(ViolationSeverity other) {
        return this.priority > other.priority;
    }

    /**
     * Check if this severity prevents loan approval
     */
    public boolean preventsApproval() {
        return blocking;
    }

    /**
     * Get CSS class name for UI styling
     */
    public String getCssClass() {
        return switch (this) {
            case ERROR -> "severity-error";
            case WARNING -> "severity-warning";
            case INFO -> "severity-info";
        };
    }

    /**
     * Get icon representation
     */
    public String getIcon() {
        return switch (this) {
            case ERROR -> "❌";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };
    }

    /**
     * Get color code for UI representation
     */
    public String getColorCode() {
        return switch (this) {
            case ERROR -> "#DC3545"; // Red
            case WARNING -> "#FFC107"; // Yellow
            case INFO -> "#17A2B8"; // Blue
        };
    }

    /**
     * Get the most severe severity from a collection
     */
    public static ViolationSeverity mostSevere(ViolationSeverity... severities) {
        ViolationSeverity mostSevere = INFO;
        for (ViolationSeverity severity : severities) {
            if (severity.isMoreSevereThan(mostSevere)) {
                mostSevere = severity;
            }
        }
        return mostSevere;
    }

    /**
     * Check if any of the provided severities is blocking
     */
    public static boolean hasBlocking(ViolationSeverity... severities) {
        for (ViolationSeverity severity : severities) {
            if (severity.isBlocking()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return displayName;
    }
}