package com.orang.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads kept alive
        executor.setCorePoolSize(5);

        // Max pool size - maximum threads created
        executor.setMaxPoolSize(20);

        // Queue capacity - how many tasks to queue before creating new threads
        executor.setQueueCapacity(100);

        // Thread name prefix (helpful for debugging logs)
        executor.setThreadNamePrefix("notification-async-");

        // Reject policy when pool is full
        // CallerRunsPolicy = run in caller's thread (prevents task loss)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Max time to wait for shutdown
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Async executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}