package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.AccountVerificationToken;
import com.example.studentmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountVerificationTokenRepository extends JpaRepository<AccountVerificationToken, Long> {

    Optional<AccountVerificationToken> findByTokenHash(String tokenHash);

    List<AccountVerificationToken> findByUserAndUsedFalse(User user);

    void deleteByUser(User user);
}
