package com.alibaba.aletheia.web.controller;

import com.alibaba.aletheia.common.model.ThreadEvent;
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
 * 线程监控控制器
 *
 * @author Aletheia Team
 */
@RestController
@RequestMapping("/api/thread")
public class ThreadController {

    @Autowired
    private DataService dataService;

    /**
     * 获取线程状态
     *
     * @param pid 进程 ID（可选）
     * @return 线程状态数据
     */
    @GetMapping("/status")
    public Map<String, Object> getThreadStatus(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        ThreadEvent latestEvent = dataService.getLatestThreadEvent(pid);
        if (latestEvent != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("totalThreadCount", latestEvent.getTotalThreadCount());
            data.put("runnableCount", latestEvent.getRunnableCount());
            data.put("blockedCount", latestEvent.getBlockedCount());
            data.put("waitingCount", latestEvent.getWaitingCount());
            data.put("timedWaitingCount", latestEvent.getTimedWaitingCount());
            result.put("data", data);
        } else {
            result.put("data", new HashMap<>());
        }

        return result;
    }

    /**
     * 获取死锁信息
     *
     * @param pid 进程 ID（可选）
     * @return 死锁信息
     */
    @GetMapping("/deadlock")
    public Map<String, Object> getDeadlockInfo(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        ThreadEvent latestEvent = dataService.getLatestThreadEvent(pid);
        if (latestEvent != null && latestEvent.getDeadlockedThreads() != null) {
            result.put("data", latestEvent.getDeadlockedThreads());
            result.put("count", latestEvent.getDeadlockedThreads().size());
        } else {
            result.put("data", List.of());
            result.put("count", 0);
        }

        return result;
    }

    /**
     * 获取锁竞争信息
     *
     * @param pid 进程 ID（可选）
     * @return 锁竞争信息
     */
    @GetMapping("/lock-contention")
    public Map<String, Object> getLockContention(@RequestParam(required = false) Long pid) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        ThreadEvent latestEvent = dataService.getLatestThreadEvent(pid);
        if (latestEvent != null && latestEvent.getLockContentionInfo() != null) {
            result.put("data", latestEvent.getLockContentionInfo());
            result.put("count", latestEvent.getLockContentionInfo().size());
        } else {
            result.put("data", List.of());
            result.put("count", 0);
        }

        return result;
    }
}
