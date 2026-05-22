package com.vladko.autoshopcore.parts.exception;

public class PartNotFoundException extends RuntimeException {

    public PartNotFoundException(Integer id) {
        super("Part with id '%s' was not found".formatted(id));
    }
}
