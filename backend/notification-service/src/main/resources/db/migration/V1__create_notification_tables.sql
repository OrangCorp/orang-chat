-- =====================================================
-- Push Subscriptions Table
-- Stores browser push notification subscriptions
-- =====================================================
CREATE TABLE push_subscriptions (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Who owns this subscription
                                    user_id UUID NOT NULL,

    -- Push service endpoint URL (unique per browser/device)
    -- Example: https://fcm.googleapis.com/fcm/send/abc123...
                                    endpoint VARCHAR(500) NOT NULL UNIQUE,

    -- Browser's public key for payload encryption (ECDH P-256)
                                    p256dh_key VARCHAR(150) NOT NULL,

    -- Authentication secret for payload encryption
                                    auth_key VARCHAR(50) NOT NULL,

    -- When subscription expires (if provided by browser)
                                    expires_at TIMESTAMP,

    -- Metadata
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    last_used_at TIMESTAMP,
                                    user_agent VARCHAR(500)
);

-- Index for finding all subscriptions for a user
CREATE INDEX idx_push_subscriptions_user_id ON push_subscriptions(user_id);

-- Index for looking up subscription by endpoint
CREATE INDEX idx_push_subscriptions_endpoint ON push_subscriptions(endpoint);


-- =====================================================
-- Notification Preferences Table
-- Per-conversation mute settings
-- =====================================================
CREATE TABLE notification_preferences (
    -- Composite primary key: one row per user+conversation pair
                                          user_id UUID NOT NULL,
                                          conversation_id UUID NOT NULL,

    -- Is this conversation muted?
                                          muted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Temporary mute end time (null = permanent mute)
                                          muted_until TIMESTAMP,

    -- When was this preference last updated
                                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          PRIMARY KEY (user_id, conversation_id)
);

-- Index for finding all muted conversations for a user
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);


-- =====================================================
-- Documentation
-- =====================================================
COMMENT ON TABLE push_subscriptions IS 'Browser push notification subscriptions';
COMMENT ON COLUMN push_subscriptions.endpoint IS 'Push service URL - unique per browser/device';
COMMENT ON COLUMN push_subscriptions.p256dh_key IS 'Browser public key for ECDH encryption';
COMMENT ON COLUMN push_subscriptions.auth_key IS 'Authentication secret for message encryption';
COMMENT ON COLUMN push_subscriptions.expires_at IS 'Subscription expiry (if browser provides it)';
COMMENT ON COLUMN push_subscriptions.user_agent IS 'Browser/device info for debugging';

COMMENT ON TABLE notification_preferences IS 'Per-conversation notification settings';
COMMENT ON COLUMN notification_preferences.muted IS 'If true, no push notifications for this conversation';
COMMENT ON COLUMN notification_preferences.muted_until IS 'If set, auto-unmute after this time';