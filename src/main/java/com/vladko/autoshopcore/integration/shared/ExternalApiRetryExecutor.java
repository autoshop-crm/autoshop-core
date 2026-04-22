package com.vladko.autoshopcore.integration.shared;

import java.time.Duration;
import java.util.function.Supplier;

public class ExternalApiRetryExecutor {

    private final int maxAttempts;
    private final Duration backoff;

    public ExternalApiRetryExecutor(int maxAttempts, Duration backoff) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoff = backoff == null ? Duration.ZERO : backoff;
    }

    public <T> T execute(Supplier<T> supplier) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (ExternalApiUnavailableException exception) {
                lastException = exception;
                sleepBeforeRetry(attempt);
            }
        }

        throw lastException;
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= maxAttempts || backoff.isZero() || backoff.isNegative()) {
            return;
        }

        try {
            Thread.sleep(backoff.toMillis() * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiUnavailableException("External API retry was interrupted", exception);
        }
    }
}
