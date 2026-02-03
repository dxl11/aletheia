package com.alibaba.aletheia.analyzer;

import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.RtEvent;
import com.alibaba.aletheia.common.model.ThreadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 告警管理器
 * 负责管理告警规则和触发告警
 *
 * @author Aletheia Team
 */
public class AlertManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertManager.class);

    /**
     * 告警监听器列表
     */
    private final List<AlertListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 异常检测器
     */
    private final AnomalyDetector anomalyDetector = new AnomalyDetector();

    /**
     * RT 基线数据（方法签名 -> 基线 P99）
     */
    private final java.util.Map<String, Double> rtBaselines = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 检查 GC 事件并触发告警
     *
     * @param gcEvent GC 事件
     */
    public void checkGcEvent(GcEvent gcEvent) {
        if (gcEvent == null) {
            return;
        }

        if (anomalyDetector.detectGcAnomaly(gcEvent)) {
            Alert alert = new Alert();
            alert.setType(AlertType.GC_STW_ANOMALY);
            alert.setSeverity(AlertSeverity.WARNING);
            alert.setMessage(String.format("GC STW anomaly detected: %dms, GC: %s", 
                    gcEvent.getPauseTimeMs(), gcEvent.getGcName()));
            alert.setTimestamp(System.currentTimeMillis());
            alert.setData(gcEvent);

            triggerAlert(alert);
        }
    }

    /**
     * 检查 RT 事件并触发告警
     *
     * @param rtEvent RT 事件
     */
    public void checkRtEvent(RtEvent rtEvent) {
        if (rtEvent == null) {
            return;
        }

        String methodSignature = rtEvent.getMethodSignature();
        Double baseline = rtBaselines.get(methodSignature);

        // 如果没有基线，使用当前 P99 作为基线
        if (baseline == null || baseline <= 0) {
            baseline = rtEvent.getP99Ms();
            rtBaselines.put(methodSignature, baseline);
            return;
        }

        if (anomalyDetector.detectRtAnomaly(rtEvent, baseline)) {
            Alert alert = new Alert();
            alert.setType(AlertType.RT_ANOMALY);
            alert.setSeverity(AlertSeverity.WARNING);
            alert.setMessage(String.format("RT anomaly detected: P99=%fms (baseline=%fms), method: %s", 
                    rtEvent.getP99Ms(), baseline, methodSignature));
            alert.setTimestamp(System.currentTimeMillis());
            alert.setData(rtEvent);

            triggerAlert(alert);
        }

        // 更新基线（滑动平均）
        updateBaseline(methodSignature, rtEvent.getP99Ms());
    }

    /**
     * 检查线程事件并触发告警
     *
     * @param threadEvent 线程事件
     */
    public void checkThreadEvent(ThreadEvent threadEvent) {
        if (threadEvent == null) {
            return;
        }

        // 检查死锁
        if (threadEvent.getDeadlockedThreads() != null 
                && !threadEvent.getDeadlockedThreads().isEmpty()) {
            Alert alert = new Alert();
            alert.setType(AlertType.DEADLOCK);
            alert.setSeverity(AlertSeverity.CRITICAL);
            alert.setMessage(String.format("Deadlock detected: %d threads", 
                    threadEvent.getDeadlockedThreads().size()));
            alert.setTimestamp(System.currentTimeMillis());
            alert.setData(threadEvent);

            triggerAlert(alert);
        }

        // 检查锁竞争
        if (threadEvent.getLockContentionInfo() != null 
                && !threadEvent.getLockContentionInfo().isEmpty()) {
            for (ThreadEvent.LockContentionInfo info : threadEvent.getLockContentionInfo()) {
                if (info.getBlockedThreadCount() > 10) {
                    Alert alert = new Alert();
                    alert.setType(AlertType.LOCK_CONTENTION);
                    alert.setSeverity(AlertSeverity.WARNING);
                    alert.setMessage(String.format("Lock contention detected: %d threads blocked on %s", 
                            info.getBlockedThreadCount(), info.getLockObject()));
                    alert.setTimestamp(System.currentTimeMillis());
                    alert.setData(info);

                    triggerAlert(alert);
                }
            }
        }
    }

    /**
     * 触发告警
     *
     * @param alert 告警信息
     */
    private void triggerAlert(Alert alert) {
        LOGGER.warn("Alert triggered: {}", alert.getMessage());

        // 通知所有监听器
        for (AlertListener listener : listeners) {
            try {
                listener.onAlert(alert);
            } catch (Exception e) {
                LOGGER.error("Error notifying alert listener", e);
            }
        }
    }

    /**
     * 更新 RT 基线（滑动平均）
     *
     * @param methodSignature 方法签名
     * @param currentP99 当前 P99
     */
    private void updateBaseline(String methodSignature, double currentP99) {
        Double baseline = rtBaselines.get(methodSignature);
        if (baseline == null) {
            baseline = currentP99;
        } else {
            // 滑动平均：新基线 = 0.9 * 旧基线 + 0.1 * 当前值
            baseline = 0.9 * baseline + 0.1 * currentP99;
        }
        rtBaselines.put(methodSignature, baseline);
    }

    /**
     * 添加告警监听器
     *
     * @param listener 监听器
     */
    public void addListener(AlertListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 移除告警监听器
     *
     * @param listener 监听器
     */
    public void removeListener(AlertListener listener) {
        listeners.remove(listener);
    }

    /**
     * 告警类型
     */
    public enum AlertType {
        GC_STW_ANOMALY,
        RT_ANOMALY,
        DEADLOCK,
        LOCK_CONTENTION,
        MEMORY_LEAK
    }

    /**
     * 告警严重程度
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * 告警信息
     */
    public static class Alert {
        private AlertType type;
        private AlertSeverity severity;
        private String message;
        private long timestamp;
        private Object data;

        public AlertType getType() {
            return type;
        }

        public void setType(AlertType type) {
            this.type = type;
        }

        public AlertSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(AlertSeverity severity) {
            this.severity = severity;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    /**
     * 告警监听器接口
     */
    public interface AlertListener {
        /**
         * 告警触发时的回调
         *
         * @param alert 告警信息
         */
        void onAlert(Alert alert);
    }
}
