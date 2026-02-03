# Aletheia 使用指南

## 快速开始

### 1. 构建项目

```bash
cd D:\aletheia
mvn clean package
```

构建完成后，JAR 文件将位于各模块的 `target` 目录下。

### 2. 使用 Agent

#### 方式一：启动时加载 Agent

```bash
java -javaagent:aletheia-agent/target/aletheia-agent-1.0.0-SNAPSHOT.jar \
     -DdataDir=D:\aletheia\data \
     YourApplication
```

#### 方式二：动态 Attach（需要实现 attach 工具）

```bash
# 需要先实现 attach 工具类
java -jar aletheia-agent.jar <pid> --dataDir=D:\aletheia\data
```

#### Agent 参数说明

- `dataDir`: 数据目录，Agent 会将采集的数据写入该目录（可选，默认使用系统临时目录）

### 3. 启动 Collector 服务

```bash
java -jar aletheia-collector/target/aletheia-collector-1.0.0-SNAPSHOT.jar \
     --dataDir=D:\aletheia\data
```

Collector 会定时读取 `dataDir` 目录下的 JSON 文件，处理 Agent 发送的数据。

### 4. 启动 Web UI

```bash
java -jar aletheia-web/target/aletheia-web-1.0.0-SNAPSHOT.jar
```

访问：http://localhost:8080/aletheia/api/dashboard/data

## 配置说明

### Agent 配置

Agent 通过 JVM 参数或 Agent 参数进行配置：

```bash
# 指定数据目录
-javaagent:aletheia-agent.jar=dataDir=D:\aletheia\data

# 或使用系统属性
-Daletheia.dataDir=D:\aletheia\data
```

### Collector 配置

Collector 通过命令行参数配置：

```bash
--dataDir=/path/to/data  # 数据目录（必须与 Agent 的 dataDir 一致）
```

### Web UI 配置

Web UI 通过 `application.yml` 配置：

```yaml
server:
  port: 8080
  servlet:
    context-path: /aletheia
```

## 数据流说明

1. **Agent 采集数据**
   - GC 事件：通过 JMX GCNotification 监听
   - 线程状态：定时采集（1秒一次）
   - 内存数据：定时采集（1秒一次）
   - RT 数据：通过字节码增强采集，时间窗口聚合（1秒窗口）

2. **Agent 发送数据**
   - 将采集的数据序列化为 JSON
   - 写入本地文件：`{dataDir}/{timestamp}-{pid}.json`
   - 默认推送频率：1秒一次

3. **Collector 处理数据**
   - 定时读取数据文件（1秒一次）
   - 解析 JSON 数据
   - 进行异常检测和分析
   - 删除已处理的文件

4. **Web UI 展示**
   - 通过 REST API 获取数据
   - 展示 GC 趋势、线程状态、内存使用等

## 监控指标说明

### GC 监控

- **GC 类型**：Young GC / Full GC
- **STW 时间**：GC 暂停时间（毫秒）
- **GC 原因**：如 Allocation Failure、System.gc 等
- **回收量**：GC 回收的内存大小（字节）

### 线程监控

- **线程数统计**：RUNNABLE / BLOCKED / WAITING / TIMED_WAITING
- **死锁检测**：自动检测死锁线程
- **锁竞争信息**：BLOCKED 线程的锁对象

### 内存监控

- **堆内存**：Eden / Survivor / Old 区使用量
- **元空间**：Metaspace 使用量
- **直接内存**：如果可获取

### RT 监控

- **P50/P99/P999**：响应时间分位数统计
- **采样率**：默认 1%，可动态调整
- **时间窗口**：默认 1秒窗口

## 注意事项

1. **数据目录权限**：确保 Agent 和 Collector 都有读写权限
2. **采样率**：RT 采样率默认 1%，过高可能影响性能
3. **GC 事件**：需要 JVM 支持 GCNotification（Java 7+）
4. **字节码增强**：会排除系统类和 Aletheia 自身类
5. **文件清理**：Collector 处理完数据文件后会删除，避免磁盘空间占用

## 故障排查

### Agent 未采集数据

1. 检查 Agent 是否成功加载：查看日志
2. 检查数据目录是否存在且有写权限
3. 检查是否有数据文件生成

### Collector 未处理数据

1. 检查 Collector 是否启动
2. 检查数据目录路径是否正确
3. 检查数据文件格式是否正确

### RT 数据为空

1. 检查采样率是否过低
2. 检查是否有方法被增强（排除系统类）
3. 检查时间窗口设置

## 性能影响

- **Agent 开销**：RT 采样率 1% 时，性能影响 < 1%
- **GC 监控**：通过 JMX，开销极小
- **线程/内存监控**：定时采集，开销可忽略
- **字节码增强**：仅在类加载时增强，运行时开销低

## 后续功能

- [ ] HTTP 推送方式（替代本地文件）
- [ ] 时序数据库存储
- [ ] CPU 火焰图集成（Async-profiler）
- [ ] 完整的 Web UI 实现
- [ ] 告警功能
- [ ] 分布式追踪集成
