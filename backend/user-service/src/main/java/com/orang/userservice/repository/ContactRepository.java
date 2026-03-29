package com.orang.userservice.repository;

import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    // Find by specific direction
    Optional<Contact> findByRequesterIdAndRecipientId(UUID requesterId, UUID recipientId);

    // Incoming pending requests
    List<Contact> findByRecipientIdAndStatus(UUID recipientId, ContactStatus status);

    // Outgoing pending requests
    List<Contact> findByRequesterIdAndStatus(UUID requesterId, ContactStatus status);

    // Users I blocked
    List<Contact> findByBlockedByAndStatus(UUID blockedBy, ContactStatus status);

    @Query("""
        SELECT c FROM Contact c 
        WHERE (c.requesterId = :userA AND c.recipientId = :userB)
           OR (c.requesterId = :userB AND c.recipientId = :userA)
        """)
    Optional<Contact> findByUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("""
        SELECT c FROM Contact c 
        WHERE c.status = 'ACCEPTED'
          AND (c.requesterId = :userId OR c.recipientId = :userId)
        """)
    List<Contact> findAcceptedContactsForUser(@Param("userId") UUID userId);

    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Contact c
        WHERE c.status = 'BLOCKED'
          AND (
              (c.requesterId = :userA AND c.recipientId = :userB)
              OR (c.requesterId = :userB AND c.recipientId = :userA)
          )
        """)
    boolean existsBlockBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Contact c
        WHERE (c.requesterId = :userA AND c.recipientId = :userB)
           OR (c.requesterId = :userB AND c.recipientId = :userA)
        """)
    boolean existsRelationshipBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}