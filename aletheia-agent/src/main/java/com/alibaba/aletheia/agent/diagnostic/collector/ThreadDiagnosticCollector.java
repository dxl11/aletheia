package com.alibaba.aletheia.agent.diagnostic.collector;

import com.alibaba.aletheia.agent.collector.BaseCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.diagnostic.sampler.CpuSampler;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * 线程诊断采集器
 * 用于诊断线程相关问题（线程很多但吞吐低）
 *
 * @author Aletheia Team
 */
public class ThreadDiagnosticCollector extends BaseCollector {

    private final ThreadMXBean threadMXBean;
    private final CpuSampler cpuSampler;
    private volatile boolean cpuSamplingEnabled = false;

    public ThreadDiagnosticCollector(AgentConfig config) {
        super(config);
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.cpuSampler = new CpuSampler();
    }

    @Override
    protected boolean isFeatureEnabled() {
        return config.isFeatureEnabled("Thread");
    }

    @Override
    protected void doStart() throws Exception {
        // 如果启用了 CPU 采样，启动 CPU 采样器
        if (cpuSamplingEnabled) {
            cpuSampler.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        cpuSampler.stop();
    }

    /**
     * 启用 CPU 采样
     */
    public void enableCpuSampling() {
        cpuSamplingEnabled = true;
        if (started) {
            cpuSampler.start();
        }
    }

    /**
     * 禁用 CPU 采样
     */
    public void disableCpuSampling() {
        cpuSamplingEnabled = false;
        cpuSampler.stop();
    }

    /**
     * 获取线程状态分布
     */
    public ThreadStateDistribution getThreadStateDistribution() {
        try {
            long[] allThreadIds = threadMXBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(allThreadIds);

            Map<Thread.State, Integer> stateCount = new HashMap<>();
            int totalThreads = 0;

            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo == null) {
                    continue;
                }
                Thread.State state = threadInfo.getThreadState();
                stateCount.put(state, stateCount.getOrDefault(state, 0) + 1);
                totalThreads++;
            }

            return new ThreadStateDistribution(totalThreads, stateCount);
        } catch (Exception e) {
            logger.error("Error getting thread state distribution", e);
            return new ThreadStateDistribution(0, Collections.emptyMap());
        }
    }

    /**
     * 获取等待时间最长的线程（Top N）
     */
    public List<ThreadWaitInfo> getTopWaitingThreads(int topN) {
        try {
            long[] allThreadIds = threadMXBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(allThreadIds, 0);

            List<ThreadWaitInfo> waitInfos = new ArrayList<>();

            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo == null) {
                    continue;
                }

                Thread.State state = threadInfo.getThreadState();
                if (state == Thread.State.BLOCKED || state == Thread.State.WAITING
                        || state == Thread.State.TIMED_WAITING) {
                    long blockedTime = threadInfo.getBlockedTime();
                    long waitedTime = threadInfo.getWaitedTime();

                    if (blockedTime > 0 || waitedTime > 0) {
                        ThreadWaitInfo info = new ThreadWaitInfo();
                        info.threadId = threadInfo.getThreadId();
                        info.threadName = threadInfo.getThreadName();
                        info.state = state.toString();
                        info.blockedTime = blockedTime;
                        info.waitedTime = waitedTime;
                        info.lockName = threadInfo.getLockName();
                        info.lockOwnerName = threadInfo.getLockOwnerName();
                        waitInfos.add(info);
                    }
                }
            }

            waitInfos.sort((a, b) -> Long.compare(
                    Math.max(b.blockedTime, b.waitedTime),
                    Math.max(a.blockedTime, a.waitedTime)
            ));

            return waitInfos.subList(0, Math.min(topN, waitInfos.size()));
        } catch (Exception e) {
            logger.error("Error getting top waiting threads", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取 CPU 占用最高的线程（Top N）
     */
    public List<CpuSampler.ThreadCpuInfo> getTopCpuThreads(int topN) {
        if (!cpuSamplingEnabled || !cpuSampler.isStarted()) {
            return Collections.emptyList();
        }
        return cpuSampler.getTopCpuThreads(topN);
    }

    /**
     * 检测死锁
     */
    public DeadlockInfo detectDeadlock() {
        try {
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            if (deadlockedThreads == null || deadlockedThreads.length == 0) {
                return new DeadlockInfo(false, Collections.emptyList());
            }

            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
            List<DeadlockThreadInfo> deadlockThreadInfos = new ArrayList<>();

            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo == null) {
                    continue;
                }
                DeadlockThreadInfo info = new DeadlockThreadInfo();
                info.threadId = threadInfo.getThreadId();
                info.threadName = threadInfo.getThreadName();
                info.state = threadInfo.getThreadState().toString();
                info.lockName = threadInfo.getLockName();
                info.lockOwnerName = threadInfo.getLockOwnerName();
                if (threadInfo.getStackTrace() != null) {
                    info.stackTrace = Arrays.toString(threadInfo.getStackTrace());
                }
                deadlockThreadInfos.add(info);
            }

            return new DeadlockInfo(true, deadlockThreadInfos);
        } catch (Exception e) {
            logger.error("Error detecting deadlock", e);
            return new DeadlockInfo(false, Collections.emptyList());
        }
    }

    /**
     * 线程状态分布
     */
    public static class ThreadStateDistribution {
        public final int totalThreads;
        public final Map<Thread.State, Integer> stateCount;

        public ThreadStateDistribution(int totalThreads, Map<Thread.State, Integer> stateCount) {
            this.totalThreads = totalThreads;
            this.stateCount = new HashMap<>(stateCount);
        }
    }

    /**
     * 线程等待信息
     */
    public static class ThreadWaitInfo {
        public long threadId;
        public String threadName;
        public String state;
        public long blockedTime; // 阻塞时间（毫秒）
        public long waitedTime; // 等待时间（毫秒）
        public String lockName;
        public String lockOwnerName;
    }

    /**
     * 死锁信息
     */
    public static class DeadlockInfo {
        public final boolean hasDeadlock;
        public final List<DeadlockThreadInfo> deadlockThreads;

        public DeadlockInfo(boolean hasDeadlock, List<DeadlockThreadInfo> deadlockThreads) {
            this.hasDeadlock = hasDeadlock;
            this.deadlockThreads = deadlockThreads;
        }
    }

    /**
     * 死锁线程信息
     */
    public static class DeadlockThreadInfo {
        public long threadId;
        public String threadName;
        public String state;
        public String lockName;
        public String lockOwnerName;
        public String stackTrace;
    }
}
