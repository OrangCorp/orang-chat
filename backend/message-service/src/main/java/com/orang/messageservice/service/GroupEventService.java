package com.orang.messageservice.service;

import com.orang.shared.event.GroupMemberEvent;
import com.orang.shared.event.GroupUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupEventService {

    private final RabbitTemplate rabbitTemplate;
    private static final String GROUP_EXCHANGE = "group.exchange";

    public void memberAdded(UUID conversationId, UUID userId, UUID addedBy) {
        GroupMemberEvent event = GroupMemberEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .triggeredBy(addedBy)
                .eventType(GroupMemberEvent.EventType.MEMBER_ADDED)
                .build();

        publish("group.member.added", event);
    }

    public void memberRemoved(UUID conversationId, UUID userId, UUID removedBy) {
        GroupMemberEvent event = GroupMemberEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .triggeredBy(removedBy)
                .eventType(GroupMemberEvent.EventType.MEMBER_REMOVED)
                .build();

        publish("group.member.removed", event);
    }

    public void memberLeft(UUID conversationId, UUID userId) {
        GroupMemberEvent event = GroupMemberEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .triggeredBy(userId)
                .eventType(GroupMemberEvent.EventType.MEMBER_LEFT)
                .build();

        publish("group.member.left", event);
    }

    public void adminPromoted(UUID conversationId, UUID userId, UUID promotedBy) {
        GroupMemberEvent event = GroupMemberEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .triggeredBy(promotedBy)
                .eventType(GroupMemberEvent.EventType.ADMIN_PROMOTED)
                .build();

        publish("group.admin.promoted", event);
    }

    public void adminDemoted(UUID conversationId, UUID userId, UUID demotedBy) {
        GroupMemberEvent event = GroupMemberEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .triggeredBy(demotedBy)
                .eventType(GroupMemberEvent.EventType.ADMIN_DEMOTED)
                .build();

        publish("group.admin.demoted", event);
    }

    public void groupRenamed(UUID conversationId, String newName, UUID renamedBy) {
        GroupUpdatedEvent event = GroupUpdatedEvent.builder()
                .conversationId(conversationId)
                .newName(newName)
                .triggeredBy(renamedBy)
                .updateType(GroupUpdatedEvent.UpdateType.RENAMED)
                .build();

        publish("group.renamed", event);
    }

    public void groupDeleted(UUID conversationId, UUID deletedBy) {
        GroupUpdatedEvent event = GroupUpdatedEvent.builder()
                .conversationId(conversationId)
                .triggeredBy(deletedBy)
                .updateType(GroupUpdatedEvent.UpdateType.DELETED)
                .build();

        publish("group.deleted", event);
    }

    private void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(GROUP_EXCHANGE, routingKey, event);
        log.info("Published {} : {}", routingKey, event);
    }
}