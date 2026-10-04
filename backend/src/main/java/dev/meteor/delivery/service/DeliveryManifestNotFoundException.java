package dev.meteor.delivery.service;

public class DeliveryManifestNotFoundException extends RuntimeException {

    public DeliveryManifestNotFoundException(long releaseId) {
        super("Delivery manifest not found for release: " + releaseId);
    }
}
