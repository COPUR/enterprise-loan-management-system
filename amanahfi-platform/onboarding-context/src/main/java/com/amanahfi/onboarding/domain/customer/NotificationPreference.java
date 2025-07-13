package com.amanahfi.onboarding.domain.customer;

/**
 * Customer Notification Preferences
 * Defines how customers prefer to receive notifications
 */
public enum NotificationPreference {
    /**
     * Email notifications only
     */
    EMAIL,
    
    /**
     * SMS notifications only
     */
    SMS,
    
    /**
     * Both email and SMS notifications
     */
    EMAIL_AND_SMS,
    
    /**
     * In-app notifications only
     */
    IN_APP,
    
    /**
     * No notifications
     */
    NONE
}