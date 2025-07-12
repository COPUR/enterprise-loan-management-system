package com.amanahfi.platform.shared.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementation of MessageService for internationalization
 * Supports Arabic, English, and other MENAT languages
 */
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    
    private final MessageSource messageSource;
    
    @Value("${amanahfi.i18n.default-language:en}")
    private String defaultLanguage;
    
    @Value("${amanahfi.i18n.fallback-language:en}")
    private String fallbackLanguage;
    
    // Supported languages in MENAT region
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        "en", // English
        "ar", // Arabic
        "fa", // Persian/Farsi
        "tr", // Turkish
        "he", // Hebrew
        "ur", // Urdu
        "hi"  // Hindi
    );
    
    // RTL languages
    private static final Set<String> RTL_LANGUAGES = Set.of("ar", "fa", "he", "ur");
    
    // Currency mappings for MENAT region
    private static final Map<String, String> CURRENCY_SYMBOLS = Map.of(
        "en", "AED", // UAE Dirham
        "ar", "د.إ",  // Arabic AED symbol
        "fa", "درهم", // Persian
        "tr", "TL",   // Turkish Lira
        "he", "₪",    // Israeli Shekel
        "ur", "AED",  // Urdu
        "hi", "AED"   // Hindi
    );
    
    // Date format patterns
    private static final Map<String, String> DATE_PATTERNS = Map.of(
        "en", "MM/dd/yyyy",
        "ar", "dd/MM/yyyy",
        "fa", "yyyy/MM/dd",
        "tr", "dd.MM.yyyy",
        "he", "dd/MM/yyyy",
        "ur", "dd/MM/yyyy",
        "hi", "dd/MM/yyyy"
    );
    
    // Number format patterns
    private static final Map<String, String> NUMBER_PATTERNS = Map.of(
        "en", "#,##0.00",
        "ar", "#٬##0٫00", // Arabic-Indic digits
        "fa", "#,##0.00",
        "tr", "#.##0,00",
        "he", "#,##0.00",
        "ur", "#,##0.00",
        "hi", "#,##,##0.00" // Indian numbering system
    );
    
    public MessageServiceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    
    @Override
    public String getMessage(String key, String languageCode) {
        return getMessage(key, getLocale(languageCode));
    }
    
    @Override
    public String getMessage(String key, String languageCode, Object... params) {
        return getMessage(key, getLocale(languageCode), params);
    }
    
    @Override
    public String getMessage(String key, String languageCode, Map<String, Object> params) {
        // Convert named parameters to positional for MessageSource
        return getMessage(key, getLocale(languageCode), params.values().toArray());
    }
    
    @Override
    public String getMessage(String key, Locale locale) {
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message not found for key '{}' and locale '{}', trying fallback", key, locale);
            return getFallbackMessage(key, locale);
        }
    }
    
    @Override
    public String getMessage(String key, Locale locale, Object... params) {
        try {
            return messageSource.getMessage(key, params, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message not found for key '{}' and locale '{}', trying fallback", key, locale);
            return getFallbackMessage(key, locale, params);
        }
    }
    
    @Override
    public String getMessage(String key, Locale locale, Map<String, Object> params) {
        return getMessage(key, locale, params.values().toArray());
    }
    
    @Override
    public boolean hasMessage(String key, String languageCode) {
        try {
            getMessage(key, languageCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Set<String> getSupportedLanguages() {
        return new HashSet<>(SUPPORTED_LANGUAGES);
    }
    
    @Override
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    @Override
    public String getFallbackLanguage() {
        return fallbackLanguage;
    }
    
    @Override
    public boolean isLanguageSupported(String languageCode) {
        return SUPPORTED_LANGUAGES.contains(languageCode);
    }
    
    @Override
    public String getCurrencySymbol(String languageCode) {
        return CURRENCY_SYMBOLS.getOrDefault(languageCode, "AED");
    }
    
    @Override
    public String getDateFormatPattern(String languageCode) {
        return DATE_PATTERNS.getOrDefault(languageCode, DATE_PATTERNS.get("en"));
    }
    
    @Override
    public String getNumberFormatPattern(String languageCode) {
        return NUMBER_PATTERNS.getOrDefault(languageCode, NUMBER_PATTERNS.get("en"));
    }
    
    @Override
    public String formatCurrency(double amount, String currencyCode, String languageCode) {
        try {
            Locale locale = getLocale(languageCode);
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
            
            // Set currency if specified
            if (currencyCode != null && !currencyCode.trim().isEmpty()) {
                try {
                    currencyFormat.setCurrency(Currency.getInstance(currencyCode));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid currency code '{}', using default", currencyCode);
                }
            }
            
            return currencyFormat.format(amount);
        } catch (Exception e) {
            log.error("Error formatting currency for amount {} in language {}", amount, languageCode, e);
            return String.format("%.2f %s", amount, getCurrencySymbol(languageCode));
        }
    }
    
    @Override
    public String formatDate(LocalDate date, String languageCode) {
        try {
            String pattern = getDateFormatPattern(languageCode);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, getLocale(languageCode));
            return date.format(formatter);
        } catch (Exception e) {
            log.error("Error formatting date {} for language {}", date, languageCode, e);
            return date.toString();
        }
    }
    
    @Override
    public String formatDateTime(LocalDateTime dateTime, String languageCode) {
        try {
            String pattern = getDateFormatPattern(languageCode) + " HH:mm:ss";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, getLocale(languageCode));
            return dateTime.format(formatter);
        } catch (Exception e) {
            log.error("Error formatting datetime {} for language {}", dateTime, languageCode, e);
            return dateTime.toString();
        }
    }
    
    @Override
    public String formatNumber(double number, String languageCode) {
        try {
            String pattern = getNumberFormatPattern(languageCode);
            DecimalFormat formatter = new DecimalFormat(pattern);
            formatter.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(getLocale(languageCode)));
            return formatter.format(number);
        } catch (Exception e) {
            log.error("Error formatting number {} for language {}", number, languageCode, e);
            return String.valueOf(number);
        }
    }
    
    @Override
    public Locale getLocale(String languageCode) {
        if (languageCode == null || !isLanguageSupported(languageCode)) {
            languageCode = defaultLanguage;
        }
        
        // Handle specific locale variants for MENAT region
        return switch (languageCode) {
            case "ar" -> new Locale("ar", "AE"); // Arabic (UAE)
            case "fa" -> new Locale("fa", "IR"); // Persian (Iran)
            case "tr" -> new Locale("tr", "TR"); // Turkish (Turkey)
            case "he" -> new Locale("he", "IL"); // Hebrew (Israel)
            case "ur" -> new Locale("ur", "PK"); // Urdu (Pakistan)
            case "hi" -> new Locale("hi", "IN"); // Hindi (India)
            default -> new Locale("en", "AE");   // English (UAE)
        };
    }
    
    @Override
    public String getLanguageCode(Locale locale) {
        return locale.getLanguage();
    }
    
    @Override
    public TextDirection getTextDirection(String languageCode) {
        return RTL_LANGUAGES.contains(languageCode) ? TextDirection.RTL : TextDirection.LTR;
    }
    
    // Helper methods
    
    private String getFallbackMessage(String key, Locale locale) {
        try {
            Locale fallbackLocale = getLocale(fallbackLanguage);
            return messageSource.getMessage(key, null, fallbackLocale);
        } catch (NoSuchMessageException e) {
            log.error("Message not found for key '{}' even in fallback language '{}'", key, fallbackLanguage);
            return "[" + key + "]"; // Return key in brackets as last resort
        }
    }
    
    private String getFallbackMessage(String key, Locale locale, Object... params) {
        try {
            Locale fallbackLocale = getLocale(fallbackLanguage);
            return messageSource.getMessage(key, params, fallbackLocale);
        } catch (NoSuchMessageException e) {
            log.error("Message not found for key '{}' even in fallback language '{}'", key, fallbackLanguage);
            return "[" + key + "]"; // Return key in brackets as last resort
        }
    }
}