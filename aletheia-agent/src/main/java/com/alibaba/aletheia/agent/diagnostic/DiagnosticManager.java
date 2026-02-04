package com.alibaba.aletheia.agent.diagnostic;

import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.diagnostic.collector.GcDiagnosticCollector;
import com.alibaba.aletheia.agent.diagnostic.collector.LockDiagnosticCollector;
import com.alibaba.aletheia.agent.diagnostic.collector.ThreadDiagnosticCollector;
import com.alibaba.aletheia.agent.diagnostic.sampler.MethodSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 诊断管理器
 * 统一管理所有诊断能力
 *
 * @author Aletheia Team
 */
public class DiagnosticManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticManager.class);

    private static volatile DiagnosticManager instance;

    private final AgentConfig config;
    private final Map<String, Object> diagnosticComponents = new ConcurrentHashMap<>();

    // 诊断组件
    private ThreadDiagnosticCollector threadDiagnosticCollector;
    private LockDiagnosticCollector lockDiagnosticCollector;
    private GcDiagnosticCollector gcDiagnosticCollector;
    private MethodSampler methodSampler;

    private volatile boolean started = false;

    private DiagnosticManager(AgentConfig config) {
        this.config = config;
    }

    /**
     * 获取单例实例
     */
    public static DiagnosticManager getInstance(AgentConfig config) {
        if (instance == null) {
            synchronized (DiagnosticManager.class) {
                if (instance == null) {
                    instance = new DiagnosticManager(config);
                }
            }
        }
        return instance;
    }

    /**
     * 启动诊断管理器
     */
    public synchronized void start() {
        if (started) {
            LOGGER.warn("DiagnosticManager already started");
            return;
        }

        try {
            LOGGER.info("Starting DiagnosticManager...");

            // 初始化诊断组件
            threadDiagnosticCollector = new ThreadDiagnosticCollector(config);
            lockDiagnosticCollector = new LockDiagnosticCollector(config);
            gcDiagnosticCollector = new GcDiagnosticCollector(config);
            methodSampler = new MethodSampler();

            diagnosticComponents.put("thread", threadDiagnosticCollector);
            diagnosticComponents.put("lock", lockDiagnosticCollector);
            diagnosticComponents.put("gc", gcDiagnosticCollector);
            diagnosticComponents.put("method", methodSampler);

            // 根据配置启动组件
            if (config.isFeatureEnabled("Thread")) {
                threadDiagnosticCollector.start();
            }
            if (config.isFeatureEnabled("Lock") || config.isFeatureEnabled("Thread")) {
                lockDiagnosticCollector.start();
            }
            if (config.isFeatureEnabled("GC")) {
                gcDiagnosticCollector.start();
            }

            started = true;
            LOGGER.info("DiagnosticManager started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start DiagnosticManager", e);
            throw new RuntimeException("DiagnosticManager start failed", e);
        }
    }

    /**
     * 停止诊断管理器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            LOGGER.info("Stopping DiagnosticManager...");

            if (threadDiagnosticCollector != null) {
                threadDiagnosticCollector.stop();
            }
            if (lockDiagnosticCollector != null) {
                lockDiagnosticCollector.stop();
            }
            if (gcDiagnosticCollector != null) {
                gcDiagnosticCollector.stop();
            }

            started = false;
            LOGGER.info("DiagnosticManager stopped");
        } catch (Exception e) {
            LOGGER.error("Error stopping DiagnosticManager", e);
        }
    }

    /**
     * 获取线程诊断采集器
     */
    public ThreadDiagnosticCollector getThreadDiagnosticCollector() {
        return threadDiagnosticCollector;
    }

    /**
     * 获取锁诊断采集器
     */
    public LockDiagnosticCollector getLockDiagnosticCollector() {
        return lockDiagnosticCollector;
    }

    /**
     * 获取 GC 诊断采集器
     */
    public GcDiagnosticCollector getGcDiagnosticCollector() {
        return gcDiagnosticCollector;
    }

    /**
     * 获取方法采样器
     */
    public MethodSampler getMethodSampler() {
        return methodSampler;
    }

    public boolean isStarted() {
        return started;
    }
}
