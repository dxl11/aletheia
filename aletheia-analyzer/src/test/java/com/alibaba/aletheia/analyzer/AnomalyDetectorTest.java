package com.alibaba.aletheia.analyzer;

import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.RtEvent;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * AnomalyDetector 测试类
 *
 * @author Aletheia Team
 */
public class AnomalyDetectorTest {

    @Test
    public void testDetectGcAnomaly() {
        AnomalyDetector detector = new AnomalyDetector();

        // 正常 GC 事件
        GcEvent normalGc = new GcEvent();
        normalGc.setPauseTimeMs(50);
        assertFalse("Normal GC should not be anomaly", detector.detectGcAnomaly(normalGc));

        // 异常 GC 事件（STW > 1秒）
        GcEvent anomalyGc = new GcEvent();
        anomalyGc.setPauseTimeMs(2000);
        assertTrue("Anomaly GC should be detected", detector.detectGcAnomaly(anomalyGc));
    }

    @Test
    public void testDetectRtAnomaly() {
        AnomalyDetector detector = new AnomalyDetector();

        // 正常 RT 事件
        RtEvent normalRt = new RtEvent();
        normalRt.setP99Ms(50.0);
        double baseline = 50.0;
        assertFalse("Normal RT should not be anomaly", 
                detector.detectRtAnomaly(normalRt, baseline));

        // 异常 RT 事件（P99 > 3倍基线）
        RtEvent anomalyRt = new RtEvent();
        anomalyRt.setP99Ms(200.0); // 4倍基线
        assertTrue("Anomaly RT should be detected", 
                detector.detectRtAnomaly(anomalyRt, baseline));
    }

    @Test
    public void testCalculateBaselineP99() {
        AnomalyDetector detector = new AnomalyDetector();

        List<RtEvent> events = new ArrayList<>();
        RtEvent event1 = new RtEvent();
        event1.setP99Ms(50.0);
        events.add(event1);

        RtEvent event2 = new RtEvent();
        event2.setP99Ms(100.0);
        events.add(event2);

        double baseline = detector.calculateBaselineP99(events);
        assertEquals("Baseline should be average", 75.0, baseline, 0.1);
    }

    @Test
    public void testCalculateBaselineEmptyList() {
        AnomalyDetector detector = new AnomalyDetector();
        double baseline = detector.calculateBaselineP99(new ArrayList<>());
        assertEquals("Baseline of empty list should be 0", 0.0, baseline, 0.0);
    }
}
