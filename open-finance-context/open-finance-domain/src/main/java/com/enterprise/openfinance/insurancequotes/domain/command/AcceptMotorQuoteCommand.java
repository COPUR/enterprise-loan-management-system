package com.enterprise.openfinance.insurancequotes.domain.command;

public record AcceptMotorQuoteCommand(
        String tppId,
        String quoteId,
        String idempotencyKey,
        String interactionId,
        String action,
        String paymentReference,
        String vehicleMake,
        String vehicleModel,
        Integer vehicleYear,
        Integer driverAge,
        Integer licenseDurationYears
) {

    public AcceptMotorQuoteCommand {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (isBlank(action) || !"ACCEPT".equalsIgnoreCase(action.trim())) {
            throw new IllegalArgumentException("action must be ACCEPT");
        }
        if (isBlank(paymentReference)) {
            throw new IllegalArgumentException("paymentReference is required");
        }

        tppId = tppId.trim();
        quoteId = quoteId.trim();
        idempotencyKey = idempotencyKey.trim();
        interactionId = interactionId.trim();
        action = "ACCEPT";
        paymentReference = paymentReference.trim();

        boolean allNull = vehicleMake == null
                && vehicleModel == null
                && vehicleYear == null
                && driverAge == null
                && licenseDurationYears == null;

        boolean allPresent = vehicleMake != null
                && vehicleModel != null
                && vehicleYear != null
                && driverAge != null
                && licenseDurationYears != null;

        if (!allNull && !allPresent) {
            throw new IllegalArgumentException("all risk snapshot fields must be provided together");
        }

        if (allPresent) {
            if (isBlank(vehicleMake) || isBlank(vehicleModel)) {
                throw new IllegalArgumentException("risk snapshot vehicle fields are required");
            }
            if (vehicleYear < 1900 || vehicleYear > 2100) {
                throw new IllegalArgumentException("risk snapshot vehicleYear is invalid");
            }
            if (driverAge < 18 || driverAge > 99) {
                throw new IllegalArgumentException("risk snapshot driverAge is invalid");
            }
            if (licenseDurationYears < 0 || licenseDurationYears > 80) {
                throw new IllegalArgumentException("risk snapshot licenseDurationYears is invalid");
            }

            vehicleMake = vehicleMake.trim().toUpperCase();
            vehicleModel = vehicleModel.trim().toUpperCase();
        }
    }

    public boolean hasRiskSnapshot() {
        return vehicleMake != null;
    }

    public String riskFingerprint() {
        if (!hasRiskSnapshot()) {
            return null;
        }
        return vehicleMake + '|' + vehicleModel + '|' + vehicleYear + '|' + driverAge + '|' + licenseDurationYears;
    }

    public String idempotencyFingerprint() {
        String snapshot = hasRiskSnapshot() ? riskFingerprint() : "NONE";
        return quoteId + '|' + action + '|' + paymentReference + '|' + snapshot;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
