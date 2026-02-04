package com.alibaba.aletheia.agent.collector.thread;

import com.alibaba.aletheia.agent.collector.BaseCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.common.model.ThreadEvent;
import com.alibaba.aletheia.common.util.TimeUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线程状态采集器
 *
 * @author Aletheia Team
 */
public class ThreadCollector extends BaseCollector {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 构造函数
     */
    public ThreadCollector(AgentConfig config) {
        super(config);
    }

    @Override
    protected boolean isFeatureEnabled() {
        return config.isFeatureEnabled("Thread");
    }

    @Override
    protected void doStart() throws Exception {
        // ThreadCollector 不需要特殊启动逻辑
    }

    @Override
    protected void doStop() throws Exception {
        // ThreadCollector 不需要特殊停止逻辑
    }

    /**
     * 采集线程数据
     *
     * @return 线程事件
     */
    public ThreadEvent collect() {
        try {
            ThreadEvent event = new ThreadEvent();
            event.setTimestampNs(TimeUtil.currentTimeNs());

            // 获取线程统计信息
            int totalThreadCount = threadMXBean.getThreadCount();
            event.setTotalThreadCount(totalThreadCount);

            // 统计各状态线程数
            ThreadInfo[] allThreads = threadMXBean.dumpAllThreads(false, false);
            int runnableCount = 0;
            int blockedCount = 0;
            int waitingCount = 0;
            int timedWaitingCount = 0;

            Map<String, List<String>> lockContentionMap = new HashMap<>();

            for (ThreadInfo threadInfo : allThreads) {
                Thread.State state = threadInfo.getThreadState();
                switch (state) {
                    case RUNNABLE:
                        runnableCount++;
                        break;
                    case BLOCKED:
                        blockedCount++;
                        // 记录锁竞争信息
                        String lockName = threadInfo.getLockName();
                        if (lockName != null) {
                            lockContentionMap.computeIfAbsent(lockName, k -> new ArrayList<>())
                                    .add(threadInfo.getThreadName());
                        }
                        break;
                    case WAITING:
                        waitingCount++;
                        break;
                    case TIMED_WAITING:
                        timedWaitingCount++;
                        break;
                    default:
                        break;
                }
            }

            event.setRunnableCount(runnableCount);
            event.setBlockedCount(blockedCount);
            event.setWaitingCount(waitingCount);
            event.setTimedWaitingCount(timedWaitingCount);

            // 检测死锁
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                List<ThreadEvent.ThreadInfo> deadlockInfoList = new ArrayList<>();
                ThreadInfo[] deadlockThreadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
                for (ThreadInfo threadInfo : deadlockThreadInfos) {
                    ThreadEvent.ThreadInfo info = new ThreadEvent.ThreadInfo();
                    info.setThreadId(threadInfo.getThreadId());
                    info.setThreadName(threadInfo.getThreadName());
                    info.setThreadState(threadInfo.getThreadState().toString());
                    if (threadInfo.getStackTrace() != null) {
                        String[] stackTrace = new String[threadInfo.getStackTrace().length];
                        for (int i = 0; i < threadInfo.getStackTrace().length; i++) {
                            stackTrace[i] = threadInfo.getStackTrace()[i].toString();
                        }
                        info.setStackTrace(stackTrace);
                    }
                    deadlockInfoList.add(info);
                }
                event.setDeadlockedThreads(deadlockInfoList);
                logger.warn("Deadlock detected: {} threads", deadlockedThreads.length);
            }

            // 构建锁竞争信息
            if (!lockContentionMap.isEmpty()) {
                List<ThreadEvent.LockContentionInfo> lockContentionInfoList = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : lockContentionMap.entrySet()) {
                    ThreadEvent.LockContentionInfo info = new ThreadEvent.LockContentionInfo();
                    info.setLockObject(entry.getKey());
                    info.setBlockedThreadCount(entry.getValue().size());
                    info.setBlockedThreads(entry.getValue());
                    lockContentionInfoList.add(info);
                }
                event.setLockContentionInfo(lockContentionInfoList);
            }

            return event;
        } catch (Exception e) {
            logger.error("Error collecting thread data", e);
            return null;
        }
    }
}
