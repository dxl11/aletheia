package com.alibaba.aletheia.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 线程事件数据模型
 *
 * @author Aletheia Team
 */
public class ThreadEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间戳（纳秒）
     */
    @JsonProperty("timestampNs")
    private long timestampNs;

    /**
     * 总线程数
     */
    @JsonProperty("totalThreadCount")
    private int totalThreadCount;

    /**
     * RUNNABLE 状态线程数
     */
    @JsonProperty("runnableCount")
    private int runnableCount;

    /**
     * BLOCKED 状态线程数
     */
    @JsonProperty("blockedCount")
    private int blockedCount;

    /**
     * WAITING 状态线程数
     */
    @JsonProperty("waitingCount")
    private int waitingCount;

    /**
     * TIMED_WAITING 状态线程数
     */
    @JsonProperty("timedWaitingCount")
    private int timedWaitingCount;

    /**
     * 死锁线程信息列表
     */
    @JsonProperty("deadlockedThreads")
    private List<ThreadInfo> deadlockedThreads;

    /**
     * 锁竞争信息列表（BLOCKED 线程的锁对象）
     */
    @JsonProperty("lockContentionInfo")
    private List<LockContentionInfo> lockContentionInfo;

    /**
     * 线程信息
     */
    public static class ThreadInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("threadId")
        private long threadId;

        @JsonProperty("threadName")
        private String threadName;

        @JsonProperty("threadState")
        private String threadState;

        @JsonProperty("stackTrace")
        private String[] stackTrace;

        public long getThreadId() {
            return threadId;
        }

        public void setThreadId(long threadId) {
            this.threadId = threadId;
        }

        public String getThreadName() {
            return threadName;
        }

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }

        public String getThreadState() {
            return threadState;
        }

        public void setThreadState(String threadState) {
            this.threadState = threadState;
        }

        public String[] getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(String[] stackTrace) {
            this.stackTrace = stackTrace;
        }
    }

    /**
     * 锁竞争信息
     */
    public static class LockContentionInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("lockObject")
        private String lockObject;

        @JsonProperty("blockedThreadCount")
        private int blockedThreadCount;

        @JsonProperty("blockedThreads")
        private List<String> blockedThreads;

        public String getLockObject() {
            return lockObject;
        }

        public void setLockObject(String lockObject) {
            this.lockObject = lockObject;
        }

        public int getBlockedThreadCount() {
            return blockedThreadCount;
        }

        public void setBlockedThreadCount(int blockedThreadCount) {
            this.blockedThreadCount = blockedThreadCount;
        }

        public List<String> getBlockedThreads() {
            return blockedThreads;
        }

        public void setBlockedThreads(List<String> blockedThreads) {
            this.blockedThreads = blockedThreads;
        }
    }

    public long getTimestampNs() {
        return timestampNs;
    }

    public void setTimestampNs(long timestampNs) {
        this.timestampNs = timestampNs;
    }

    public int getTotalThreadCount() {
        return totalThreadCount;
    }

    public void setTotalThreadCount(int totalThreadCount) {
        this.totalThreadCount = totalThreadCount;
    }

    public int getRunnableCount() {
        return runnableCount;
    }

    public void setRunnableCount(int runnableCount) {
        this.runnableCount = runnableCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public int getTimedWaitingCount() {
        return timedWaitingCount;
    }

    public void setTimedWaitingCount(int timedWaitingCount) {
        this.timedWaitingCount = timedWaitingCount;
    }

    public List<ThreadInfo> getDeadlockedThreads() {
        return deadlockedThreads;
    }

    public void setDeadlockedThreads(List<ThreadInfo> deadlockedThreads) {
        this.deadlockedThreads = deadlockedThreads;
    }

    public List<LockContentionInfo> getLockContentionInfo() {
        return lockContentionInfo;
    }

    public void setLockContentionInfo(List<LockContentionInfo> lockContentionInfo) {
        this.lockContentionInfo = lockContentionInfo;
    }

    @Override
    public String toString() {
        return "ThreadEvent{"
                + "totalThreadCount=" + totalThreadCount
                + ", runnableCount=" + runnableCount
                + ", blockedCount=" + blockedCount
                + ", waitingCount=" + waitingCount
                + '}';
    }
}
