package com.alibaba.aletheia.common.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * TimeUtil 测试类
 *
 * @author Aletheia Team
 */
public class TimeUtilTest {

    @Test
    public void testCurrentTimeNs() {
        long time1 = TimeUtil.currentTimeNs();
        assertTrue("Time should be positive", time1 > 0);

        long time2 = TimeUtil.currentTimeNs();
        assertTrue("Time should increase", time2 >= time1);
    }

    @Test
    public void testCurrentTimeMs() {
        long time1 = TimeUtil.currentTimeMs();
        assertTrue("Time should be positive", time1 > 0);

        long time2 = TimeUtil.currentTimeMs();
        assertTrue("Time should increase", time2 >= time1);
    }

    @Test
    public void testNanosToMillis() {
        long nanos = 1_000_000L;
        long millis = TimeUtil.nanosToMillis(nanos);
        assertEquals("1 million nanos should equal 1 millis", 1, millis);

        nanos = 5_000_000L;
        millis = TimeUtil.nanosToMillis(nanos);
        assertEquals("5 million nanos should equal 5 millis", 5, millis);
    }

    @Test
    public void testMillisToNanos() {
        long millis = 1;
        long nanos = TimeUtil.millisToNanos(millis);
        assertEquals("1 millis should equal 1 million nanos", 1_000_000L, nanos);

        millis = 5;
        nanos = TimeUtil.millisToNanos(millis);
        assertEquals("5 millis should equal 5 million nanos", 5_000_000L, nanos);
    }

    @Test
    public void testConversionRoundTrip() {
        long originalMillis = 100;
        long nanos = TimeUtil.millisToNanos(originalMillis);
        long convertedMillis = TimeUtil.nanosToMillis(nanos);
        assertEquals("Round trip conversion should be equal", originalMillis, convertedMillis);
    }
}
