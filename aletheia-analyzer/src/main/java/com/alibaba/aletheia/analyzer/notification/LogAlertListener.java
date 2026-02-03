package com.alibaba.aletheia.analyzer.notification;

import com.alibaba.aletheia.analyzer.AlertManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志告警监听器
 * 将告警信息记录到日志
 *
 * @author Aletheia Team
 */
public class LogAlertListener implements AlertManager.AlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogAlertListener.class);

    @Override
    public void onAlert(AlertManager.Alert alert) {
        if (alert == null) {
            return;
        }

        String level = getLogLevel(alert.getSeverity());
        String message = String.format("[ALERT] %s - %s", alert.getType(), alert.getMessage());

        switch (level) {
            case "ERROR":
                LOGGER.error(message);
                break;
            case "WARN":
                LOGGER.warn(message);
                break;
            default:
                LOGGER.info(message);
                break;
        }
    }

    /**
     * 根据告警严重程度获取日志级别
     *
     * @param severity 告警严重程度
     * @return 日志级别
     */
    private String getLogLevel(AlertManager.AlertSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return "ERROR";
            case WARNING:
                return "WARN";
            default:
                return "INFO";
        }
    }
}
