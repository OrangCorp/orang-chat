package com.orang.messageservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MentionParserService {

    private static final Pattern MENTION_PATTERN =
            Pattern.compile(
                    "@([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})",
                    Pattern.CASE_INSENSITIVE
            ); // UUID-like pattern

    public Set<UUID> extractMentions(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }

        Set<UUID> mentionedIds = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            try {
                mentionedIds.add(UUID.fromString(matcher.group(1)));
            } catch (IllegalArgumentException e) {
                log.debug("Skipping malformed UUID in mention: {}", matcher.group(1));
            }
        }

        return mentionedIds;
    }

    public Set<UUID> validateMentions(Set<UUID> mentionedIds, Set<UUID> participantIds) {
        return mentionedIds.stream()
                .filter(participantIds::contains)
                .collect(Collectors.toSet());
    }
}
