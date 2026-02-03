package com.alibaba.aletheia.agent;

import com.alibaba.aletheia.agent.collector.DataCollector;
import com.alibaba.aletheia.agent.transformer.MethodTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * Aletheia Agent 入口类
 *
 * @author Aletheia Team
 */
public class AletheiaAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AletheiaAgent.class);

    /**
     * Premain 方法，在 JVM 启动时调用
     *
     * @param agentArgs Agent 参数
     * @param inst Instrumentation 实例
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("Aletheia Agent premain called with args: {}", agentArgs);
        initAgent(agentArgs, inst);
    }

    /**
     * Agentmain 方法，用于动态 attach 到运行中的 JVM
     *
     * @param agentArgs Agent 参数
     * @param inst Instrumentation 实例
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("Aletheia Agent agentmain called with args: {}", agentArgs);
        initAgent(agentArgs, inst);
    }

    /**
     * 初始化 Agent
     *
     * @param agentArgs Agent 参数（格式：dataDir=/path/to/data）
     * @param inst Instrumentation 实例
     */
    private static void initAgent(String agentArgs, Instrumentation inst) {
        try {
            // 解析 Agent 参数
            String dataDir = parseDataDir(agentArgs);

            // 添加字节码转换器
            inst.addTransformer(new MethodTransformer(), true);

            // 启动数据采集器
            DataCollector.getInstance().start(dataDir);

            LOGGER.info("Aletheia Agent initialized successfully, dataDir: {}", dataDir);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Aletheia Agent", e);
            throw new RuntimeException("Aletheia Agent initialization failed", e);
        }
    }

    /**
     * 解析 Agent 参数，提取数据目录
     *
     * @param agentArgs Agent 参数
     * @return 数据目录，如果未指定则返回 null
     */
    private static String parseDataDir(String agentArgs) {
        if (agentArgs == null || agentArgs.isEmpty()) {
            return null;
        }

        // 简单解析：dataDir=/path/to/data
        String[] parts = agentArgs.split("=");
        if (parts.length == 2 && "dataDir".equals(parts[0].trim())) {
            return parts[1].trim();
        }

        return null;
    }
}
