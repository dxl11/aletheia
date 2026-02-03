package com.alibaba.aletheia.agent.collector.gc;

import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class GcEventCollector implements NotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcEventCollector.class);

    private final List<GcEvent> gcEvents = new CopyOnWriteArrayList<>();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    /**
     * 构造函数，注册 GC 通知监听器
     */
    public GcEventCollector() {
        try {
            // 为每个 GC MXBean 注册监听器
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                if (gcBean instanceof javax.management.NotificationEmitter) {
                    ((javax.management.NotificationEmitter) gcBean)
                            .addNotificationListener(this, null, null);
                }
            }

            LOGGER.info("GcEventCollector initialized, monitoring {} GC beans", gcBeans.size());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize GcEventCollector", e);
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
            LOGGER.error("Error handling GC notification", e);
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
            LOGGER.debug("GC event collected: {}", event);
        } catch (Exception e) {
            LOGGER.error("Error processing GC notification", e);
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
            LOGGER.error("Error updating memory info", e);
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
