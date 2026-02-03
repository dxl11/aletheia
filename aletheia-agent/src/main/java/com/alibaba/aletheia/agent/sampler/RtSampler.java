package com.alibaba.aletheia.agent.sampler;

import com.alibaba.aletheia.common.constant.AletheiaConstants;
import com.alibaba.aletheia.common.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RT 采样器
 * 使用 ThreadLocalRandom 进行低开销采样
 *
 * @author Aletheia Team
 */
public class RtSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtSampler.class);

    /**
     * RT 聚合器实例
     */
    private static final RtAggregator RT_AGGREGATOR = new RtAggregator();

    /**
     * 当前采样率
     */
    private static volatile double sampleRate = AletheiaConstants.DEFAULT_SAMPLE_RATE;

    /**
     * 方法开始时间存储（ThreadLocal）
     */
    private static final ThreadLocal<Long> METHOD_START_TIME = new ThreadLocal<>();

    /**
     * 总采样次数
     */
    private static final AtomicLong TOTAL_SAMPLES = new AtomicLong(0);

    /**
     * 方法开始时的回调
     *
     * @param methodSignature 方法签名
     */
    public static void onMethodStart(String methodSignature) {
        // 采样率判断：使用 ThreadLocalRandom 避免锁竞争
        if (ThreadLocalRandom.current().nextDouble() > sampleRate) {
            return;
        }

        try {
            METHOD_START_TIME.set(TimeUtil.currentTimeNs());
            TOTAL_SAMPLES.incrementAndGet();
        } catch (Exception e) {
            // 避免采样逻辑影响业务代码
            LOGGER.debug("Error in onMethodStart", e);
        }
    }

    /**
     * 方法结束时的回调
     *
     * @param methodSignature 方法签名
     */
    public static void onMethodEnd(String methodSignature) {
        Long startTime = METHOD_START_TIME.get();
        if (startTime == null) {
            return;
        }

        try {
            long endTime = TimeUtil.currentTimeNs();
            long rtNs = endTime - startTime;
            long rtMs = TimeUtil.nanosToMillis(rtNs);

            // 记录 RT 数据到聚合器
            RT_AGGREGATOR.recordRt(methodSignature, rtMs);

            METHOD_START_TIME.remove();
        } catch (Exception e) {
            // 避免采样逻辑影响业务代码
            LOGGER.debug("Error in onMethodEnd", e);
            METHOD_START_TIME.remove();
        }
    }

    /**
     * 获取并清空 RT 事件列表
     *
     * @return RT 事件列表
     */
    public static java.util.List<com.alibaba.aletheia.common.model.RtEvent> getAndClearRtEvents() {
        return RT_AGGREGATOR.getAndClearRtEvents();
    }

    /**
     * 设置采样率
     *
     * @param rate 采样率（0.0 - 1.0）
     */
    public static void setSampleRate(double rate) {
        if (rate < AletheiaConstants.MIN_SAMPLE_RATE) {
            rate = AletheiaConstants.MIN_SAMPLE_RATE;
        }
        if (rate > AletheiaConstants.MAX_SAMPLE_RATE) {
            rate = AletheiaConstants.MAX_SAMPLE_RATE;
        }
        sampleRate = rate;
        LOGGER.info("Sample rate updated to: {}", sampleRate);
    }

    /**
     * 获取当前采样率
     *
     * @return 采样率
     */
    public static double getSampleRate() {
        return sampleRate;
    }

    /**
     * 获取总采样次数
     *
     * @return 总采样次数
     */
    public static long getTotalSamples() {
        return TOTAL_SAMPLES.get();
    }
}
