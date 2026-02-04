package com.alibaba.aletheia.agent.collector.gc;

import com.alibaba.aletheia.agent.collector.BaseCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.util.TimeUtil;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GC 事件采集器
 * 通过 JMX GCNotification 监听 GC 事件
 *
 * @author Aletheia Team
 */
public class GcEventCollector extends BaseCollector implements NotificationListener {

    private final List<GcEvent> gcEvents = new CopyOnWriteArrayList<>();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private List<GarbageCollectorMXBean> gcBeans;

    /**
     * 构造函数
     */
    public GcEventCollector(AgentConfig config) {
        super(config);
    }

    @Override
    protected boolean isFeatureEnabled() {
        return config.isFeatureEnabled("GC");
    }

    @Override
    protected void doStart() throws Exception {
        // 为每个 GC MXBean 注册监听器
        gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean instanceof javax.management.NotificationEmitter) {
                ((javax.management.NotificationEmitter) gcBean)
                        .addNotificationListener(this, null, null);
            }
        }
        logger.info("GcEventCollector started, monitoring {} GC beans", gcBeans.size());
    }

    @Override
    protected void doStop() throws Exception {
        // 移除监听器
        if (gcBeans != null) {
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                if (gcBean instanceof javax.management.NotificationEmitter) {
                    try {
                        ((javax.management.NotificationEmitter) gcBean)
                                .removeNotificationListener(this);
                    } catch (Exception e) {
                        logger.warn("Failed to remove GC notification listener", e);
                    }
                }
            }
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        try {
            String type = notification.getType();
            if ("com.sun.management.gc.notification".equals(type)) {
                processGcNotification(notification);
            }
        } catch (Exception e) {
            logger.error("Error handling GC notification", e);
        }
    }

    /**
     * 处理 GC 通知
     *
     * @param notification GC 通知
     */
    private void processGcNotification(Notification notification) {
        try {
            CompositeData cd = (CompositeData) notification.getUserData();
            String gcName = (String) cd.get("gcName");
            String gcAction = (String) cd.get("gcAction");
            String gcCause = (String) cd.get("gcCause");
            Long startTime = (Long) cd.get("startTime");
            Long endTime = (Long) cd.get("endTime");
            Long duration = (Long) cd.get("duration");

            GcEvent event = new GcEvent();
            event.setGcName(gcName);
            event.setGcCause(gcCause);
            event.setStartTimeNs(TimeUtil.millisToNanos(startTime));
            event.setEndTimeNs(TimeUtil.millisToNanos(endTime));
            event.setPauseTimeMs(duration);

            // 判断 GC 类型
            if (gcAction.contains("major") || gcAction.contains("full")) {
                event.setGcType("Full GC");
            } else {
                event.setGcType("Young GC");
            }

            // 获取内存信息
            updateMemoryInfo(event);

            gcEvents.add(event);
            logger.debug("GC event collected: {}", event);
        } catch (Exception e) {
            logger.error("Error processing GC notification", e);
        }
    }

    /**
     * 更新内存信息
     *
     * @param event GC 事件
     */
    private void updateMemoryInfo(GcEvent event) {
        try {
            List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean pool : pools) {
                String poolName = pool.getName();
                long used = pool.getUsage().getUsed();

                if (poolName.contains("Eden")) {
                    event.setEdenUsedBytes(used);
                } else if (poolName.contains("Survivor")) {
                    event.setSurvivorUsedBytes(used);
                } else if (poolName.contains("Old") || poolName.contains("Tenured")) {
                    event.setOldUsedBytes(used);
                }
            }

            // 堆内存信息
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            event.setHeapUsedAfterBytes(heapUsed);
            // TODO: 记录 GC 前的堆内存使用量（需要提前记录）
        } catch (Exception e) {
            logger.error("Error updating memory info", e);
        }
    }

    /**
     * 获取并清空 GC 事件列表
     *
     * @return GC 事件列表
     */
    public List<GcEvent> getAndClearGcEvents() {
        List<GcEvent> result = new ArrayList<>(gcEvents);
        gcEvents.clear();
        return result;
    }
}
