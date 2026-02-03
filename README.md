# Aletheia - JVM Monitoring and Diagnostics Tool

Aletheia（希腊语：真理、真相）是一个面向生产环境的低侵入、低开销 JVM 监控与诊断工具。

## 项目概述

Aletheia 旨在解决高并发 Java 后端在生产环境中遇到的真实问题：
- GC STW 时间过长
- 锁竞争导致 RT 抖动
- CPU 飙高但无法定位热点
- 内存泄漏（堆内/堆外）
- RT 抖动根因分析

## 核心特性

### MVP 版本功能

1. **GC 监控**
   - GC 次数、耗时统计（Young GC / Full GC）
   - STW 时间分布统计（P50/P99/P999）
   - GC 前后堆内存变化

2. **CPU 火焰图**
   - 集成 Async-profiler，生成 CPU 火焰图
   - 支持按时间范围查询

3. **线程状态监控**
   - 线程数统计（RUNNABLE / BLOCKED / WAITING）
   - 锁竞争检测（BLOCKED 线程的锁对象）
   - 死锁检测

4. **内存监控**
   - 堆内存使用（Eden / Survivor / Old）
   - 元空间使用
   - 直接内存使用（如果可获取）

5. **RT 抖动检测**
   - 关键方法 RT 统计（P50/P99/P999）
   - RT 异常告警（P99 突增 > 3 倍）

## 项目结构

```
aletheia/
├── aletheia-common/      # 公共模块（数据模型、工具类）
├── aletheia-agent/       # Java Agent 模块（字节码增强、数据采集）
├── aletheia-collector/   # 数据采集服务模块
├── aletheia-analyzer/    # 分析引擎模块
└── aletheia-web/         # Web UI 模块
```

## 技术栈

- **Java Agent**: ASM 字节码增强
- **数据采集**: JMX + ThreadMXBean
- **CPU 分析**: Async-profiler（集成）
- **数据传输**: HTTP 推送 / 本地文件
- **存储**: 本地文件（JSON）或轻量级时序数据库
- **展示**: Spring Boot Web UI

## 代码规范

本项目严格遵循**阿里巴巴 Java 开发手册**（Alibaba Java Coding Guidelines）。

## 快速开始

### 构建项目

```bash
mvn clean install
```

### 使用 Agent

```bash
# Attach to running JVM
java -jar aletheia-agent.jar <pid>

# Or start with agent
java -javaagent:aletheia-agent.jar YourApplication
```

## 许可证

Copyright © Alibaba Group
