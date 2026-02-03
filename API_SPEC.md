# Aletheia API 规范文档

## 1. Agent 与 Collector 通信协议

### 1.1 数据格式

使用 JSON 格式进行数据传输，数据模型定义在 `aletheia-common` 模块中。

#### AgentData 结构
```json
{
  "pid": 12345,
  "jvmName": "MyApplication",
  "timestampNs": 1234567890123456789,
  "gcEvents": [...],
  "threadEvent": {...},
  "memoryEvent": {...},
  "rtEvents": [...]
}
```

### 1.2 传输方式

#### 方式一：本地文件（推荐）
- Agent 将数据写入本地文件（JSON 格式）
- Collector 定时读取文件并处理
- 文件路径：`{dataDir}/{timestamp}-{pid}.json`
- 优点：无网络开销，可靠性高
- 缺点：需要共享文件系统

#### 方式二：HTTP 推送
- Agent 通过 HTTP POST 推送数据到 Collector
- 端点：`POST /api/collector/data`
- 优点：支持远程采集
- 缺点：网络开销，需要 Collector 服务运行

### 1.3 数据推送频率

- 默认：1 秒一次
- 可配置：通过 Agent 参数 `pushIntervalMs` 设置

## 2. Collector API

### 2.1 接收 Agent 数据

**端点**：`POST /api/collector/data`

**请求体**：
```json
{
  "pid": 12345,
  "jvmName": "MyApplication",
  "timestampNs": 1234567890123456789,
  "gcEvents": [...],
  "threadEvent": {...},
  "memoryEvent": {...},
  "rtEvents": [...]
}
```

**响应**：
```json
{
  "success": true,
  "message": "Data received"
}
```

## 3. Web API

### 3.1 获取仪表盘数据

**端点**：`GET /api/dashboard/data`

**响应**：
```json
{
  "status": "ok",
  "data": {
    "gcStats": {...},
    "threadStats": {...},
    "memoryStats": {...},
    "rtStats": {...}
  }
}
```

### 3.2 获取 GC 趋势

**端点**：`GET /api/gc/trend?startTime={startTime}&endTime={endTime}`

**响应**：
```json
{
  "data": [
    {
      "timestamp": 1234567890,
      "gcType": "Young GC",
      "pauseTimeMs": 50,
      "reclaimedBytes": 1024000
    }
  ]
}
```

### 3.3 获取线程状态

**端点**：`GET /api/thread/status`

**响应**：
```json
{
  "totalThreadCount": 100,
  "runnableCount": 80,
  "blockedCount": 10,
  "waitingCount": 10,
  "deadlockedThreads": []
}
```

### 3.4 获取 CPU 火焰图

**端点**：`GET /api/cpu/flamegraph?pid={pid}&duration={duration}`

**响应**：
- 返回 SVG 格式的火焰图数据

## 4. 数据模型

### 4.1 GcEvent
- `gcType`: String - GC 类型（Young GC / Full GC）
- `gcName`: String - GC 名称
- `startTimeNs`: long - 开始时间（纳秒）
- `endTimeNs`: long - 结束时间（纳秒）
- `pauseTimeMs`: long - STW 时间（毫秒）
- `gcCause`: String - GC 原因
- `heapUsedBeforeBytes`: long - GC 前堆内存
- `heapUsedAfterBytes`: long - GC 后堆内存
- `reclaimedBytes`: long - 回收量

### 4.2 ThreadEvent
- `timestampNs`: long - 时间戳
- `totalThreadCount`: int - 总线程数
- `runnableCount`: int - RUNNABLE 线程数
- `blockedCount`: int - BLOCKED 线程数
- `waitingCount`: int - WAITING 线程数
- `deadlockedThreads`: List<ThreadInfo> - 死锁线程
- `lockContentionInfo`: List<LockContentionInfo> - 锁竞争信息

### 4.3 MemoryEvent
- `timestampNs`: long - 时间戳
- `heapUsedBytes`: long - 堆内存使用量
- `heapMaxBytes`: long - 堆内存最大值
- `edenUsedBytes`: long - Eden 区使用量
- `oldUsedBytes`: long - Old 区使用量
- `metaspaceUsedBytes`: long - 元空间使用量

### 4.4 RtEvent
- `methodSignature`: String - 方法签名
- `windowStartNs`: long - 窗口开始时间
- `windowEndNs`: long - 窗口结束时间
- `sampleCount`: int - 采样次数
- `p50Ms`: double - P50 RT
- `p99Ms`: double - P99 RT
- `p999Ms`: double - P999 RT

## 5. 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 500 | 服务器内部错误 |

## 6. 版本信息

- API 版本：v1.0
- 数据格式版本：1.0
