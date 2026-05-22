package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.customerauth.dto.*;

public interface CustomerAuthGateway {
    CustomerAuthTokensDTO register(CustomerRegisterRequestDTO request);
    CustomerAuthTokensDTO login(CustomerLoginRequestDTO request);
    CustomerAuthTokensDTO refresh(CustomerRefreshRequestDTO request);
    void logout(CustomerLogoutRequestDTO request);
    void forgotPassword(CustomerForgotPasswordRequestDTO request);
    void resetPassword(CustomerResetPasswordRequestDTO request);
    CustomerAuthTokensDTO verifyEmail(CustomerVerifyEmailRequestDTO request);
}
