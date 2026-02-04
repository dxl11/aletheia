package com.alibaba.aletheia.agent.diagnostic.collector;

import com.alibaba.aletheia.agent.collector.BaseCollector;
import com.alibaba.aletheia.agent.collector.gc.GcEventCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.common.model.GcEvent;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * GC 诊断采集器
 * 用于诊断 GC 相关问题（Full GC STW 时间长等）
 *
 * @author Aletheia Team
 */
public class GcDiagnosticCollector extends BaseCollector {

    private final MemoryMXBean memoryMXBean;
    private GcEventCollector gcEventCollector;

    public GcDiagnosticCollector(AgentConfig config) {
        super(config);
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    protected boolean isFeatureEnabled() {
        return config.isFeatureEnabled("GC");
    }

    @Override
    protected void doStart() throws Exception {
        // 初始化 GC 事件采集器
        gcEventCollector = new GcEventCollector(config);
        gcEventCollector.start();
    }

    @Override
    protected void doStop() throws Exception {
        if (gcEventCollector != null) {
            gcEventCollector.stop();
        }
    }

    /**
     * 获取最近的 GC 事件
     */
    public List<GcEvent> getRecentGcEvents(int count) {
        if (gcEventCollector == null) {
            return new ArrayList<>();
        }
        List<GcEvent> allEvents = gcEventCollector.getAndClearGcEvents();
        int size = allEvents.size();
        return allEvents.subList(Math.max(0, size - count), size);
    }

    /**
     * 获取 Full GC 事件
     */
    public List<GcEvent> getFullGcEvents() {
        if (gcEventCollector == null) {
            return new ArrayList<>();
        }
        List<GcEvent> allEvents = gcEventCollector.getAndClearGcEvents();
        List<GcEvent> fullGcEvents = new ArrayList<>();
        for (GcEvent event : allEvents) {
            if ("Full GC".equals(event.getGcType())) {
                fullGcEvents.add(event);
            }
        }
        return fullGcEvents;
    }

    /**
     * 获取 Finalizer 队列大小
     * 通过反射获取 java.lang.ref.Finalizer 队列
     */
    public int getFinalizerQueueSize() {
        try {
            Class<?> finalizerClass = Class.forName("java.lang.ref.Finalizer");
            Field queueField = finalizerClass.getDeclaredField("queue");
            queueField.setAccessible(true);
            Object queue = queueField.get(null);
            if (queue instanceof java.lang.ref.ReferenceQueue) {
                // 无法直接获取队列大小，返回 -1 表示无法获取
                return -1;
            }
        } catch (Exception e) {
            logger.debug("Failed to get finalizer queue size", e);
        }
        return -1;
    }

    /**
     * 获取堆内存使用情况
     */
    public HeapMemoryInfo getHeapMemoryInfo() {
        try {
            long used = memoryMXBean.getHeapMemoryUsage().getUsed();
            long max = memoryMXBean.getHeapMemoryUsage().getMax();
            long committed = memoryMXBean.getHeapMemoryUsage().getCommitted();
            return new HeapMemoryInfo(used, max, committed);
        } catch (Exception e) {
            logger.error("Error getting heap memory info", e);
            return new HeapMemoryInfo(0, 0, 0);
        }
    }

    /**
     * 堆内存信息
     */
    public static class HeapMemoryInfo {
        public final long used;
        public final long max;
        public final long committed;

        public HeapMemoryInfo(long used, long max, long committed) {
            this.used = used;
            this.max = max;
            this.committed = committed;
        }

        /**
         * 获取使用率
         */
        public double getUsageRate() {
            return max > 0 ? (double) used / max : 0.0;
        }
    }
}
