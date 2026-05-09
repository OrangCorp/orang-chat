package com.orang.notificationservice.job;

import com.orang.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    @Value("${notification.retention.days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "NotificationCleanup_oldNotifications",
            lockAtMostFor = "15m",
            lockAtLeastFor = "5m"
    )
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = notificationRepository.deleteOlderThan(cutoff);

        if (deleted > 0) {
            log.info("Cleanup job deleted {} notifications older than {} days",
                    deleted, retentionDays);
        } else {
            log.debug("Cleanup job: no old notifications to remove");
        }
    }
}