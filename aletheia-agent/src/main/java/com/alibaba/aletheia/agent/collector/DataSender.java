package com.alibaba.aletheia.agent.collector;

import com.alibaba.aletheia.common.model.AgentData;
import com.alibaba.aletheia.common.util.JsonUtil;
import com.alibaba.aletheia.common.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 数据发送器
 * 负责将 Agent 采集的数据发送到 Collector（本地文件方式）
 *
 * @author Aletheia Team
 */
public class DataSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSender.class);

    /**
     * 数据目录
     */
    private final String dataDir;

    /**
     * JVM 进程 ID
     */
    private final long pid;

    /**
     * JVM 名称
     */
    private final String jvmName;

    /**
     * 构造函数
     *
     * @param dataDir 数据目录
     */
    public DataSender(String dataDir) {
        this.dataDir = dataDir != null ? dataDir : System.getProperty("java.io.tmpdir") + "/aletheia";
        this.pid = getPid();
        this.jvmName = ManagementFactory.getRuntimeMXBean().getName();

        // 创建数据目录
        try {
            Path dataPath = Paths.get(this.dataDir);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }
            LOGGER.info("DataSender initialized, dataDir: {}, pid: {}", this.dataDir, this.pid);
        } catch (IOException e) {
            LOGGER.error("Failed to create data directory: {}", this.dataDir, e);
        }
    }

    /**
     * 发送 Agent 数据
     *
     * @param agentData Agent 数据
     */
    public void send(AgentData agentData) {
        if (agentData == null) {
            return;
        }

        try {
            // 设置基本信息
            agentData.setPid(pid);
            agentData.setJvmName(jvmName);
            agentData.setTimestampNs(TimeUtil.currentTimeNs());

            // 序列化为 JSON
            String json = JsonUtil.toJson(agentData);
            if (json == null) {
                LOGGER.warn("Failed to serialize agent data");
                return;
            }

            // 写入文件（使用临时文件 + 原子移动，避免读取到不完整文件）
            String fileName = String.format("%d-%d.json", TimeUtil.currentTimeMs(), pid);
            Path filePath = Paths.get(dataDir, fileName);
            Path tempPath = Paths.get(dataDir, fileName + ".tmp");

            // 先写入临时文件
            Files.write(tempPath, json.getBytes("UTF-8"), StandardOpenOption.CREATE, 
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            // 原子移动到目标文件
            Files.move(tempPath, filePath, java.nio.file.StandardCopyOption.ATOMIC_MOVE, 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            LOGGER.debug("Agent data sent to file: {}", filePath);
        } catch (Exception e) {
            LOGGER.error("Error sending agent data", e);
        }
    }

    /**
     * 获取当前 JVM 进程 ID
     *
     * @return 进程 ID
     */
    private long getPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(name.split("@")[0]);
        } catch (Exception e) {
            LOGGER.warn("Failed to get PID", e);
            return -1;
        }
    }

    /**
     * 获取数据目录
     *
     * @return 数据目录
     */
    public String getDataDir() {
        return dataDir;
    }
}
