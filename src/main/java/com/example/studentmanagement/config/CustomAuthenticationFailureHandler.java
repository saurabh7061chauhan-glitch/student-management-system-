package com.example.studentmanagement.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.studentmanagement.service.AuditLogService;
import com.example.studentmanagement.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String ip = request.getRemoteAddr();

        if (username != null) {
            loginAttemptService.loginFailed(username);
        }
        if (ip != null) {
            loginAttemptService.loginFailed(ip);
        }

        auditLogService.log(username != null ? username : "Unknown", "UNKNOWN", "LOGIN_FAILED", "User", username,
                "Failed login attempt: " + exception.getMessage(), ip);

        if (exception instanceof DisabledException) {
            response.sendRedirect("/login?error=unverified");
        } else if (exception instanceof LockedException || (username != null && loginAttemptService.isBlocked(username)) || (ip != null && loginAttemptService.isBlocked(ip))) {
            response.sendRedirect("/login?error=locked");
        } else {
            response.sendRedirect("/login?error=invalid");
        }
    }
}
