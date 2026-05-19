package com.orang.chatservice.listener;

import com.orang.shared.event.GroupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GroupEventListener groupEventListener;

    private UUID conversationId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
    }

    @Test
    void handleGroupEventBroadcastsToGroupTopic() {
        GroupEvent event = mock(GroupEvent.class);
        when(event.getConversationId()).thenReturn(conversationId);

        groupEventListener.handleGroupEvent(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                event
        );
    }
}
