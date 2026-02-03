package com.alibaba.aletheia.analyzer;

import com.alibaba.aletheia.common.constant.AletheiaConstants;
import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.RtEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 异常检测器
 * 基于历史基线检测 GC/RT 异常
 *
 * @author Aletheia Team
 */
public class AnomalyDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetector.class);

    /**
     * 检测 GC STW 异常
     *
     * @param gcEvent GC 事件
     * @return true 如果异常
     */
    public boolean detectGcAnomaly(GcEvent gcEvent) {
        if (gcEvent == null) {
            return false;
        }

        // STW 时间超过阈值
        if (gcEvent.getPauseTimeMs() > AletheiaConstants.GC_STW_ALERT_THRESHOLD_MS) {
            LOGGER.warn("GC STW anomaly detected: {}ms, GC: {}", 
                    gcEvent.getPauseTimeMs(), gcEvent.getGcName());
            return true;
        }

        return false;
    }

    /**
     * 检测 RT 异常
     *
     * @param rtEvent RT 事件
     * @param baselineP99 基线 P99
     * @return true 如果异常
     */
    public boolean detectRtAnomaly(RtEvent rtEvent, double baselineP99) {
        if (rtEvent == null || baselineP99 <= 0) {
            return false;
        }

        double currentP99 = rtEvent.getP99Ms();
        double threshold = baselineP99 * AletheiaConstants.RT_ALERT_THRESHOLD_MULTIPLIER;

        if (currentP99 > threshold) {
            LOGGER.warn("RT anomaly detected: {}ms (baseline: {}ms), method: {}", 
                    currentP99, baselineP99, rtEvent.getMethodSignature());
            return true;
        }

        return false;
    }

    /**
     * 计算 RT 基线（简化实现，使用历史数据的平均值）
     *
     * @param rtEvents RT 事件列表
     * @return 基线 P99
     */
    public double calculateBaselineP99(List<RtEvent> rtEvents) {
        if (rtEvents == null || rtEvents.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (RtEvent event : rtEvents) {
            sum += event.getP99Ms();
        }

        return sum / rtEvents.size();
    }

    /**
     * 检测内存泄漏（简化实现，检查内存持续增长）
     *
     * @param currentUsed 当前使用量
     * @param previousUsed 之前使用量
     * @param threshold 增长阈值（百分比）
     * @return true 如果检测到内存泄漏
     */
    public boolean detectMemoryLeak(long currentUsed, long previousUsed, double threshold) {
        if (previousUsed <= 0) {
            return false;
        }

        double growthRate = (double) (currentUsed - previousUsed) / previousUsed;
        return growthRate > threshold;
    }
}
