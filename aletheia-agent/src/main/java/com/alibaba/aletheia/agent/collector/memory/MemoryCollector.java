package com.alibaba.aletheia.agent.collector.memory;

import com.alibaba.aletheia.common.model.MemoryEvent;
import com.alibaba.aletheia.common.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;

/**
 * 内存数据采集器
 *
 * @author Aletheia Team
 */
public class MemoryCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCollector.class);

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    /**
     * 采集内存数据
     *
     * @return 内存事件
     */
    public MemoryEvent collect() {
        try {
            MemoryEvent event = new MemoryEvent();
            event.setTimestampNs(TimeUtil.currentTimeNs());

            // 堆内存信息
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            event.setHeapUsedBytes(heapUsed);
            event.setHeapMaxBytes(heapMax);

            // 各内存池信息
            List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean pool : pools) {
                String poolName = pool.getName();
                long used = pool.getUsage().getUsed();
                long max = pool.getUsage().getMax();

                if (poolName.contains("Eden")) {
                    event.setEdenUsedBytes(used);
                    event.setEdenMaxBytes(max);
                } else if (poolName.contains("Survivor")) {
                    event.setSurvivorUsedBytes(used);
                    event.setSurvivorMaxBytes(max);
                } else if (poolName.contains("Old") || poolName.contains("Tenured")) {
                    event.setOldUsedBytes(used);
                    event.setOldMaxBytes(max);
                } else if (poolName.contains("Metaspace")) {
                    event.setMetaspaceUsedBytes(used);
                    event.setMetaspaceMaxBytes(max);
                }
            }

            // 直接内存（如果可获取）
            try {
                sun.misc.VM.maxDirectMemory();
                // TODO: 通过反射或其他方式获取实际使用的直接内存
            } catch (Exception e) {
                // 忽略，直接内存可能无法获取
            }

            return event;
        } catch (Exception e) {
            LOGGER.error("Error collecting memory data", e);
            return null;
        }
    }
}
