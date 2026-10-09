package com.example.studentmanagement.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.studentmanagement.service.AuditLogService;
import com.example.studentmanagement.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String ip = request.getRemoteAddr();

        loginAttemptService.loginSucceeded(username);
        loginAttemptService.loginSucceeded(ip);

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().orElse("USER");

        auditLogService.log(username, role, "LOGIN_SUCCESS", "User", username,
                "User logged in successfully", ip);

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if ("ROLE_FACULTY".equals(authority.getAuthority())) {
                response.sendRedirect("/faculty/dashboard");
                return;
            } else if ("ROLE_STUDENT".equals(authority.getAuthority())) {
                response.sendRedirect("/student/dashboard");
                return;
            }
        }
        response.sendRedirect("/");
    }
}
