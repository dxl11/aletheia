package com.alibaba.aletheia.collector.storage;

import com.alibaba.aletheia.common.model.AgentData;
import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.MemoryEvent;
import com.alibaba.aletheia.common.model.RtEvent;
import com.alibaba.aletheia.common.model.ThreadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 数据存储层
 * 使用内存缓存存储最近的数据（可扩展到时序数据库）
 *
 * @author Aletheia Team
 */
public class DataStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStorage.class);

    /**
     * 最大缓存数据条数
     */
    private static final int MAX_CACHE_SIZE = 10000;

    /**
     * GC 事件缓存（按 PID 分组）
     */
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<GcEvent>> gcEventsCache = 
            new ConcurrentHashMap<>();

    /**
     * 线程事件缓存（按 PID 分组）
     */
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<ThreadEvent>> threadEventsCache = 
            new ConcurrentHashMap<>();

    /**
     * 内存事件缓存（按 PID 分组）
     */
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<MemoryEvent>> memoryEventsCache = 
            new ConcurrentHashMap<>();

    /**
     * RT 事件缓存（按 PID 分组）
     */
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<RtEvent>> rtEventsCache = 
            new ConcurrentHashMap<>();

    /**
     * 数据统计（按 PID）
     */
    private final ConcurrentHashMap<Long, DataStats> statsCache = new ConcurrentHashMap<>();

    /**
     * 存储 Agent 数据
     *
     * @param agentData Agent 数据
     */
    public void store(AgentData agentData) {
        if (agentData == null) {
            return;
        }

        long pid = agentData.getPid();

        try {
            // 存储 GC 事件
            if (agentData.getGcEvents() != null && !agentData.getGcEvents().isEmpty()) {
                ConcurrentLinkedQueue<GcEvent> gcQueue = gcEventsCache.computeIfAbsent(pid, 
                        k -> new ConcurrentLinkedQueue<>());
                for (GcEvent event : agentData.getGcEvents()) {
                    gcQueue.offer(event);
                    if (gcQueue.size() > MAX_CACHE_SIZE) {
                        gcQueue.poll(); // 移除最旧的数据
                    }
                }
            }

            // 存储线程事件
            if (agentData.getThreadEvent() != null) {
                ConcurrentLinkedQueue<ThreadEvent> threadQueue = threadEventsCache.computeIfAbsent(pid, 
                        k -> new ConcurrentLinkedQueue<>());
                threadQueue.offer(agentData.getThreadEvent());
                if (threadQueue.size() > MAX_CACHE_SIZE) {
                    threadQueue.poll();
                }
            }

            // 存储内存事件
            if (agentData.getMemoryEvent() != null) {
                ConcurrentLinkedQueue<MemoryEvent> memoryQueue = memoryEventsCache.computeIfAbsent(pid, 
                        k -> new ConcurrentLinkedQueue<>());
                memoryQueue.offer(agentData.getMemoryEvent());
                if (memoryQueue.size() > MAX_CACHE_SIZE) {
                    memoryQueue.poll();
                }
            }

            // 存储 RT 事件
            if (agentData.getRtEvents() != null && !agentData.getRtEvents().isEmpty()) {
                ConcurrentLinkedQueue<RtEvent> rtQueue = rtEventsCache.computeIfAbsent(pid, 
                        k -> new ConcurrentLinkedQueue<>());
                for (RtEvent event : agentData.getRtEvents()) {
                    rtQueue.offer(event);
                    if (rtQueue.size() > MAX_CACHE_SIZE) {
                        rtQueue.poll();
                    }
                }
            }

            // 更新统计信息
            updateStats(pid, agentData);
        } catch (Exception e) {
            LOGGER.error("Error storing agent data", e);
        }
    }

    /**
     * 获取 GC 事件列表
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量
     * @return GC 事件列表
     */
    public List<GcEvent> getGcEvents(Long pid, int limit) {
        if (pid != null) {
            ConcurrentLinkedQueue<GcEvent> queue = gcEventsCache.get(pid);
            if (queue == null) {
                return new ArrayList<>();
            }
            return queue.stream().limit(limit).collect(Collectors.toList());
        }

        // 返回所有 PID 的数据
        List<GcEvent> result = new ArrayList<>();
        for (ConcurrentLinkedQueue<GcEvent> queue : gcEventsCache.values()) {
            result.addAll(queue.stream().limit(limit).collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * 获取线程事件列表
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量
     * @return 线程事件列表
     */
    public List<ThreadEvent> getThreadEvents(Long pid, int limit) {
        if (pid != null) {
            ConcurrentLinkedQueue<ThreadEvent> queue = threadEventsCache.get(pid);
            if (queue == null) {
                return new ArrayList<>();
            }
            return queue.stream().limit(limit).collect(Collectors.toList());
        }

        List<ThreadEvent> result = new ArrayList<>();
        for (ConcurrentLinkedQueue<ThreadEvent> queue : threadEventsCache.values()) {
            result.addAll(queue.stream().limit(limit).collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * 获取内存事件列表
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量
     * @return 内存事件列表
     */
    public List<MemoryEvent> getMemoryEvents(Long pid, int limit) {
        if (pid != null) {
            ConcurrentLinkedQueue<MemoryEvent> queue = memoryEventsCache.get(pid);
            if (queue == null) {
                return new ArrayList<>();
            }
            return queue.stream().limit(limit).collect(Collectors.toList());
        }

        List<MemoryEvent> result = new ArrayList<>();
        for (ConcurrentLinkedQueue<MemoryEvent> queue : memoryEventsCache.values()) {
            result.addAll(queue.stream().limit(limit).collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * 获取 RT 事件列表
     *
     * @param pid 进程 ID（可选）
     * @param methodSignature 方法签名（可选）
     * @param limit 限制数量
     * @return RT 事件列表
     */
    public List<RtEvent> getRtEvents(Long pid, String methodSignature, int limit) {
        List<RtEvent> result = new ArrayList<>();

        if (pid != null) {
            ConcurrentLinkedQueue<RtEvent> queue = rtEventsCache.get(pid);
            if (queue != null) {
                result.addAll(queue);
            }
        } else {
            for (ConcurrentLinkedQueue<RtEvent> queue : rtEventsCache.values()) {
                result.addAll(queue);
            }
        }

        // 按方法签名过滤
        if (methodSignature != null && !methodSignature.isEmpty()) {
            result = result.stream()
                    .filter(event -> methodSignature.equals(event.getMethodSignature()))
                    .collect(Collectors.toList());
        }

        return result.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 获取最新的线程事件
     *
     * @param pid 进程 ID
     * @return 最新的线程事件
     */
    public ThreadEvent getLatestThreadEvent(Long pid) {
        ConcurrentLinkedQueue<ThreadEvent> queue = threadEventsCache.get(pid);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.peekLast();
    }

    /**
     * 获取最新的内存事件
     *
     * @param pid 进程 ID
     * @return 最新的内存事件
     */
    public MemoryEvent getLatestMemoryEvent(Long pid) {
        ConcurrentLinkedQueue<MemoryEvent> queue = memoryEventsCache.get(pid);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.peekLast();
    }

    /**
     * 获取数据统计信息
     *
     * @param pid 进程 ID（可选）
     * @return 数据统计信息
     */
    public DataStats getStats(Long pid) {
        if (pid != null) {
            return statsCache.getOrDefault(pid, new DataStats());
        }

        // 汇总所有 PID 的统计信息
        DataStats totalStats = new DataStats();
        for (DataStats stats : statsCache.values()) {
            totalStats.add(stats);
        }
        return totalStats;
    }

    /**
     * 更新统计信息
     *
     * @param pid 进程 ID
     * @param agentData Agent 数据
     */
    private void updateStats(Long pid, AgentData agentData) {
        DataStats stats = statsCache.computeIfAbsent(pid, k -> new DataStats());
        stats.update(agentData);
    }

    /**
     * 数据统计信息
     */
    public static class DataStats {
        private final AtomicLong gcEventCount = new AtomicLong(0);
        private final AtomicLong rtEventCount = new AtomicLong(0);
        private final AtomicLong threadEventCount = new AtomicLong(0);
        private final AtomicLong memoryEventCount = new AtomicLong(0);

        public void update(AgentData agentData) {
            if (agentData.getGcEvents() != null) {
                gcEventCount.addAndGet(agentData.getGcEvents().size());
            }
            if (agentData.getRtEvents() != null) {
                rtEventCount.addAndGet(agentData.getRtEvents().size());
            }
            if (agentData.getThreadEvent() != null) {
                threadEventCount.incrementAndGet();
            }
            if (agentData.getMemoryEvent() != null) {
                memoryEventCount.incrementAndGet();
            }
        }

        public void add(DataStats other) {
            gcEventCount.addAndGet(other.gcEventCount.get());
            rtEventCount.addAndGet(other.rtEventCount.get());
            threadEventCount.addAndGet(other.threadEventCount.get());
            memoryEventCount.addAndGet(other.memoryEventCount.get());
        }

        public long getGcEventCount() {
            return gcEventCount.get();
        }

        public long getRtEventCount() {
            return rtEventCount.get();
        }

        public long getThreadEventCount() {
            return threadEventCount.get();
        }

        public long getMemoryEventCount() {
            return memoryEventCount.get();
        }
    }
}
