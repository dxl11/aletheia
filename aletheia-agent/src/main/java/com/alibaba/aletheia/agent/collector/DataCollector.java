package com.alibaba.aletheia.agent.collector;

import com.alibaba.aletheia.agent.collector.gc.GcEventCollector;
import com.alibaba.aletheia.agent.collector.memory.MemoryCollector;
import com.alibaba.aletheia.agent.collector.thread.ThreadCollector;
import com.alibaba.aletheia.agent.sampler.RtSampler;
import com.alibaba.aletheia.common.constant.AletheiaConstants;
import com.alibaba.aletheia.common.model.AgentData;
import com.alibaba.aletheia.common.model.MemoryEvent;
import com.alibaba.aletheia.common.model.ThreadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据采集器主类
 * 负责协调各个子采集器的工作
 *
 * @author Aletheia Team
 */
public class DataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataCollector.class);

    private static final DataCollector INSTANCE = new DataCollector();

    private ScheduledExecutorService scheduler;
    private GcEventCollector gcEventCollector;
    private ThreadCollector threadCollector;
    private MemoryCollector memoryCollector;
    private DataSender dataSender;
    private com.alibaba.aletheia.agent.sampler.AdaptiveSampler adaptiveSampler;

    private volatile boolean started = false;

    /**
     * 私有构造函数
     */
    private DataCollector() {
    }

    /**
     * 获取单例实例
     *
     * @return DataCollector 实例
     */
    public static DataCollector getInstance() {
        return INSTANCE;
    }

    /**
     * 启动数据采集器
     *
     * @param dataDir 数据目录（可选，默认使用系统临时目录）
     */
    public synchronized void start(String dataDir) {
        if (started) {
            LOGGER.warn("DataCollector already started");
            return;
        }

        try {
            LOGGER.info("Starting DataCollector...");

            // 初始化各个采集器
            gcEventCollector = new GcEventCollector();
            threadCollector = new ThreadCollector();
            memoryCollector = new MemoryCollector();
            dataSender = new DataSender(dataDir);
            adaptiveSampler = new com.alibaba.aletheia.agent.sampler.AdaptiveSampler();

            // 创建调度器
            scheduler = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "Aletheia-DataCollector");
                t.setDaemon(true);
                return t;
            });

            // 启动定时采集任务（1秒一次）
            long pushIntervalMs = AletheiaConstants.DEFAULT_PUSH_INTERVAL_MS;
            scheduler.scheduleAtFixedRate(this::collectAndSendData, 0, 
                    pushIntervalMs, TimeUnit.MILLISECONDS);

            // 启动自适应采样率调整（5秒一次）
            scheduler.scheduleAtFixedRate(adaptiveSampler::updateSampleRate, 5, 5, TimeUnit.SECONDS);

            started = true;
            LOGGER.info("DataCollector started successfully, pushInterval: {}ms", pushIntervalMs);
        } catch (Exception e) {
            LOGGER.error("Failed to start DataCollector", e);
            throw new RuntimeException("DataCollector start failed", e);
        }
    }

    /**
     * 启动数据采集器（使用默认数据目录）
     */
    public synchronized void start() {
        start(null);
    }

    /**
     * 停止数据采集器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            LOGGER.info("Stopping DataCollector...");

            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            started = false;
            LOGGER.info("DataCollector stopped");
        } catch (Exception e) {
            LOGGER.error("Error stopping DataCollector", e);
        }
    }

    /**
     * 采集数据并发送
     */
    private void collectAndSendData() {
        try {
            // 创建 AgentData 对象
            AgentData agentData = new AgentData();

            // 采集 GC 事件
            List<com.alibaba.aletheia.common.model.GcEvent> gcEvents = 
                    gcEventCollector.getAndClearGcEvents();
            if (gcEvents != null && !gcEvents.isEmpty()) {
                agentData.setGcEvents(gcEvents);
            }

            // 采集线程数据
            ThreadEvent threadEvent = threadCollector.collect();
            if (threadEvent != null) {
                agentData.setThreadEvent(threadEvent);
            }

            // 采集内存数据
            MemoryEvent memoryEvent = memoryCollector.collect();
            if (memoryEvent != null) {
                agentData.setMemoryEvent(memoryEvent);
            }

            // 采集 RT 数据
            List<com.alibaba.aletheia.common.model.RtEvent> rtEvents = 
                    RtSampler.getAndClearRtEvents();
            if (rtEvents != null && !rtEvents.isEmpty()) {
                agentData.setRtEvents(rtEvents);
            }

            // 发送数据（如果有数据）
            if (hasData(agentData)) {
                dataSender.send(agentData);
            }
        } catch (Exception e) {
            LOGGER.error("Error collecting and sending data", e);
        }
    }

    /**
     * 检查 AgentData 是否有数据
     *
     * @param agentData Agent 数据
     * @return true 如果有数据
     */
    private boolean hasData(AgentData agentData) {
        return (agentData.getGcEvents() != null && !agentData.getGcEvents().isEmpty())
                || agentData.getThreadEvent() != null
                || agentData.getMemoryEvent() != null
                || (agentData.getRtEvents() != null && !agentData.getRtEvents().isEmpty());
    }

    /**
     * 检查是否已启动
     *
     * @return true 如果已启动
     */
    public boolean isStarted() {
        return started;
    }
}
