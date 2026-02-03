package com.alibaba.aletheia.web.service;

import com.alibaba.aletheia.collector.CollectorService;
import com.alibaba.aletheia.collector.storage.DataStorage;
import com.alibaba.aletheia.common.model.GcEvent;
import com.alibaba.aletheia.common.model.MemoryEvent;
import com.alibaba.aletheia.common.model.RtEvent;
import com.alibaba.aletheia.common.model.ThreadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据服务
 * 提供数据查询功能
 *
 * @author Aletheia Team
 */
@Service
public class DataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);

    @Autowired(required = false)
    private CollectorService collectorService;

    /**
     * 获取 GC 事件列表
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量
     * @return GC 事件列表
     */
    public List<GcEvent> getGcEvents(Long pid, int limit) {
        if (collectorService == null) {
            LOGGER.warn("CollectorService not available");
            return List.of();
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return List.of();
        }

        return storage.getGcEvents(pid, limit);
    }

    /**
     * 获取线程事件列表
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量
     * @return 线程事件列表
     */
    public List<ThreadEvent> getThreadEvents(Long pid, int limit) {
        if (collectorService == null) {
            return List.of();
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return List.of();
        }

        return storage.getThreadEvents(pid, limit);
    }

    /**
     * 获取最新的线程事件
     *
     * @param pid 进程 ID
     * @return 最新的线程事件
     */
    public ThreadEvent getLatestThreadEvent(Long pid) {
        if (collectorService == null) {
            return null;
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return null;
        }

        return storage.getLatestThreadEvent(pid);
    }

    /**
     * 获取内存事件列表
     *
     * @param pid 进程 ID（可选）
     * @param limit 限制数量
     * @return 内存事件列表
     */
    public List<MemoryEvent> getMemoryEvents(Long pid, int limit) {
        if (collectorService == null) {
            return List.of();
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return List.of();
        }

        return storage.getMemoryEvents(pid, limit);
    }

    /**
     * 获取最新的内存事件
     *
     * @param pid 进程 ID
     * @return 最新的内存事件
     */
    public MemoryEvent getLatestMemoryEvent(Long pid) {
        if (collectorService == null) {
            return null;
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return null;
        }

        return storage.getLatestMemoryEvent(pid);
    }

    /**
     * 获取 RT 事件列表
     *
     * @param pid 进程 ID（可选）
     * @param methodSignature 方法签名（可选）
     * @param limit 限制数量
     * @return RT 事件列表
     */
    public List<RtEvent> getRtEvents(Long pid, String methodSignature, int limit) {
        if (collectorService == null) {
            return List.of();
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return List.of();
        }

        return storage.getRtEvents(pid, methodSignature, limit);
    }

    /**
     * 获取数据统计信息
     *
     * @param pid 进程 ID（可选）
     * @return 数据统计信息
     */
    public DataStorage.DataStats getStats(Long pid) {
        if (collectorService == null) {
            return new DataStorage.DataStats();
        }

        DataStorage storage = collectorService.getDataStorage();
        if (storage == null) {
            return new DataStorage.DataStats();
        }

        return storage.getStats(pid);
    }
}
