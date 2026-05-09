-- =====================================================
-- Notifications Table
-- Persistent notification inbox with grouping support
-- =====================================================
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Who receives this notification
                               user_id UUID NOT NULL,

    -- Notification type for filtering and display
                               type VARCHAR(50) NOT NULL,

    -- Display content (stored denormalized — no joins at read time)
                               title VARCHAR(255) NOT NULL,
                               body VARCHAR(1000) NOT NULL,

    -- Grouping: multiple events → single notification with count
    -- Example: "conv:{uuid}:messages", "conv:{uuid}:reactions"
    -- NULL means this notification type is never grouped
                               group_key VARCHAR(255),
                               group_count INT NOT NULL DEFAULT 1,

    -- Read state
                               read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at TIMESTAMP,

    -- Deep linking metadata
                               conversation_id UUID,
                               message_id UUID,
                               actor_id UUID,

    -- Timestamps
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Indexes
-- =====================================================

-- Primary inbox query: user's notifications, unread first, then by date
-- Covers: WHERE user_id = ? ORDER BY read ASC, created_at DESC
CREATE INDEX idx_notifications_user_read_date
    ON notifications(user_id, read, created_at DESC);

-- Upsert lookup: find existing unread grouped notification
-- Partial: only indexes unread rows with a group_key (much smaller)
CREATE INDEX idx_notifications_group_key
    ON notifications(user_id, group_key)
    WHERE group_key IS NOT NULL AND read = FALSE;

-- Cleanup job: delete old notifications by date
CREATE INDEX idx_notifications_created_at
    ON notifications(created_at);

-- Unread count query: COUNT WHERE user_id = ? AND read = FALSE
CREATE INDEX idx_notifications_unread
    ON notifications(user_id)
    WHERE read = FALSE;

-- =====================================================
-- Comments
-- =====================================================

COMMENT ON TABLE notifications IS 'Persistent notification inbox with conversation-level grouping';
COMMENT ON COLUMN notifications.type IS 'Event type: NEW_MESSAGE, REACTION, MENTION, etc.';
COMMENT ON COLUMN notifications.group_key IS 'Groups related notifications. conv:{id}:messages, conv:{id}:reactions. NULL = no grouping';
COMMENT ON COLUMN notifications.group_count IS 'How many events this notification represents. Incremented on group update';
COMMENT ON COLUMN notifications.read IS 'False = unread (appears at top of inbox)';
COMMENT ON COLUMN notifications.read_at IS 'When user marked this notification as read';
COMMENT ON COLUMN notifications.actor_id IS 'Who triggered this notification (sender, reactor, etc.)';
COMMENT ON COLUMN notifications.conversation_id IS 'For deep linking to conversation';
COMMENT ON COLUMN notifications.message_id IS 'Points to latest message in group, or specific message for mentions/reactions';