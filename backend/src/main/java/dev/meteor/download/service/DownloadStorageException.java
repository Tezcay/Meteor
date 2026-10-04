package dev.meteor.download.service;

public class DownloadStorageException extends RuntimeException {

    public DownloadStorageException(Throwable cause) {
        super("The authorized file could not be read", cause);
    }
}
