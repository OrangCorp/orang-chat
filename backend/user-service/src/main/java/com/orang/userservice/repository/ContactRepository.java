package com.orang.userservice.repository;

import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByUserId(UUID userId);
    Optional<Contact> findByUserIdAndContactUserId(UUID userId, UUID contactUserId);
    List<Contact> findByUserIdAndStatus(UUID userId, ContactStatus status);
    boolean existsByUserIdAndContactUserId(UUID userId, UUID contactUserId);
}