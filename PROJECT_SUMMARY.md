# Aletheia 项目总结

## 项目概述

Aletheia 是一个面向生产环境的低侵入、低开销 JVM 监控与诊断工具，旨在解决高并发 Java 后端在生产环境中遇到的真实问题。

## 已完成的工作

### 1. 项目结构
- ✅ 创建了 Maven 多模块项目结构
- ✅ 5 个核心模块：common、agent、collector、analyzer、web
- ✅ 符合阿里巴巴 Java 开发手册规范

### 2. 数据模型（aletheia-common）
- ✅ `GcEvent` - GC 事件数据模型
- ✅ `ThreadEvent` - 线程事件数据模型
- ✅ `MemoryEvent` - 内存事件数据模型
- ✅ `RtEvent` - RT（响应时间）事件数据模型
- ✅ `AgentData` - Agent 数据包模型
- ✅ `AletheiaConstants` - 常量定义
- ✅ `TimeUtil` - 时间工具类
- ✅ `JsonUtil` - JSON 工具类

### 3. Agent 模块（aletheia-agent）
- ✅ `AletheiaAgent` - Agent 入口类（支持 premain 和 agentmain）
- ✅ `MethodTransformer` - 字节码转换器（使用 ASM）
- ✅ `RtSampler` - RT 采样器（低开销采样）
- ✅ `DataCollector` - 数据采集器主类
- ✅ `GcEventCollector` - GC 事件采集器（JMX）
- ✅ `ThreadCollector` - 线程状态采集器
- ✅ `MemoryCollector` - 内存数据采集器

### 4. Collector 模块（aletheia-collector）
- ✅ `CollectorService` - Collector 服务主类
- ✅ 支持本地文件读取和 HTTP 接收

### 5. Analyzer 模块（aletheia-analyzer）
- ✅ `AnomalyDetector` - 异常检测器
- ✅ GC STW 异常检测
- ✅ RT 异常检测

### 6. Web 模块（aletheia-web）
- ✅ Spring Boot 应用入口
- ✅ `DashboardController` - 仪表盘控制器
- ✅ 配置文件（application.yml）

### 7. 文档
- ✅ `README.md` - 项目说明文档
- ✅ `CODING_STANDARDS.md` - 代码规范文档
- ✅ `API_SPEC.md` - API 规范文档
- ✅ `.gitignore` - Git 忽略文件配置

## 技术栈

- **Java**: 1.8+
- **Maven**: 多模块项目
- **字节码增强**: ASM 9.5
- **高性能队列**: Disruptor 3.4.4
- **JSON 序列化**: Jackson 2.15.2
- **Web 框架**: Spring Boot 2.7.14
- **日志**: SLF4J + Logback

## MVP 功能实现状态

### 核心功能
1. ✅ **GC 监控** - 通过 JMX GCNotification 实现
2. ⚠️ **CPU 火焰图** - 需要集成 Async-profiler（待实现）
3. ✅ **线程状态监控** - 通过 ThreadMXBean 实现
4. ✅ **内存监控** - 通过 MemoryMXBean 实现
5. ✅ **RT 抖动检测** - 通过字节码增强实现（基础框架）

### 待完善功能
- RT 数据聚合和统计（P50/P99/P999）
- Agent 数据推送机制（本地文件/HTTP）
- 时序数据存储
- Web UI 完整实现
- Async-profiler 集成

## 项目结构

```
D:\aletheia\
├── aletheia-common\          # 公共模块
│   └── src\main\java\com\alibaba\aletheia\common\
│       ├── model\            # 数据模型
│       ├── util\             # 工具类
│       └── constant\         # 常量定义
├── aletheia-agent\          # Agent 模块
│   └── src\main\java\com\alibaba\aletheia\agent\
│       ├── transformer\      # 字节码增强
│       ├── collector\         # 数据采集
│       └── sampler\          # 采样器
├── aletheia-collector\       # Collector 模块
├── aletheia-analyzer\        # Analyzer 模块
├── aletheia-web\             # Web 模块
├── pom.xml                   # 父 POM
├── README.md
├── CODING_STANDARDS.md
├── API_SPEC.md
└── .gitignore
```

## 下一步工作

1. **完善 RT 数据采集**
   - 实现 RT 数据聚合（时间窗口）
   - 实现分位数统计（P50/P99/P999）

2. **实现数据推送**
   - 完善本地文件写入机制
   - 实现 HTTP 推送功能

3. **集成 Async-profiler**
   - 封装 Async-profiler 调用
   - 实现 CPU 火焰图生成

4. **实现数据存储**
   - 集成时序数据库（可选）
   - 实现数据查询接口

5. **完善 Web UI**
   - 实现完整的仪表盘
   - 实现 GC 趋势图
   - 实现线程状态图

6. **测试和优化**
   - 单元测试
   - 性能测试
   - 代码规范检查

## 使用说明

### 构建项目
```bash
cd D:\aletheia
mvn clean install
```

### 使用 Agent
```bash
# 方式一：启动时加载
java -javaagent:aletheia-agent.jar YourApplication

# 方式二：动态 attach（需要实现 attach 工具）
java -jar aletheia-agent.jar <pid>
```

### 启动 Collector
```bash
java -jar aletheia-collector.jar --dataDir=D:\aletheia\data
```

### 启动 Web UI
```bash
java -jar aletheia-web.jar
```

## 注意事项

1. Agent 模块需要独立打包，包含所有依赖
2. 字节码增强可能影响性能，需要合理控制采样率
3. GC 事件采集需要 JVM 支持 GCNotification（Java 7+）
4. 直接内存监控可能需要特殊权限或 JVM 参数

## 代码规范

本项目严格遵循**阿里巴巴 Java 开发手册**，请参考 `CODING_STANDARDS.md`。

## 许可证

Copyright © Alibaba Group
