package com.prolizwebservices.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async configuration for parallel SOAP calls
 * Dramatically improves cache loading performance
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Value("${async.soap.core-pool-size:10}")
    private int corePoolSize;
    
    @Value("${async.soap.max-pool-size:25}")
    private int maxPoolSize;
    
    @Value("${async.soap.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${async.soap.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * SOAP calls için optimize edilmiş thread pool
     */
    @Bean(name = "soapTaskExecutor")
    public Executor soapTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("SOAP-Thread-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Genel async işlemler için thread pool
     */
    @Bean(name = "generalTaskExecutor") 
    public Executor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("Async-Thread-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return generalTaskExecutor();
    }
}
