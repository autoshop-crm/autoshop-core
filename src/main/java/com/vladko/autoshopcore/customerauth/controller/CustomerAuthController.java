package com.vladko.autoshopcore.customerauth.controller;

import com.vladko.autoshopcore.customerauth.dto.*;
import com.vladko.autoshopcore.customerauth.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/register")
    public ResponseEntity<CustomerAuthResponseDTO> register(@Valid @RequestBody CustomerRegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAuthService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerAuthResponseDTO> login(@Valid @RequestBody CustomerLoginRequestDTO request) {
        return ResponseEntity.ok(customerAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<CustomerAuthResponseDTO> refresh(@Valid @RequestBody CustomerRefreshRequestDTO request) {
        return ResponseEntity.ok(customerAuthService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody CustomerLogoutRequestDTO request) {
        customerAuthService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<CustomerAuthActionResponseDTO> forgotPassword(@Valid @RequestBody CustomerForgotPasswordRequestDTO request) {
        customerAuthService.forgotPassword(request);
        return ResponseEntity.ok(CustomerAuthActionResponseDTO.builder().success(true).message("Password recovery flow started").build());
    }

    @PostMapping("/password/reset")
    public ResponseEntity<CustomerAuthActionResponseDTO> resetPassword(@Valid @RequestBody CustomerResetPasswordRequestDTO request) {
        customerAuthService.resetPassword(request);
        return ResponseEntity.ok(CustomerAuthActionResponseDTO.builder().success(true).message("Password has been reset").build());
    }

    @PostMapping("/email/verify")
    public ResponseEntity<CustomerAuthResponseDTO> verifyEmail(@Valid @RequestBody CustomerVerifyEmailRequestDTO request) {
        return ResponseEntity.ok(customerAuthService.verifyEmail(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerAuthMeResponseDTO> me() {
        return ResponseEntity.ok(customerAuthService.me());
    }
}
