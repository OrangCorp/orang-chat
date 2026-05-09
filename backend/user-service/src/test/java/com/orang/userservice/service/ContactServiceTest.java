package com.orang.userservice.service;

import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.event.ContactRequestSentEvent;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ConflictException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.ContactStatus;
import com.orang.userservice.repository.ContactRepository;
import com.orang.userservice.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private PresenceService presenceService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(profileRepository, contactRepository, presenceService, rabbitTemplate);
    }

    @Test
    void sendRequestPublishesContactRequestEvent() {
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

        when(profileRepository.existsById(recipientId)).thenReturn(true);
        when(contactRepository.existsRelationshipBetweenUsers(requesterId, recipientId)).thenReturn(false);
        when(contactRepository.existsBlockBetweenUsers(requesterId, recipientId)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(contactId);
            contact.setStatus(ContactStatus.PENDING);
            return contact;
        });

        Contact contact = contactService.sendRequest(requesterId, recipientId);

        assertThat(contact.getId()).isEqualTo(contactId);

        ArgumentCaptor<ContactRequestSentEvent> eventCaptor = ArgumentCaptor.forClass(ContactRequestSentEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.CONTACT_EXCHANGE),
                eq(RabbitMQConstants.CONTACT_REQUEST_SENT_KEY),
                eventCaptor.capture()
        );

        ContactRequestSentEvent event = eventCaptor.getValue();
        assertThat(event.getContactId()).isEqualTo(contactId);
        assertThat(event.getRequesterId()).isEqualTo(requesterId);
        assertThat(event.getRecipientId()).isEqualTo(recipientId);
    }

    @Test
    void sendRequestRegistersAfterCommitCallbackWhenSynchronizationIsActive() {
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

        when(profileRepository.existsById(recipientId)).thenReturn(true);
        when(contactRepository.existsRelationshipBetweenUsers(requesterId, recipientId)).thenReturn(false);
        when(contactRepository.existsBlockBetweenUsers(requesterId, recipientId)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(contactId);
            return contact;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            Contact contact = contactService.sendRequest(requesterId, recipientId);

            assertThat(contact.getId()).isEqualTo(contactId);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().getFirst();
            synchronization.afterCommit();

            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConstants.CONTACT_EXCHANGE),
                    eq(RabbitMQConstants.CONTACT_REQUEST_SENT_KEY),
                    any(ContactRequestSentEvent.class)
            );
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendRequestRejectsInvalidInputsAndConflicts() {
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

        assertThatThrownBy(() -> contactService.sendRequest(requesterId, requesterId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("yourself");

        when(profileRepository.existsById(recipientId)).thenReturn(false);
        assertThatThrownBy(() -> contactService.sendRequest(requesterId, recipientId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        when(profileRepository.existsById(recipientId)).thenReturn(true);
        when(contactRepository.existsRelationshipBetweenUsers(requesterId, recipientId)).thenReturn(true);
        assertThatThrownBy(() -> contactService.sendRequest(requesterId, recipientId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Relationship already exists");

        when(contactRepository.existsRelationshipBetweenUsers(requesterId, recipientId)).thenReturn(false);
        when(contactRepository.existsBlockBetweenUsers(requesterId, recipientId)).thenReturn(true);
        assertThatThrownBy(() -> contactService.sendRequest(requesterId, recipientId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot send request");

        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void removeContactAllowsRequesterToDeleteAcceptedContact() {
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

        Contact contact = Contact.builder()
                .id(contactId)
                .requesterId(requesterId)
                .recipientId(recipientId)
                .status(ContactStatus.ACCEPTED)
                .build();

        when(contactRepository.findById(contactId)).thenReturn(java.util.Optional.of(contact));

        contactService.removeContact(requesterId, contactId);

        verify(contactRepository).delete(contact);
    }

    @Test
    void removeContactAllowsRecipientToDeleteAcceptedContact() {
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

        Contact contact = Contact.builder()
                .id(contactId)
                .requesterId(requesterId)
                .recipientId(recipientId)
                .status(ContactStatus.ACCEPTED)
                .build();

        when(contactRepository.findById(contactId)).thenReturn(java.util.Optional.of(contact));

        contactService.removeContact(recipientId, contactId);

        verify(contactRepository).delete(contact);
    }

    @Test
    void acceptRequestCoversMissingPendingAndPermissionChecks() {
    UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    Contact acceptedContact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.ACCEPTED)
        .build();

    Contact pendingContact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.PENDING)
        .build();

    when(contactRepository.findById(contactId)).thenReturn(Optional.empty(), Optional.of(acceptedContact), Optional.of(pendingContact));

    assertThatThrownBy(() -> contactService.acceptRequest(recipientId, contactId))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> contactService.acceptRequest(recipientId, contactId))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("not pending");

    assertThatThrownBy(() -> contactService.acceptRequest(requesterId, contactId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("recipient");
    }

    @Test
    void acceptRequestUpdatesPendingContact() {
    UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    Contact contact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.PENDING)
        .build();

    when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
    when(contactRepository.save(contact)).thenAnswer(invocation -> invocation.getArgument(0));

    Contact result = contactService.acceptRequest(recipientId, contactId);

    assertThat(result.getStatus()).isEqualTo(ContactStatus.ACCEPTED);
    assertThat(result.getAcceptedAt()).isNotNull();
    verify(contactRepository).save(contact);
    }

    @Test
    void rejectAndCancelRequestsHandleMissingRecordsPermissionAndSuccess() {
    UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    Contact pendingContact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.PENDING)
        .build();

    Contact acceptedContact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.ACCEPTED)
        .build();

    when(contactRepository.findById(contactId)).thenReturn(Optional.empty(), Optional.of(acceptedContact), Optional.of(pendingContact));

    assertThatThrownBy(() -> contactService.rejectRequest(recipientId, contactId))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> contactService.rejectRequest(recipientId, contactId))
        .isInstanceOf(ConflictException.class);

    assertThatThrownBy(() -> contactService.rejectRequest(requesterId, contactId))
        .isInstanceOf(ForbiddenException.class);

    contactService.rejectRequest(recipientId, contactId);
    verify(contactRepository).delete(pendingContact);

    when(contactRepository.findById(contactId)).thenReturn(Optional.of(acceptedContact), Optional.of(pendingContact));

    assertThatThrownBy(() -> contactService.cancelRequest(requesterId, contactId))
        .isInstanceOf(ConflictException.class);

    assertThatThrownBy(() -> contactService.cancelRequest(recipientId, contactId))
        .isInstanceOf(ForbiddenException.class);

    contactService.cancelRequest(requesterId, contactId);
    verify(contactRepository, org.mockito.Mockito.times(2)).delete(pendingContact);
    }

    @Test
    void removeContactRejectsWrongStatesAndUnauthorizedUsers() {
    UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    Contact pendingContact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.PENDING)
        .build();

    Contact acceptedContact = Contact.builder()
        .id(contactId)
        .requesterId(requesterId)
        .recipientId(recipientId)
        .status(ContactStatus.ACCEPTED)
        .build();

    Contact unrelatedContact = Contact.builder()
        .id(contactId)
        .requesterId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .recipientId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
        .status(ContactStatus.ACCEPTED)
        .build();

    when(contactRepository.findById(contactId)).thenReturn(Optional.empty(), Optional.of(pendingContact), Optional.of(acceptedContact), Optional.of(acceptedContact), Optional.of(unrelatedContact));

    assertThatThrownBy(() -> contactService.removeContact(requesterId, contactId))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> contactService.removeContact(requesterId, contactId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not accepted");

    assertThatThrownBy(() -> contactService.removeContact(UUID.fromString("33333333-3333-3333-3333-333333333333"), contactId))
        .isInstanceOf(ForbiddenException.class);

    contactService.removeContact(requesterId, contactId);
    verify(contactRepository).delete(acceptedContact);
    }

    @Test
    void blockAndUnblockUserCoverSuccessAndFailurePaths() {
    UUID blockerId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    UUID targetId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    Contact acceptedContact = Contact.builder()
        .id(contactId)
        .requesterId(blockerId)
        .recipientId(targetId)
        .status(ContactStatus.ACCEPTED)
        .build();

    Contact blockedContact = Contact.builder()
        .id(contactId)
        .requesterId(blockerId)
        .recipientId(targetId)
        .status(ContactStatus.BLOCKED)
        .blockedBy(blockerId)
        .build();

    when(profileRepository.existsById(targetId)).thenReturn(false, true, true, true);
    when(contactRepository.findByUsers(blockerId, targetId)).thenReturn(Optional.of(acceptedContact), Optional.of(blockedContact), Optional.empty(), Optional.of(blockedContact));
    when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

    assertThatThrownBy(() -> contactService.blockUser(blockerId, blockerId))
        .isInstanceOf(BadRequestException.class);

    assertThatThrownBy(() -> contactService.blockUser(blockerId, targetId))
        .isInstanceOf(ResourceNotFoundException.class);

    Contact result = contactService.blockUser(blockerId, targetId);
    assertThat(result.getStatus()).isEqualTo(ContactStatus.BLOCKED);
    assertThat(result.getBlockedBy()).isEqualTo(blockerId);

    assertThatThrownBy(() -> contactService.blockUser(blockerId, targetId))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Cannot block this user");

    assertThatThrownBy(() -> contactService.unblockUser(blockerId, targetId))
        .isInstanceOf(ResourceNotFoundException.class);

    blockedContact.setBlockedBy(UUID.fromString("99999999-9999-9999-9999-999999999999"));
    when(contactRepository.findByUsers(blockerId, targetId)).thenReturn(Optional.of(blockedContact));
    assertThatThrownBy(() -> contactService.unblockUser(blockerId, targetId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Cannot unblock this user");

    blockedContact.setBlockedBy(blockerId);
    blockedContact.setStatus(ContactStatus.ACCEPTED);
    assertThatThrownBy(() -> contactService.unblockUser(blockerId, targetId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not blocked");

    blockedContact.setStatus(ContactStatus.BLOCKED);
    contactService.unblockUser(blockerId, targetId);
    verify(contactRepository).delete(blockedContact);
    }

    @Test
    void queryMethodsDelegateToRepository() {
    UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");

    Contact contact = Contact.builder()
        .id(UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111"))
        .requesterId(userId)
        .recipientId(UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2"))
        .status(ContactStatus.ACCEPTED)
        .createdAt(LocalDateTime.of(2026, 5, 4, 10, 0))
        .build();

    when(contactRepository.findAcceptedContactsForUser(userId)).thenReturn(List.of(contact));
    when(contactRepository.findByRecipientIdAndStatus(userId, ContactStatus.PENDING)).thenReturn(List.of(contact));
    when(contactRepository.findByRequesterIdAndStatus(userId, ContactStatus.PENDING)).thenReturn(List.of(contact));
    when(contactRepository.findByBlockedByAndStatus(userId, ContactStatus.BLOCKED)).thenReturn(List.of(contact));

    assertThat(contactService.getAcceptedContacts(userId)).containsExactly(contact);
    assertThat(contactService.getIncomingRequests(userId)).containsExactly(contact);
    assertThat(contactService.getOutgoingRequests(userId)).containsExactly(contact);
    assertThat(contactService.getBlockedUsers(userId)).containsExactly(contact);
    }
}