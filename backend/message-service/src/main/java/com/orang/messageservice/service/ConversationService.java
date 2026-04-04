package com.orang.messageservice.service;

import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.ConversationParticipant;
import com.orang.messageservice.entity.ConversationParticipantId;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final GroupEventService groupEventService;

    public List<ConversationResponse> getConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findByParticipantIdsContaining(userId);
        return conversations.stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(UUID userId1, UUID userId2) {
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        Optional<Conversation> existingConversation =
                conversationRepository.findDirectConversationBetween(userId1, userId2);

        if (existingConversation.isPresent()) {
            return toConversationResponse(existingConversation.get());
        }

        Conversation newConversation = Conversation.builder()
                .type(Conversation.ConversationType.DIRECT)
                .build();

        addParticipant(newConversation, userId1, ConversationParticipant.ParticipantRole.MEMBER, null);
        addParticipant(newConversation, userId2, ConversationParticipant.ParticipantRole.MEMBER, null);

        return toConversationResponse(conversationRepository.save(newConversation));
    }

    @Transactional
    public ConversationResponse createGroupConversation(String name, Set<UUID> participantIds, UUID creatorId) {
        Set<UUID> allParticipantIds = new HashSet<>(participantIds);
        allParticipantIds.add(creatorId);

        if (allParticipantIds.size() < 3) {
            throw new BadRequestException("A group conversation must have at least 3 participants");
        }

        Conversation newConversation = Conversation.builder()
                .type(Conversation.ConversationType.GROUP)
                .name(name)
                .createdBy(creatorId)
                .build();

        addParticipant(newConversation, creatorId, ConversationParticipant.ParticipantRole.ADMIN, null);

        for (UUID participantId : participantIds) {
            if (!participantId.equals(creatorId)) {
                addParticipant(newConversation, participantId, ConversationParticipant.ParticipantRole.MEMBER, creatorId);
            }
        }

        return toConversationResponse(conversationRepository.save(newConversation));
    }

    private void addParticipant(Conversation conversation, UUID userId,
                                ConversationParticipant.ParticipantRole role, UUID addedBy) {
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversation.getId(), userId))
                .conversation(conversation)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .addedBy(addedBy)
                .build();

        conversation.getParticipants().add(participant);
    }

    @Transactional
    public ConversationResponse addParticipants(UUID conversationId, Set<UUID> userIds, UUID addedBy) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot add participants to a direct conversation");
        }

        // Verify requester is a participant
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(addedBy));

        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }

        // Get existing participant IDs
        Set<UUID> existingIds = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUserId)
                .collect(Collectors.toSet());

        // Add only new participants
        for (UUID userId : userIds) {
            if (!existingIds.contains(userId)) {
                addParticipant(conversation, userId, ConversationParticipant.ParticipantRole.MEMBER, addedBy);
                groupEventService.memberAdded(conversationId, userId, addedBy);
            }
        }

        return toConversationResponse(conversationRepository.save(conversation));
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        List<ConversationResponse.ParticipantInfo> participants = conversation.getParticipants().stream()
                .map(p -> ConversationResponse.ParticipantInfo.builder()
                        .userId(p.getUserId())
                        .role(p.getRole().name())
                        .joinedAt(p.getJoinedAt())
                        .addedBy(p.getAddedBy())
                        .build())
                .toList();

        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(conversation.getName())
                .createdBy(conversation.getCreatedBy())
                .participants(participants)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    public void verifyParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }
    }

    @Transactional
    public void removeParticipant(UUID conversationId, UUID userIdToRemove, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot remove participants from a direct conversation");
        }

        // Find requester's participant record
        ConversationParticipant requester = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(requesterId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        // Only admins can remove others
        if (requester.getRole() != ConversationParticipant.ParticipantRole.ADMIN) {
            throw new ForbiddenException("Only admins can remove participants");
        }

        // Find participant to remove
        ConversationParticipant toRemove = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userIdToRemove))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User is not a participant"));

        // Prevent self-removal via this endpoint (use leave instead)
        if (userIdToRemove.equals(requesterId)) {
            throw new BadRequestException("Use the leave endpoint to remove yourself");
        }

        conversation.getParticipants().remove(toRemove);

        // If removed user was admin and now no admins left, promote oldest member
        promoteOldestMemberIfNoAdmins(conversation);

        // Save the updated conversation
        conversationRepository.save(conversation);

        groupEventService.memberRemoved(conversationId, userIdToRemove, requesterId);
    }

    private void promoteOldestMemberIfNoAdmins(Conversation conversation) {
        boolean hasAdmin = conversation.getParticipants().stream()
                .anyMatch(p -> p.getRole() == ConversationParticipant.ParticipantRole.ADMIN);

        if (!hasAdmin && !conversation.getParticipants().isEmpty()) {
            // Find oldest member by joinedAt
            ConversationParticipant oldest = conversation.getParticipants().stream()
                    .min(Comparator.comparing(ConversationParticipant::getJoinedAt))
                    .orElseThrow();

            oldest.setRole(ConversationParticipant.ParticipantRole.ADMIN);
        }
    }

    @Transactional
    public void leaveConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot leave a direct conversation");
        }

        ConversationParticipant participant = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        conversation.getParticipants().remove(participant);

        // If no participants left, delete the conversation
        if (conversation.getParticipants().isEmpty()) {
            conversationRepository.delete(conversation);
            return;
        }

        // If leaving user was admin, promote oldest member if no admins remain
        promoteOldestMemberIfNoAdmins(conversation);

        conversationRepository.save(conversation);

        groupEventService.memberLeft(conversationId, userId);
    }

    @Transactional
    public ConversationResponse renameConversation(UUID conversationId, String newName, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot rename a direct conversation");
        }

        ConversationParticipant requester = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(requesterId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        if (requester.getRole() != ConversationParticipant.ParticipantRole.ADMIN) {
            throw new ForbiddenException("Only admins can rename the conversation");
        }

        conversation.setName(newName);

        groupEventService.groupRenamed(conversationId, newName, requesterId);

        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public ConversationResponse promoteParticipant(UUID conversationId, UUID userIdToPromote, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot promote participants in a direct conversation");
        }

        ConversationParticipant requester = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(requesterId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        if (requester.getRole() != ConversationParticipant.ParticipantRole.ADMIN) {
            throw new ForbiddenException("Only admins can promote participants");
        }

        ConversationParticipant toPromote = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userIdToPromote))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User is not a participant"));

        if (toPromote.getRole() == ConversationParticipant.ParticipantRole.ADMIN) {
            throw new BadRequestException("User is already an admin");
        }

        toPromote.setRole(ConversationParticipant.ParticipantRole.ADMIN);
        groupEventService.adminPromoted(conversationId, userIdToPromote, requesterId);

        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public ConversationResponse demoteParticipant(UUID conversationId, UUID userIdToDemote, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot demote participants in a direct conversation");
        }

        ConversationParticipant requester = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(requesterId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        if (requester.getRole() != ConversationParticipant.ParticipantRole.ADMIN) {
            throw new ForbiddenException("Only admins can demote participants");
        }

        ConversationParticipant toDemote = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userIdToDemote))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User is not a participant"));

        if (toDemote.getRole() != ConversationParticipant.ParticipantRole.ADMIN) {
            throw new BadRequestException("User is not an admin");
        }

        // Count admins
        long adminCount = conversation.getParticipants().stream()
                .filter(p -> p.getRole() == ConversationParticipant.ParticipantRole.ADMIN)
                .count();

        if (adminCount <= 1) {
            throw new BadRequestException("Cannot demote the only admin");
        }

        toDemote.setRole(ConversationParticipant.ParticipantRole.MEMBER);
        groupEventService.adminDemoted(conversationId, userIdToDemote, requesterId);

        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot delete a direct conversation");
        }

        ConversationParticipant requester = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(requesterId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this conversation"));

        if (requester.getRole() != ConversationParticipant.ParticipantRole.ADMIN) {
            throw new ForbiddenException("Only admins can delete the conversation");
        }

        conversationRepository.delete(conversation);
        groupEventService.groupDeleted(conversationId, requesterId);
    }

    public boolean isAdmin(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        return conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .map(p -> p.getRole() == ConversationParticipant.ParticipantRole.ADMIN)
                .orElse(false);
    }
}