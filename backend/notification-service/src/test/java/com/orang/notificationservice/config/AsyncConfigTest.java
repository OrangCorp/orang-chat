package com.orang.notificationservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    @DisplayName("getAsyncExecutor configures the expected thread pool")
    void getAsyncExecutorConfiguresExpectedThreadPool() {
        Executor executor = new AsyncConfig().getAsyncExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(5);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(20);
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(100);
    }
}