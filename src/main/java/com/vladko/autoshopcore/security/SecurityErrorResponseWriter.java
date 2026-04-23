package com.vladko.autoshopcore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladko.autoshopcore.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      HttpStatus status,
                      String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        ));
    }
}
