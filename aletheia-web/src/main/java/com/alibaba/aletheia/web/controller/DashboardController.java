package com.alibaba.aletheia.web.controller;

import com.alibaba.aletheia.web.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘控制器
 *
 * @author Aletheia Team
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DataService dataService;

    /**
     * 获取仪表盘数据
     *
     * @param pid 进程 ID（可选）
     * @return 仪表盘数据
     */
    @GetMapping("/data")
    public Map<String, Object> getDashboardData(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        
        // GC 数据
        Map<String, Object> gcData = new HashMap<>();
        gcData.put("latest", dataService.getGcEvents(pid, 1));
        data.put("gc", gcData);

        // 线程数据
        Map<String, Object> threadData = new HashMap<>();
        threadData.put("latest", dataService.getLatestThreadEvent(pid));
        data.put("thread", threadData);

        // 内存数据
        Map<String, Object> memoryData = new HashMap<>();
        memoryData.put("latest", dataService.getLatestMemoryEvent(pid));
        data.put("memory", memoryData);

        // RT 数据
        Map<String, Object> rtData = new HashMap<>();
        rtData.put("latest", dataService.getRtEvents(pid, null, 10));
        data.put("rt", rtData);

        // 统计信息
        com.alibaba.aletheia.collector.storage.DataStorage.DataStats stats = dataService.getStats(pid);
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("gcEventCount", stats.getGcEventCount());
        statsData.put("rtEventCount", stats.getRtEventCount());
        statsData.put("threadEventCount", stats.getThreadEventCount());
        statsData.put("memoryEventCount", stats.getMemoryEventCount());
        data.put("stats", statsData);

        result.put("data", data);
        return result;
    }

    /**
     * 获取系统概览
     *
     * @param pid 进程 ID（可选）
     * @return 系统概览数据
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        com.alibaba.aletheia.collector.storage.DataStorage.DataStats stats = dataService.getStats(pid);
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalEvents", stats.getGcEventCount() + stats.getRtEventCount() 
                + stats.getThreadEventCount() + stats.getMemoryEventCount());
        overview.put("gcEvents", stats.getGcEventCount());
        overview.put("rtEvents", stats.getRtEventCount());
        overview.put("threadEvents", stats.getThreadEventCount());
        overview.put("memoryEvents", stats.getMemoryEventCount());

        result.put("data", overview);
        return result;
    }
}
