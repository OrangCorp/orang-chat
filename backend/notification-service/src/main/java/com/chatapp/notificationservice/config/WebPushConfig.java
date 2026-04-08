package com.chatapp.notificationservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
@Slf4j
public class WebPushConfig {

    @Value("${web-push.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${web-push.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${web-push.vapid.subject}")
    private String vapidSubject;

    @PostConstruct
    public void init() {
        // Add Bouncy Castle provider if not already present
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Registered Bouncy Castle security provider");
        }
    }

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        PushService pushService = new PushService();
        pushService.setPublicKey(vapidPublicKey);
        pushService.setPrivateKey(vapidPrivateKey);
        pushService.setSubject(vapidSubject);

        log.info("Web Push Service initialized");
        log.info("VAPID subject: {}", vapidSubject);
        log.debug("VAPID public key: {}...", vapidPublicKey.substring(0, Math.min(20, vapidPublicKey.length())));

        return pushService;
    }

    @Bean
    public String vapidPublicKey() {
        return vapidPublicKey;
    }
}
