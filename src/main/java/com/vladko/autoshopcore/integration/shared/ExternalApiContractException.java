package com.vladko.autoshopcore.integration.shared;

public class ExternalApiContractException extends ExternalApiException {

    public ExternalApiContractException(String message) {
        super(message);
    }

    public ExternalApiContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
