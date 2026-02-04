package com.alibaba.aletheia.agent.diagnostic.sampler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 方法采样器
 * 用于诊断方法热点和执行时间分布
 * 支持采样方式，降低开销
 *
 * @author Aletheia Team
 */
public class MethodSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodSampler.class);

    private final Map<String, MethodInfo> methodInfoMap = new ConcurrentHashMap<>();
    private volatile double sampleRate = 0.01; // 默认 1% 采样率
    private final AtomicLong totalSamples = new AtomicLong(0);

    /**
     * 记录方法执行
     *
     * @param methodSignature 方法签名
     * @param executionTimeNs 执行时间（纳秒）
     */
    public void recordMethod(String methodSignature, long executionTimeNs) {
        // 采样判断
        if (ThreadLocalRandom.current().nextDouble() > sampleRate) {
            return;
        }

        try {
            MethodInfo info = methodInfoMap.computeIfAbsent(methodSignature,
                    k -> new MethodInfo(methodSignature));
            info.recordExecution(executionTimeNs);
            totalSamples.incrementAndGet();
        } catch (Exception e) {
            LOGGER.debug("Error recording method execution", e);
        }
    }

    /**
     * 获取方法热点（Top N）
     *
     * @param topN Top N
     * @return 方法热点列表
     */
    public List<MethodInfo> getHotMethods(int topN) {
        List<MethodInfo> list = new ArrayList<>(methodInfoMap.values());
        list.sort((a, b) -> Long.compare(b.totalTime.get(), a.totalTime.get()));
        return list.subList(0, Math.min(topN, list.size()));
    }

    /**
     * 获取所有方法信息
     */
    public Map<String, MethodInfo> getAllMethodInfo() {
        return new HashMap<>(methodInfoMap);
    }

    /**
     * 获取指定方法的信息
     */
    public MethodInfo getMethodInfo(String methodSignature) {
        return methodInfoMap.get(methodSignature);
    }

    /**
     * 设置采样率
     */
    public void setSampleRate(double sampleRate) {
        if (sampleRate < 0 || sampleRate > 1) {
            throw new IllegalArgumentException("Sample rate must be between 0 and 1");
        }
        this.sampleRate = sampleRate;
    }

    /**
     * 获取采样率
     */
    public double getSampleRate() {
        return sampleRate;
    }

    /**
     * 获取总采样次数
     */
    public long getTotalSamples() {
        return totalSamples.get();
    }

    /**
     * 清理统计数据
     */
    public void clear() {
        methodInfoMap.clear();
        totalSamples.set(0);
    }

    /**
     * 方法信息
     */
    public static class MethodInfo {
        public final String methodSignature;
        public final AtomicLong invokeCount = new AtomicLong(0);
        public final AtomicLong totalTime = new AtomicLong(0);
        public final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong maxTime = new AtomicLong(0);

        // 用于计算分位数
        private final List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());

        public MethodInfo(String methodSignature) {
            this.methodSignature = methodSignature;
        }

        public void recordExecution(long executionTimeNs) {
            invokeCount.incrementAndGet();
            totalTime.addAndGet(executionTimeNs);

            long currentMin = minTime.get();
            while (executionTimeNs < currentMin && !minTime.compareAndSet(currentMin, executionTimeNs)) {
                currentMin = minTime.get();
            }

            long currentMax = maxTime.get();
            while (executionTimeNs > currentMax && !maxTime.compareAndSet(currentMax, executionTimeNs)) {
                currentMax = maxTime.get();
            }

            // 记录执行时间用于分位数计算（限制大小，避免内存溢出）
            synchronized (executionTimes) {
                if (executionTimes.size() < 10000) {
                    executionTimes.add(executionTimeNs);
                }
            }
        }

        /**
         * 获取平均执行时间（纳秒）
         */
        public long getAvgTime() {
            long count = invokeCount.get();
            return count > 0 ? totalTime.get() / count : 0;
        }

        /**
         * 计算分位数（P50/P99/P999）
         */
        public long getPercentile(double percentile) {
            synchronized (executionTimes) {
                if (executionTimes.isEmpty()) {
                    return 0;
                }
                List<Long> sorted = new ArrayList<>(executionTimes);
                Collections.sort(sorted);
                int index = (int) Math.ceil(sorted.size() * percentile) - 1;
                index = Math.max(0, Math.min(index, sorted.size() - 1));
                return sorted.get(index);
            }
        }
    }
}
