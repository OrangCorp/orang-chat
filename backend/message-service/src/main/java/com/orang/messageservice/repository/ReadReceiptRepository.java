package com.orang.messageservice.repository;

import com.orang.messageservice.entity.ReadReceipt;
import com.orang.messageservice.entity.ReadReceiptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, ReadReceiptId> {

    List<ReadReceipt> findByIdUserId(UUID userId);
    List<ReadReceipt> findByIdConversationId(UUID conversationId);
}
