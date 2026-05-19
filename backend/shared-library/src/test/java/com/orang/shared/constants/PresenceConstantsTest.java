package com.orang.shared.constants;

import com.orang.shared.event.GroupMemberEvent;
import com.orang.shared.event.GroupUpdatedEvent;
import com.orang.shared.event.MessagePinnedEvent;
import com.orang.shared.event.MessageReactionEvent;
import com.orang.shared.presence.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceConstantsTest {

    @Test
    @DisplayName("builds expected redis keys")
    void keyHelpers_BuildExpectedValues() {
        assertThat(PresenceConstants.userSessionsKey("42")).isEqualTo("user:42:sessions");
        assertThat(PresenceConstants.userLastActivityKey("42")).isEqualTo("user:42:lastActivity");
        assertThat(PresenceConstants.sessionMetaKey("abc")).isEqualTo("session:abc:meta");
    }

    @Test
    @DisplayName("private constructor remains inaccessible by default")
    void privateConstructor_IsPrivate() throws Exception {
        Constructor<PresenceConstants> constructor = PresenceConstants.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    @DisplayName("presence status enum constants are available")
    void userStatus_EnumValues_AreStable() {
        assertThat(UserStatus.values()).containsExactly(UserStatus.ONLINE, UserStatus.AWAY, UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("group member event type enum constants are available")
    void groupMemberEventType_EnumValues_AreStable() {
        assertThat(GroupMemberEvent.EventType.values())
                .containsExactly(
                        GroupMemberEvent.EventType.MEMBER_ADDED,
                        GroupMemberEvent.EventType.MEMBER_REMOVED,
                        GroupMemberEvent.EventType.MEMBER_LEFT,
                        GroupMemberEvent.EventType.ADMIN_PROMOTED,
                        GroupMemberEvent.EventType.ADMIN_DEMOTED
                );
    }

    @Test
    @DisplayName("message reaction action enum constants are available")
    void messageReactionAction_EnumValues_AreStable() {
        assertThat(MessageReactionEvent.Action.values())
                .containsExactly(
                        MessageReactionEvent.Action.ADDED,
                        MessageReactionEvent.Action.REMOVED,
                        MessageReactionEvent.Action.CHANGED
                );
    }

    @Test
    @DisplayName("message pinned action enum constants are available")
    void messagePinnedAction_EnumValues_AreStable() {
        assertThat(MessagePinnedEvent.Action.values())
                .containsExactly(
                        MessagePinnedEvent.Action.PINNED,
                        MessagePinnedEvent.Action.UNPINNED
                );
    }

    @Test
    @DisplayName("group update type enum constants are available")
    void groupUpdateType_EnumValues_AreStable() {
        assertThat(GroupUpdatedEvent.UpdateType.values())
                .containsExactly(
                        GroupUpdatedEvent.UpdateType.RENAMED,
                        GroupUpdatedEvent.UpdateType.DELETED
                );
    }
}
