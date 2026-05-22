package com.vladko.autoshopcore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class AuthServiceClientTest {

    private final AuthServiceProperties properties = new AuthServiceProperties(
            "http://auth-service",
            "/api/auth/validate",
            "/api/customer-auth/register",
            "/api/customer-auth/login",
            "/api/customer-auth/refresh",
            "/api/customer-auth/logout",
            "/api/customer-auth/password/forgot",
            "/api/customer-auth/password/reset",
            "/api/customer-auth/email/verify",
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            true
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateAccessTokenReturnsAuthenticatedUser() {
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientAuthServiceClient client = new RestClientAuthServiceClient(builder.build(), properties, objectMapper);

        server.expect(requestTo("http://auth-service/api/auth/validate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andRespond(withSuccess("""
                        {
                          "valid": true,
                          "userId": 42,
                          "email": "manager@autoshop.local",
                          "roles": ["MANAGER"],
                          "tokenType": "access",
                          "jti": "token-jti",
                          "expiresAt": "2026-04-21T09:15:00Z",
                          "message": null
                        }
                        """, MediaType.APPLICATION_JSON));

        AuthenticatedUser user = client.validateAccessToken("valid-token");

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.email()).isEqualTo("manager@autoshop.local");
        assertThat(user.roles()).containsExactly("MANAGER");
        assertThat(user.jti()).isEqualTo("token-jti");
        server.verify();
    }

    @Test
    void validateAccessTokenRejectsUnauthorizedToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientAuthServiceClient client = new RestClientAuthServiceClient(builder.build(), properties, objectMapper);

        server.expect(requestTo("http://auth-service/api/auth/validate"))
                .andRespond(withUnauthorizedRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "valid": false,
                                  "message": "Access token is revoked"
                                }
                                """));

        assertThatThrownBy(() -> client.validateAccessToken("revoked-token"))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("Access token is revoked");
        server.verify();
    }

    @Test
    void validateAccessTokenFailsClosedWhenAuthServiceReturnsServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientAuthServiceClient client = new RestClientAuthServiceClient(builder.build(), properties, objectMapper);

        server.expect(requestTo("http://auth-service/api/auth/validate"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.validateAccessToken("valid-token"))
                .isInstanceOf(AuthServiceUnavailableException.class)
                .hasMessage("Authentication service is unavailable");
        server.verify();
    }
}
