package com.alibaba.aletheia.agent.control;

/**
 * Agent 控制 MBean 接口
 * 提供运行时控制能力
 *
 * @author Aletheia Team
 */
public interface AgentControlMBean {

    /**
     * 启用功能
     *
     * @param feature 功能名称（RT, GC, Memory, Thread）
     */
    void enableFeature(String feature);

    /**
     * 禁用功能
     *
     * @param feature 功能名称
     */
    void disableFeature(String feature);

    /**
     * 检查功能是否启用
     *
     * @param feature 功能名称
     * @return true 如果启用
     */
    boolean isFeatureEnabled(String feature);

    /**
     * 设置采样率
     *
     * @param feature 功能名称
     * @param rate 采样率（0.0 - 1.0）
     */
    void setSampleRate(String feature, double rate);

    /**
     * 获取采样率
     *
     * @param feature 功能名称
     * @return 采样率
     */
    double getSampleRate(String feature);

    /**
     * 添加包含模式（白名单）
     *
     * @param pattern 模式（如 com/example/service）
     */
    void addIncludePattern(String pattern);

    /**
     * 移除包含模式
     *
     * @param pattern 模式
     */
    void removeIncludePattern(String pattern);

    /**
     * 添加排除模式（黑名单）
     *
     * @param pattern 模式
     */
    void addExcludePattern(String pattern);

    /**
     * 移除排除模式
     *
     * @param pattern 模式
     */
    void removeExcludePattern(String pattern);

    /**
     * 获取 Agent 状态
     *
     * @return 状态信息（JSON 格式）
     */
    String getStatus();

    /**
     * 关闭 Agent
     */
    void shutdown();
}
