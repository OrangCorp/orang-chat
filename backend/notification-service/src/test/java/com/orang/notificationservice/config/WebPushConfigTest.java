package com.orang.notificationservice.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushConfigTest {

    @AfterEach
    void tearDown() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @Test
    @DisplayName("init registers Bouncy Castle and pushService configures VAPID values")
    void initRegistersBcAndPushServiceConfiguresVapidValues() throws Exception {
        WebPushConfig config = new WebPushConfig();
        ReflectionTestUtils.setField(config, "vapidPublicKey", "BJpbtLz1TmeDHqF-SHenqYTjmGFD_rOAomOUXE-aHQ0g8_sENBPI8PiECPHo3Hn566y8BcCS52szGZtQWV0A_vU");
        ReflectionTestUtils.setField(config, "vapidPrivateKey", "Hf17E44Uk_VegHbti_boVg68zgFQV6pXZ8Pg3Ct5ylk");
        ReflectionTestUtils.setField(config, "vapidSubject", "mailto:test@example.com");

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        config.init();

        PushService pushService = config.pushService();

        assertThat(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isNotNull();
        assertThat(pushService).isNotNull();
        assertThat(config.vapidPublicKey()).isEqualTo("BJpbtLz1TmeDHqF-SHenqYTjmGFD_rOAomOUXE-aHQ0g8_sENBPI8PiECPHo3Hn566y8BcCS52szGZtQWV0A_vU");
    }
}