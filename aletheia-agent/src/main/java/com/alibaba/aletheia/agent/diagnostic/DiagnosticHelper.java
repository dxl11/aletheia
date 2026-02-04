package com.alibaba.aletheia.agent.diagnostic;

import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.diagnostic.collector.LockDiagnosticCollector;

/**
 * 诊断辅助类
 * 提供静态方法供字节码增强调用
 *
 * @author Aletheia Team
 */
public class DiagnosticHelper {

    private static volatile DiagnosticManager diagnosticManager;
    private static volatile AgentConfig config;

    /**
     * 初始化（由 AgentBootstrap 调用）
     */
    public static void init(AgentConfig config) {
        DiagnosticHelper.config = config;
        DiagnosticHelper.diagnosticManager = DiagnosticManager.getInstance(config);
    }

    /**
     * 获取 LockDiagnosticCollector（供字节码增强调用）
     */
    public static LockDiagnosticCollector getLockDiagnosticCollector() {
        if (diagnosticManager == null) {
            return null;
        }
        return diagnosticManager.getLockDiagnosticCollector();
    }

    /**
     * 记录锁获取
     */
    public static void recordLockAcquire(String lockIdentity, long threadId) {
        LockDiagnosticCollector collector = getLockDiagnosticCollector();
        if (collector != null) {
            collector.recordLockAcquire(lockIdentity, threadId);
        }
    }

    /**
     * 记录锁释放
     */
    public static void recordLockRelease(String lockIdentity, long threadId) {
        LockDiagnosticCollector collector = getLockDiagnosticCollector();
        if (collector != null) {
            collector.recordLockRelease(lockIdentity, threadId);
        }
    }
}
