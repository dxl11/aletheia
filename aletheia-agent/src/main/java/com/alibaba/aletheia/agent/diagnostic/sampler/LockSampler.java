package com.alibaba.aletheia.agent.diagnostic.sampler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 锁采样器
 * 用于诊断锁竞争问题
 * 记录锁等待时间、锁持有时间、锁竞争统计
 *
 * @author Aletheia Team
 */
public class LockSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockSampler.class);

    private final ThreadMXBean threadMXBean;
    private final Map<String, LockInfo> lockInfoMap = new ConcurrentHashMap<>();
    private final Map<Long, ThreadLockInfo> threadLockMap = new ConcurrentHashMap<>();

    // 锁竞争统计
    private final AtomicLong totalLockContentionCount = new AtomicLong(0);
    private final AtomicLong totalLockWaitTime = new AtomicLong(0);

    public LockSampler() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * 记录锁获取
     *
     * @param lockIdentity 锁标识（类名+字段名或对象hashCode）
     * @param threadId 线程ID
     */
    public void recordLockAcquire(String lockIdentity, long threadId) {
        try {
            long currentTime = System.nanoTime();
            LockInfo lockInfo = lockInfoMap.computeIfAbsent(lockIdentity, k -> new LockInfo(lockIdentity));
            lockInfo.lastAcquireTime = currentTime;
            lockInfo.currentHolderThreadId = threadId;
            lockInfo.acquireCount.incrementAndGet();

            ThreadLockInfo threadInfo = threadLockMap.computeIfAbsent(threadId,
                    k -> new ThreadLockInfo(threadId));
            threadInfo.currentLock = lockIdentity;
            threadInfo.lockAcquireTime = currentTime;
        } catch (Exception e) {
            LOGGER.debug("Error recording lock acquire", e);
        }
    }

    /**
     * 记录锁释放
     *
     * @param lockIdentity 锁标识
     * @param threadId 线程ID
     */
    public void recordLockRelease(String lockIdentity, long threadId) {
        try {
            long currentTime = System.nanoTime();
            LockInfo lockInfo = lockInfoMap.get(lockIdentity);
            if (lockInfo != null && lockInfo.currentHolderThreadId == threadId) {
                long holdTime = currentTime - lockInfo.lastAcquireTime;
                lockInfo.totalHoldTime.addAndGet(holdTime);
                lockInfo.currentHolderThreadId = -1;
            }

            ThreadLockInfo threadInfo = threadLockMap.get(threadId);
            if (threadInfo != null && lockIdentity.equals(threadInfo.currentLock)) {
                threadInfo.currentLock = null;
            }
        } catch (Exception e) {
            LOGGER.debug("Error recording lock release", e);
        }
    }

    /**
     * 记录锁等待
     *
     * @param lockIdentity 锁标识
     * @param threadId 线程ID
     * @param waitTime 等待时间（纳秒）
     */
    public void recordLockWait(String lockIdentity, long threadId, long waitTime) {
        try {
            LockInfo lockInfo = lockInfoMap.computeIfAbsent(lockIdentity, k -> new LockInfo(lockIdentity));
            lockInfo.waitCount.incrementAndGet();
            lockInfo.totalWaitTime.addAndGet(waitTime);
            totalLockContentionCount.incrementAndGet();
            totalLockWaitTime.addAndGet(waitTime);
        } catch (Exception e) {
            LOGGER.debug("Error recording lock wait", e);
        }
    }

    /**
     * 采样当前锁竞争情况
     */
    public void sampleLockContention() {
        try {
            long[] allThreadIds = threadMXBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(allThreadIds);

            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo == null) {
                    continue;
                }

                Thread.State state = threadInfo.getThreadState();
                if (state == Thread.State.BLOCKED) {
                    String lockName = threadInfo.getLockName();
                    if (lockName != null) {
                        LockInfo lockInfo = lockInfoMap.computeIfAbsent(lockName, k -> new LockInfo(lockName));
                        lockInfo.blockedThreadCount.incrementAndGet();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error sampling lock contention", e);
        }
    }

    /**
     * 获取锁竞争最严重的锁（Top N）
     *
     * @param topN Top N
     * @return 锁竞争信息列表
     */
    public List<LockInfo> getTopContendedLocks(int topN) {
        List<LockInfo> list = new ArrayList<>(lockInfoMap.values());
        list.sort((a, b) -> Long.compare(b.waitCount.get(), a.waitCount.get()));
        return list.subList(0, Math.min(topN, list.size()));
    }

    /**
     * 获取所有锁信息
     */
    public Map<String, LockInfo> getAllLockInfo() {
        return new HashMap<>(lockInfoMap);
    }

    /**
     * 获取总锁竞争统计
     */
    public LockContentionStats getContentionStats() {
        return new LockContentionStats(
                totalLockContentionCount.get(),
                totalLockWaitTime.get(),
                lockInfoMap.size()
        );
    }

    /**
     * 清理统计数据
     */
    public void clear() {
        lockInfoMap.clear();
        threadLockMap.clear();
        totalLockContentionCount.set(0);
        totalLockWaitTime.set(0);
    }

    /**
     * 锁信息
     */
    public static class LockInfo {
        public final String lockIdentity;
        public final AtomicLong acquireCount = new AtomicLong(0);
        public final AtomicLong waitCount = new AtomicLong(0);
        public final AtomicLong totalHoldTime = new AtomicLong(0);
        public final AtomicLong totalWaitTime = new AtomicLong(0);
        public final AtomicLong blockedThreadCount = new AtomicLong(0);
        public long currentHolderThreadId = -1;
        public long lastAcquireTime = 0;

        public LockInfo(String lockIdentity) {
            this.lockIdentity = lockIdentity;
        }

        /**
         * 获取平均持有时间（纳秒）
         */
        public long getAvgHoldTime() {
            long count = acquireCount.get();
            return count > 0 ? totalHoldTime.get() / count : 0;
        }

        /**
         * 获取平均等待时间（纳秒）
         */
        public long getAvgWaitTime() {
            long count = waitCount.get();
            return count > 0 ? totalWaitTime.get() / count : 0;
        }
    }

    /**
     * 线程锁信息
     */
    public static class ThreadLockInfo {
        public final long threadId;
        public String currentLock;
        public long lockAcquireTime;

        public ThreadLockInfo(long threadId) {
            this.threadId = threadId;
        }
    }

    /**
     * 锁竞争统计
     */
    public static class LockContentionStats {
        public final long totalContentionCount;
        public final long totalWaitTime;
        public final int lockCount;

        public LockContentionStats(long totalContentionCount, long totalWaitTime, int lockCount) {
            this.totalContentionCount = totalContentionCount;
            this.totalWaitTime = totalWaitTime;
            this.lockCount = lockCount;
        }
    }
}
