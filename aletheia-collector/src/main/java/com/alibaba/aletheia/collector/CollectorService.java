package com.alibaba.aletheia.collector;

import com.alibaba.aletheia.analyzer.AlertManager;
import com.alibaba.aletheia.common.model.AgentData;
import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.RtEvent;
import com.alibaba.aletheia.common.model.ThreadEvent;
import com.alibaba.aletheia.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Collector 服务主类
 * 负责接收 Agent 数据并进行存储
 *
 * @author Aletheia Team
 */
public class CollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorService.class);

    private ScheduledExecutorService scheduler;
    private String dataDir;
    private AlertManager alertManager;
    private com.alibaba.aletheia.collector.storage.DataStorage dataStorage;
    private volatile boolean started = false;

    /**
     * 构造函数
     *
     * @param dataDir 数据存储目录
     */
    public CollectorService(String dataDir) {
        this.dataDir = dataDir;
        this.alertManager = new AlertManager();
        this.dataStorage = new com.alibaba.aletheia.collector.storage.DataStorage();

        // 注册默认的日志告警监听器
        alertManager.addListener(new com.alibaba.aletheia.analyzer.notification.LogAlertListener());
    }

    /**
     * 启动 Collector 服务
     */
    public void start() {
        if (started) {
            LOGGER.warn("CollectorService already started");
            return;
        }

        try {
            LOGGER.info("Starting CollectorService, dataDir: {}", dataDir);

            // 创建数据目录
            Path dataPath = Paths.get(dataDir);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }

            // 创建调度器
            scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "Aletheia-CollectorService");
                t.setDaemon(false);
                return t;
            });

            // 启动数据读取任务（从本地文件读取 Agent 数据）
            scheduler.scheduleAtFixedRate(this::processDataFiles, 0, 1, TimeUnit.SECONDS);

            started = true;
            LOGGER.info("CollectorService started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start CollectorService", e);
            throw new RuntimeException("CollectorService start failed", e);
        }
    }

    /**
     * 停止 Collector 服务
     */
    public void stop() {
        if (!started) {
            return;
        }

        try {
            LOGGER.info("Stopping CollectorService...");

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
            LOGGER.info("CollectorService stopped");
        } catch (Exception e) {
            LOGGER.error("Error stopping CollectorService", e);
        }
    }

    /**
     * 处理数据文件
     */
    private void processDataFiles() {
        try {
            Path dataPath = Paths.get(dataDir);
            if (!Files.exists(dataPath)) {
                return;
            }

            // 读取数据文件（优化：只读取 .json 文件，忽略 .tmp 文件）
            try (java.util.stream.Stream<Path> stream = Files.list(dataPath)) {
                stream.filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith(".json") && !fileName.endsWith(".tmp");
                })
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .limit(100) // 限制每次处理的文件数量，避免一次性处理过多文件
                .forEach(this::processDataFile);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing data files", e);
        }
    }

    /**
     * 处理单个数据文件
     *
     * @param filePath 文件路径
     */
    private void processDataFile(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath), "UTF-8");
            AgentData agentData = JsonUtil.fromJson(content, AgentData.class);
            if (agentData != null) {
                // 处理数据
                processAgentData(agentData);
                LOGGER.debug("Processed agent data from file: {}", filePath.getFileName());
            } else {
                LOGGER.warn("Failed to parse agent data from file: {}", filePath);
            }

            // 删除已处理的文件
            Files.delete(filePath);
        } catch (IOException e) {
            LOGGER.error("Error processing data file: {}", filePath, e);
        }
    }

    /**
     * 处理 Agent 数据
     *
     * @param agentData Agent 数据
     */
    private void processAgentData(AgentData agentData) {
        try {
            // 处理 GC 事件
            if (agentData.getGcEvents() != null && !agentData.getGcEvents().isEmpty()) {
                LOGGER.info("Received {} GC events from PID {}", 
                        agentData.getGcEvents().size(), agentData.getPid());
                for (GcEvent gcEvent : agentData.getGcEvents()) {
                    alertManager.checkGcEvent(gcEvent);
                }
            }

            // 处理线程事件
            ThreadEvent threadEvent = agentData.getThreadEvent();
            if (threadEvent != null) {
                alertManager.checkThreadEvent(threadEvent);
                if (threadEvent.getDeadlockedThreads() != null 
                        && !threadEvent.getDeadlockedThreads().isEmpty()) {
                    LOGGER.warn("Deadlock detected in PID {}: {} threads", 
                            agentData.getPid(), threadEvent.getDeadlockedThreads().size());
                }
            }

            // 处理 RT 事件
            if (agentData.getRtEvents() != null && !agentData.getRtEvents().isEmpty()) {
                LOGGER.debug("Received {} RT events from PID {}", 
                        agentData.getRtEvents().size(), agentData.getPid());
                for (RtEvent rtEvent : agentData.getRtEvents()) {
                    alertManager.checkRtEvent(rtEvent);
                }
            }

            // 存储数据
            dataStorage.store(agentData);
        } catch (Exception e) {
            LOGGER.error("Error processing agent data", e);
        }
    }

    /**
     * 获取告警管理器
     *
     * @return 告警管理器
     */
    public AlertManager getAlertManager() {
        return alertManager;
    }

    /**
     * 获取数据存储
     *
     * @return 数据存储
     */
    public com.alibaba.aletheia.collector.storage.DataStorage getDataStorage() {
        return dataStorage;
    }

    /**
     * 接收 Agent 数据（HTTP 接口）
     *
     * @param agentData Agent 数据
     */
    public void receiveData(AgentData agentData) {
        try {
            // TODO: 存储到时序数据库
            String json = JsonUtil.toJson(agentData);
            LOGGER.debug("Received agent data: {}", json);
        } catch (Exception e) {
            LOGGER.error("Error receiving agent data", e);
        }
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
