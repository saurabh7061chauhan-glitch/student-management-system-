package com.example.studentmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@college.edu}")
    private String fromEmail;

    // Simulated Outbox for easy demo and development testing
    public static class SentEmailRecord {
        private String to;
        private String subject;
        private String content;
        private String link;
        private LocalDateTime sentAt = LocalDateTime.now();

        public SentEmailRecord(String to, String subject, String content, String link) {
            this.to = to;
            this.subject = subject;
            this.content = content;
            this.link = link;
        }

        public String getTo() { return to; }
        public String getSubject() { return subject; }
        public String getContent() { return content; }
        public String getLink() { return link; }
        public LocalDateTime getSentAt() { return sentAt; }
    }

    private final List<SentEmailRecord> outbox = Collections.synchronizedList(new ArrayList<>());

    public List<SentEmailRecord> getRecentOutbox() {
        return new ArrayList<>(outbox);
    }

    public void sendVerificationEmail(String toEmail, String studentName, String token) {
        String verifyUrl = baseUrl + "/verify-account?token=" + token;
        String subject = "Verify Your Student Account - Student Management System";
        String body = "Dear " + studentName + ",\n\n" +
                "Welcome to the Student Management System.\n" +
                "Your student account has been created by the administrator.\n\n" +
                "Please verify your email address to activate your account by clicking the link below:\n" +
                verifyUrl + "\n\n" +
                "This verification link will expire in 24 hours.\n" +
                "If you did not expect this account, please contact the administrator.\n\n" +
                "Regards,\nAcademic Administration Office";

        recordAndSend(toEmail, subject, body, verifyUrl);
    }

    public void sendPasswordResetEmail(String toEmail, String studentName, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        String subject = "Password Reset Request - Student Management System";
        String body = "Dear " + studentName + ",\n\n" +
                "We received a request to reset your password for the Student Management System.\n\n" +
                "Click the link below to set a new password:\n" +
                resetUrl + "\n\n" +
                "This password reset link will expire in 30 minutes.\n" +
                "If you did not request a password reset, please ignore this email or notify the administrator immediately.\n\n" +
                "Regards,\nSecurity Office";

        recordAndSend(toEmail, subject, body, resetUrl);
    }

    public void sendPasswordChangeOtpEmail(String toEmail, String userName, String otpCode) {
        String subject = "Your Password Change Verification Code - Student Management System";
        String body = "Dear " + userName + ",\n\n" +
                "You requested to change your account password.\n\n" +
                "Your email verification code (OTP) is:\n\n" +
                "    [" + otpCode + "]\n\n" +
                "This verification code is valid for 15 minutes.\n" +
                "Enter this code in the password change form to complete updating your password.\n\n" +
                "If you did NOT request this change, please contact the administrator immediately.\n\n" +
                "Regards,\nSecurity Office";

        recordAndSend(toEmail, subject, body, baseUrl + "/student/settings");
    }

    private void recordAndSend(String to, String subject, String body, String link) {
        SentEmailRecord record = new SentEmailRecord(to, subject, body, link);
        outbox.add(0, record);
        if (outbox.size() > 50) {
            outbox.remove(outbox.size() - 1);
        }

        logger.info("==================== EMAIL DISPATCH ====================");
        logger.info("TO: {}", to);
        logger.info("SUBJECT: {}", subject);
        logger.info("LINK: {}", link);
        logger.info("========================================================");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Email successfully dispatched via SMTP to {}", to);
            } catch (Exception e) {
                logger.warn("SMTP send failed (SMTP might not be configured). Link is still recorded in outbox/log: {}", e.getMessage());
            }
        } else {
            logger.info("No SMTP JavaMailSender configured. Email captured in outbox successfully.");
        }
    }
}
