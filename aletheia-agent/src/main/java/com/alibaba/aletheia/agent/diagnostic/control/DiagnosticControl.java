package com.alibaba.aletheia.agent.diagnostic.control;

import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.diagnostic.DiagnosticManager;
import com.alibaba.aletheia.agent.diagnostic.collector.GcDiagnosticCollector;
import com.alibaba.aletheia.agent.diagnostic.collector.LockDiagnosticCollector;
import com.alibaba.aletheia.agent.diagnostic.collector.ThreadDiagnosticCollector;
import com.alibaba.aletheia.agent.diagnostic.sampler.MethodSampler;
import com.alibaba.aletheia.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 诊断控制 MBean 实现
 *
 * @author Aletheia Team
 */
public class DiagnosticControl implements DiagnosticControlMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticControl.class);

    private final AgentConfig config;
    private final DiagnosticManager diagnosticManager;

    public DiagnosticControl(AgentConfig config, DiagnosticManager diagnosticManager) {
        this.config = config;
        this.diagnosticManager = diagnosticManager;
    }

    @Override
    public void enableDiagnostic(String feature) {
        config.enableFeature(feature);
        LOGGER.info("Diagnostic feature {} enabled via JMX", feature);
    }

    @Override
    public void disableDiagnostic(String feature) {
        config.disableFeature(feature);
        LOGGER.info("Diagnostic feature {} disabled via JMX", feature);
    }

    @Override
    public boolean isDiagnosticEnabled(String feature) {
        return config.isFeatureEnabled(feature);
    }

    @Override
    public String getTopCpuThreads(int topN) {
        try {
            ThreadDiagnosticCollector collector = diagnosticManager.getThreadDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(Collections.emptyList());
            }
            List<com.alibaba.aletheia.agent.diagnostic.sampler.CpuSampler.ThreadCpuInfo> threads =
                    collector.getTopCpuThreads(topN);
            return JsonUtil.toJson(threads);
        } catch (Exception e) {
            LOGGER.error("Error getting top CPU threads", e);
            return JsonUtil.toJson(Collections.emptyList());
        }
    }

    @Override
    public String getThreadStateDistribution() {
        try {
            ThreadDiagnosticCollector collector = diagnosticManager.getThreadDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(new ThreadDiagnosticCollector.ThreadStateDistribution(0, Collections.emptyMap()));
            }
            ThreadDiagnosticCollector.ThreadStateDistribution distribution =
                    collector.getThreadStateDistribution();
            return JsonUtil.toJson(distribution);
        } catch (Exception e) {
            LOGGER.error("Error getting thread state distribution", e);
            return JsonUtil.toJson(new ThreadDiagnosticCollector.ThreadStateDistribution(0, Collections.emptyMap()));
        }
    }

    @Override
    public String getTopWaitingThreads(int topN) {
        try {
            ThreadDiagnosticCollector collector = diagnosticManager.getThreadDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(Collections.emptyList());
            }
            List<ThreadDiagnosticCollector.ThreadWaitInfo> threads = collector.getTopWaitingThreads(topN);
            return JsonUtil.toJson(threads);
        } catch (Exception e) {
            LOGGER.error("Error getting top waiting threads", e);
            return JsonUtil.toJson(Collections.emptyList());
        }
    }

    @Override
    public String detectDeadlock() {
        try {
            ThreadDiagnosticCollector collector = diagnosticManager.getThreadDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(new ThreadDiagnosticCollector.DeadlockInfo(false, Collections.emptyList()));
            }
            ThreadDiagnosticCollector.DeadlockInfo deadlockInfo = collector.detectDeadlock();
            return JsonUtil.toJson(deadlockInfo);
        } catch (Exception e) {
            LOGGER.error("Error detecting deadlock", e);
            return JsonUtil.toJson(new ThreadDiagnosticCollector.DeadlockInfo(false, Collections.emptyList()));
        }
    }

    @Override
    public String getTopContendedLocks(int topN) {
        try {
            LockDiagnosticCollector collector = diagnosticManager.getLockDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(Collections.emptyList());
            }
            List<com.alibaba.aletheia.agent.diagnostic.sampler.LockSampler.LockInfo> locks =
                    collector.getTopContendedLocks(topN);
            return JsonUtil.toJson(locks);
        } catch (Exception e) {
            LOGGER.error("Error getting top contended locks", e);
            return JsonUtil.toJson(Collections.emptyList());
        }
    }

    @Override
    public String getLockContentionStats() {
        try {
            LockDiagnosticCollector collector = diagnosticManager.getLockDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(new com.alibaba.aletheia.agent.diagnostic.sampler.LockSampler.LockContentionStats(0, 0, 0));
            }
            com.alibaba.aletheia.agent.diagnostic.sampler.LockSampler.LockContentionStats stats =
                    collector.getContentionStats();
            return JsonUtil.toJson(stats);
        } catch (Exception e) {
            LOGGER.error("Error getting lock contention stats", e);
            return JsonUtil.toJson(new com.alibaba.aletheia.agent.diagnostic.sampler.LockSampler.LockContentionStats(0, 0, 0));
        }
    }

    @Override
    public String getHotMethods(int topN) {
        try {
            MethodSampler sampler = diagnosticManager.getMethodSampler();
            if (sampler == null) {
                return JsonUtil.toJson(Collections.emptyList());
            }
            List<MethodSampler.MethodInfo> methods = sampler.getHotMethods(topN);
            return JsonUtil.toJson(methods);
        } catch (Exception e) {
            LOGGER.error("Error getting hot methods", e);
            return JsonUtil.toJson(Collections.emptyList());
        }
    }

    @Override
    public String getRecentGcEvents(int count) {
        try {
            GcDiagnosticCollector collector = diagnosticManager.getGcDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(Collections.emptyList());
            }
            List<com.alibaba.aletheia.common.model.GcEvent> events = collector.getRecentGcEvents(count);
            return JsonUtil.toJson(events);
        } catch (Exception e) {
            LOGGER.error("Error getting recent GC events", e);
            return JsonUtil.toJson(Collections.emptyList());
        }
    }

    @Override
    public String getFullGcEvents() {
        try {
            GcDiagnosticCollector collector = diagnosticManager.getGcDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(Collections.emptyList());
            }
            List<com.alibaba.aletheia.common.model.GcEvent> events = collector.getFullGcEvents();
            return JsonUtil.toJson(events);
        } catch (Exception e) {
            LOGGER.error("Error getting Full GC events", e);
            return JsonUtil.toJson(Collections.emptyList());
        }
    }

    @Override
    public String getHeapMemoryInfo() {
        try {
            GcDiagnosticCollector collector = diagnosticManager.getGcDiagnosticCollector();
            if (collector == null) {
                return JsonUtil.toJson(new GcDiagnosticCollector.HeapMemoryInfo(0, 0, 0));
            }
            GcDiagnosticCollector.HeapMemoryInfo info = collector.getHeapMemoryInfo();
            return JsonUtil.toJson(info);
        } catch (Exception e) {
            LOGGER.error("Error getting heap memory info", e);
            return JsonUtil.toJson(new GcDiagnosticCollector.HeapMemoryInfo(0, 0, 0));
        }
    }

    @Override
    public void setMethodSampleRate(double rate) {
        try {
            MethodSampler sampler = diagnosticManager.getMethodSampler();
            if (sampler != null) {
                sampler.setSampleRate(rate);
                LOGGER.info("Method sample rate set to {} via JMX", rate);
            }
        } catch (Exception e) {
            LOGGER.error("Error setting method sample rate", e);
            throw new RuntimeException("Failed to set method sample rate", e);
        }
    }

    @Override
    public void enableCpuSampling() {
        try {
            ThreadDiagnosticCollector collector = diagnosticManager.getThreadDiagnosticCollector();
            if (collector != null) {
                collector.enableCpuSampling();
                LOGGER.info("CPU sampling enabled via JMX");
            }
        } catch (Exception e) {
            LOGGER.error("Error enabling CPU sampling", e);
        }
    }

    @Override
    public void disableCpuSampling() {
        try {
            ThreadDiagnosticCollector collector = diagnosticManager.getThreadDiagnosticCollector();
            if (collector != null) {
                collector.disableCpuSampling();
                LOGGER.info("CPU sampling disabled via JMX");
            }
        } catch (Exception e) {
            LOGGER.error("Error disabling CPU sampling", e);
        }
    }

    @Override
    public void clearDiagnosticData() {
        try {
            MethodSampler sampler = diagnosticManager.getMethodSampler();
            if (sampler != null) {
                sampler.clear();
            }
            LockDiagnosticCollector lockCollector = diagnosticManager.getLockDiagnosticCollector();
            if (lockCollector != null) {
                lockCollector.getLockSampler().clear();
            }
            LOGGER.info("Diagnostic data cleared via JMX");
        } catch (Exception e) {
            LOGGER.error("Error clearing diagnostic data", e);
        }
    }
}
