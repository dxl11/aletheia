package com.alibaba.aletheia.common.util;

import com.alibaba.aletheia.common.model.GcEvent;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JsonUtil 测试类
 *
 * @author Aletheia Team
 */
public class JsonUtilTest {

    @Test
    public void testToJson() {
        GcEvent event = new GcEvent();
        event.setGcType("Young GC");
        event.setGcName("G1 Young Generation");
        event.setPauseTimeMs(50);

        String json = JsonUtil.toJson(event);
        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain gcType", json.contains("gcType"));
        assertTrue("JSON should contain Young GC", json.contains("Young GC"));
    }

    @Test
    public void testFromJson() {
        String json = "{\"gcType\":\"Young GC\",\"gcName\":\"G1 Young Generation\",\"pauseTimeMs\":50}";
        GcEvent event = JsonUtil.fromJson(json, GcEvent.class);

        assertNotNull("Event should not be null", event);
        assertEquals("GC type should match", "Young GC", event.getGcType());
        assertEquals("GC name should match", "G1 Young Generation", event.getGcName());
        assertEquals("Pause time should match", 50, event.getPauseTimeMs());
    }

    @Test
    public void testRoundTrip() {
        GcEvent original = new GcEvent();
        original.setGcType("Full GC");
        original.setGcName("G1 Old Generation");
        original.setPauseTimeMs(1000);

        String json = JsonUtil.toJson(original);
        GcEvent converted = JsonUtil.fromJson(json, GcEvent.class);

        assertNotNull("Converted event should not be null", converted);
        assertEquals("GC type should match", original.getGcType(), converted.getGcType());
        assertEquals("GC name should match", original.getGcName(), converted.getGcName());
        assertEquals("Pause time should match", original.getPauseTimeMs(), converted.getPauseTimeMs());
    }

    @Test
    public void testNullHandling() {
        String json = JsonUtil.toJson(null);
        assertNull("JSON of null should be null", json);

        GcEvent event = JsonUtil.fromJson(null, GcEvent.class);
        assertNull("Event from null JSON should be null", event);

        event = JsonUtil.fromJson("", GcEvent.class);
        assertNull("Event from empty JSON should be null", event);
    }
}
