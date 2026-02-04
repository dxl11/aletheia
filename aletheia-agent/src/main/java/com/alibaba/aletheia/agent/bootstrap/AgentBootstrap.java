package com.alibaba.aletheia.agent.bootstrap;

import com.alibaba.aletheia.agent.collector.CollectorManager;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.config.ConfigLoader;
import com.alibaba.aletheia.agent.control.AgentControl;
import com.alibaba.aletheia.agent.diagnostic.DiagnosticHelper;
import com.alibaba.aletheia.agent.diagnostic.DiagnosticManager;
import com.alibaba.aletheia.agent.diagnostic.control.DiagnosticControl;
import com.alibaba.aletheia.agent.exporter.ExporterManager;
import com.alibaba.aletheia.agent.transformer.TransformerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;

/**
 * Agent 启动器
 * 负责初始化 Agent 核心组件
 *
 * @author Aletheia Team
 */
public class AgentBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentBootstrap.class);

    private static volatile AgentBootstrap instance;
    private static volatile boolean initialized = false;

    private AgentConfig config;
    private TransformerManager transformerManager;
    private CollectorManager collectorManager;
    private ExporterManager exporterManager;
    private AgentControl agentControl;
    private Instrumentation instrumentation;
    private boolean isPremain;

    private AgentBootstrap() {
    }

    /**
     * 获取单例实例
     */
    public static AgentBootstrap getInstance() {
        if (instance == null) {
            synchronized (AgentBootstrap.class) {
                if (instance == null) {
                    instance = new AgentBootstrap();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化 Agent（premain）
     */
    public synchronized void initPremain(String agentArgs, Instrumentation inst) {
        if (initialized) {
            LOGGER.warn("Agent already initialized");
            return;
        }

        this.isPremain = true;
        initAgent(agentArgs, inst, false); // premain 时类未加载，canRetransform=false
    }

    /**
     * 初始化 Agent（agentmain）
     */
    public synchronized void initAgentmain(String agentArgs, Instrumentation inst) {
        if (initialized) {
            LOGGER.warn("Agent already initialized");
            return;
        }

        this.isPremain = false;
        initAgent(agentArgs, inst, true); // agentmain 时需要 retransform
    }

    /**
     * 初始化 Agent
     */
    private void initAgent(String agentArgs, Instrumentation inst, boolean canRetransform) {
        try {
            LOGGER.info("Initializing Aletheia Agent (mode: {})", isPremain ? "premain" : "agentmain");

            this.instrumentation = inst;

            // 1. 初始化配置
            config = AgentConfig.getInstance();
            config.parseAgentArgs(agentArgs);

            // 2. 加载配置文件（如果指定）
            String configPath = System.getProperty("aletheia.config");
            if (configPath != null) {
                ConfigLoader.loadFromFile(configPath, config);
            }

            // 3. 初始化 TransformerManager
            transformerManager = new TransformerManager(instrumentation, config);
            transformerManager.initDefaultTransformers(canRetransform);

            // 4. 初始化 ExporterManager
            exporterManager = new ExporterManager(config);
            exporterManager.start();

            // 5. 初始化 CollectorManager
            collectorManager = new CollectorManager(config);
            collectorManager.setExporterManager(exporterManager);
            collectorManager.start();

            // 6. 初始化诊断管理器
            DiagnosticManager diagnosticManager = DiagnosticManager.getInstance(config);
            diagnosticManager.start();
            DiagnosticHelper.init(config);

            // 7. 注册 JMX MBean
            registerMBean(diagnosticManager);

            // 8. 注册 Shutdown Hook
            registerShutdownHook();

            initialized = true;
            LOGGER.info("Aletheia Agent initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Aletheia Agent", e);
            throw new RuntimeException("Agent initialization failed", e);
        }
    }

    /**
     * 注册 JMX MBean
     */
    private void registerMBean(DiagnosticManager diagnosticManager) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            // 注册 AgentControl MBean
            ObjectName agentControlName = new ObjectName("com.alibaba.aletheia:type=AgentControl");
            agentControl = new AgentControl(config, transformerManager, collectorManager);
            mbs.registerMBean(agentControl, agentControlName);
            LOGGER.info("AgentControl MBean registered: {}", agentControlName);

            // 注册 DiagnosticControl MBean
            ObjectName diagnosticControlName = new ObjectName("com.alibaba.aletheia:type=DiagnosticControl");
            DiagnosticControl diagnosticControl = new DiagnosticControl(config, diagnosticManager);
            mbs.registerMBean(diagnosticControl, diagnosticControlName);
            LOGGER.info("DiagnosticControl MBean registered: {}", diagnosticControlName);
        } catch (Exception e) {
            LOGGER.error("Failed to register MBeans", e);
        }
    }

    /**
     * 注册 Shutdown Hook
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Agent shutdown hook triggered");
            shutdown();
        }, "Aletheia-ShutdownHook"));
    }

    /**
     * 关闭 Agent
     */
    public synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            LOGGER.info("Shutting down Aletheia Agent...");

            if (collectorManager != null) {
                collectorManager.stop();
            }

            if (exporterManager != null) {
                exporterManager.stop();
            }

            // 注销 MBean
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                ObjectName name = new ObjectName("com.alibaba.aletheia:type=AgentControl");
                if (mbs.isRegistered(name)) {
                    mbs.unregisterMBean(name);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister MBean", e);
            }

            initialized = false;
            LOGGER.info("Aletheia Agent shut down");
        } catch (Exception e) {
            LOGGER.error("Error shutting down Agent", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
