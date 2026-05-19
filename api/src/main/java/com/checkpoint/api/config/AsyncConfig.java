package com.checkpoint.api.config;

import java.time.Clock;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous event processing.
 * Enables {@code @Async} support and provides a custom thread pool executor.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a thread pool task executor for async event listeners.
     *
     * @return the configured executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("gamification-");
        executor.initialize();
        return executor;
    }

    /**
     * Exposes the system UTC clock as a bean so time-dependent services can be
     * tested deterministically by replacing it with a fixed clock.
     *
     * @return a system clock in UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
