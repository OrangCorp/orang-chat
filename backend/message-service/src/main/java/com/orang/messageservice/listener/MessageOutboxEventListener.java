package com.orang.messageservice.listener;

import com.orang.messageservice.event.GroupMemberInternalEvent;
import com.orang.messageservice.event.GroupUpdatedInternalEvent;
import com.orang.messageservice.event.MessageDeletedInternalEvent;
import com.orang.messageservice.event.MessageEditedInternalEvent;
import com.orang.messageservice.event.MessagePinChangedInternalEvent;
import com.orang.messageservice.event.MessageReactionChangedInternalEvent;
import com.orang.messageservice.service.GroupEventService;
import com.orang.messageservice.service.MessageEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageOutboxEventListener {

    private final MessageEventPublisher messageEventPublisher;
    private final GroupEventService groupEventService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageEdited(MessageEditedInternalEvent event) {
        log.debug("Publishing MessageEditedEvent after commit for message {}", event.getMessageId());
        messageEventPublisher.publishMessageEdited(
                event.getMessageId(),
                event.getConversationId(),
                event.getUserId(),
                event.getNewContent(),
                event.getEditedAt()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeletedInternalEvent event) {
        log.debug("Publishing MessageDeletedEvent after commit for message {}", event.getMessageId());
        messageEventPublisher.publishMessageDeleted(
                event.getMessageId(),
                event.getConversationId(),
                event.getDeletedBy(),
                event.getDeletedAt()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagePinChanged(MessagePinChangedInternalEvent event) {
        log.debug("Publishing MessagePinChangedEvent after commit for message {}", event.getMessageId());
        messageEventPublisher.publishMessagePinChanged(
                event.getMessageId(),
                event.getConversationId(),
                event.getUserId(),
                event.getAction()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageReactionChanged(MessageReactionChangedInternalEvent event) {
        log.debug("Publishing MessageReactionChangedEvent after commit for message {}", event.getMessageId());
        messageEventPublisher.publishReactionChanged(
                event.getMessageId(),
                event.getConversationId(),
                event.getUserId(),
                event.getAction(),
                event.getReactionType(),
                event.getCurrentCounts()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupMember(GroupMemberInternalEvent event) {
        log.debug("Publishing GroupMemberEvent after commit: {} for conversation {}",
                event.getEventType(), event.getConversationId());
        switch (event.getEventType()) {
            case MEMBER_ADDED ->
                    groupEventService.memberAdded(event.getConversationId(), event.getUserId(), event.getTriggeredBy());
            case MEMBER_REMOVED ->
                    groupEventService.memberRemoved(event.getConversationId(), event.getUserId(), event.getTriggeredBy());
            case MEMBER_LEFT ->
                    groupEventService.memberLeft(event.getConversationId(), event.getUserId());
            case ADMIN_PROMOTED ->
                    groupEventService.adminPromoted(event.getConversationId(), event.getUserId(), event.getTriggeredBy());
            case ADMIN_DEMOTED ->
                    groupEventService.adminDemoted(event.getConversationId(), event.getUserId(), event.getTriggeredBy());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupUpdated(GroupUpdatedInternalEvent event) {
        log.debug("Publishing GroupUpdatedEvent after commit: {} for conversation {}",
                event.getUpdateType(), event.getConversationId());
        switch (event.getUpdateType()) {
            case RENAMED ->
                    groupEventService.groupRenamed(event.getConversationId(), event.getNewName(), event.getTriggeredBy());
            case DELETED ->
                    groupEventService.groupDeleted(event.getConversationId(), event.getTriggeredBy());
        }
    }
}
