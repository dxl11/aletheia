package com.alibaba.aletheia.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 配置文件加载器
 * 支持从文件系统加载配置
 *
 * @author Aletheia Team
 */
public class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);

    /**
     * 从文件加载配置
     */
    public static void loadFromFile(String configPath, AgentConfig config) {
        if (configPath == null || configPath.isEmpty()) {
            return;
        }

        File configFile = new File(configPath);
        if (!configFile.exists() || !configFile.isFile()) {
            LOGGER.warn("Config file not found: {}", configPath);
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
            loadFromProperties(props, config);
            LOGGER.info("Config loaded from file: {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to load config from file: {}", configPath, e);
        }
    }

    /**
     * 从 Properties 加载配置
     */
    private static void loadFromProperties(Properties props, AgentConfig config) {
        // 加载功能开关
        String rtEnabled = props.getProperty("feature.rt.enabled", "false");
        if ("true".equalsIgnoreCase(rtEnabled)) {
            config.enableFeature("RT");
        }

        String gcEnabled = props.getProperty("feature.gc.enabled", "false");
        if ("true".equalsIgnoreCase(gcEnabled)) {
            config.enableFeature("GC");
        }

        String memoryEnabled = props.getProperty("feature.memory.enabled", "false");
        if ("true".equalsIgnoreCase(memoryEnabled)) {
            config.enableFeature("Memory");
        }

        String threadEnabled = props.getProperty("feature.thread.enabled", "false");
        if ("true".equalsIgnoreCase(threadEnabled)) {
            config.enableFeature("Thread");
        }

        // 加载采样率
        String rtSampleRate = props.getProperty("sample.rate.rt");
        if (rtSampleRate != null) {
            try {
                config.setSampleRate("RT", Double.parseDouble(rtSampleRate));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid sample.rate.rt: {}", rtSampleRate);
            }
        }

        // 加载数据目录
        String dataDir = props.getProperty("data.dir");
        if (dataDir != null) {
            config.setDataDir(dataDir);
        }

        // 加载推送间隔
        String pushInterval = props.getProperty("push.interval.ms");
        if (pushInterval != null) {
            try {
                config.setPushIntervalMs(Long.parseLong(pushInterval));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid push.interval.ms: {}", pushInterval);
            }
        }

        // 加载包含模式
        String includePatterns = props.getProperty("include.patterns");
        if (includePatterns != null) {
            String[] patterns = includePatterns.split(",");
            for (String pattern : patterns) {
                config.addIncludePattern(pattern.trim());
            }
        }

        // 加载排除模式
        String excludePatterns = props.getProperty("exclude.patterns");
        if (excludePatterns != null) {
            String[] patterns = excludePatterns.split(",");
            for (String pattern : patterns) {
                config.addExcludePattern(pattern.trim());
            }
        }
    }
}
