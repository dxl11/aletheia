package com.alibaba.aletheia.common.util;

/**
 * 时间工具类
 *
 * @author Aletheia Team
 */
public final class TimeUtil {

    /**
     * 纳秒转毫秒
     */
    private static final long NANOS_PER_MILLIS = 1_000_000L;

    /**
     * 毫秒转纳秒
     */
    private static final long MILLIS_PER_NANOS = 1_000_000L;

    /**
     * 获取当前时间戳（纳秒）
     *
     * @return 纳秒时间戳
     */
    public static long currentTimeNs() {
        return System.nanoTime();
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 毫秒时间戳
     */
    public static long currentTimeMs() {
        return System.currentTimeMillis();
    }

    /**
     * 纳秒转毫秒
     *
     * @param nanos 纳秒
     * @return 毫秒
     */
    public static long nanosToMillis(long nanos) {
        return nanos / NANOS_PER_MILLIS;
    }

    /**
     * 毫秒转纳秒
     *
     * @param millis 毫秒
     * @return 纳秒
     */
    public static long millisToNanos(long millis) {
        return millis * MILLIS_PER_NANOS;
    }

    /**
     * 私有构造函数，防止实例化
     */
    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
