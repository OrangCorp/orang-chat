package com.orang.shared.autoconfigure;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SharedSchedulerAutoConfigurationTest {

    @Test
    @DisplayName("lockProvider creates a RedisLockProvider")
    void lockProvider_CreatesRedisLockProvider() {
        SharedSchedulerAutoConfiguration configuration = new SharedSchedulerAutoConfiguration();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        LockProvider lockProvider = configuration.lockProvider(connectionFactory);

        assertThat(lockProvider).isInstanceOf(RedisLockProvider.class);
    }

    @Test
    @DisplayName("missing lock provider configuration fails fast")
    void missingLockProviderConfiguration_FailsFast() {
        SharedSchedulerAutoConfiguration.MissingLockProviderConfiguration configuration =
                new SharedSchedulerAutoConfiguration.MissingLockProviderConfiguration();

        assertThatThrownBy(configuration::lockProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@EnableSchedulerLock");
    }
}