package com.orang.shared.autoconfigure;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@AutoConfiguration
@ConditionalOnClass({LockProvider.class, RedisConnectionFactory.class})
@ConditionalOnMissingBean(RedisConnectionFactory.class)
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SharedSchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "orangchat-lock");
    }
}