package com.amanahfi.platform.shared.i18n;

import java.util.Locale;
import java.util.Map;

/**
 * Service for internationalization and localization
 */
public interface MessageService {
    
    /**
     * Get localized message by key
     */
    String getMessage(String key, String languageCode);
    
    /**
     * Get localized message with parameters
     */
    String getMessage(String key, String languageCode, Object... params);
    
    /**
     * Get localized message with named parameters
     */
    String getMessage(String key, String languageCode, Map<String, Object> params);
    
    /**
     * Get localized message using locale
     */
    String getMessage(String key, Locale locale);
    
    /**
     * Get localized message with parameters using locale
     */
    String getMessage(String key, Locale locale, Object... params);
    
    /**
     * Get localized message with named parameters using locale
     */
    String getMessage(String key, Locale locale, Map<String, Object> params);
    
    /**
     * Check if message key exists for language
     */
    boolean hasMessage(String key, String languageCode);
    
    /**
     * Get supported languages
     */
    java.util.Set<String> getSupportedLanguages();
    
    /**
     * Get default language
     */
    String getDefaultLanguage();
    
    /**
     * Get fallback language
     */
    String getFallbackLanguage();
    
    /**
     * Validate language code
     */
    boolean isLanguageSupported(String languageCode);
    
    /**
     * Get currency symbol for locale
     */
    String getCurrencySymbol(String languageCode);
    
    /**
     * Get date format pattern for locale
     */
    String getDateFormatPattern(String languageCode);
    
    /**
     * Get number format pattern for locale
     */
    String getNumberFormatPattern(String languageCode);
    
    /**
     * Format amount with currency for locale
     */
    String formatCurrency(double amount, String currencyCode, String languageCode);
    
    /**
     * Format date for locale
     */
    String formatDate(java.time.LocalDate date, String languageCode);
    
    /**
     * Format datetime for locale
     */
    String formatDateTime(java.time.LocalDateTime dateTime, String languageCode);
    
    /**
     * Format number for locale
     */
    String formatNumber(double number, String languageCode);
    
    /**
     * Get locale from language code
     */
    Locale getLocale(String languageCode);
    
    /**
     * Get language code from locale
     */
    String getLanguageCode(Locale locale);
    
    /**
     * Get direction for language (LTR/RTL)
     */
    TextDirection getTextDirection(String languageCode);
    
    /**
     * Text direction enumeration
     */
    enum TextDirection {
        LTR, // Left to Right
        RTL  // Right to Left (Arabic)
    }
}