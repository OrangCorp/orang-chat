package com.orang.messageservice.service;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentionParserServiceTest {

    private final MentionParserService mentionParserService = new MentionParserService();

    @Test
    void extractMentionsReturnsEmptyForNullAndBlank() {
        assertTrue(mentionParserService.extractMentions(null).isEmpty());
        assertTrue(mentionParserService.extractMentions("   ").isEmpty());
    }

    @Test
    void extractMentionsParsesUuidMentionsCaseInsensitiveAndDeduplicates() {
        UUID userId = UUID.randomUUID();
        String content = "hello @" + userId + " and again @" + userId.toString().toUpperCase();

        Set<UUID> mentioned = mentionParserService.extractMentions(content);

        assertEquals(1, mentioned.size());
        assertTrue(mentioned.contains(userId));
    }

    @Test
    void validateMentionsKeepsOnlyConversationParticipants() {
        UUID participant = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();

        Set<UUID> validated = mentionParserService.validateMentions(
                Set.of(participant, outsider),
                Set.of(participant)
        );

        assertEquals(Set.of(participant), validated);
    }
}