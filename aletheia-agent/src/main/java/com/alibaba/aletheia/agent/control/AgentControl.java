package com.alibaba.aletheia.agent.control;

import com.alibaba.aletheia.agent.collector.CollectorManager;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.transformer.TransformerManager;
import com.alibaba.aletheia.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Agent 控制 MBean 实现
 *
 * @author Aletheia Team
 */
public class AgentControl implements AgentControlMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentControl.class);

    private final AgentConfig config;
    private final TransformerManager transformerManager;
    private final CollectorManager collectorManager;
    private volatile boolean shutdown = false;

    public AgentControl(AgentConfig config, TransformerManager transformerManager,
                       CollectorManager collectorManager) {
        this.config = config;
        this.transformerManager = transformerManager;
        this.collectorManager = collectorManager;
    }

    @Override
    public void enableFeature(String feature) {
        if (shutdown) {
            LOGGER.warn("Agent is shutting down, cannot enable feature: {}", feature);
            return;
        }

        config.enableFeature(feature);

        // 启用对应的采集器
        if (collectorManager != null) {
            collectorManager.enableCollector(feature);
        }

        // 启用对应的 Transformer
        if (transformerManager != null) {
            transformerManager.enableTransformer(feature);
        }

        LOGGER.info("Feature {} enabled via JMX", feature);
    }

    @Override
    public void disableFeature(String feature) {
        config.disableFeature(feature);

        // 禁用对应的采集器
        if (collectorManager != null) {
            collectorManager.disableCollector(feature);
        }

        // 禁用对应的 Transformer
        if (transformerManager != null) {
            transformerManager.disableTransformer(feature);
        }

        LOGGER.info("Feature {} disabled via JMX", feature);
    }

    @Override
    public boolean isFeatureEnabled(String feature) {
        return config.isFeatureEnabled(feature);
    }

    @Override
    public void setSampleRate(String feature, double rate) {
        if (rate < 0 || rate > 1) {
            throw new IllegalArgumentException("Sample rate must be between 0 and 1");
        }
        config.setSampleRate(feature, rate);
        LOGGER.info("Sample rate for {} set to {} via JMX", feature, rate);
    }

    @Override
    public double getSampleRate(String feature) {
        return config.getSampleRate(feature);
    }

    @Override
    public void addIncludePattern(String pattern) {
        config.addIncludePattern(pattern);
    }

    @Override
    public void removeIncludePattern(String pattern) {
        config.removeIncludePattern(pattern);
    }

    @Override
    public void addExcludePattern(String pattern) {
        config.addExcludePattern(pattern);
    }

    @Override
    public void removeExcludePattern(String pattern) {
        config.removeExcludePattern(pattern);
    }

    @Override
    public String getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("shutdown", shutdown);

        // 功能状态
        Map<String, Boolean> features = new HashMap<>();
        features.put("RT", config.isFeatureEnabled("RT"));
        features.put("GC", config.isFeatureEnabled("GC"));
        features.put("Memory", config.isFeatureEnabled("Memory"));
        features.put("Thread", config.isFeatureEnabled("Thread"));
        status.put("features", features);

        // 采样率
        Map<String, Double> sampleRates = new HashMap<>();
        sampleRates.put("RT", config.getSampleRate("RT"));
        sampleRates.put("GC", config.getSampleRate("GC"));
        sampleRates.put("Memory", config.getSampleRate("Memory"));
        sampleRates.put("Thread", config.getSampleRate("Thread"));
        status.put("sampleRates", sampleRates);

        // 包含模式
        Set<String> includePatterns = config.getIncludePatterns();
        status.put("includePatterns", includePatterns);

        // 排除模式
        Set<String> excludePatterns = config.getExcludePatterns();
        status.put("excludePatterns", excludePatterns);

        // Collector 状态
        if (collectorManager != null) {
            status.put("collectorStarted", collectorManager.isStarted());
        }

        return JsonUtil.toJson(status);
    }

    @Override
    public void shutdown() {
        LOGGER.info("Agent shutdown requested via JMX");
        shutdown = true;

        // 停止采集器
        if (collectorManager != null) {
            collectorManager.stop();
        }

        // 注意：不能移除 Transformer，因为类已经加载
        // Transformer 会通过功能开关自动跳过增强
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
