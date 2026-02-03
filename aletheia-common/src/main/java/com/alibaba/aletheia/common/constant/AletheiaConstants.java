package com.alibaba.aletheia.common.constant;

/**
 * Aletheia 常量定义
 *
 * @author Aletheia Team
 */
public final class AletheiaConstants {

    /**
     * 默认采样率（1%）
     */
    public static final double DEFAULT_SAMPLE_RATE = 0.01;

    /**
     * 最大采样率（10%）
     */
    public static final double MAX_SAMPLE_RATE = 0.1;

    /**
     * 最小采样率（0.1%）
     */
    public static final double MIN_SAMPLE_RATE = 0.001;

    /**
     * 默认时间窗口大小（毫秒）
     */
    public static final long DEFAULT_WINDOW_SIZE_MS = 1000;

    /**
     * 默认数据推送间隔（毫秒）
     */
    public static final long DEFAULT_PUSH_INTERVAL_MS = 1000;

    /**
     * RingBuffer 默认大小
     */
    public static final int DEFAULT_RING_BUFFER_SIZE = 1024 * 8;

    /**
     * RT 异常告警阈值倍数
     */
    public static final double RT_ALERT_THRESHOLD_MULTIPLIER = 3.0;

    /**
     * GC STW 告警阈值（毫秒）
     */
    public static final long GC_STW_ALERT_THRESHOLD_MS = 1000;

    /**
     * CPU 使用率阈值（用于自适应采样率）
     */
    public static final double CPU_HIGH_THRESHOLD = 0.8;
    public static final double CPU_LOW_THRESHOLD = 0.5;

    /**
     * 私有构造函数，防止实例化
     */
    private AletheiaConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
