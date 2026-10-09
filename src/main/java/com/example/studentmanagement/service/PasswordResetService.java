package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.PasswordResetToken;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.User;
import com.example.studentmanagement.exception.InvalidTokenException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.PasswordResetTokenRepository;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private com.example.studentmanagement.repository.TeacherRepository teacherRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.studentmanagement.repository.PasswordChangeOtpRepository otpRepository;

    @Transactional
    public void initiateForgotPassword(String identifier) {
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            Optional<Student> studentOpt = studentRepository.findByStudentId(identifier);
            if (studentOpt.isPresent() && studentOpt.get().getUser() != null) {
                userOpt = Optional.of(studentOpt.get().getUser());
            }
        }

        if (userOpt.isEmpty()) {
            Optional<com.example.studentmanagement.entity.Teacher> teacherOpt = teacherRepository.findByTeacherId(identifier);
            if (teacherOpt.isPresent() && teacherOpt.get().getUser() != null) {
                userOpt = Optional.of(teacherOpt.get().getUser());
            }
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Invalidate older unused tokens
            List<PasswordResetToken> oldTokens = resetTokenRepository.findByUserAndUsedFalse(user);
            for (PasswordResetToken t : oldTokens) {
                t.setUsed(true);
                resetTokenRepository.save(t);
            }

            String rawToken = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken resetToken = new PasswordResetToken(
                    user,
                    rawToken,
                    LocalDateTime.now().plusMinutes(30)
            );
            resetTokenRepository.save(resetToken);

            String displayName = "User";
            Optional<Student> student = studentRepository.findByUser(user);
            if (student.isPresent()) {
                displayName = student.get().getFullName();
            } else {
                Optional<com.example.studentmanagement.entity.Teacher> teacher = teacherRepository.findByUser(user);
                if (teacher.isPresent()) {
                    displayName = teacher.get().getName();
                }
            }

            emailService.sendPasswordResetEmail(user.getUsername(), displayName, rawToken);
        }
        // Always return generic response to avoid enumeration
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent password reset link."));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("This password reset link has already been used.");
        }

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("This password reset link has expired. Please submit a new request.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public String generateAndSendPasswordChangeOtp(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Invalidate previous unused OTPs
        List<com.example.studentmanagement.entity.PasswordChangeOtp> oldOtps = otpRepository.findByUserAndUsedFalse(user);
        for (com.example.studentmanagement.entity.PasswordChangeOtp o : oldOtps) {
            o.setUsed(true);
            otpRepository.save(o);
        }

        // Generate 6-digit OTP code
        int codeNum = 100000 + new java.security.SecureRandom().nextInt(900000);
        String otpCode = String.valueOf(codeNum);

        com.example.studentmanagement.entity.PasswordChangeOtp otp = new com.example.studentmanagement.entity.PasswordChangeOtp(
                user,
                otpCode,
                LocalDateTime.now().plusMinutes(15)
        );
        otpRepository.save(otp);

        String displayName = user.getUsername();
        Optional<Student> student = studentRepository.findByUser(user);
        if (student.isPresent()) {
            displayName = student.get().getFullName();
        }

        emailService.sendPasswordChangeOtpEmail(user.getUsername(), displayName, otpCode);
        return otpCode;
    }

    @Transactional
    public void changePasswordWithOtp(String username, String currentPassword, String newPassword, String otpCode) {
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Email verification code is required.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        com.example.studentmanagement.entity.PasswordChangeOtp otp = otpRepository
                .findByUserAndOtpCodeAndUsedFalse(user, otpCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code. Please check your email or request a new code."));

        if (otp.isExpired()) {
            throw new IllegalArgumentException("This verification code has expired. Please request a new code.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otp.setUsed(true);
        otpRepository.save(otp);
    }
}

