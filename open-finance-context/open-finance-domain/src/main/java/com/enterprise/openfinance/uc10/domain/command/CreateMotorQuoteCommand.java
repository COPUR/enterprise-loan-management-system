package com.enterprise.openfinance.uc10.domain.command;

public record CreateMotorQuoteCommand(
        String tppId,
        String interactionId,
        String vehicleMake,
        String vehicleModel,
        int vehicleYear,
        int driverAge,
        int licenseDurationYears
) {

    public CreateMotorQuoteCommand {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (isBlank(vehicleMake)) {
            throw new IllegalArgumentException("vehicleMake is required");
        }
        if (isBlank(vehicleModel)) {
            throw new IllegalArgumentException("vehicleModel is required");
        }
        if (vehicleYear < 1900 || vehicleYear > 2100) {
            throw new IllegalArgumentException("vehicleYear is invalid");
        }
        if (driverAge < 18 || driverAge > 99) {
            throw new IllegalArgumentException("driverAge is invalid");
        }
        if (licenseDurationYears < 0 || licenseDurationYears > 80) {
            throw new IllegalArgumentException("licenseDurationYears is invalid");
        }

        tppId = tppId.trim();
        interactionId = interactionId.trim();
        vehicleMake = vehicleMake.trim().toUpperCase();
        vehicleModel = vehicleModel.trim().toUpperCase();
    }

    public String riskFingerprint() {
        return vehicleMake + '|' + vehicleModel + '|' + vehicleYear + '|' + driverAge + '|' + licenseDurationYears;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
