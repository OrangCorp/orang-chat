package com.orang.messageservice.entity;

import lombok.Getter;

@Getter
public enum ReactionType {
    LIKE("👍"),
    HEART("❤️"),
    LAUGH("😂"),
    WOW("😮"),
    SAD("😢"),
    ANGRY("😡"),
    ORANG("🍊");

    private final String emoji;

    ReactionType(String emoji) {
        this.emoji = emoji;
    }

}