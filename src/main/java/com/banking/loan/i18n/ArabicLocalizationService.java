package com.banking.loan.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comprehensive Arabic localization service with Islamic banking features
 * Supports Hijri calendar, Arabic numerals, and Islamic financial terminology
 */
@Service
@Slf4j
public class ArabicLocalizationService {

    private static final Locale ARABIC_LOCALE = new Locale("ar", "SA"); // Arabic (Saudi Arabia)
    private static final Locale ARABIC_UAE_LOCALE = new Locale("ar", "AE");
    private static final Locale ARABIC_EGYPT_LOCALE = new Locale("ar", "EG");
    
    // Arabic-Indic numerals (٠١٢٣٤٥٦٧٨٩)
    private static final String[] ARABIC_INDIC_NUMERALS = {"٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"};
    private static final String[] WESTERN_NUMERALS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    
    // Islamic banking terminology
    private static final String[] ISLAMIC_BANKING_TERMS = {
        "مرابحة", "إجارة", "مشاركة", "مضاربة", "سلم", "استصناع", "تكافل", "شريعة"
    };
    
    // Hijri month names
    private static final String[] HIJRI_MONTHS = {
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };
    
    // Days of the week in Arabic
    private static final String[] ARABIC_DAYS = {
        "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    };

    /**
     * Convert Gregorian date to Hijri date
     */
    public String convertToHijri(LocalDate gregorianDate) {
        try {
            HijrahDate hijrahDate = HijrahChronology.INSTANCE.date(gregorianDate);
            
            int day = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH);
            int month = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
            int year = hijrahDate.get(java.time.temporal.ChronoField.YEAR);
            
            String dayArabic = convertToArabicNumerals(String.valueOf(day));
            String monthName = HIJRI_MONTHS[month - 1];
            String yearArabic = convertToArabicNumerals(String.valueOf(year));
            
            return String.format("%s %s %s هـ", dayArabic, monthName, yearArabic);
        } catch (Exception e) {
            log.error("Error converting Gregorian date {} to Hijri", gregorianDate, e);
            return formatArabicDate(gregorianDate);
        }
    }

    /**
     * Convert Hijri date to Gregorian date
     */
    public LocalDate convertFromHijri(HijrahDate hijrahDate) {
        try {
            return LocalDate.from(hijrahDate);
        } catch (Exception e) {
            log.error("Error converting Hijri date {} to Gregorian", hijrahDate, e);
            return LocalDate.now();
        }
    }

    /**
     * Format date in Arabic locale
     */
    public String formatArabicDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(ARABIC_LOCALE);
        return date.format(formatter);
    }

    /**
     * Convert Western numerals to Arabic-Indic numerals
     */
    public String convertToArabicNumerals(String input) {
        if (input == null) return null;
        
        String result = input;
        for (int i = 0; i < WESTERN_NUMERALS.length; i++) {
            result = result.replace(WESTERN_NUMERALS[i], ARABIC_INDIC_NUMERALS[i]);
        }
        return result;
    }

    /**
     * Convert Arabic-Indic numerals to Western numerals
     */
    public String convertFromArabicNumerals(String input) {
        if (input == null) return null;
        
        String result = input;
        for (int i = 0; i < ARABIC_INDIC_NUMERALS.length; i++) {
            result = result.replace(ARABIC_INDIC_NUMERALS[i], WESTERN_NUMERALS[i]);
        }
        return result;
    }

    /**
     * Format currency amount in Arabic
     */
    public String formatCurrencyArabic(double amount, String currencyCode) {
        String formattedAmount = String.format("%.2f", amount);
        String arabicAmount = convertToArabicNumerals(formattedAmount);
        
        return switch (currencyCode.toUpperCase()) {
            case "SAR" -> arabicAmount + " ريال سعودي";
            case "AED" -> arabicAmount + " درهم إماراتي";
            case "USD" -> arabicAmount + " دولار أمريكي";
            case "EUR" -> arabicAmount + " يورو";
            case "EGP" -> arabicAmount + " جنيه مصري";
            case "KWD" -> arabicAmount + " دينار كويتي";
            case "BHD" -> arabicAmount + " دينار بحريني";
            case "QAR" -> arabicAmount + " ريال قطري";
            case "OMR" -> arabicAmount + " ريال عماني";
            case "JOD" -> arabicAmount + " دينار أردني";
            default -> arabicAmount + " " + currencyCode;
        };
    }

    /**
     * Format loan details in Arabic
     */
    public String formatLoanDetailsArabic(double amount, int installments, String productType) {
        String arabicAmount = formatCurrencyArabic(amount, "SAR");
        String arabicInstallments = convertToArabicNumerals(String.valueOf(installments));
        
        String productTypeArabic = switch (productType.toLowerCase()) {
            case "murabaha" -> "مرابحة";
            case "ijara" -> "إجارة";
            case "musharaka" -> "مشاركة";
            case "mudaraba" -> "مضاربة";
            case "personal" -> "شخصي";
            case "mortgage" -> "عقاري";
            case "auto" -> "سيارات";
            case "business" -> "تجاري";
            default -> productType;
        };
        
        return String.format("قرض %s بمبلغ %s على %s قسط", productTypeArabic, arabicAmount, arabicInstallments);
    }

    /**
     * Validate Arabic text input
     */
    public boolean isValidArabicText(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // Arabic Unicode range: \u0600-\u06FF
        // Arabic Supplement: \u0750-\u077F
        // Arabic Extended-A: \u08A0-\u08FF
        Pattern arabicPattern = Pattern.compile("^[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\s\\d.,'-]+$");
        return arabicPattern.matcher(input.trim()).matches();
    }

    /**
     * Get Islamic banking product names in Arabic
     */
    public String getIslamicProductName(String productCode) {
        return switch (productCode.toUpperCase()) {
            case "MUR" -> "مرابحة - بيع بالتقسيط";
            case "IJA" -> "إجارة - تأجير منتهي بالتمليك";
            case "MUS" -> "مشاركة - شراكة في رأس المال";
            case "MUD" -> "مضاربة - استثمار بالوكالة";
            case "SAL" -> "سلم - بيع آجل بثمن عاجل";
            case "IST" -> "استصناع - تمويل التصنيع";
            case "TAK" -> "تكافل - التأمين الإسلامي";
            case "QAR" -> "قرض حسن - بدون فوائد";
            default -> "منتج مصرفي إسلامي";
        };
    }

    /**
     * Format prayer times for Islamic banking calendar
     */
    public String formatPrayerTimesContext(LocalDate date) {
        String hijriDate = convertToHijri(date);
        return String.format("التاريخ الهجري: %s", hijriDate);
    }

    /**
     * Check if date falls on Islamic holidays
     */
    public boolean isIslamicHoliday(LocalDate date) {
        HijrahDate hijrahDate = HijrahChronology.INSTANCE.date(date);
        int month = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
        int day = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH);
        
        // Major Islamic holidays
        return (month == 1 && day == 1) ||  // Islamic New Year
               (month == 3 && day == 12) || // Maulid (Prophet's Birthday)
               (month == 9) ||              // Ramadan month
               (month == 10 && day <= 3) || // Eid al-Fitr
               (month == 12 && day >= 10 && day <= 13); // Eid al-Adha
    }

    /**
     * Get business day status considering Islamic calendar
     */
    public String getBusinessDayStatus(LocalDate date) {
        if (isIslamicHoliday(date)) {
            return "عطلة إسلامية";
        }
        
        // Friday is the holy day in Islamic calendar
        if (date.getDayOfWeek().getValue() == 5) { // Friday
            return "يوم الجمعة - عطلة أسبوعية";
        }
        
        return "يوم عمل";
    }

    /**
     * Format Islamic banking compliance message
     */
    public String formatShariahComplianceMessage(boolean isCompliant) {
        if (isCompliant) {
            return "✓ متوافق مع أحكام الشريعة الإسلامية";
        } else {
            return "✗ غير متوافق مع أحكام الشريعة الإسلامية";
        }
    }

    /**
     * Convert amount to Arabic words (for legal documents)
     */
    public String convertAmountToArabicWords(double amount) {
        // This is a simplified version - would need full implementation for production
        String[] ones = {"", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"};
        String[] tens = {"", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"};
        String[] hundreds = {"", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"};
        
        int wholeAmount = (int) amount;
        int decimals = (int) ((amount - wholeAmount) * 100);
        
        // Simplified conversion - would need complete implementation
        if (wholeAmount < 10) {
            return ones[wholeAmount] + (decimals > 0 ? " و " + decimals + " هللة" : "");
        }
        
        return convertToArabicNumerals(String.valueOf(wholeAmount)) + " ريال" + 
               (decimals > 0 ? " و " + convertToArabicNumerals(String.valueOf(decimals)) + " هللة" : "");
    }

    /**
     * Get RTL (Right-to-Left) direction indicator
     */
    public String getRTLDirection() {
        return "rtl";
    }

    /**
     * Get Arabic locale based on country
     */
    public Locale getArabicLocale(String countryCode) {
        return switch (countryCode.toUpperCase()) {
            case "SA" -> ARABIC_LOCALE;
            case "AE" -> ARABIC_UAE_LOCALE;
            case "EG" -> ARABIC_EGYPT_LOCALE;
            default -> ARABIC_LOCALE;
        };
    }
}