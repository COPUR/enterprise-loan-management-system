package com.banking.loan.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Islamic Banking compliance functional tests
 * Tests Sharia-compliant banking operations, Arabic localization, and Islamic finance products
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Islamic Banking Functional Tests")
public class IslamicBankingFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should process Murabaha financing application")
    public void shouldProcessMurabahaFinancing() throws Exception {
        // Given: Murabaha financing request (Islamic profit-sharing)
        String murabahaJson = """
            {
                "customerId": "CUST-ISLAMIC-001",
                "amount": 50000.00,
                "termInMonths": 24,
                "loanType": "MURABAHA",
                "purpose": "Home furnishing",
                "collateralDescription": "Property documents",
                "monthlyIncome": 8000.00,
                "islamicCompliance": true,
                "profitRate": 8.5,
                "commodity": "Gold",
                "purchasePrice": 50000.00,
                "sellingPrice": 58000.00
            }
            """;

        // When: Submit Murabaha application
        mockMvc.perform(post("/api/v1/islamic/murabaha/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(murabahaJson)
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("MURABAHA"))
                .andExpect(jsonPath("$.islamicCompliance").value(true))
                .andExpect(jsonPath("$.profitRate").value(8.5))
                .andReturn();

        System.out.println("✅ Murabaha Financing: Successfully processed Murabaha financing application");
    }

    @Test
    @DisplayName("Should process Ijara leasing application")
    public void shouldProcessIjaraLeasing() throws Exception {
        // Given: Ijara leasing request (Islamic leasing)
        String ijaraJson = """
            {
                "customerId": "CUST-IJARA-001",
                "amount": 75000.00,
                "termInMonths": 36,
                "loanType": "IJARA",
                "purpose": "Vehicle leasing",
                "collateralDescription": "Vehicle registration",
                "monthlyIncome": 10000.00,
                "islamicCompliance": true,
                "assetDescription": "Toyota Camry 2024",
                "rentalAmount": 2500.00,
                "residualValue": 25000.00,
                "ownershipTransfer": true
            }
            """;

        // When: Submit Ijara application
        mockMvc.perform(post("/api/v1/islamic/ijara/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ijaraJson)
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("IJARA"))
                .andExpect(jsonPath("$.ownershipTransfer").value(true))
                .andReturn();

        System.out.println("✅ Ijara Leasing: Successfully processed Ijara leasing application");
    }

    @Test
    @DisplayName("Should validate Sharia compliance")
    public void shouldValidateShariahCompliance() throws Exception {
        // Given: Non-compliant financing request (interest-based)
        String nonCompliantJson = """
            {
                "customerId": "CUST-NON-COMPLIANT",
                "amount": 30000.00,
                "termInMonths": 12,
                "loanType": "CONVENTIONAL_INTEREST",
                "purpose": "Gambling business",
                "interestRate": 15.0,
                "islamicCompliance": false
            }
            """;

        // When & Then: Should reject non-Sharia compliant request
        mockMvc.perform(post("/api/v1/islamic/financing/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nonCompliantJson)
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Sharia")))
                .andReturn();

        System.out.println("✅ Sharia Compliance: Successfully validated Sharia compliance rules");
    }

    @Test
    @DisplayName("Should handle Arabic localization")
    public void shouldHandleArabicLocalization() throws Exception {
        // When: Request loan information in Arabic
        mockMvc.perform(get("/api/v1/loans/LOAN-ARABIC-001")
                .header("Authorization", "Bearer islamic-token")
                .header("Accept-Language", "ar-SA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currency").exists())
                .andReturn();

        System.out.println("✅ Arabic Localization: Successfully handled Arabic localization");
    }

    @Test
    @DisplayName("Should convert dates to Hijri calendar")
    public void shouldConvertDatesToHijriCalendar() throws Exception {
        // When: Request date conversion to Hijri
        mockMvc.perform(get("/api/v1/islamic/calendar/convert")
                .param("gregorianDate", "2024-01-15")
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hijriDate").exists())
                .andExpect(jsonPath("$.gregorianDate").value("2024-01-15"))
                .andReturn();

        System.out.println("✅ Hijri Calendar: Successfully converted dates to Hijri calendar");
    }

    @Test
    @DisplayName("Should handle Islamic banking holidays")
    public void shouldHandleIslamicBankingHolidays() throws Exception {
        // When: Check if date is Islamic holiday
        mockMvc.perform(get("/api/v1/islamic/calendar/holidays")
                .param("date", "2024-04-10") // Example: Eid al-Fitr
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHoliday").exists())
                .andExpect(jsonPath("$.holidayName").exists())
                .andReturn();

        System.out.println("✅ Islamic Holidays: Successfully handled Islamic banking holidays");
    }

    @Test
    @DisplayName("Should process Musharaka partnership financing")
    public void shouldProcessMushараkaFinancing() throws Exception {
        // Given: Musharaka partnership request
        String mushараkaJson = """
            {
                "customerId": "CUST-MUSHARAKA-001",
                "amount": 100000.00,
                "termInMonths": 48,
                "loanType": "MUSHARAKA",
                "purpose": "Business partnership",
                "collateralDescription": "Business assets",
                "monthlyIncome": 15000.00,
                "islamicCompliance": true,
                "partnershipRatio": 0.6,
                "profitSharingRatio": 0.4,
                "businessType": "Halal food restaurant",
                "expectedProfitMargin": 12.0
            }
            """;

        // When: Submit Musharaka application
        mockMvc.perform(post("/api/v1/islamic/musharaka/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mushараkaJson)
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("MUSHARAKA"))
                .andExpect(jsonPath("$.partnershipRatio").value(0.6))
                .andReturn();

        System.out.println("✅ Musharaka Partnership: Successfully processed Musharaka partnership financing");
    }

    @Test
    @DisplayName("Should format currency in Arabic numerals")
    public void shouldFormatCurrencyInArabicNumerals() throws Exception {
        // When: Request currency formatting in Arabic
        mockMvc.perform(get("/api/v1/islamic/localization/currency")
                .param("amount", "50000.00")
                .param("currency", "SAR")
                .param("locale", "ar-SA")
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formattedAmount").exists())
                .andExpect(jsonPath("$.arabicNumerals").exists())
                .andReturn();

        System.out.println("✅ Arabic Currency: Successfully formatted currency in Arabic numerals");
    }

    @Test
    @DisplayName("Should validate prohibited business activities")
    public void shouldValidateProhibitedBusinessActivities() throws Exception {
        // Given: Financing request for prohibited activity
        String prohibitedJson = """
            {
                "customerId": "CUST-PROHIBITED-001",
                "amount": 25000.00,
                "termInMonths": 18,
                "loanType": "BUSINESS",
                "purpose": "Alcohol distribution business",
                "islamicCompliance": true
            }
            """;

        // When & Then: Should reject prohibited business activity
        mockMvc.perform(post("/api/v1/islamic/financing/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(prohibitedJson)
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value(containsString("prohibited")))
                .andReturn();

        System.out.println("✅ Prohibited Activities: Successfully validated prohibited business activities");
    }

    @Test
    @DisplayName("Should generate Islamic banking compliance report")
    public void shouldGenerateIslamicBankingComplianceReport() throws Exception {
        // When: Generate Sharia compliance report
        mockMvc.perform(get("/api/v1/islamic/compliance/report")
                .param("customerId", "CUST-ISLAMIC-001")
                .param("fromDate", "2024-01-01")
                .param("toDate", "2024-12-31")
                .header("Authorization", "Bearer islamic-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complianceStatus").exists())
                .andExpect(jsonPath("$.shariahBoard").exists())
                .andExpect(jsonPath("$.certificationDate").exists())
                .andReturn();

        System.out.println("✅ Compliance Report: Successfully generated Islamic banking compliance report");
    }
}