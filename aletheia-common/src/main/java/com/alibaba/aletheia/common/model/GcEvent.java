package com.alibaba.aletheia.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * GC 事件数据模型
 *
 * @author Aletheia Team
 */
public class GcEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * GC 类型：Young GC / Full GC
     */
    @JsonProperty("gcType")
    private String gcType;

    /**
     * GC 名称（如 G1 Young Generation）
     */
    @JsonProperty("gcName")
    private String gcName;

    /**
     * GC 开始时间戳（纳秒）
     */
    @JsonProperty("startTimeNs")
    private long startTimeNs;

    /**
     * GC 结束时间戳（纳秒）
     */
    @JsonProperty("endTimeNs")
    private long endTimeNs;

    /**
     * STW 暂停时间（毫秒）
     */
    @JsonProperty("pauseTimeMs")
    private long pauseTimeMs;

    /**
     * GC 原因（如 Allocation Failure）
     */
    @JsonProperty("gcCause")
    private String gcCause;

    /**
     * GC 前堆内存使用量（字节）
     */
    @JsonProperty("heapUsedBeforeBytes")
    private long heapUsedBeforeBytes;

    /**
     * GC 后堆内存使用量（字节）
     */
    @JsonProperty("heapUsedAfterBytes")
    private long heapUsedAfterBytes;

    /**
     * GC 回收量（字节）
     */
    @JsonProperty("reclaimedBytes")
    private long reclaimedBytes;

    /**
     * Eden 区使用量（字节）
     */
    @JsonProperty("edenUsedBytes")
    private long edenUsedBytes;

    /**
     * Survivor 区使用量（字节）
     */
    @JsonProperty("survivorUsedBytes")
    private long survivorUsedBytes;

    /**
     * Old 区使用量（字节）
     */
    @JsonProperty("oldUsedBytes")
    private long oldUsedBytes;

    public String getGcType() {
        return gcType;
    }

    public void setGcType(String gcType) {
        this.gcType = gcType;
    }

    public String getGcName() {
        return gcName;
    }

    public void setGcName(String gcName) {
        this.gcName = gcName;
    }

    public long getStartTimeNs() {
        return startTimeNs;
    }

    public void setStartTimeNs(long startTimeNs) {
        this.startTimeNs = startTimeNs;
    }

    public long getEndTimeNs() {
        return endTimeNs;
    }

    public void setEndTimeNs(long endTimeNs) {
        this.endTimeNs = endTimeNs;
    }

    public long getPauseTimeMs() {
        return pauseTimeMs;
    }

    public void setPauseTimeMs(long pauseTimeMs) {
        this.pauseTimeMs = pauseTimeMs;
    }

    public String getGcCause() {
        return gcCause;
    }

    public void setGcCause(String gcCause) {
        this.gcCause = gcCause;
    }

    public long getHeapUsedBeforeBytes() {
        return heapUsedBeforeBytes;
    }

    public void setHeapUsedBeforeBytes(long heapUsedBeforeBytes) {
        this.heapUsedBeforeBytes = heapUsedBeforeBytes;
    }

    public long getHeapUsedAfterBytes() {
        return heapUsedAfterBytes;
    }

    public void setHeapUsedAfterBytes(long heapUsedAfterBytes) {
        this.heapUsedAfterBytes = heapUsedAfterBytes;
    }

    public long getReclaimedBytes() {
        return reclaimedBytes;
    }

    public void setReclaimedBytes(long reclaimedBytes) {
        this.reclaimedBytes = reclaimedBytes;
    }

    public long getEdenUsedBytes() {
        return edenUsedBytes;
    }

    public void setEdenUsedBytes(long edenUsedBytes) {
        this.edenUsedBytes = edenUsedBytes;
    }

    public long getSurvivorUsedBytes() {
        return survivorUsedBytes;
    }

    public void setSurvivorUsedBytes(long survivorUsedBytes) {
        this.survivorUsedBytes = survivorUsedBytes;
    }

    public long getOldUsedBytes() {
        return oldUsedBytes;
    }

    public void setOldUsedBytes(long oldUsedBytes) {
        this.oldUsedBytes = oldUsedBytes;
    }

    @Override
    public String toString() {
        return "GcEvent{"
                + "gcType='" + gcType + '\''
                + ", gcName='" + gcName + '\''
                + ", pauseTimeMs=" + pauseTimeMs
                + ", gcCause='" + gcCause + '\''
                + ", reclaimedBytes=" + reclaimedBytes
                + '}';
    }
}
