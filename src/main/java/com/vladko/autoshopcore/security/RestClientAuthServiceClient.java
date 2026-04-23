package com.vladko.autoshopcore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;

@Component
public class RestClientAuthServiceClient implements AuthServiceClient {

    private final RestClient authServiceRestClient;
    private final AuthServiceProperties properties;
    private final ObjectMapper objectMapper;

    public RestClientAuthServiceClient(@Qualifier("authServiceRestClient") RestClient authServiceRestClient,
                                       AuthServiceProperties properties,
                                       ObjectMapper objectMapper) {
        this.authServiceRestClient = authServiceRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AuthenticatedUser validateAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new InvalidAccessTokenException("Access token is missing");
        }

        try {
            ResponseEntity<AuthTokenValidationResponse> response = authServiceRestClient.post()
                    .uri(properties.validatePath())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim())
                    .retrieve()
                    .toEntity(AuthTokenValidationResponse.class);

            return toAuthenticatedUser(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new InvalidAccessTokenException(resolveInvalidTokenMessage(exception), exception);
        } catch (ResourceAccessException exception) {
            throw new AuthServiceUnavailableException("Authentication service is unavailable", exception);
        } catch (RestClientException exception) {
            if (isServerError(exception)) {
                throw new AuthServiceUnavailableException("Authentication service is unavailable", exception);
            }
            throw new InvalidAccessTokenException("Access token is invalid", exception);
        }
    }

    private AuthenticatedUser toAuthenticatedUser(AuthTokenValidationResponse response) {
        if (response == null || !response.valid()) {
            throw new InvalidAccessTokenException(messageOrDefault(response, "Access token is invalid"));
        }

        if (!"access".equals(response.tokenType())
                || response.userId() == null
                || !StringUtils.hasText(response.email())
                || response.roles() == null
                || response.roles().isEmpty()
                || !StringUtils.hasText(response.jti())
                || response.expiresAt() == null) {
            throw new InvalidAccessTokenException("Access token is invalid");
        }

        return new AuthenticatedUser(
                response.userId(),
                response.email(),
                Set.copyOf(response.roles()),
                response.jti(),
                response.expiresAt()
        );
    }

    private String resolveInvalidTokenMessage(HttpClientErrorException exception) {
        try {
            AuthTokenValidationResponse response = objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    AuthTokenValidationResponse.class
            );
            return messageOrDefault(response, "Access token is invalid");
        } catch (Exception ignored) {
            return "Access token is invalid";
        }
    }

    private String messageOrDefault(AuthTokenValidationResponse response, String defaultMessage) {
        if (response != null && StringUtils.hasText(response.message())) {
            return response.message();
        }
        return defaultMessage;
    }

    private boolean isServerError(RestClientException exception) {
        return exception instanceof RestClientResponseException responseException
                && HttpStatusCode.valueOf(responseException.getStatusCode().value()).is5xxServerError();
    }
}
