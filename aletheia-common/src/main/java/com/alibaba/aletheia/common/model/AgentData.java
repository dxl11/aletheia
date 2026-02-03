package com.alibaba.aletheia.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 采集的数据包（包含多种事件类型）
 *
 * @author Aletheia Team
 */
public class AgentData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JVM 进程 ID
     */
    @JsonProperty("pid")
    private long pid;

    /**
     * JVM 名称
     */
    @JsonProperty("jvmName")
    private String jvmName;

    /**
     * 时间戳（纳秒）
     */
    @JsonProperty("timestampNs")
    private long timestampNs;

    /**
     * GC 事件列表
     */
    @JsonProperty("gcEvents")
    private List<GcEvent> gcEvents;

    /**
     * 线程事件
     */
    @JsonProperty("threadEvent")
    private ThreadEvent threadEvent;

    /**
     * 内存事件
     */
    @JsonProperty("memoryEvent")
    private MemoryEvent memoryEvent;

    /**
     * RT 事件列表
     */
    @JsonProperty("rtEvents")
    private List<RtEvent> rtEvents;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getJvmName() {
        return jvmName;
    }

    public void setJvmName(String jvmName) {
        this.jvmName = jvmName;
    }

    public long getTimestampNs() {
        return timestampNs;
    }

    public void setTimestampNs(long timestampNs) {
        this.timestampNs = timestampNs;
    }

    public List<GcEvent> getGcEvents() {
        return gcEvents;
    }

    public void setGcEvents(List<GcEvent> gcEvents) {
        this.gcEvents = gcEvents;
    }

    public ThreadEvent getThreadEvent() {
        return threadEvent;
    }

    public void setThreadEvent(ThreadEvent threadEvent) {
        this.threadEvent = threadEvent;
    }

    public MemoryEvent getMemoryEvent() {
        return memoryEvent;
    }

    public void setMemoryEvent(MemoryEvent memoryEvent) {
        this.memoryEvent = memoryEvent;
    }

    public List<RtEvent> getRtEvents() {
        return rtEvents;
    }

    public void setRtEvents(List<RtEvent> rtEvents) {
        this.rtEvents = rtEvents;
    }

    @Override
    public String toString() {
        return "AgentData{"
                + "pid=" + pid
                + ", jvmName='" + jvmName + '\''
                + ", timestampNs=" + timestampNs
                + ", gcEventsCount=" + (gcEvents != null ? gcEvents.size() : 0)
                + ", rtEventsCount=" + (rtEvents != null ? rtEvents.size() : 0)
                + '}';
    }
}
