package com.alibaba.aletheia.web.controller;

import com.alibaba.aletheia.common.model.MemoryEvent;
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
 * 内存监控控制器
 *
 * @author Aletheia Team
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    @Autowired
    private DataService dataService;

    /**
     * 获取内存使用情况
     *
     * @param pid 进程 ID（可选）
     * @return 内存使用数据
     */
    @GetMapping("/usage")
    public Map<String, Object> getMemoryUsage(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        MemoryEvent latestEvent = dataService.getLatestMemoryEvent(pid);
        if (latestEvent != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("heapUsedBytes", latestEvent.getHeapUsedBytes());
            data.put("heapMaxBytes", latestEvent.getHeapMaxBytes());
            data.put("edenUsedBytes", latestEvent.getEdenUsedBytes());
            data.put("oldUsedBytes", latestEvent.getOldUsedBytes());
            data.put("metaspaceUsedBytes", latestEvent.getMetaspaceUsedBytes());
            result.put("data", data);
        } else {
            result.put("data", new HashMap<>());
        }

        return result;
    }

    /**
     * 获取内存趋势
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量（默认 100）
     * @return 内存趋势数据
     */
    @GetMapping("/trend")
    public Map<String, Object> getMemoryTrend(
            @RequestParam(required = false) Long pid,
            @RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        List<MemoryEvent> events = dataService.getMemoryEvents(pid, limit);
        result.put("data", events);
        result.put("count", events.size());

        return result;
    }
}
