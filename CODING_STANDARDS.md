# Aletheia 代码规范文档

本项目严格遵循**阿里巴巴 Java 开发手册**（Alibaba Java Coding Guidelines）。

## 1. 命名规范

### 1.1 包名
- 全部小写，单数形式
- 示例：`com.alibaba.aletheia.agent.collector`

### 1.2 类名
- UpperCamelCase（大驼峰）
- 示例：`GcEventCollector`、`ThreadStateMonitor`
- 抽象类：以 `Abstract` 或 `Base` 开头，如 `AbstractCollector`
- 工具类：以 `Util` 或 `Utils` 结尾，如 `TimeUtil`

### 1.3 方法名
- lowerCamelCase（小驼峰）
- 示例：`collectGcEvent()`、`getThreadInfo()`

### 1.4 常量
- 全大写，下划线分隔
- 示例：`MAX_SAMPLE_RATE`、`DEFAULT_WINDOW_SIZE`

### 1.5 变量
- lowerCamelCase（小驼峰）
- 布尔类型：以 `is`、`has`、`can` 开头
- 示例：`isStarted`、`hasData`、`canCollect`

## 2. 代码风格

### 2.1 缩进
- 使用 4 个空格，禁止使用 Tab

### 2.2 行长度
- 单行不超过 120 个字符

### 2.3 大括号
- 左大括号前不换行
- 右大括号前换行
```java
if (condition) {
    // code
}
```

### 2.4 方法参数
- 不超过 5 个参数
- 超过时封装为对象

### 2.5 方法长度
- 不超过 80 行

### 2.6 类长度
- 不超过 500 行

## 3. 编程规约

### 3.1 禁止使用魔法值
```java
// 错误
if (count > 100) { ... }

// 正确
private static final int MAX_COUNT = 100;
if (count > MAX_COUNT) { ... }
```

### 3.2 禁止在循环中进行数据库操作、远程调用
```java
// 错误
for (String id : ids) {
    userService.getUser(id); // 远程调用
}

// 正确
List<String> users = userService.batchGetUsers(ids);
```

### 3.3 使用 ThreadLocalRandom 而非 Random
```java
// 正确
ThreadLocalRandom.current().nextDouble();
```

### 3.4 集合初始化时指定容量
```java
// 正确
List<String> list = new ArrayList<>(16);
Map<String, Object> map = new HashMap<>(16);
```

### 3.5 字符串拼接
- 循环中使用 `StringBuilder`
- 简单拼接可使用 `+`

### 3.6 日志规范
- 使用 SLF4J，禁止直接使用 Log4j/Logback API
- 日志级别：
  - ERROR：系统错误，需要立即处理
  - WARN：警告信息，可能的问题
  - INFO：重要业务流程信息
  - DEBUG：调试信息，生产环境关闭

### 3.7 异常处理
- 禁止捕获异常后不做任何处理（至少记录日志）
- 禁止使用 `catch (Exception e) {}`
- 使用具体的异常类型

## 4. 性能规约

### 4.1 避免在循环中使用 `+` 进行字符串拼接
```java
// 错误
String result = "";
for (String s : list) {
    result += s;
}

// 正确
StringBuilder sb = new StringBuilder();
for (String s : list) {
    sb.append(s);
}
String result = sb.toString();
```

### 4.2 使用 `entrySet()` 遍历 Map
```java
// 错误
for (String key : map.keySet()) {
    Object value = map.get(key);
}

// 正确
for (Map.Entry<String, Object> entry : map.entrySet()) {
    String key = entry.getKey();
    Object value = entry.getValue();
}
```

### 4.3 使用 `ArrayList` 而非 `LinkedList`
- 除非需要频繁插入删除操作

### 4.4 合理使用线程池
- 禁止 `new Thread()`
- 使用 `Executors` 或自定义 `ThreadPoolExecutor`

### 4.5 使用无锁数据结构
- `ConcurrentHashMap`、`Disruptor` 等

## 5. 注释规约

### 5.1 JavaDoc
- 类、方法必须添加 JavaDoc 注释
- 公共 API 必须完整注释

### 5.2 业务逻辑注释
- 复杂业务逻辑必须添加注释说明

### 5.3 注释掉的代码
- 必须说明原因，并标注删除时间
- 建议直接删除，使用版本控制管理

## 6. 工具检查

### 6.1 Alibaba Java Coding Guidelines Plugin
- 安装 IDEA 插件：Alibaba Java Coding Guidelines
- 运行代码检查：右键 -> Coding Guidelines -> Run

### 6.2 Maven 插件
```xml
<plugin>
    <groupId>com.alibaba.p3c</groupId>
    <artifactId>p3c-pmd</artifactId>
    <version>2.1.1</version>
</plugin>
```

## 7. 代码审查清单

- [ ] 命名符合规范
- [ ] 无魔法值
- [ ] 异常处理完整
- [ ] 日志级别正确
- [ ] 性能优化（集合容量、字符串拼接等）
- [ ] JavaDoc 完整
- [ ] 通过 Alibaba 规范检查
