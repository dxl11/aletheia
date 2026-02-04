package com.alibaba.aletheia.agent.collector;

import com.alibaba.aletheia.agent.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础采集器
 * 提供统一的采集接口和生命周期管理
 *
 * @author Aletheia Team
 */
public abstract class BaseCollector {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AgentConfig config;
    protected volatile boolean started = false;

    public BaseCollector(AgentConfig config) {
        this.config = config;
    }

    /**
     * 启动采集器
     */
    public synchronized void start() {
        if (started) {
            logger.warn("Collector {} already started", getClass().getSimpleName());
            return;
        }

        try {
            doStart();
            started = true;
            logger.info("Collector {} started", getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Failed to start collector {}", getClass().getSimpleName(), e);
            throw new RuntimeException("Collector start failed", e);
        }
    }

    /**
     * 停止采集器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            doStop();
            started = false;
            logger.info("Collector {} stopped", getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Error stopping collector {}", getClass().getSimpleName(), e);
        }
    }

    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * 检查功能是否启用
     */
    protected abstract boolean isFeatureEnabled();

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
}
