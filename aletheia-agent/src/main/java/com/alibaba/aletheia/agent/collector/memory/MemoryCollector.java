package com.alibaba.aletheia.agent.collector.memory;

import com.alibaba.aletheia.agent.collector.BaseCollector;
import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.common.model.MemoryEvent;
import com.alibaba.aletheia.common.util.TimeUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 内存数据采集器
 *
 * @author Aletheia Team
 */
public class MemoryCollector extends BaseCollector {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    /**
     * 构造函数
     */
    public MemoryCollector(AgentConfig config) {
        super(config);
    }

    @Override
    protected boolean isFeatureEnabled() {
        return config.isFeatureEnabled("Memory");
    }

    @Override
    protected void doStart() throws Exception {
        // MemoryCollector 不需要特殊启动逻辑
    }

    @Override
    protected void doStop() throws Exception {
        // MemoryCollector 不需要特殊停止逻辑
    }

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
                // 通过反射获取 java.nio.Bits 中的直接内存使用量
                Class<?> bitsClass = Class.forName("java.nio.Bits");
                Field reservedMemoryField = bitsClass.getDeclaredField("reservedMemory");
                reservedMemoryField.setAccessible(true);
                Long reservedMemory = (Long) reservedMemoryField.get(null);
                
                if (reservedMemory != null) {
                    event.setDirectMemoryUsedBytes(reservedMemory);
                }
            } catch (Exception e) {
                // 忽略，直接内存可能无法获取（某些 JVM 版本或配置可能不支持）
                logger.debug("Unable to get direct memory usage", e);
            }

            return event;
        } catch (Exception e) {
            logger.error("Error collecting memory data", e);
            return null;
        }
    }
}
