package dev.meteor.download.service;

public class DownloadIntegrityException extends RuntimeException {

    public DownloadIntegrityException() {
        super("Stored file metadata does not match its content");
    }
}
