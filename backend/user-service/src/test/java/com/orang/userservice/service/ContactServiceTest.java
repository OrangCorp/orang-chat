package com.orang.userservice.service;

import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.event.ContactRequestSentEvent;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}