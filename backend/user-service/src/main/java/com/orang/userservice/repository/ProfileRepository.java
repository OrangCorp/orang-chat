package com.orang.userservice.repository;

import com.orang.userservice.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    List<Profile> findByDisplayNameContainingIgnoreCase(String displayName);
    boolean existsByUserId(UUID userId);
}