package com.alibaba.aletheia.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collector 服务启动类
 *
 * @author Aletheia Team
 */
public class CollectorMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorMain.class);

    /**
     * 主方法
     *
     * @param args 命令行参数：--dataDir=/path/to/data
     */
    public static void main(String[] args) {
        String dataDir = parseDataDir(args);
        if (dataDir == null) {
            dataDir = System.getProperty("java.io.tmpdir") + "/aletheia";
            LOGGER.info("Using default data directory: {}", dataDir);
        }

        CollectorService collectorService = new CollectorService(dataDir);
        collectorService.start();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down CollectorService...");
            collectorService.stop();
        }));

        // 保持运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            LOGGER.info("CollectorService interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 解析命令行参数，提取数据目录
     *
     * @param args 命令行参数
     * @return 数据目录
     */
    private static String parseDataDir(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (String arg : args) {
            if (arg.startsWith("--dataDir=")) {
                return arg.substring("--dataDir=".length());
            }
        }

        return null;
    }
}
