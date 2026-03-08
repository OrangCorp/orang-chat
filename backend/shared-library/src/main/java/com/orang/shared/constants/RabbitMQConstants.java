package com.orang.shared.constants;

public class RabbitMQConstants {
    public static final String MESSAGE_EXCHANGE = "message.exchange";
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    public static final String MESSAGE_PERSIST_QUEUE = "message.persist.queue";
    public static final String MESSAGE_DELIVERY_QUEUE = "message.delivery.queue";
    public static final String USER_STATUS_QUEUE = "user.status.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    public static final String MESSAGE_SENT_KEY = "message.sent";
    public static final String MESSAGE_DELIVERED_KEY = "message.delivered";
    public static final String MESSAGE_READ_KEY = "message.read";
    public static final String USER_ONLINE_KEY = "user.online";
    public static final String USER_OFFLINE_KEY = "user.offline";
    public static final String TYPING_INDICATOR_KEY = "typing.indicator";

    private RabbitMQConstants() {}
}