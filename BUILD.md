# Aletheia 构建指南

## 前置要求

- JDK 1.8 或更高版本
- Maven 3.6 或更高版本

## 构建步骤

### 1. 完整构建

```bash
cd D:\aletheia
mvn clean install
```

这将构建所有模块并运行测试。

### 2. 跳过测试构建

```bash
mvn clean install -DskipTests
```

### 3. 单独构建模块

```bash
# 构建 common 模块
cd aletheia-common
mvn clean package

# 构建 agent 模块
cd ../aletheia-agent
mvn clean package

# 构建 collector 模块
cd ../aletheia-collector
mvn clean package

# 构建 web 模块
cd ../aletheia-web
mvn clean package
```

## 打包说明

### Agent JAR

Agent 模块需要打包成可执行的 JAR，包含所有依赖：

```bash
cd aletheia-agent
mvn clean package
```

生成的 JAR 文件：`target/aletheia-agent-1.0.0-SNAPSHOT.jar`

### Collector JAR

Collector 模块使用 shade 插件打包：

```bash
cd aletheia-collector
mvn clean package
```

生成的 JAR 文件：`target/aletheia-collector-1.0.0-SNAPSHOT.jar`

### Web JAR

Web 模块使用 Spring Boot Maven 插件打包：

```bash
cd aletheia-web
mvn clean package
```

生成的 JAR 文件：`target/aletheia-web-1.0.0-SNAPSHOT.jar`

## 输出目录

构建完成后，所有 JAR 文件位于各模块的 `target` 目录下。

建议创建 `dist` 目录统一管理：

```bash
mkdir dist
cp aletheia-agent/target/*.jar dist/
cp aletheia-collector/target/*.jar dist/
cp aletheia-web/target/*.jar dist/
```

## 代码规范检查

使用 Alibaba Java Coding Guidelines 插件检查代码：

```bash
# 安装插件（如果使用 Maven）
mvn com.alibaba.p3c:p3c-pmd:check
```

或在 IDEA 中：
1. 安装 Alibaba Java Coding Guidelines 插件
2. 右键项目 -> Coding Guidelines -> Run

## 测试

运行所有测试：

```bash
mvn test
```

运行特定模块的测试：

```bash
cd aletheia-common
mvn test
```

## 清理

清理所有构建产物：

```bash
mvn clean
```

## 常见问题

### 1. 编译错误：找不到符号

确保先构建 `aletheia-common` 模块：

```bash
cd aletheia-common
mvn install
```

### 2. 测试失败

检查测试环境配置，或跳过测试：

```bash
mvn install -DskipTests
```

### 3. 依赖下载失败

检查 Maven 配置和网络连接，或使用国内镜像：

```xml
<mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```
