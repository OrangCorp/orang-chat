package com.orang.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orang.notificationservice.dto.NotificationPayload;
import com.orang.notificationservice.entity.PushSubscription;
import com.orang.notificationservice.repository.NotificationPreferencesRepository;
import com.orang.notificationservice.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    private PushService pushService;

    @Mock
    private PushSubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationPreferencesService preferencesService;

    @Mock
    private ObjectMapper objectMapper;

    private WebPushService webPushService;
    private String subscriptionPublicKey;
    private String subscriptionAuthKey;

    private static final UUID USER_ID = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    private static final UUID OTHER_USER_ID = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    private static final UUID CONVERSATION_ID = UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd");
    private static final UUID MESSAGE_ID = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    @BeforeEach
    void setUp() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        generateSubscriptionKeys();
        webPushService = new WebPushService(pushService, subscriptionRepository, preferencesService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @Test
    @DisplayName("sendToUser returns early when no subscriptions exist")
    void sendToUserReturnsEarlyWhenNoSubscriptionsExist() throws Exception {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        webPushService.sendToUser(USER_ID, payload());

        verify(pushService, never()).send(any());
    }

    @Test
    @DisplayName("sendToUser updates last used timestamp on success")
    void sendToUserUpdatesLastUsedTimestampOnSuccess() throws Exception {
        PushSubscription subscription = subscription(USER_ID);
        HttpResponse response = responseWithStatus(201);

        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(subscription));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pushService.send(any())).thenReturn(response);

        webPushService.sendToUser(USER_ID, payload());

        assertThat(subscription.getLastUsedAt()).isNotNull();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("sendToUser deletes expired subscriptions for 410 and 404 responses")
    void sendToUserDeletesExpiredSubscriptionsFor410And404Responses() throws Exception {
        PushSubscription expired = subscription(USER_ID);
        PushSubscription missing = subscription(OTHER_USER_ID);
        HttpResponse expiredResponse = responseWithStatus(410);
        HttpResponse missingResponse = responseWithStatus(404);

        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(expired), List.of(missing));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(pushService.send(any())).thenReturn(expiredResponse, missingResponse);

        webPushService.sendToUser(USER_ID, payload());
        webPushService.sendToUser(USER_ID, payload());

        verify(subscriptionRepository).delete(expired);
        verify(subscriptionRepository).delete(missing);
    }

    @Test
    @DisplayName("sendToConversation excludes sender and muted recipients")
    void sendToConversationExcludesSenderAndMutedRecipients() {
        WebPushService spyService = spy(webPushService);
        doNothing().when(spyService).sendToUser(any(), any());
        UUID mutedUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID allowedUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(preferencesService.isMuted(mutedUserId, CONVERSATION_ID)).thenReturn(true);
        when(preferencesService.isMuted(allowedUserId, CONVERSATION_ID)).thenReturn(false);

        spyService.sendToConversation(
                CONVERSATION_ID,
            Set.of(USER_ID, mutedUserId, allowedUserId),
                USER_ID,
                payload());

        verify(spyService, never()).sendToUser(eq(USER_ID), any());
        verify(spyService, never()).sendToUser(eq(mutedUserId), any());
        verify(spyService).sendToUser(eq(allowedUserId), any());
        verify(preferencesService).isMuted(mutedUserId, CONVERSATION_ID);
        verify(preferencesService).isMuted(allowedUserId, CONVERSATION_ID);
    }

    private NotificationPayload payload() {
        return NotificationPayload.builder()
                .title("New Message")
                .body("Hello")
                .data(NotificationPayload.NotificationData.builder()
                        .type("new_message")
                        .conversationId(CONVERSATION_ID)
                        .messageId(MESSAGE_ID)
                        .url("/conversations/" + CONVERSATION_ID)
                        .build())
                .build();
    }

    private PushSubscription subscription(UUID userId) {
        return PushSubscription.builder()
                .id(UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111"))
                .userId(userId)
                .endpoint("https://push.example.com/subscriptions/endpoint-1234567890")
                .p256dhKey(subscriptionPublicKey)
                .authKey(subscriptionAuthKey)
                .createdAt(LocalDateTime.of(2026, 5, 5, 9, 0))
                .lastUsedAt(LocalDateTime.of(2026, 5, 5, 9, 30))
                .build();
    }

    private void generateSubscriptionKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("prime256v1"));
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            subscriptionPublicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    Utils.encode((ECPublicKey) keyPair.getPublic())
            );
            subscriptionAuthKey = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | java.security.InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse responseWithStatus(int statusCode) {
        HttpResponse response = org.mockito.Mockito.mock(HttpResponse.class);
        StatusLine statusLine = org.mockito.Mockito.mock(StatusLine.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        return response;
    }
}