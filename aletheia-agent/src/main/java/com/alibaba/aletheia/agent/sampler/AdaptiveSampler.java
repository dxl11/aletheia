package com.alibaba.aletheia.agent.sampler;

import com.alibaba.aletheia.common.constant.AletheiaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * 自适应采样率控制器
 * 根据 CPU 使用率动态调整采样率
 *
 * @author Aletheia Team
 */
public class AdaptiveSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveSampler.class);

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    /**
     * 更新采样率（根据 CPU 使用率）
     */
    public void updateSampleRate() {
        double cpuUsage = getCpuUsage();
        double newSampleRate;

        if (cpuUsage > AletheiaConstants.CPU_HIGH_THRESHOLD) {
            // CPU 使用率高，降低采样率
            newSampleRate = AletheiaConstants.MIN_SAMPLE_RATE;
        } else if (cpuUsage < AletheiaConstants.CPU_LOW_THRESHOLD) {
            // CPU 使用率低，提高采样率
            newSampleRate = AletheiaConstants.MAX_SAMPLE_RATE;
        } else {
            // CPU 使用率中等，使用默认采样率
            newSampleRate = AletheiaConstants.DEFAULT_SAMPLE_RATE;
        }

        RtSampler.setSampleRate(newSampleRate);
        LOGGER.debug("Sample rate updated to {} based on CPU usage: {}", newSampleRate, cpuUsage);
    }

    /**
     * 获取 CPU 使用率
     *
     * @return CPU 使用率（0.0 - 1.0）
     */
    private double getCpuUsage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                        (com.sun.management.OperatingSystemMXBean) osBean;
                return sunOsBean.getProcessCpuLoad();
            }
            return osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();
        } catch (Exception e) {
            LOGGER.warn("Failed to get CPU usage", e);
            return 0.5; // 默认返回中等值
        }
    }
}
