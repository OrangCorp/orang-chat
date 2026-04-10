package com.orang.notificationservice.repository;

import com.orang.notificationservice.entity.NotificationPreferences;
import com.orang.notificationservice.entity.NotificationPreferencesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationPreferencesRepository
        extends JpaRepository<NotificationPreferences, NotificationPreferencesId> {

    List<NotificationPreferences> findByIdUserId(UUID userId);
    List<NotificationPreferences> findByIdUserIdAndMutedTrue(UUID userId);
}
