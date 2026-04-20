package com.orang.userservice.service;

import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.event.ContactRequestSentEvent;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ConflictException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.userservice.dto.ContactResponse;
import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.ContactStatus;
import com.orang.userservice.entity.Profile;
import com.orang.userservice.repository.ContactRepository;
import com.orang.userservice.repository.ProfileRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ProfileRepository profileRepository;
    private final ContactRepository contactRepository;
    private final PresenceService presenceService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Contact sendRequest(UUID requesterId, UUID recipientId) {
        if (requesterId.equals(recipientId)) {
            throw new BadRequestException("Cannot send request to yourself");
        }

        if (!profileRepository.existsById(recipientId)) {
            throw new ResourceNotFoundException("User not found");
        }

        if (contactRepository.existsRelationshipBetweenUsers(requesterId, recipientId)) {
            throw new ConflictException("Relationship already exists with this user");
        }

        if (contactRepository.existsBlockBetweenUsers(requesterId, recipientId)) {
            throw new ConflictException("Cannot send request to this user");
        }

        Contact contact = Contact.builder()
                .requesterId(requesterId)
                .recipientId(recipientId)
                .status(ContactStatus.PENDING)
                .build();

        Contact savedContact = contactRepository.save(contact);
        publishContactRequestSentEventAfterCommit(savedContact);
        return savedContact;
    }

    @Transactional
    public Contact acceptRequest(UUID userId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found"));

        if (contact.getStatus() != ContactStatus.PENDING) {
            throw new ConflictException("Contact request is not pending");
        }

        if (!contact.isRecipient(userId)) {
            throw new ForbiddenException("Only the recipient can accept this request");
        }

        contact.setStatus(ContactStatus.ACCEPTED);
        contact.setAcceptedAt(LocalDateTime.now());

        return contactRepository.save(contact);
    }

    @Transactional
    public void rejectRequest(UUID userId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found"));

        if (contact.getStatus() != ContactStatus.PENDING) {
            throw new ConflictException("Contact request is not pending");
        }

        if (!contact.isRecipient(userId)) {
            throw new ForbiddenException("Only the recipient can reject this request");
        }

        contactRepository.delete(contact);
    }

    @Transactional
    public void cancelRequest(UUID userId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found"));

        if (contact.getStatus() != ContactStatus.PENDING) {
            throw new ConflictException("Contact request is not pending");
        }

        if (!contact.isRequester(userId)) {
            throw new ForbiddenException("Only the requester can cancel this request");
        }

        contactRepository.delete(contact);
    }

    @Transactional
    public void removeContact(UUID userId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact request not found"));

        if (contact.getStatus() != ContactStatus.ACCEPTED) {
            throw new BadRequestException("Contact request is not accepted");
        }

        if (!contact.involvesUser(userId)) {
            throw new ForbiddenException("You are not a part of this contact");
        }

        contactRepository.delete(contact);
    }

    @Transactional
    public Contact blockUser(UUID blockerId, UUID targetId) {
        if (blockerId.equals(targetId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        if (!profileRepository.existsById(targetId)) {
            throw new ResourceNotFoundException("User not found");
        }

        Optional<Contact> existingContact = contactRepository.findByUsers(blockerId, targetId);

        if (existingContact.isPresent()) {
            Contact contact = existingContact.get();

            if (contact.getStatus() == ContactStatus.BLOCKED) {
                throw new ConflictException("Cannot block this user");
            }

            contact.setStatus(ContactStatus.BLOCKED);
            contact.setBlockedBy(blockerId);

            return contactRepository.save(contact);
        } else {
            Contact contact = Contact.builder()
                    .requesterId(blockerId)
                    .recipientId(targetId)
                    .status(ContactStatus.BLOCKED)
                    .blockedBy(blockerId)
                    .build();
            return contactRepository.save(contact);
        }
    }

    @Transactional
    public void unblockUser(UUID unblockerId, UUID targetId) {
        Contact contact = contactRepository.findByUsers(unblockerId, targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (contact.getStatus() != ContactStatus.BLOCKED) {
            throw new BadRequestException("User is not blocked");
        }

        if (contact.getBlockedBy() != unblockerId) {
            throw new ForbiddenException("Cannot unblock this user");
        }

        contactRepository.delete(contact);
    }

    public List<Contact> getAcceptedContacts(UUID userId) {
        return contactRepository.findAcceptedContactsForUser(userId);
    }

    public List<Contact> getIncomingRequests(UUID userId) {
        return contactRepository.findByRecipientIdAndStatus(userId, ContactStatus.PENDING);
    }

    public List<Contact> getOutgoingRequests(UUID userId) {
        return contactRepository.findByRequesterIdAndStatus(userId, ContactStatus.PENDING);
    }

    public List<Contact> getBlockedUsers(UUID userId) {
        return contactRepository.findByBlockedByAndStatus(userId, ContactStatus.BLOCKED);
    }

    private void publishContactRequestSentEventAfterCommit(Contact contact) {
        ContactRequestSentEvent event = ContactRequestSentEvent.builder()
                .contactId(contact.getId())
                .requesterId(contact.getRequesterId())
                .recipientId(contact.getRecipientId())
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConstants.CONTACT_EXCHANGE,
                            RabbitMQConstants.CONTACT_REQUEST_SENT_KEY,
                            event
                    );
                }
            });
        } else {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTACT_EXCHANGE,
                    RabbitMQConstants.CONTACT_REQUEST_SENT_KEY,
                    event
            );
        }
    }
}
