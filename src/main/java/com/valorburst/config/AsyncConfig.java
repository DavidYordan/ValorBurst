package com.valorburst.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.valorburst.util.DedupingExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "missionExecutor")
    public DedupingExecutor missionExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        delegate.setCorePoolSize(5);
        // 最大线程数
        delegate.setMaxPoolSize(10);
        // 队列容量
        delegate.setQueueCapacity(100);
        // 线程空闲时间
        delegate.setKeepAliveSeconds(60);
        // 线程池中的线程名前缀
        delegate.setThreadNamePrefix("MissionExecutor-");
        // 初始化
        delegate.initialize();
        return new DedupingExecutor(delegate);
    }
}
