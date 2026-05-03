package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.ConversationParticipant;
import com.orang.messageservice.entity.ConversationParticipantId;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.MessageMention;
import com.orang.messageservice.mapper.MessageMapper;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.messageservice.repository.MessageMentionRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private ConversationService conversationService;

	@Mock
	private MessageMapper messageMapper;

	@Mock
	private MessageEventPublisher messageEventPublisher;

	@Mock
	private AttachmentService attachmentService;

	@Mock
	private MentionParserService mentionParserService;

	@Mock
	private MessageMentionRepository messageMentionRepository;

	@InjectMocks
	private MessageService messageService;

	private UUID conversationId;
	private UUID senderId;
	private UUID otherUserId;
	private Conversation conversation;

	@BeforeEach
	void setUp() {
		conversationId = UUID.randomUUID();
		senderId = UUID.randomUUID();
		otherUserId = UUID.randomUUID();

		conversation = Conversation.builder()
				.id(conversationId)
				.participants(Set.of(
						participant(conversationId, senderId),
						participant(conversationId, otherUserId)
				))
				.build();
	}

	@Test
	void getMessagesForConversationThrowsWhenConversationMissing() {
		UUID requesterId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> messageService.getMessagesForConversation(conversationId, requesterId, pageable));
	}

	@Test
	void getMessagesForConversationThrowsWhenRequesterNotParticipant() {
		UUID requesterId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

		assertThrows(ForbiddenException.class,
				() -> messageService.getMessagesForConversation(conversationId, requesterId, pageable));
	}

	@Test
	void getMessagesForConversationReturnsMappedPageForParticipant() {
		Pageable pageable = PageRequest.of(0, 10);
		Message message = Message.builder()
				.id(UUID.randomUUID())
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.createdAt(LocalDateTime.now())
				.build();

		MessageResponse response = MessageResponse.builder()
				.id(message.getId())
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
		when(messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable))
				.thenReturn(new PageImpl<>(List.of(message)));
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(response);

		Page<MessageResponse> result = messageService.getMessagesForConversation(conversationId, senderId, pageable);

		assertEquals(1, result.getTotalElements());
		assertEquals(response.getId(), result.getContent().getFirst().getId());
	}

	@Test
	void saveMessageThrowsWhenConversationMissing() {
		when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> messageService.saveMessage(conversationId, senderId, "hello", List.of(), null, null));
	}

	@Test
	void saveMessageThrowsWhenSenderNotParticipant() {
		UUID outsider = UUID.randomUUID();
		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

		assertThrows(IllegalArgumentException.class,
				() -> messageService.saveMessage(conversationId, outsider, "hello", List.of(), null, null));
	}

	@Test
	void saveMessageKeepsReplyReferenceWhenReplyExists() {
		UUID replyToId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();

		Message saved = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.replyToMessageId(replyToId)
				.build();

		MessageResponse mapped = MessageResponse.builder().id(messageId).build();

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
		when(messageRepository.existsByIdAndConversationId(replyToId, conversationId)).thenReturn(true);
		when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(saved);
		when(mentionParserService.extractMentions("hello")).thenReturn(Set.of());
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

		messageService.saveMessage(conversationId, senderId, "hello", List.of(), replyToId, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(messageRepository).saveAndFlush(captor.capture());
		assertEquals(replyToId, captor.getValue().getReplyToMessageId());
	}

	@Test
	void saveMessageDropsReplyReferenceWhenReplyMissing() {
		UUID replyToId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();

		Message saved = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		MessageResponse mapped = MessageResponse.builder().id(messageId).build();

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
		when(messageRepository.existsByIdAndConversationId(replyToId, conversationId)).thenReturn(false);
		when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(saved);
		when(mentionParserService.extractMentions("hello")).thenReturn(Set.of());
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

		messageService.saveMessage(conversationId, senderId, "hello", List.of(), replyToId, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(messageRepository).saveAndFlush(captor.capture());
		assertNull(captor.getValue().getReplyToMessageId());
	}

	@Test
	void saveMessageLinksAttachmentsAndReloadsEntity() {
		UUID messageId = UUID.randomUUID();
		UUID attachmentId = UUID.randomUUID();
		List<UUID> attachmentIds = List.of(attachmentId);

		Message flushed = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("with attachment")
				.build();

		Message reloaded = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("with attachment")
				.build();

		MessageResponse mapped = MessageResponse.builder().id(messageId).build();

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
		when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(flushed);
		when(messageRepository.findById(messageId)).thenReturn(Optional.of(reloaded));
		when(mentionParserService.extractMentions("with attachment")).thenReturn(Set.of());
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

		messageService.saveMessage(conversationId, senderId, "with attachment", attachmentIds, null, null);

		verify(attachmentService).linkAttachmentsToMessage(attachmentIds, messageId, senderId);
		verify(messageRepository).findById(messageId);
	}

	@Test
	void saveMessagePersistsAndPublishesMentionsForValidParticipants() {
		UUID messageId = UUID.randomUUID();
		UUID mentionedUser = otherUserId;

		Message saved = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hi @" + mentionedUser)
				.build();

		MessageResponse mapped = MessageResponse.builder().id(messageId).build();

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
		when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(saved);
		when(mentionParserService.extractMentions(saved.getContent())).thenReturn(Set.of(mentionedUser, UUID.randomUUID()));
		when(mentionParserService.validateMentions(anySet(), anySet())).thenReturn(Set.of(mentionedUser));
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

		messageService.saveMessage(conversationId, senderId, saved.getContent(), List.of(), null, null);

		ArgumentCaptor<List<MessageMention>> mentionsCaptor = ArgumentCaptor.forClass(List.class);
		verify(messageMentionRepository).saveAll(mentionsCaptor.capture());
		assertEquals(1, mentionsCaptor.getValue().size());
		assertEquals(mentionedUser, mentionsCaptor.getValue().getFirst().getMentionedUserId());

		verify(messageEventPublisher).publishMentionEvent(
				eq(messageId),
				eq(conversationId),
				eq(senderId),
				eq(mentionedUser),
				eq(saved.getContent())
		);
	}

	@Test
	void saveMessageSkipsMentionSaveWhenNoValidMentions() {
		UUID messageId = UUID.randomUUID();

		Message saved = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("no valid mentions")
				.build();

		MessageResponse mapped = MessageResponse.builder().id(messageId).build();

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
		when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(saved);
		when(mentionParserService.extractMentions(saved.getContent())).thenReturn(Set.of(UUID.randomUUID()));
		when(mentionParserService.validateMentions(anySet(), anySet())).thenReturn(Set.of());
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

		messageService.saveMessage(conversationId, senderId, saved.getContent(), List.of(), null, null);

		verify(messageMentionRepository, never()).saveAll(anyList());
		verify(messageEventPublisher, never()).publishMentionEvent(any(), any(), any(), any(), any());
	}

	@Test
	void editMessageThrowsWhenCallerIsNotSender() {
		UUID messageId = UUID.randomUUID();
		Message existing = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(existing));

		assertThrows(ForbiddenException.class,
				() -> messageService.editMessage(messageId, UUID.randomUUID(), "updated"));
	}

	@Test
	void editMessageUpdatesAndPublishesEvent() {
		UUID messageId = UUID.randomUUID();
		Message existing = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		MessageResponse mapped = MessageResponse.builder().id(messageId).content("updated").build();

		when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(existing));
		when(messageRepository.save(existing)).thenReturn(existing);
		when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

		MessageResponse result = messageService.editMessage(messageId, senderId, "updated");

		assertEquals(messageId, result.getId());
		assertEquals("updated", existing.getContent());
		verify(messageEventPublisher).publishMessageEdited(
				eq(messageId),
				eq(conversationId),
				eq(senderId),
				eq("updated"),
				eq(existing.getEditedAt())
		);
	}

	@Test
	void deleteMessageThrowsWhenCallerIsNeitherSenderNorAdmin() {
		UUID messageId = UUID.randomUUID();
		UUID outsider = UUID.randomUUID();

		Message existing = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(existing));
		when(conversationService.isAdmin(conversationId, outsider)).thenReturn(false);

		assertThrows(ForbiddenException.class,
				() -> messageService.deleteMessage(messageId, outsider));
	}

	@Test
	void deleteMessageAllowsSender() {
		UUID messageId = UUID.randomUUID();

		Message existing = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(existing));
		when(messageRepository.save(existing)).thenReturn(existing);

		messageService.deleteMessage(messageId, senderId);

		verify(messageEventPublisher).publishMessageDeleted(
				eq(messageId),
				eq(conversationId),
				eq(senderId),
				eq(existing.getDeletedAt())
		);
	}

	@Test
	void deleteMessageAllowsAdmin() {
		UUID messageId = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();

		Message existing = Message.builder()
				.id(messageId)
				.conversationId(conversationId)
				.senderId(senderId)
				.content("hello")
				.build();

		when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(existing));
		when(conversationService.isAdmin(conversationId, adminId)).thenReturn(true);
		when(messageRepository.save(existing)).thenReturn(existing);

		messageService.deleteMessage(messageId, adminId);

		verify(messageEventPublisher).publishMessageDeleted(
				eq(messageId),
				eq(conversationId),
				eq(adminId),
				eq(existing.getDeletedAt())
		);
	}

	private ConversationParticipant participant(UUID convId, UUID userId) {
		return ConversationParticipant.builder()
				.id(ConversationParticipantId.builder()
						.conversationId(convId)
						.userId(userId)
						.build())
				.role(ConversationParticipant.ParticipantRole.MEMBER)
				.joinedAt(LocalDateTime.now())
				.build();
	}
}
