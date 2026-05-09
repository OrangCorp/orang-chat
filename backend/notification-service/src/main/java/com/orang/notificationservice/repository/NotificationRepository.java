package com.orang.notificationservice.repository;

import com.orang.notificationservice.entity.Notification;
import com.orang.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.userId = :userId
        ORDER BY n.read ASC, n.createdAt DESC
        """)
    Page<Notification> findByUserIdOrdered(
            @Param("userId") UUID userId,
            Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.userId = :userId
          AND n.groupKey = :groupKey
          AND n.read = FALSE
        """)
    Optional<Notification> findUnreadByGroupKey(
            @Param("userId") UUID userId,
            @Param("groupKey") String groupKey);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = TRUE, n.readAt = CURRENT_TIMESTAMP
        WHERE n.userId = :userId AND n.read = FALSE
        """)
    int markAllAsRead(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);

    @Modifying
    @Query("""
        DELETE FROM Notification n
        WHERE n.createdAt < :cutoff
        """)
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    Page<Notification> findByUserIdAndType(
            UUID userId,
            NotificationType type,
            Pageable pageable);
}