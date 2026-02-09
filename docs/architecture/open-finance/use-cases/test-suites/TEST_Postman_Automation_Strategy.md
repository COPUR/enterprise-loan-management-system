# Postman Automation Strategy for Open Finance

This document outlines how to structure the Postman Collection to automate the execution of the Test Suites defined in the previous documents.

## 1. Collection Structure
Organize the collection folders to map directly to the Use Cases.

* **Folder:** `_Setup`
    * `Get Client Credentials` (Stores `client_token`)
    * `Generate JWS Signature` (Pre-request script)
* **Folder:** `UC001 - PFM`
    * `Get Accounts` (Tests: Schema, Status 200)
    * `Get Transactions` (Tests: Date filtering)
* **Folder:** `UC006 - Payments`
    * `Create Consent` (Capture `consent_id`)
    * `Authorize Consent` (Mock User Auth)
    * `Submit Payment` (Tests: Idempotency)

## 2. Common Test Scripts (JavaScript)
Apply these scripts at the **Collection Level** to ensure consistent testing across all endpoints.

### A. Response Time Assertion (NFR)
```javascript
// Check if response time is within SLA (500ms)
pm.test("SLA Violation Check: Response time < 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});
```

### B. Standard Header Validation

```javascript
// Validate standard security headers
pm.test("Security Headers Present", function () {
    pm.response.to.have.header("x-fapi-interaction-id");
    pm.response.to.have.header("Strict-Transport-Security");
});
```

### C. Idempotency Check Logic

Use this in the `Payments` folder.

```javascript
// Store the first response
if (!pm.collectionVariables.get("first_response_signature")) {
    pm.collectionVariables.set("first_response_signature", pm.response.headers.get("x-jws-signature"));
} else {
    // Compare second response
    pm.test("Idempotency: Signatures Match", function () {
        pm.expect(pm.response.headers.get("x-jws-signature")).to.eql(pm.collectionVariables.get("first_response_signature"));
    });
}
```

## 3. Environment Variables

Define these variables in your Postman Environment (`OFTF_Sandbox`):

| Variable | Description | Example |
| --- | --- | --- |
| `base_url` | API Gateway URL | `https://api.sandbox.openfinance.ae` |
| `client_id` | TPP Client ID | `TPP_12345` |
| `signing_key` | Private Key for JWS | `-----BEGIN PRIVATE KEY...` |
| `test_iban` | Valid IBAN for testing | `AE21000...` |
| `consent_id` | Dynamic ID from previous step | -- |

## 4. Continuous Integration (CI/CD)

To run these suites in a CI pipeline (Jenkins/GitLab CI), use **Newman**.

```bash
# Command to run the Payment Suite specifically
newman run "OpenFinance_Collection.json" \
    --folder "UC006 - Payments" \
    --environment "OFTF_Sandbox.json" \
    --reporters cli,html \
    --reporter-html-export report.html
```
