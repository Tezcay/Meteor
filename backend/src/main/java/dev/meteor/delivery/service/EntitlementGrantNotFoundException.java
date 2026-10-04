package dev.meteor.delivery.service;

public class EntitlementGrantNotFoundException extends RuntimeException {

    public EntitlementGrantNotFoundException(long grantId) {
        super("Active entitlement grant " + grantId + " was not found");
    }
}
