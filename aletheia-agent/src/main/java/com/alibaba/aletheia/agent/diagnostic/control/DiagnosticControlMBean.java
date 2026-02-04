package com.alibaba.aletheia.agent.diagnostic.control;

/**
 * 诊断控制 MBean 接口
 * 提供诊断能力的运行时控制接口
 *
 * @author Aletheia Team
 */
public interface DiagnosticControlMBean {

    /**
     * 启用诊断功能
     *
     * @param feature 功能名称（CPU, Lock, Method, GC, Thread）
     */
    void enableDiagnostic(String feature);

    /**
     * 禁用诊断功能
     *
     * @param feature 功能名称
     */
    void disableDiagnostic(String feature);

    /**
     * 检查诊断功能是否启用
     *
     * @param feature 功能名称
     * @return true 如果启用
     */
    boolean isDiagnosticEnabled(String feature);

    /**
     * 获取 CPU 占用最高的线程（Top N）
     *
     * @param topN Top N
     * @return JSON 格式的线程 CPU 信息
     */
    String getTopCpuThreads(int topN);

    /**
     * 获取线程状态分布
     *
     * @return JSON 格式的线程状态分布
     */
    String getThreadStateDistribution();

    /**
     * 获取等待时间最长的线程（Top N）
     *
     * @param topN Top N
     * @return JSON 格式的线程等待信息
     */
    String getTopWaitingThreads(int topN);

    /**
     * 检测死锁
     *
     * @return JSON 格式的死锁信息
     */
    String detectDeadlock();

    /**
     * 获取锁竞争最严重的锁（Top N）
     *
     * @param topN Top N
     * @return JSON 格式的锁竞争信息
     */
    String getTopContendedLocks(int topN);

    /**
     * 获取锁竞争统计
     *
     * @return JSON 格式的锁竞争统计
     */
    String getLockContentionStats();

    /**
     * 获取方法热点（Top N）
     *
     * @param topN Top N
     * @return JSON 格式的方法热点信息
     */
    String getHotMethods(int topN);

    /**
     * 获取最近的 GC 事件
     *
     * @param count 事件数量
     * @return JSON 格式的 GC 事件列表
     */
    String getRecentGcEvents(int count);

    /**
     * 获取 Full GC 事件
     *
     * @return JSON 格式的 Full GC 事件列表
     */
    String getFullGcEvents();

    /**
     * 获取堆内存信息
     *
     * @return JSON 格式的堆内存信息
     */
    String getHeapMemoryInfo();

    /**
     * 设置方法采样率
     *
     * @param rate 采样率（0.0 - 1.0）
     */
    void setMethodSampleRate(double rate);

    /**
     * 启用 CPU 采样
     */
    void enableCpuSampling();

    /**
     * 禁用 CPU 采样
     */
    void disableCpuSampling();

    /**
     * 清理诊断数据
     */
    void clearDiagnosticData();
}
