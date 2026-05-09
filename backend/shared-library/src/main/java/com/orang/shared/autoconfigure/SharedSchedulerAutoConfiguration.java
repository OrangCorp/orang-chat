package com.orang.shared.autoconfigure;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@AutoConfiguration
@AutoConfigureAfter({RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
@ConditionalOnClass(LockProvider.class)
public class SharedSchedulerAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "orangchat-lock");
    }

    /**
     * This now only runs if someone explicitly added @EnableSchedulerLock
     * but forgot to provide Redis.
     */
    @Configuration
    @ConditionalOnMissingBean(LockProvider.class)
    @ConditionalOnBean(annotation = EnableSchedulerLock.class)
    public static class MissingLockProviderConfiguration {
        @Bean
        public LockProvider lockProvider() {
            throw new IllegalStateException(
                    "ShedLock is enabled via @EnableSchedulerLock but no RedisLockProvider could be created. " +
                            "Check your Redis configuration!"
            );
        }
    }
}
