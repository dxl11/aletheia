package com.alibaba.aletheia.web.controller;

import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.web.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GC 监控控制器
 *
 * @author Aletheia Team
 */
@RestController
@RequestMapping("/api/gc")
public class GcController {

    @Autowired
    private DataService dataService;

    /**
     * 获取 GC 趋势数据
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量（默认 100）
     * @return GC 趋势数据
     */
    @GetMapping("/trend")
    public Map<String, Object> getGcTrend(
            @RequestParam(required = false) Long pid,
            @RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        List<GcEvent> events = dataService.getGcEvents(pid, limit);
        result.put("data", events);
        result.put("count", events.size());

        return result;
    }

    /**
     * 获取 GC 统计信息
     *
     * @param pid 进程 ID（可选）
     * @return GC 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getGcStats(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        com.alibaba.aletheia.collector.storage.DataStorage.DataStats stats = dataService.getStats(pid);
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("gcEventCount", stats.getGcEventCount());
        statsData.put("totalEventCount", stats.getGcEventCount() + stats.getRtEventCount() 
                + stats.getThreadEventCount() + stats.getMemoryEventCount());

        result.put("data", statsData);
        return result;
    }
}
