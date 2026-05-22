package com.vladko.autoshopcore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<AuthServiceClient> authServiceClientProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityErrorResponseWriter responseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/actuator/health".equals(path) || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            commenceUnauthorized(request, response, "Bearer token is required");
            return;
        }

        String token = authorizationHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            commenceUnauthorized(request, response, "Bearer token is missing");
            return;
        }

        try {
            AuthServiceClient authServiceClient = authServiceClientProvider.getIfAvailable();
            if (authServiceClient == null) {
                throw new AuthServiceUnavailableException("Authentication service is unavailable");
            }

            AuthenticatedUser authenticatedUser = authServiceClient.validateAccessToken(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    authenticatedUser.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toSet())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidAccessTokenException exception) {
            SecurityContextHolder.clearContext();
            commenceUnauthorized(request, response, exception.getMessage());
        } catch (AuthServiceUnavailableException exception) {
            SecurityContextHolder.clearContext();
            responseWriter.write(request, response, HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    private void commenceUnauthorized(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String message) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(message));
    }
}
