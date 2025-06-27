package com.banking.loan.application.commands;

import java.util.Map;

public record GenerateRecommendationsCommand(
    String customerId,
    String recommendationType,
    Map<String, Object> customerProfile,
    Map<String, Object> preferences
) {}