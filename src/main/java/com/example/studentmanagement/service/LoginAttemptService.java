package com.example.studentmanagement.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_TIME_MINUTES = 15;

    private static class Attempt {
        int count;
        LocalDateTime lastAttempt;
        LocalDateTime lockedUntil;

        Attempt(int count, LocalDateTime lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }

    private final ConcurrentHashMap<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(key.toLowerCase().trim());
        }
    }

    public void loginFailed(String key) {
        if (key == null) return;
        String cleanKey = key.toLowerCase().trim();
        attemptsCache.compute(cleanKey, (k, existing) -> {
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                return new Attempt(1, now);
            }
            if (existing.lockedUntil != null && now.isAfter(existing.lockedUntil)) {
                // Lock expired, reset
                return new Attempt(1, now);
            }
            existing.count++;
            existing.lastAttempt = now;
            if (existing.count >= MAX_ATTEMPTS) {
                existing.lockedUntil = now.plusMinutes(LOCK_TIME_MINUTES);
            }
            return existing;
        });
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        Attempt attempt = attemptsCache.get(key.toLowerCase().trim());
        if (attempt == null || attempt.lockedUntil == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(attempt.lockedUntil)) {
            attemptsCache.remove(key.toLowerCase().trim());
            return false;
        }
        return true;
    }

    public long getRemainingLockMinutes(String key) {
        if (key == null) return 0;
        Attempt attempt = attemptsCache.get(key.toLowerCase().trim());
        if (attempt == null || attempt.lockedUntil == null) return 0;
        long mins = java.time.Duration.between(LocalDateTime.now(), attempt.lockedUntil).toMinutes();
        return Math.max(1, mins);
    }
}
