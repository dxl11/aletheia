package com.alibaba.aletheia.agent.collector;

import com.alibaba.aletheia.agent.collector.gc.GcEventCollector;
import com.alibaba.aletheia.agent.collector.memory.MemoryCollector;
import com.alibaba.aletheia.agent.collector.thread.ThreadCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.exporter.ExporterManager;
import com.alibaba.aletheia.common.model.AgentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 采集器管理器
 * 统一管理所有采集器，支持按需启停
 *
 * @author Aletheia Team
 */
public class CollectorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorManager.class);

    private final AgentConfig config;
    private final Map<String, BaseCollector> collectors = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ExporterManager exporterManager;
    private volatile boolean started = false;

    public CollectorManager(AgentConfig config) {
        this.config = config;
    }

    /**
     * 设置导出器管理器
     */
    public void setExporterManager(ExporterManager exporterManager) {
        this.exporterManager = exporterManager;
    }

    /**
     * 启动采集器管理器
     */
    public synchronized void start() {
        if (started) {
            LOGGER.warn("CollectorManager already started");
            return;
        }

        try {
            LOGGER.info("Starting CollectorManager...");

            // 初始化各个采集器
            GcEventCollector gcCollector = new GcEventCollector(config);
            MemoryCollector memoryCollector = new MemoryCollector(config);
            ThreadCollector threadCollector = new ThreadCollector(config);

            collectors.put("GC", gcCollector);
            collectors.put("Memory", memoryCollector);
            collectors.put("Thread", threadCollector);

            // 根据功能开关启动采集器
            if (config.isFeatureEnabled("GC")) {
                gcCollector.start();
            }
            if (config.isFeatureEnabled("Memory")) {
                memoryCollector.start();
            }
            if (config.isFeatureEnabled("Thread")) {
                threadCollector.start();
            }

            // 创建调度器
            scheduler = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "Aletheia-CollectorManager");
                t.setDaemon(true);
                return t;
            });

            // 启动定时采集任务
            long pushIntervalMs = config.getPushIntervalMs();
            scheduler.scheduleAtFixedRate(this::collectAndExportData, 0,
                    pushIntervalMs, TimeUnit.MILLISECONDS);

            started = true;
            LOGGER.info("CollectorManager started successfully, pushInterval: {}ms", pushIntervalMs);
        } catch (Exception e) {
            LOGGER.error("Failed to start CollectorManager", e);
            throw new RuntimeException("CollectorManager start failed", e);
        }
    }

    /**
     * 停止采集器管理器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            LOGGER.info("Stopping CollectorManager...");

            // 停止所有采集器
            for (BaseCollector collector : collectors.values()) {
                collector.stop();
            }

            // 关闭调度器
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
            LOGGER.info("CollectorManager stopped");
        } catch (Exception e) {
            LOGGER.error("Error stopping CollectorManager", e);
        }
    }

    /**
     * 注册采集器
     */
    public void registerCollector(String name, BaseCollector collector) {
        collectors.put(name, collector);
        if (started && collector.isFeatureEnabled()) {
            collector.start();
        }
    }

    /**
     * 启用采集器
     */
    public void enableCollector(String name) {
        BaseCollector collector = collectors.get(name);
        if (collector != null && !collector.isStarted()) {
            collector.start();
            LOGGER.info("Collector {} enabled", name);
        }
    }

    /**
     * 禁用采集器
     */
    public void disableCollector(String name) {
        BaseCollector collector = collectors.get(name);
        if (collector != null && collector.isStarted()) {
            collector.stop();
            LOGGER.info("Collector {} disabled", name);
        }
    }

    /**
     * 采集数据并导出
     */
    private void collectAndExportData() {
        try {
            AgentData agentData = new AgentData();

            // 采集 GC 事件
            if (config.isFeatureEnabled("GC")) {
                GcEventCollector gcCollector = (GcEventCollector) collectors.get("GC");
                if (gcCollector != null && gcCollector.isStarted()) {
                    List<com.alibaba.aletheia.common.model.GcEvent> gcEvents =
                            gcCollector.getAndClearGcEvents();
                    if (gcEvents != null && !gcEvents.isEmpty()) {
                        agentData.setGcEvents(gcEvents);
                    }
                }
            }

            // 采集线程数据
            if (config.isFeatureEnabled("Thread")) {
                ThreadCollector threadCollector = (ThreadCollector) collectors.get("Thread");
                if (threadCollector != null && threadCollector.isStarted()) {
                    com.alibaba.aletheia.common.model.ThreadEvent threadEvent = threadCollector.collect();
                    if (threadEvent != null) {
                        agentData.setThreadEvent(threadEvent);
                    }
                }
            }

            // 采集内存数据
            if (config.isFeatureEnabled("Memory")) {
                MemoryCollector memoryCollector = (MemoryCollector) collectors.get("Memory");
                if (memoryCollector != null && memoryCollector.isStarted()) {
                    com.alibaba.aletheia.common.model.MemoryEvent memoryEvent = memoryCollector.collect();
                    if (memoryEvent != null) {
                        agentData.setMemoryEvent(memoryEvent);
                    }
                }
            }

            // 采集 RT 数据（由 RtSampler 管理）
            if (config.isFeatureEnabled("RT")) {
                List<com.alibaba.aletheia.common.model.RtEvent> rtEvents =
                        com.alibaba.aletheia.agent.sampler.RtSampler.getAndClearRtEvents();
                if (rtEvents != null && !rtEvents.isEmpty()) {
                    agentData.setRtEvents(rtEvents);
                }
            }

            // 导出数据（如果有数据）
            if (hasData(agentData) && exporterManager != null) {
                exporterManager.export(agentData);
            }
        } catch (Exception e) {
            LOGGER.error("Error collecting and exporting data", e);
        }
    }

    /**
     * 检查 AgentData 是否有数据
     */
    private boolean hasData(AgentData agentData) {
        return (agentData.getGcEvents() != null && !agentData.getGcEvents().isEmpty())
                || agentData.getThreadEvent() != null
                || agentData.getMemoryEvent() != null
                || (agentData.getRtEvents() != null && !agentData.getRtEvents().isEmpty());
    }

    public boolean isStarted() {
        return started;
    }
}
