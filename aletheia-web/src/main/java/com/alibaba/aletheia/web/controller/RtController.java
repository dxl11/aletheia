package com.alibaba.aletheia.web.controller;

import com.alibaba.aletheia.common.model.RtEvent;
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
 * RT 监控控制器
 *
 * @author Aletheia Team
 */
@RestController
@RequestMapping("/api/rt")
public class RtController {

    @Autowired
    private DataService dataService;

    /**
     * 获取 RT 统计信息
     *
     * @param pid 进程 ID（可选）
     * @param methodSignature 方法签名（可选）
     * @param limit 限制数量（默认 100）
     * @return RT 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getRtStats(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) String methodSignature,
            @RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        List<RtEvent> events = dataService.getRtEvents(pid, methodSignature, limit);
        result.put("data", events);
        result.put("count", events.size());

        return result;
    }

    /**
     * 获取 RT 趋势
     *
     * @param pid 进程 ID（可选）
     * @param methodSignature 方法签名（可选）
     * @param limit 限制数量（默认 100）
     * @return RT 趋势数据
     */
    @GetMapping("/trend")
    public Map<String, Object> getRtTrend(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) String methodSignature,
            @RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        List<RtEvent> events = dataService.getRtEvents(pid, methodSignature, limit);
        result.put("data", events);
        result.put("count", events.size());

        return result;
    }

    /**
     * 获取 RT 异常方法列表
     *
     * @param pid 进程 ID（可选）
     * @return RT 异常方法列表
     */
    @GetMapping("/anomalies")
    public Map<String, Object> getRtAnomalies(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        // TODO: 从 Analyzer 获取 RT 异常方法列表（需要告警管理器支持）
        result.put("data", List.of());
        result.put("count", 0);
        return result;
    }
}
