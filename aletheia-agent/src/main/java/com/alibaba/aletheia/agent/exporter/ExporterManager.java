package com.alibaba.aletheia.agent.exporter;

import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.common.model.AgentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导出器管理器
 * 统一管理数据导出，支持多种导出方式
 *
 * @author Aletheia Team
 */
public class ExporterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExporterManager.class);

    private final AgentConfig config;
    private final Map<String, BaseExporter> exporters = new ConcurrentHashMap<>();
    private volatile boolean started = false;

    public ExporterManager(AgentConfig config) {
        this.config = config;
    }

    /**
     * 启动导出器管理器
     */
    public synchronized void start() {
        if (started) {
            LOGGER.warn("ExporterManager already started");
            return;
        }

        try {
            LOGGER.info("Starting ExporterManager...");

            // 初始化默认导出器（文件导出器）
            FileExporter fileExporter = new FileExporter(config);
            registerExporter("file", fileExporter);

            started = true;
            LOGGER.info("ExporterManager started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start ExporterManager", e);
            throw new RuntimeException("ExporterManager start failed", e);
        }
    }

    /**
     * 停止导出器管理器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            LOGGER.info("Stopping ExporterManager...");

            // 停止所有导出器
            for (BaseExporter exporter : exporters.values()) {
                exporter.stop();
            }

            started = false;
            LOGGER.info("ExporterManager stopped");
        } catch (Exception e) {
            LOGGER.error("Error stopping ExporterManager", e);
        }
    }

    /**
     * 注册导出器
     */
    public void registerExporter(String name, BaseExporter exporter) {
        exporters.put(name, exporter);
        if (started) {
            exporter.start();
        }
    }

    /**
     * 导出数据
     * 将数据发送到所有已启动的导出器
     */
    public void export(AgentData agentData) {
        if (!started) {
            LOGGER.warn("ExporterManager not started");
            return;
        }

        for (BaseExporter exporter : exporters.values()) {
            if (exporter.isStarted()) {
                exporter.export(agentData);
            }
        }
    }

    public boolean isStarted() {
        return started;
    }
}
