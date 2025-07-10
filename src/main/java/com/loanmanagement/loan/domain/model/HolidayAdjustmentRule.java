package com.loanmanagement.loan.domain.model;

/**
 * Enumeration of holiday adjustment rules for payment schedules.
 */
public enum HolidayAdjustmentRule {
    
    /**
     * No adjustment - payment due on original date regardless of holiday.
     */
    NO_ADJUSTMENT("No Adjustment"),
    
    /**
     * Move to following business day if due date falls on holiday.
     */
    FOLLOWING_BUSINESS_DAY("Following Business Day"),
    
    /**
     * Move to preceding business day if due date falls on holiday.
     */
    PRECEDING_BUSINESS_DAY("Preceding Business Day"),
    
    /**
     * Move to modified following business day (following, unless that pushes into next month, then preceding).
     */
    MODIFIED_FOLLOWING("Modified Following Business Day"),
    
    /**
     * Move to modified preceding business day (preceding, unless that pushes into previous month, then following).
     */
    MODIFIED_PRECEDING("Modified Preceding Business Day"),
    
    /**
     * Move to nearest business day (whichever is closer).
     */
    NEAREST_BUSINESS_DAY("Nearest Business Day"),
    
    /**
     * Move to end of month if due date falls on holiday.
     */
    END_OF_MONTH("End of Month"),
    
    /**
     * Grace period - allow payment within specified days without penalty.
     */
    GRACE_PERIOD("Grace Period"),
    
    /**
     * Automatic extension - extend due date by specified number of days.
     */
    AUTOMATIC_EXTENSION("Automatic Extension"),
    
    /**
     * Weekend adjustment - move weekend due dates to Monday.
     */
    WEEKEND_TO_MONDAY("Weekend to Monday"),
    
    /**
     * Weekend adjustment - move weekend due dates to Friday.
     */
    WEEKEND_TO_FRIDAY("Weekend to Friday"),
    
    /**
     * Federal holiday adjustment - adjust for federal holidays only.
     */
    FEDERAL_HOLIDAYS_ONLY("Federal Holidays Only"),
    
    /**
     * State holiday adjustment - adjust for state holidays.
     */
    STATE_HOLIDAYS("State Holidays"),
    
    /**
     * Bank holiday adjustment - adjust for bank holidays.
     */
    BANK_HOLIDAYS("Bank Holidays"),
    
    /**
     * Religious holiday adjustment - adjust for religious holidays.
     */
    RELIGIOUS_HOLIDAYS("Religious Holidays"),
    
    /**
     * Regional holiday adjustment - adjust for regional holidays.
     */
    REGIONAL_HOLIDAYS("Regional Holidays"),
    
    /**
     * Custom holiday calendar - use predefined custom holiday calendar.
     */
    CUSTOM_CALENDAR("Custom Calendar"),
    
    /**
     * Business day convention for T+0 settlement.
     */
    SAME_BUSINESS_DAY("Same Business Day"),
    
    /**
     * Business day convention for T+1 settlement.
     */
    NEXT_BUSINESS_DAY("Next Business Day"),
    
    /**
     * Month-end adjustment - if original date is month-end, keep month-end after adjustment.
     */
    MONTH_END_STICKY("Month End Sticky"),
    
    /**
     * Quarter-end adjustment - special handling for quarter-end dates.
     */
    QUARTER_END_ADJUSTMENT("Quarter End Adjustment"),
    
    /**
     * Year-end adjustment - special handling for year-end dates.
     */
    YEAR_END_ADJUSTMENT("Year End Adjustment");
    
    private final String description;
    
    HolidayAdjustmentRule(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this rule adjusts for weekends.
     */
    public boolean adjustsForWeekends() {
        return this == FOLLOWING_BUSINESS_DAY ||
               this == PRECEDING_BUSINESS_DAY ||
               this == MODIFIED_FOLLOWING ||
               this == MODIFIED_PRECEDING ||
               this == NEAREST_BUSINESS_DAY ||
               this == WEEKEND_TO_MONDAY ||
               this == WEEKEND_TO_FRIDAY ||
               this == SAME_BUSINESS_DAY ||
               this == NEXT_BUSINESS_DAY;
    }
    
    /**
     * Checks if this rule adjusts for holidays.
     */
    public boolean adjustsForHolidays() {
        return this == FOLLOWING_BUSINESS_DAY ||
               this == PRECEDING_BUSINESS_DAY ||
               this == MODIFIED_FOLLOWING ||
               this == MODIFIED_PRECEDING ||
               this == NEAREST_BUSINESS_DAY ||
               this == FEDERAL_HOLIDAYS_ONLY ||
               this == STATE_HOLIDAYS ||
               this == BANK_HOLIDAYS ||
               this == RELIGIOUS_HOLIDAYS ||
               this == REGIONAL_HOLIDAYS ||
               this == CUSTOM_CALENDAR;
    }
    
    /**
     * Checks if this rule moves the date forward.
     */
    public boolean movesForward() {
        return this == FOLLOWING_BUSINESS_DAY ||
               this == NEXT_BUSINESS_DAY ||
               this == WEEKEND_TO_MONDAY ||
               this == AUTOMATIC_EXTENSION;
    }
    
    /**
     * Checks if this rule moves the date backward.
     */
    public boolean movesBackward() {
        return this == PRECEDING_BUSINESS_DAY ||
               this == WEEKEND_TO_FRIDAY;
    }
    
    /**
     * Checks if this rule has conditional logic.
     */
    public boolean hasConditionalLogic() {
        return this == MODIFIED_FOLLOWING ||
               this == MODIFIED_PRECEDING ||
               this == NEAREST_BUSINESS_DAY ||
               this == MONTH_END_STICKY;
    }
    
    /**
     * Checks if this rule provides grace period.
     */
    public boolean providesGracePeriod() {
        return this == GRACE_PERIOD ||
               this == AUTOMATIC_EXTENSION;
    }
    
    /**
     * Checks if this rule is month-sensitive.
     */
    public boolean isMonthSensitive() {
        return this == MODIFIED_FOLLOWING ||
               this == MODIFIED_PRECEDING ||
               this == END_OF_MONTH ||
               this == MONTH_END_STICKY;
    }
    
    /**
     * Checks if this rule is period-sensitive (quarter/year).
     */
    public boolean isPeriodSensitive() {
        return this == QUARTER_END_ADJUSTMENT ||
               this == YEAR_END_ADJUSTMENT;
    }
    
    /**
     * Gets the typical adjustment days for this rule.
     */
    public Integer getTypicalAdjustmentDays() {
        switch (this) {
            case NO_ADJUSTMENT:
                return 0;
            case FOLLOWING_BUSINESS_DAY:
            case NEXT_BUSINESS_DAY:
                return 1;
            case PRECEDING_BUSINESS_DAY:
                return -1;
            case WEEKEND_TO_MONDAY:
                return 2; // Maximum for Sunday
            case WEEKEND_TO_FRIDAY:
                return -2; // Maximum for Saturday
            case GRACE_PERIOD:
                return 5; // Typical grace period
            case AUTOMATIC_EXTENSION:
                return 3; // Typical extension
            default:
                return null; // Variable based on circumstances
        }
    }
    
    /**
     * Gets the priority order for this rule (lower number = higher priority).
     */
    public Integer getPriorityOrder() {
        switch (this) {
            case NO_ADJUSTMENT:
                return 1;
            case SAME_BUSINESS_DAY:
                return 2;
            case FOLLOWING_BUSINESS_DAY:
                return 3;
            case MODIFIED_FOLLOWING:
                return 4;
            case PRECEDING_BUSINESS_DAY:
                return 5;
            case MODIFIED_PRECEDING:
                return 6;
            case NEAREST_BUSINESS_DAY:
                return 7;
            case GRACE_PERIOD:
                return 8;
            case AUTOMATIC_EXTENSION:
                return 9;
            default:
                return 10;
        }
    }
    
    /**
     * Checks if this rule is compatible with another rule.
     */
    public boolean isCompatibleWith(HolidayAdjustmentRule other) {
        if (other == null) {
            return false;
        }
        
        // Same rule is always compatible
        if (this == other) {
            return true;
        }
        
        // NO_ADJUSTMENT is not compatible with adjustment rules
        if (this == NO_ADJUSTMENT || other == NO_ADJUSTMENT) {
            return false;
        }
        
        // Grace period and extension rules can be combined with business day rules
        if ((this.providesGracePeriod() && other.adjustsForWeekends()) ||
            (other.providesGracePeriod() && this.adjustsForWeekends())) {
            return true;
        }
        
        // Business day rules are generally not compatible with each other
        if (this.adjustsForWeekends() && other.adjustsForWeekends()) {
            return false;
        }
        
        return true;
    }
}