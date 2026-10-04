package dev.meteor.download.service;

public class DownloadNotFoundException extends RuntimeException {

    public DownloadNotFoundException() {
        super("Download authorization or file was not found");
    }
}
