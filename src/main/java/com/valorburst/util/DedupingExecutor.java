package com.valorburst.util;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class DedupingExecutor implements Executor {

    private final ThreadPoolTaskExecutor delegate;
    private final Set<String> taskKeysInProgress = ConcurrentHashMap.newKeySet();

    public DedupingExecutor(ThreadPoolTaskExecutor delegate) {
        this.delegate = delegate;
    }

    public void execute(Runnable task, String taskKey) {
        if (!taskKeysInProgress.add(taskKey)) {
            // 已经有这个任务在跑或排队了，跳过
            return;
        }

        delegate.execute(() -> {
            try {
                task.run();
            } finally {
                taskKeysInProgress.remove(taskKey);
            }
        });
    }

    // 兼容普通 Runnable
    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }
}
