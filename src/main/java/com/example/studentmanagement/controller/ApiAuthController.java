package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.ApiResponse;
import com.example.studentmanagement.dto.ForgotPasswordRequest;
import com.example.studentmanagement.dto.PasswordResetRequest;
import com.example.studentmanagement.service.PasswordResetService;
import com.example.studentmanagement.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiateForgotPassword(request.getIdentifier());
        return ResponseEntity.ok(ApiResponse.ok("If the account exists, a password reset email has been dispatched."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Passwords do not match."));
        }
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody Map<String, String> payload) {
        String identifier = payload.get("identifier");
        if (identifier == null || identifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Identifier is required."));
        }
        verificationService.resendVerification(identifier);
        return ResponseEntity.ok(ApiResponse.ok("If an unverified account exists, a verification email has been resent."));
    }

    @PostMapping("/verify-account")
    public ResponseEntity<ApiResponse<Void>> verifyAccount(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Token is required."));
        }
        verificationService.verifyToken(token);
        return ResponseEntity.ok(ApiResponse.ok("Account verified successfully."));
    }
}
