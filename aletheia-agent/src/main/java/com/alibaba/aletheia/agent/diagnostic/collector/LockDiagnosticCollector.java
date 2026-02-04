package com.alibaba.aletheia.agent.diagnostic.collector;

import com.alibaba.aletheia.agent.collector.BaseCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.diagnostic.sampler.LockSampler;

import java.util.List;
import java.util.Map;

/**
 * 锁诊断采集器
 * 用于诊断锁竞争问题
 *
 * @author Aletheia Team
 */
public class LockDiagnosticCollector extends BaseCollector {

    private final LockSampler lockSampler;

    public LockDiagnosticCollector(AgentConfig config) {
        super(config);
        this.lockSampler = new LockSampler();
    }

    @Override
    protected boolean isFeatureEnabled() {
        // 锁诊断需要单独启用
        return config.isFeatureEnabled("Lock") || config.isFeatureEnabled("Thread");
    }

    @Override
    protected void doStart() throws Exception {
        // LockSampler 不需要特殊启动逻辑
    }

    @Override
    protected void doStop() throws Exception {
        // LockSampler 不需要特殊停止逻辑
    }

    /**
     * 采样锁竞争情况
     */
    public void sampleLockContention() {
        lockSampler.sampleLockContention();
    }

    /**
     * 获取锁竞争最严重的锁（Top N）
     */
    public List<LockSampler.LockInfo> getTopContendedLocks(int topN) {
        return lockSampler.getTopContendedLocks(topN);
    }

    /**
     * 获取所有锁信息
     */
    public Map<String, LockSampler.LockInfo> getAllLockInfo() {
        return lockSampler.getAllLockInfo();
    }

    /**
     * 获取锁竞争统计
     */
    public LockSampler.LockContentionStats getContentionStats() {
        return lockSampler.getContentionStats();
    }

    /**
     * 记录锁获取
     */
    public void recordLockAcquire(String lockIdentity, long threadId) {
        lockSampler.recordLockAcquire(lockIdentity, threadId);
    }

    /**
     * 记录锁释放
     */
    public void recordLockRelease(String lockIdentity, long threadId) {
        lockSampler.recordLockRelease(lockIdentity, threadId);
    }

    /**
     * 记录锁等待
     */
    public void recordLockWait(String lockIdentity, long threadId, long waitTime) {
        lockSampler.recordLockWait(lockIdentity, threadId, waitTime);
    }

    public LockSampler getLockSampler() {
        return lockSampler;
    }
}
