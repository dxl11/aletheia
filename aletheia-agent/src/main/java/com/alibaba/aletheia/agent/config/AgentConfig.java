package com.alibaba.aletheia.agent.config;

import com.alibaba.aletheia.common.constant.AletheiaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 配置管理类
 * 支持线程安全的热更新
 *
 * @author Aletheia Team
 */
public class AgentConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentConfig.class);

    private static volatile AgentConfig instance;

    // 功能开关
    private final Map<String, Boolean> featureEnabled = new ConcurrentHashMap<>();

    // 采样率配置
    private final Map<String, Double> sampleRates = new ConcurrentHashMap<>();

    // 包含模式（白名单）
    private final Set<String> includePatterns = ConcurrentHashMap.newKeySet();

    // 排除模式（黑名单）
    private final Set<String> excludePatterns = ConcurrentHashMap.newKeySet();

    // 数据目录
    private volatile String dataDir;

    // 推送间隔（毫秒）
    private volatile long pushIntervalMs = AletheiaConstants.DEFAULT_PUSH_INTERVAL_MS;

    // 最大类大小（字节），超过此大小的类不增强
    private volatile int maxClassSize = 1024 * 1024; // 1MB

    private AgentConfig() {
        // 初始化默认配置
        initDefaultConfig();
    }

    /**
     * 获取单例实例
     */
    public static AgentConfig getInstance() {
        if (instance == null) {
            synchronized (AgentConfig.class) {
                if (instance == null) {
                    instance = new AgentConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化默认配置
     */
    private void initDefaultConfig() {
        // 默认所有功能关闭（按需开启）
        featureEnabled.put("RT", false);
        featureEnabled.put("GC", false);
        featureEnabled.put("Memory", false);
        featureEnabled.put("Thread", false);
        featureEnabled.put("Lock", false); // 锁诊断功能
        featureEnabled.put("CPU", false); // CPU 诊断功能
        featureEnabled.put("Method", false); // 方法热点诊断功能

        // 默认采样率
        sampleRates.put("RT", AletheiaConstants.DEFAULT_SAMPLE_RATE);
        sampleRates.put("GC", 1.0); // GC 事件全量采集
        sampleRates.put("Memory", 1.0); // 内存数据全量采集
        sampleRates.put("Thread", 1.0); // 线程数据全量采集

        // 默认排除模式（系统类）
        excludePatterns.add("java/");
        excludePatterns.add("javax/");
        excludePatterns.add("sun/");
        excludePatterns.add("com/sun/");
        excludePatterns.add("jdk/internal/");
        excludePatterns.add("com/alibaba/aletheia/");
        excludePatterns.add("org/slf4j/");
        excludePatterns.add("ch/qos/logback/");
        excludePatterns.add("org/objectweb/asm/");
    }

    /**
     * 启用功能
     */
    public void enableFeature(String feature) {
        featureEnabled.put(feature, true);
        LOGGER.info("Feature {} enabled", feature);
    }

    /**
     * 禁用功能
     */
    public void disableFeature(String feature) {
        featureEnabled.put(feature, false);
        LOGGER.info("Feature {} disabled", feature);
    }

    /**
     * 检查功能是否启用
     */
    public boolean isFeatureEnabled(String feature) {
        return featureEnabled.getOrDefault(feature, false);
    }

    /**
     * 设置采样率
     */
    public void setSampleRate(String feature, double rate) {
        if (rate < 0 || rate > 1) {
            throw new IllegalArgumentException("Sample rate must be between 0 and 1");
        }
        sampleRates.put(feature, rate);
        LOGGER.info("Sample rate for {} set to {}", feature, rate);
    }

    /**
     * 获取采样率
     */
    public double getSampleRate(String feature) {
        return sampleRates.getOrDefault(feature, AletheiaConstants.DEFAULT_SAMPLE_RATE);
    }

    /**
     * 添加包含模式
     */
    public void addIncludePattern(String pattern) {
        includePatterns.add(pattern);
        LOGGER.info("Include pattern added: {}", pattern);
    }

    /**
     * 移除包含模式
     */
    public void removeIncludePattern(String pattern) {
        includePatterns.remove(pattern);
        LOGGER.info("Include pattern removed: {}", pattern);
    }

    /**
     * 添加排除模式
     */
    public void addExcludePattern(String pattern) {
        excludePatterns.add(pattern);
        LOGGER.info("Exclude pattern added: {}", pattern);
    }

    /**
     * 移除排除模式
     */
    public void removeExcludePattern(String pattern) {
        excludePatterns.remove(pattern);
        LOGGER.info("Exclude pattern removed: {}", pattern);
    }

    /**
     * 获取所有包含模式
     */
    public Set<String> getIncludePatterns() {
        return new HashSet<>(includePatterns);
    }

    /**
     * 获取所有排除模式
     */
    public Set<String> getExcludePatterns() {
        return new HashSet<>(excludePatterns);
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public long getPushIntervalMs() {
        return pushIntervalMs;
    }

    public void setPushIntervalMs(long pushIntervalMs) {
        this.pushIntervalMs = pushIntervalMs;
    }

    public int getMaxClassSize() {
        return maxClassSize;
    }

    public void setMaxClassSize(int maxClassSize) {
        this.maxClassSize = maxClassSize;
    }

    /**
     * 解析 Agent 参数
     * 格式：key1=value1,key2=value2
     */
    public void parseAgentArgs(String agentArgs) {
        if (agentArgs == null || agentArgs.isEmpty()) {
            return;
        }

        String[] pairs = agentArgs.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();

                switch (key) {
                    case "dataDir":
                        setDataDir(value);
                        break;
                    case "pushIntervalMs":
                        try {
                            setPushIntervalMs(Long.parseLong(value));
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid pushIntervalMs: {}", value);
                        }
                        break;
                    case "maxClassSize":
                        try {
                            setMaxClassSize(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid maxClassSize: {}", value);
                        }
                        break;
                    default:
                        LOGGER.debug("Unknown config key: {}", key);
                }
            }
        }
    }
}
