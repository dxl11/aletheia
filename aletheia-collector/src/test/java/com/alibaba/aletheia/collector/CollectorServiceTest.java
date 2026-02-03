package com.alibaba.aletheia.collector;

import com.alibaba.aletheia.common.model.AgentData;
import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.MemoryEvent;
import com.alibaba.aletheia.common.model.ThreadEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * CollectorService 集成测试
 *
 * @author Aletheia Team
 */
public class CollectorServiceTest {

    private CollectorService collectorService;
    private String testDataDir;

    @Before
    public void setUp() throws Exception {
        // 创建临时测试目录
        testDataDir = System.getProperty("java.io.tmpdir") + "/aletheia-test-" + System.currentTimeMillis();
        Files.createDirectories(Paths.get(testDataDir));

        collectorService = new CollectorService(testDataDir);
    }

    @After
    public void tearDown() throws Exception {
        if (collectorService != null && collectorService.isStarted()) {
            collectorService.stop();
        }

        // 清理测试目录
        if (testDataDir != null) {
            deleteDirectory(new File(testDataDir));
        }
    }

    @Test
    public void testStartAndStop() {
        assertFalse("Service should not be started initially", collectorService.isStarted());
        
        collectorService.start();
        assertTrue("Service should be started", collectorService.isStarted());

        collectorService.stop();
        assertFalse("Service should be stopped", collectorService.isStarted());
    }

    @Test
    public void testProcessAgentData() throws Exception {
        collectorService.start();

        // 创建测试数据文件
        AgentData agentData = createTestAgentData();
        String json = com.alibaba.aletheia.common.util.JsonUtil.toJson(agentData);
        
        String fileName = System.currentTimeMillis() + "-12345.json";
        Path filePath = Paths.get(testDataDir, fileName);
        Files.write(filePath, json.getBytes("UTF-8"));

        // 等待处理
        Thread.sleep(2000);

        // 验证数据已存储
        com.alibaba.aletheia.collector.storage.DataStorage storage = collectorService.getDataStorage();
        assertNotNull("DataStorage should not be null", storage);

        List<GcEvent> gcEvents = storage.getGcEvents(12345L, 10);
        assertTrue("Should have GC events", gcEvents.size() > 0);
    }

    /**
     * 创建测试用的 AgentData
     */
    private AgentData createTestAgentData() {
        AgentData agentData = new AgentData();
        agentData.setPid(12345L);
        agentData.setJvmName("TestJVM");
        agentData.setTimestampNs(System.nanoTime());

        // GC 事件
        GcEvent gcEvent = new GcEvent();
        gcEvent.setGcType("Young GC");
        gcEvent.setGcName("G1 Young Generation");
        gcEvent.setPauseTimeMs(50);
        List<GcEvent> gcEvents = new ArrayList<>();
        gcEvents.add(gcEvent);
        agentData.setGcEvents(gcEvents);

        // 线程事件
        ThreadEvent threadEvent = new ThreadEvent();
        threadEvent.setTotalThreadCount(100);
        threadEvent.setRunnableCount(80);
        threadEvent.setBlockedCount(10);
        agentData.setThreadEvent(threadEvent);

        // 内存事件
        MemoryEvent memoryEvent = new MemoryEvent();
        memoryEvent.setHeapUsedBytes(1024 * 1024 * 512); // 512MB
        memoryEvent.setHeapMaxBytes(1024 * 1024 * 1024); // 1GB
        agentData.setMemoryEvent(memoryEvent);

        return agentData;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
