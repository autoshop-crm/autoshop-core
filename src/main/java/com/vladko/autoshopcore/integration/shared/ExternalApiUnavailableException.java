package com.vladko.autoshopcore.integration.shared;

public class ExternalApiUnavailableException extends ExternalApiException {

    public ExternalApiUnavailableException(String message) {
        super(message);
    }

    public ExternalApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
