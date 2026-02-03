package com.alibaba.aletheia.agent.sampler;

import com.alibaba.aletheia.common.constant.AletheiaConstants;
import com.alibaba.aletheia.common.model.RtEvent;
import com.alibaba.aletheia.common.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RT 数据聚合器
 * 负责时间窗口内的 RT 数据聚合和分位数统计
 *
 * @author Aletheia Team
 */
public class RtAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtAggregator.class);

    /**
     * 方法签名 -> RT 数据列表的映射
     */
    private final ConcurrentHashMap<String, List<Long>> methodRtMap = new ConcurrentHashMap<>();

    /**
     * 当前时间窗口开始时间（纳秒）
     */
    private volatile long currentWindowStartNs = TimeUtil.currentTimeNs();

    /**
     * 时间窗口大小（纳秒）
     */
    private final long windowSizeNs = TimeUtil.millisToNanos(AletheiaConstants.DEFAULT_WINDOW_SIZE_MS);

    /**
     * 记录 RT 数据
     *
     * @param methodSignature 方法签名
     * @param rtMs RT（毫秒）
     */
    public void recordRt(String methodSignature, long rtMs) {
        if (methodSignature == null || methodSignature.isEmpty()) {
            return;
        }

        try {
            // 检查是否需要切换到新的时间窗口
            long currentTime = TimeUtil.currentTimeNs();
            if (currentTime - currentWindowStartNs >= windowSizeNs) {
                flushWindow();
                currentWindowStartNs = currentTime;
            }

            // 记录 RT 数据
            methodRtMap.computeIfAbsent(methodSignature, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(rtMs);
        } catch (Exception e) {
            LOGGER.debug("Error recording RT data", e);
        }
    }

    /**
     * 获取并清空当前窗口的 RT 事件列表
     *
     * @return RT 事件列表
     */
    public List<RtEvent> getAndClearRtEvents() {
        List<RtEvent> events = new ArrayList<>();
        long windowEndNs = TimeUtil.currentTimeNs();

        for (String methodSignature : methodRtMap.keySet()) {
            List<Long> rtList = methodRtMap.remove(methodSignature);
            if (rtList == null || rtList.isEmpty()) {
                continue;
            }

            RtEvent event = calculateRtEvent(methodSignature, currentWindowStartNs, windowEndNs, rtList);
            if (event != null) {
                events.add(event);
            }
        }

        return events;
    }

    /**
     * 刷新时间窗口（计算统计信息并清空数据）
     */
    public void flushWindow() {
        getAndClearRtEvents();
        currentWindowStartNs = TimeUtil.currentTimeNs();
    }

    /**
     * 计算 RT 事件统计信息
     *
     * @param methodSignature 方法签名
     * @param windowStartNs 窗口开始时间
     * @param windowEndNs 窗口结束时间
     * @param rtList RT 数据列表
     * @return RT 事件
     */
    private RtEvent calculateRtEvent(String methodSignature, long windowStartNs, 
                                      long windowEndNs, List<Long> rtList) {
        if (rtList == null || rtList.isEmpty()) {
            return null;
        }

        // 排序以便计算分位数
        List<Long> sortedList = new ArrayList<>(rtList);
        Collections.sort(sortedList);

        int count = sortedList.size();
        RtEvent event = new RtEvent();
        event.setMethodSignature(methodSignature);
        event.setWindowStartNs(windowStartNs);
        event.setWindowEndNs(windowEndNs);
        event.setSampleCount(count);

        // 计算最小值
        event.setMinMs(sortedList.get(0));

        // 计算最大值
        event.setMaxMs(sortedList.get(count - 1));

        // 计算平均值
        long sum = 0;
        for (Long rt : sortedList) {
            sum += rt;
        }
        event.setAvgMs((double) sum / count);

        // 计算 P50
        event.setP50Ms(calculatePercentile(sortedList, 50));

        // 计算 P99
        event.setP99Ms(calculatePercentile(sortedList, 99));

        // 计算 P999
        event.setP999Ms(calculatePercentile(sortedList, 99.9));

        return event;
    }

    /**
     * 计算分位数
     *
     * @param sortedList 已排序的列表
     * @param percentile 百分位数（0-100）
     * @return 分位数值
     */
    private double calculatePercentile(List<Long> sortedList, double percentile) {
        if (sortedList == null || sortedList.isEmpty()) {
            return 0.0;
        }

        int count = sortedList.size();
        double index = (percentile / 100.0) * (count - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedList.get(lowerIndex);
        }

        double weight = index - lowerIndex;
        return sortedList.get(lowerIndex) * (1 - weight) + sortedList.get(upperIndex) * weight;
    }

    /**
     * 获取当前窗口开始时间
     *
     * @return 窗口开始时间（纳秒）
     */
    public long getCurrentWindowStartNs() {
        return currentWindowStartNs;
    }
}
