package com.alibaba.aletheia.agent.exporter;

import com.alibaba.aletheia.common.model.AgentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础导出器
 * 提供统一的导出接口
 *
 * @author Aletheia Team
 */
public abstract class BaseExporter {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected volatile boolean started = false;

    /**
     * 导出数据
     *
     * @param agentData Agent 数据
     */
    public final void export(AgentData agentData) {
        if (!started) {
            logger.warn("Exporter {} not started", getClass().getSimpleName());
            return;
        }

        try {
            doExport(agentData);
        } catch (Exception e) {
            logger.error("Failed to export data", e);
        }
    }

    /**
     * 启动导出器
     */
    public synchronized void start() {
        if (started) {
            logger.warn("Exporter {} already started", getClass().getSimpleName());
            return;
        }

        try {
            doStart();
            started = true;
            logger.info("Exporter {} started", getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Failed to start exporter {}", getClass().getSimpleName(), e);
            throw new RuntimeException("Exporter start failed", e);
        }
    }

    /**
     * 停止导出器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            doStop();
            started = false;
            logger.info("Exporter {} stopped", getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Error stopping exporter {}", getClass().getSimpleName(), e);
        }
    }

    /**
     * 执行导出逻辑
     * 子类实现此方法
     */
    protected abstract void doExport(AgentData agentData) throws Exception;

    /**
     * 执行启动逻辑
     * 子类实现此方法
     */
    protected abstract void doStart() throws Exception;

    /**
     * 执行停止逻辑
     * 子类实现此方法
     */
    protected abstract void doStop() throws Exception;

    public boolean isStarted() {
        return started;
    }
}
