package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.ForgotPasswordRequest;
import com.example.studentmanagement.dto.PasswordResetRequest;
import com.example.studentmanagement.exception.InvalidTokenException;
import com.example.studentmanagement.service.PasswordResetService;
import com.example.studentmanagement.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                return "redirect:/admin/dashboard";
            }
            boolean isFaculty = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
            if (isFaculty) {
                return "redirect:/faculty/dashboard";
            }
            return "redirect:/student/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "verified", required = false) String verified,
            @RequestParam(value = "reset", required = false) String reset,
            @RequestParam(value = "role", required = false) String role,
            Model model) {

        if ("invalid".equals(error)) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        } else if ("unverified".equals(error)) {
            model.addAttribute("errorMessage", "Please verify your email address before logging in, or account may be disabled.");
            model.addAttribute("showResendLink", true);
        }

        if (logout != null) {
            model.addAttribute("infoMessage", "You have been logged out successfully.");
        }

        if (verified != null) {
            model.addAttribute("successMessage", "Your account has been verified successfully. You can now log in.");
        }

        if (reset != null) {
            model.addAttribute("successMessage", "Password reset successful! You can now log in with your new password.");
        }

        model.addAttribute("initialRole", role != null ? role.toLowerCase() : "faculty");

        return "auth/login";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "redirect:/login?role=admin";
    }

    @GetMapping("/faculty/login")
    public String facultyLogin() {
        return "redirect:/login?role=faculty";
    }

    @GetMapping("/student/login")
    public String studentLogin() {
        return "redirect:/login?role=student";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotRequest", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@ModelAttribute ForgotPasswordRequest request,
                                       RedirectAttributes redirectAttributes) {
        passwordResetService.initiateForgotPassword(request.getIdentifier());
        redirectAttributes.addFlashAttribute("successMessage",
                "If an account matching '" + request.getIdentifier() + "' exists, a password reset link has been dispatched.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token == null || token.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Password reset token is missing.");
            return "auth/reset-password";
        }
        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken(token);
        model.addAttribute("resetRequest", request);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@ModelAttribute PasswordResetRequest request,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            model.addAttribute("resetRequest", request);
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            redirectAttributes.addAttribute("reset", "true");
            return "redirect:/login";
        } catch (InvalidTokenException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("resetRequest", request);
            return "auth/reset-password";
        }
    }

    @GetMapping("/verify-account")
    public String verifyAccount(@RequestParam(value = "token", required = false) String token,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (token == null || token.trim().isEmpty()) {
            model.addAttribute("error", "Verification token is missing.");
            return "auth/verify-status";
        }

        try {
            verificationService.verifyToken(token);
            redirectAttributes.addAttribute("verified", "true");
            return "redirect:/login";
        } catch (InvalidTokenException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/verify-status";
        }
    }

    @GetMapping("/resend-verification")
    public String resendVerificationPage() {
        return "auth/resend-verification";
    }

    @PostMapping("/resend-verification")
    public String handleResendVerification(@RequestParam("identifier") String identifier,
                                          RedirectAttributes redirectAttributes) {
        verificationService.resendVerification(identifier);
        redirectAttributes.addFlashAttribute("successMessage",
                "If an account exists with '" + identifier + "', a new verification email has been sent.");
        return "redirect:/resend-verification";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
