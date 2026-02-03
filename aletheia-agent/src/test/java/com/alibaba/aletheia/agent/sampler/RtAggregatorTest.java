package com.alibaba.aletheia.agent.sampler;

import com.alibaba.aletheia.common.model.RtEvent;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * RtAggregator 测试类
 *
 * @author Aletheia Team
 */
public class RtAggregatorTest {

    @Test
    public void testRecordRt() {
        RtAggregator aggregator = new RtAggregator();
        String methodSignature = "com.example.Test.method";

        aggregator.recordRt(methodSignature, 10);
        aggregator.recordRt(methodSignature, 20);
        aggregator.recordRt(methodSignature, 30);

        List<RtEvent> events = aggregator.getAndClearRtEvents();
        assertEquals("Should have one event", 1, events.size());

        RtEvent event = events.get(0);
        assertEquals("Method signature should match", methodSignature, event.getMethodSignature());
        assertEquals("Sample count should be 3", 3, event.getSampleCount());
    }

    @Test
    public void testPercentileCalculation() {
        RtAggregator aggregator = new RtAggregator();
        String methodSignature = "com.example.Test.method";

        // 记录 100 个数据点，值从 1 到 100
        for (int i = 1; i <= 100; i++) {
            aggregator.recordRt(methodSignature, i);
        }

        List<RtEvent> events = aggregator.getAndClearRtEvents();
        assertEquals("Should have one event", 1, events.size());

        RtEvent event = events.get(0);
        assertEquals("P50 should be around 50", 50.0, event.getP50Ms(), 1.0);
        assertEquals("P99 should be around 99", 99.0, event.getP99Ms(), 1.0);
        assertEquals("Min should be 1", 1.0, event.getMinMs(), 0.1);
        assertEquals("Max should be 100", 100.0, event.getMaxMs(), 0.1);
    }

    @Test
    public void testEmptyData() {
        RtAggregator aggregator = new RtAggregator();
        List<RtEvent> events = aggregator.getAndClearRtEvents();
        assertTrue("Should return empty list", events.isEmpty());
    }

    @Test
    public void testMultipleMethods() {
        RtAggregator aggregator = new RtAggregator();

        aggregator.recordRt("com.example.Method1", 10);
        aggregator.recordRt("com.example.Method2", 20);
        aggregator.recordRt("com.example.Method1", 15);

        List<RtEvent> events = aggregator.getAndClearRtEvents();
        assertEquals("Should have two events", 2, events.size());
    }
}
