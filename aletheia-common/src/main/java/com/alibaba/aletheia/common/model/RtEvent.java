package com.alibaba.aletheia.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * RT（响应时间）事件数据模型
 *
 * @author Aletheia Team
 */
public class RtEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 方法签名（类名+方法名）
     */
    @JsonProperty("methodSignature")
    private String methodSignature;

    /**
     * 时间窗口开始时间戳（纳秒）
     */
    @JsonProperty("windowStartNs")
    private long windowStartNs;

    /**
     * 时间窗口结束时间戳（纳秒）
     */
    @JsonProperty("windowEndNs")
    private long windowEndNs;

    /**
     * 采样次数
     */
    @JsonProperty("sampleCount")
    private int sampleCount;

    /**
     * P50 RT（毫秒）
     */
    @JsonProperty("p50Ms")
    private double p50Ms;

    /**
     * P99 RT（毫秒）
     */
    @JsonProperty("p99Ms")
    private double p99Ms;

    /**
     * P999 RT（毫秒）
     */
    @JsonProperty("p999Ms")
    private double p999Ms;

    /**
     * 最小 RT（毫秒）
     */
    @JsonProperty("minMs")
    private double minMs;

    /**
     * 最大 RT（毫秒）
     */
    @JsonProperty("maxMs")
    private double maxMs;

    /**
     * 平均 RT（毫秒）
     */
    @JsonProperty("avgMs")
    private double avgMs;

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public long getWindowStartNs() {
        return windowStartNs;
    }

    public void setWindowStartNs(long windowStartNs) {
        this.windowStartNs = windowStartNs;
    }

    public long getWindowEndNs() {
        return windowEndNs;
    }

    public void setWindowEndNs(long windowEndNs) {
        this.windowEndNs = windowEndNs;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public double getP50Ms() {
        return p50Ms;
    }

    public void setP50Ms(double p50Ms) {
        this.p50Ms = p50Ms;
    }

    public double getP99Ms() {
        return p99Ms;
    }

    public void setP99Ms(double p99Ms) {
        this.p99Ms = p99Ms;
    }

    public double getP999Ms() {
        return p999Ms;
    }

    public void setP999Ms(double p999Ms) {
        this.p999Ms = p999Ms;
    }

    public double getMinMs() {
        return minMs;
    }

    public void setMinMs(double minMs) {
        this.minMs = minMs;
    }

    public double getMaxMs() {
        return maxMs;
    }

    public void setMaxMs(double maxMs) {
        this.maxMs = maxMs;
    }

    public double getAvgMs() {
        return avgMs;
    }

    public void setAvgMs(double avgMs) {
        this.avgMs = avgMs;
    }

    @Override
    public String toString() {
        return "RtEvent{"
                + "methodSignature='" + methodSignature + '\''
                + ", sampleCount=" + sampleCount
                + ", p50Ms=" + p50Ms
                + ", p99Ms=" + p99Ms
                + ", p999Ms=" + p999Ms
                + '}';
    }
}
