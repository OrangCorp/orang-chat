package com.orang.messageservice.dto;

import com.orang.messageservice.entity.ReactionType;

public interface ReactionCountProjection {

    ReactionType getReactionType();

    Long getCount();
}
