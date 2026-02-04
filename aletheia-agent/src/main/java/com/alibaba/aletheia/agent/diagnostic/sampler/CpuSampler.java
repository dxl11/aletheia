package com.alibaba.aletheia.agent.diagnostic.sampler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CPU 采样器
 * 用于诊断 CPU 飙高问题
 * 通过定时采样线程 CPU 时间，识别 CPU 占用最高的线程
 *
 * @author Aletheia Team
 */
public class CpuSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpuSampler.class);

    private final ThreadMXBean threadMXBean;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, ThreadCpuInfo> threadCpuMap = new ConcurrentHashMap<>();
    private volatile boolean started = false;

    // 采样间隔（毫秒）
    private volatile long sampleIntervalMs = 100; // 默认 100ms

    public CpuSampler() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        if (!threadMXBean.isThreadCpuTimeSupported()) {
            LOGGER.warn("Thread CPU time is not supported on this JVM");
        }
        if (threadMXBean.isThreadCpuTimeSupported() && !threadMXBean.isThreadCpuTimeEnabled()) {
            threadMXBean.setThreadCpuTimeEnabled(true);
        }
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "Aletheia-CpuSampler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动 CPU 采样
     */
    public synchronized void start() {
        if (started) {
            return;
        }

        scheduler.scheduleAtFixedRate(this::sample, 0, sampleIntervalMs, TimeUnit.MILLISECONDS);
        started = true;
        LOGGER.info("CpuSampler started, sampleInterval: {}ms", sampleIntervalMs);
    }

    /**
     * 停止 CPU 采样
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        started = false;
        LOGGER.info("CpuSampler stopped");
    }

    /**
     * 执行采样
     */
    private void sample() {
        if (!threadMXBean.isThreadCpuTimeSupported()) {
            return;
        }

        try {
            long[] allThreadIds = threadMXBean.getAllThreadIds();
            long currentTime = System.currentTimeMillis();

            for (long threadId : allThreadIds) {
                long cpuTime = threadMXBean.getThreadCpuTime(threadId);
                if (cpuTime == -1) {
                    continue;
                }

                ThreadCpuInfo info = threadCpuMap.computeIfAbsent(threadId,
                        k -> new ThreadCpuInfo(threadId, cpuTime, currentTime));

                // 计算 CPU 时间增量（纳秒转毫秒）
                long deltaCpuTime = cpuTime - info.lastCpuTime;
                long deltaWallTime = currentTime - info.lastSampleTime;

                if (deltaWallTime > 0) {
                    // CPU 使用率 = CPU时间增量 / 墙钟时间增量
                    double cpuUsage = (deltaCpuTime / 1_000_000.0) / deltaWallTime;
                    info.cpuUsage = cpuUsage;
                    info.totalCpuTime = cpuTime;
                    info.lastCpuTime = cpuTime;
                    info.lastSampleTime = currentTime;
                }
            }

            // 清理已不存在的线程
            Set<Long> currentThreadIds = new HashSet<>();
            for (long threadId : allThreadIds) {
                currentThreadIds.add(threadId);
            }
            threadCpuMap.keySet().retainAll(currentThreadIds);

        } catch (Exception e) {
            LOGGER.warn("Error sampling CPU time", e);
        }
    }

    /**
     * 获取 CPU 占用最高的线程（Top N）
     *
     * @param topN Top N
     * @return CPU 占用最高的线程列表
     */
    public List<ThreadCpuInfo> getTopCpuThreads(int topN) {
        List<ThreadCpuInfo> list = new ArrayList<>(threadCpuMap.values());
        list.sort((a, b) -> Double.compare(b.cpuUsage, a.cpuUsage));
        return list.subList(0, Math.min(topN, list.size()));
    }

    /**
     * 获取所有线程的 CPU 信息
     */
    public Map<Long, ThreadCpuInfo> getAllThreadCpuInfo() {
        return new HashMap<>(threadCpuMap);
    }

    /**
     * 获取指定线程的 CPU 信息
     */
    public ThreadCpuInfo getThreadCpuInfo(long threadId) {
        return threadCpuMap.get(threadId);
    }

    /**
     * 设置采样间隔
     */
    public void setSampleIntervalMs(long sampleIntervalMs) {
        this.sampleIntervalMs = sampleIntervalMs;
        // 如果已启动，需要重启
        if (started) {
            stop();
            start();
        }
    }

    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * 线程 CPU 信息
     */
    public static class ThreadCpuInfo {
        public final long threadId;
        public long totalCpuTime; // 总 CPU 时间（纳秒）
        public double cpuUsage; // CPU 使用率（0.0 - 1.0）
        public long lastCpuTime;
        public long lastSampleTime;

        public ThreadCpuInfo(long threadId, long cpuTime, long sampleTime) {
            this.threadId = threadId;
            this.totalCpuTime = cpuTime;
            this.lastCpuTime = cpuTime;
            this.lastSampleTime = sampleTime;
            this.cpuUsage = 0.0;
        }
    }
}
