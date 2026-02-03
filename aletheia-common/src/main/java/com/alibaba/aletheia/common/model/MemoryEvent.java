package com.alibaba.aletheia.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * 内存事件数据模型
 *
 * @author Aletheia Team
 */
public class MemoryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间戳（纳秒）
     */
    @JsonProperty("timestampNs")
    private long timestampNs;

    /**
     * 堆内存使用量（字节）
     */
    @JsonProperty("heapUsedBytes")
    private long heapUsedBytes;

    /**
     * 堆内存最大值（字节）
     */
    @JsonProperty("heapMaxBytes")
    private long heapMaxBytes;

    /**
     * Eden 区使用量（字节）
     */
    @JsonProperty("edenUsedBytes")
    private long edenUsedBytes;

    /**
     * Eden 区最大值（字节）
     */
    @JsonProperty("edenMaxBytes")
    private long edenMaxBytes;

    /**
     * Survivor 区使用量（字节）
     */
    @JsonProperty("survivorUsedBytes")
    private long survivorUsedBytes;

    /**
     * Survivor 区最大值（字节）
     */
    @JsonProperty("survivorMaxBytes")
    private long survivorMaxBytes;

    /**
     * Old 区使用量（字节）
     */
    @JsonProperty("oldUsedBytes")
    private long oldUsedBytes;

    /**
     * Old 区最大值（字节）
     */
    @JsonProperty("oldMaxBytes")
    private long oldMaxBytes;

    /**
     * 元空间使用量（字节）
     */
    @JsonProperty("metaspaceUsedBytes")
    private long metaspaceUsedBytes;

    /**
     * 元空间最大值（字节）
     */
    @JsonProperty("metaspaceMaxBytes")
    private long metaspaceMaxBytes;

    /**
     * 直接内存使用量（字节），如果可获取
     */
    @JsonProperty("directMemoryUsedBytes")
    private Long directMemoryUsedBytes;

    public long getTimestampNs() {
        return timestampNs;
    }

    public void setTimestampNs(long timestampNs) {
        this.timestampNs = timestampNs;
    }

    public long getHeapUsedBytes() {
        return heapUsedBytes;
    }

    public void setHeapUsedBytes(long heapUsedBytes) {
        this.heapUsedBytes = heapUsedBytes;
    }

    public long getHeapMaxBytes() {
        return heapMaxBytes;
    }

    public void setHeapMaxBytes(long heapMaxBytes) {
        this.heapMaxBytes = heapMaxBytes;
    }

    public long getEdenUsedBytes() {
        return edenUsedBytes;
    }

    public void setEdenUsedBytes(long edenUsedBytes) {
        this.edenUsedBytes = edenUsedBytes;
    }

    public long getEdenMaxBytes() {
        return edenMaxBytes;
    }

    public void setEdenMaxBytes(long edenMaxBytes) {
        this.edenMaxBytes = edenMaxBytes;
    }

    public long getSurvivorUsedBytes() {
        return survivorUsedBytes;
    }

    public void setSurvivorUsedBytes(long survivorUsedBytes) {
        this.survivorUsedBytes = survivorUsedBytes;
    }

    public long getSurvivorMaxBytes() {
        return survivorMaxBytes;
    }

    public void setSurvivorMaxBytes(long survivorMaxBytes) {
        this.survivorMaxBytes = survivorMaxBytes;
    }

    public long getOldUsedBytes() {
        return oldUsedBytes;
    }

    public void setOldUsedBytes(long oldUsedBytes) {
        this.oldUsedBytes = oldUsedBytes;
    }

    public long getOldMaxBytes() {
        return oldMaxBytes;
    }

    public void setOldMaxBytes(long oldMaxBytes) {
        this.oldMaxBytes = oldMaxBytes;
    }

    public long getMetaspaceUsedBytes() {
        return metaspaceUsedBytes;
    }

    public void setMetaspaceUsedBytes(long metaspaceUsedBytes) {
        this.metaspaceUsedBytes = metaspaceUsedBytes;
    }

    public long getMetaspaceMaxBytes() {
        return metaspaceMaxBytes;
    }

    public void setMetaspaceMaxBytes(long metaspaceMaxBytes) {
        this.metaspaceMaxBytes = metaspaceMaxBytes;
    }

    public Long getDirectMemoryUsedBytes() {
        return directMemoryUsedBytes;
    }

    public void setDirectMemoryUsedBytes(Long directMemoryUsedBytes) {
        this.directMemoryUsedBytes = directMemoryUsedBytes;
    }

    @Override
    public String toString() {
        return "MemoryEvent{"
                + "heapUsedBytes=" + heapUsedBytes
                + ", heapMaxBytes=" + heapMaxBytes
                + ", edenUsedBytes=" + edenUsedBytes
                + ", oldUsedBytes=" + oldUsedBytes
                + ", metaspaceUsedBytes=" + metaspaceUsedBytes
                + '}';
    }
}
