package com.alibaba.aletheia.web.config;

import com.alibaba.aletheia.collector.CollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CollectorService 配置
 * 用于在 Web 模块中注入 CollectorService
 *
 * @author Aletheia Team
 */
@Configuration
public class CollectorServiceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorServiceConfig.class);

    @Value("${aletheia.collector.dataDir:${java.io.tmpdir}/aletheia}")
    private String dataDir;

    /**
     * 创建 CollectorService Bean
     * 注意：如果 CollectorService 已经作为独立服务运行，这里可能不需要
     *
     * @return CollectorService 实例（可选）
     */
    @Bean
    public CollectorService collectorService() {
        LOGGER.info("Initializing CollectorService with dataDir: {}", dataDir);
        CollectorService service = new CollectorService(dataDir);
        // 注意：这里不自动启动，由外部控制启动时机
        return service;
    }
}
