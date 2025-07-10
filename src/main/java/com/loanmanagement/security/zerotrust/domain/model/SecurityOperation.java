package com.loanmanagement.security.zerotrust.domain.model;

/**
 * Security operation types
 */
public enum SecurityOperation {
    LOGIN,
    LOGOUT,
    TRANSACTION,
    DATA_ACCESS,
    CONFIGURATION_CHANGE,
    ADMINISTRATIVE_ACTION,
    API_CALL,
    RESOURCE_ACCESS
}