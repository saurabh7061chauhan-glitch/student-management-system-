
package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.AccountVerificationToken;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.User;
import com.example.studentmanagement.exception.InvalidTokenException;
import com.example.studentmanagement.repository.AccountVerificationTokenRepository;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationService {

    @Autowired
    private AccountVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private com.example.studentmanagement.repository.TeacherRepository teacherRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public AccountVerificationToken createVerificationToken(User user, String studentName) {
        // Invalidate prior unused tokens
        List<AccountVerificationToken> activeTokens = tokenRepository.findByUserAndUsedFalse(user);
        for (AccountVerificationToken oldToken : activeTokens) {
            oldToken.setUsed(true);
            tokenRepository.save(oldToken);
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        AccountVerificationToken verificationToken = new AccountVerificationToken(
                user,
                rawToken,
                LocalDateTime.now().plusHours(24)
        );
        AccountVerificationToken saved = tokenRepository.save(verificationToken);

        // Send email
        emailService.sendVerificationEmail(user.getUsername(), studentName, rawToken);

        return saved;
    }

    @Transactional
    public boolean verifyToken(String token) {
        AccountVerificationToken verificationToken = tokenRepository.findByTokenHash(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent verification token."));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("This verification token has already been used.");
        }

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired. Please request a new verification email.");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        // Update student account status if applicable
        Optional<Student> studentOpt = studentRepository.findByUser(user);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            student.setAccountStatus("Active");
            studentRepository.save(student);
        }

        // Update faculty account status if applicable
        Optional<com.example.studentmanagement.entity.Teacher> teacherOpt = teacherRepository.findByUser(user);
        if (teacherOpt.isPresent()) {
            com.example.studentmanagement.entity.Teacher teacher = teacherOpt.get();
            teacher.setStatus("Active");
            teacherRepository.save(teacher);
        }

        verificationToken.setUsed(true);
        verificationToken.setVerifiedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        return true;
    }

    @Transactional
    public void resendVerification(String identifier) {
        // Find user by email or studentId or teacherId
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            // Check if studentId provided
            Optional<Student> studentOpt = studentRepository.findByStudentId(identifier);
            if (studentOpt.isPresent() && studentOpt.get().getUser() != null) {
                userOpt = Optional.of(studentOpt.get().getUser());
            }
        }

        if (userOpt.isEmpty()) {
            // Check if teacherId provided
            Optional<com.example.studentmanagement.entity.Teacher> teacherOpt = teacherRepository.findByTeacherId(identifier);
            if (teacherOpt.isPresent() && teacherOpt.get().getUser() != null) {
                userOpt = Optional.of(teacherOpt.get().getUser());
            }
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isEmailVerified()) {
                String recipientName = "User";
                Optional<Student> student = studentRepository.findByUser(user);
                if (student.isPresent()) {
                    recipientName = student.get().getFullName();
                } else {
                    Optional<com.example.studentmanagement.entity.Teacher> teacher = teacherRepository.findByUser(user);
                    if (teacher.isPresent()) {
                        recipientName = teacher.get().getName();
                    }
                }
                createVerificationToken(user, recipientName);
            }
        }
        // Always return silently to prevent user enumeration
    }
}
