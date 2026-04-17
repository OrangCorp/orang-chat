package com.orang.authservice.job;

import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnverifiedAccountCleanupJob {

    private final UserRepository userRepository;

    @Value("${app.verification.unverified-account-ttl-days}")
    private int unverifiedAccountTtlDays;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupUnverifiedAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(unverifiedAccountTtlDays);

        List<User> unverifiedUsers = userRepository
                .findByEmailVerifiedFalseAndCreatedAtBefore(cutoff);

        if (unverifiedUsers.isEmpty()) {
            log.debug("No unverified accounts to clean up");
            return;
        }

        log.info("Cleaning up {} unverified accounts older than {} days",
                unverifiedUsers.size(), unverifiedAccountTtlDays);

        userRepository.deleteAll(unverifiedUsers);

        log.info("Unverified account cleanup complete");
    }
}