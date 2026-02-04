package com.alibaba.aletheia.agent;

import com.alibaba.aletheia.agent.bootstrap.AgentBootstrap;
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
        AgentBootstrap.getInstance().initPremain(agentArgs, inst);
    }

    /**
     * Agentmain 方法，用于动态 attach 到运行中的 JVM
     *
     * @param agentArgs Agent 参数
     * @param inst Instrumentation 实例
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("Aletheia Agent agentmain called with args: {}", agentArgs);
        AgentBootstrap.getInstance().initAgentmain(agentArgs, inst);
    }
}
