package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.PasswordChangeOtp;
import com.example.studentmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordChangeOtpRepository extends JpaRepository<PasswordChangeOtp, Long> {

    Optional<PasswordChangeOtp> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    Optional<PasswordChangeOtp> findByUserAndOtpCodeAndUsedFalse(User user, String otpCode);

    List<PasswordChangeOtp> findByUserAndUsedFalse(User user);
}
