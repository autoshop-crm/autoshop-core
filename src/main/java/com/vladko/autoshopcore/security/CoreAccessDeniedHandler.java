package com.vladko.autoshopcore.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@RequiredArgsConstructor
public class CoreAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        responseWriter.write(request, response, HttpStatus.FORBIDDEN, "Access denied");
    }
}
