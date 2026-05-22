package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.customerauth.dto.*;
import com.vladko.autoshopcore.integration.shared.ExternalApiAuthenticationException;
import com.vladko.autoshopcore.integration.shared.ExternalApiContractException;
import com.vladko.autoshopcore.integration.shared.ExternalApiUnavailableException;
import com.vladko.autoshopcore.security.AuthServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class RestClientCustomerAuthGateway implements CustomerAuthGateway {

    @Qualifier("authServiceRestClient")
    private final RestClient authServiceRestClient;
    private final AuthServiceProperties properties;

    @Override
    public CustomerAuthTokensDTO register(CustomerRegisterRequestDTO request) {
        return post(properties.customerRegisterPath(), request);
    }

    @Override
    public CustomerAuthTokensDTO login(CustomerLoginRequestDTO request) {
        return post(properties.customerLoginPath(), request);
    }

    @Override
    public CustomerAuthTokensDTO refresh(CustomerRefreshRequestDTO request) {
        return post(properties.customerRefreshPath(), request);
    }

    @Override
    public void logout(CustomerLogoutRequestDTO request) {
        authServiceRestClient.post().uri(properties.customerLogoutPath()).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();
    }

    @Override
    public void forgotPassword(CustomerForgotPasswordRequestDTO request) {
        authServiceRestClient.post().uri(properties.customerForgotPasswordPath()).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();
    }

    @Override
    public void resetPassword(CustomerResetPasswordRequestDTO request) {
        authServiceRestClient.post().uri(properties.customerResetPasswordPath()).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();
    }

    @Override
    public CustomerAuthTokensDTO verifyEmail(CustomerVerifyEmailRequestDTO request) {
        return post(properties.customerVerifyEmailPath(), request);
    }

    private CustomerAuthTokensDTO post(String path, Object request) {
        try {
            return authServiceRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CustomerAuthTokensDTO.class);
        } catch (ResourceAccessException exception) {
            throw new ExternalApiUnavailableException("Auth service is unavailable", exception);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new ExternalApiAuthenticationException("Auth service rejected credentials");
            }
            if (HttpStatusCode.valueOf(exception.getStatusCode().value()).is4xxClientError()) {
                throw new ExternalApiContractException(resolveMessage(exception, "Auth service rejected request"), exception);
            }
            throw new ExternalApiUnavailableException("Auth service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new ExternalApiUnavailableException("Auth service is unavailable", exception);
        }
    }

    private String resolveMessage(RestClientResponseException exception, String defaultMessage) {
        String body = exception.getResponseBodyAsString();
        return body == null || body.isBlank() ? defaultMessage : body;
    }
}
