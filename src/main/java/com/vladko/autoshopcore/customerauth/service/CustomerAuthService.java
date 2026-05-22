package com.vladko.autoshopcore.customerauth.service;

import com.vladko.autoshopcore.customerauth.dto.*;

public interface CustomerAuthService {
    CustomerAuthResponseDTO register(CustomerRegisterRequestDTO request);
    CustomerAuthResponseDTO login(CustomerLoginRequestDTO request);
    CustomerAuthResponseDTO refresh(CustomerRefreshRequestDTO request);
    void logout(CustomerLogoutRequestDTO request);
    void forgotPassword(CustomerForgotPasswordRequestDTO request);
    void resetPassword(CustomerResetPasswordRequestDTO request);
    CustomerAuthResponseDTO verifyEmail(CustomerVerifyEmailRequestDTO request);
    CustomerAuthMeResponseDTO me();
}
