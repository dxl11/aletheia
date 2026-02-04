package com.alibaba.aletheia.agent.exporter;

import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.common.model.AgentData;
import com.alibaba.aletheia.common.util.JsonUtil;
import com.alibaba.aletheia.common.util.TimeUtil;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * 文件导出器
 * 将数据导出到本地文件
 *
 * @author Aletheia Team
 */
public class FileExporter extends BaseExporter {

    private final String dataDir;
    private final long pid;
    private final String jvmName;

    public FileExporter(AgentConfig config) {
        this.dataDir = config.getDataDir() != null
                ? config.getDataDir()
                : System.getProperty("java.io.tmpdir") + "/aletheia";
        this.pid = getPid();
        this.jvmName = ManagementFactory.getRuntimeMXBean().getName();
    }

    @Override
    protected void doStart() throws Exception {
        // 创建数据目录
        Path dataPath = Paths.get(dataDir);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
        logger.info("FileExporter started, dataDir: {}, pid: {}", dataDir, pid);
    }

    @Override
    protected void doStop() throws Exception {
        // FileExporter 不需要特殊停止逻辑
    }

    @Override
    protected void doExport(AgentData agentData) throws Exception {
        if (agentData == null) {
            return;
        }

        // 设置基本信息
        agentData.setPid(pid);
        agentData.setJvmName(jvmName);
        agentData.setTimestampNs(TimeUtil.currentTimeNs());

        // 序列化为 JSON
        String json = JsonUtil.toJson(agentData);
        if (json == null) {
            logger.warn("Failed to serialize agent data");
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
        Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);

        logger.debug("Agent data exported to file: {}", filePath);
    }

    /**
     * 获取当前 JVM 进程 ID
     */
    private long getPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(name.split("@")[0]);
        } catch (Exception e) {
            logger.warn("Failed to get PID", e);
            return -1;
        }
    }
}
