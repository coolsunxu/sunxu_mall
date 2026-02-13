---
name: large-class-refactoring
description: Refactor large Spring Configuration classes by domain splitting, extract long methods by responsibility, and eliminate redundant synchronized blocks. Use when a class exceeds 500 lines, a method exceeds 80 lines, or duplicated code patterns are detected across multiple classes.
---

# Large Class Refactoring

## Quick Start

1. 识别目标：类 > 500 行 或 方法 > 80 行。
2. 选择合适的拆分策略（见下方三种模式）。
3. 拆分后验证编译通过，检查 Spring 注入和 Bean 注册是否正确。

## Pattern 1: @Configuration 按领域拆分

适用于包含大量 `@Bean` 方法的配置类（如 `MetricConfiguration` 1056 行）。

**步骤**：
1. 按业务领域分组 `@Bean` 方法（如连接、报文、Kafka、Netty 内存）。
2. 为每个领域创建独立的 `@Configuration` 类。
3. 原类保留为文档索引类（`final class` + 私有构造器 + Javadoc 指向新类）。

**命名规范**：`{Domain}MetricConfiguration.java`

```java
// 原类变为文档索引
public final class MetricConfiguration {
    private MetricConfiguration() {}
    // @see ConnectionMetricConfiguration
    // @see MqttPacketMetricConfiguration
    // @see KafkaMetricConfiguration
    // @see NettyMetricConfiguration
}
```

**拆分依据参考表**：

| 领域 | Bean 特征 | 拆分到 |
|---|---|---|
| 连接/断连 | `METRIC_RUNNING_CONNECTION`, `METRIC_PACKET_CONNECT/DISCONNECT` | `ConnectionMetricConfiguration` |
| MQTT 报文 | `METRIC_PACKET_PUBLISH`, Timer, DistributionSummary | `MqttPacketMetricConfiguration` |
| Kafka | `kafka.*` 前缀 | `KafkaMetricConfiguration` |
| Netty 内存 | `netty.*` 前缀 | `NettyMetricConfiguration` |

## Pattern 2: 长方法按职责拆分

适用于包含 `switch/case` 或 `if-else` 链的大方法。

**步骤**：
1. 识别方法中的独立逻辑分支（如 QoS 0/1/2）。
2. 每个分支提取为私有方法，命名 `{action}{Qualifier}`（如 `publishQos0`、`publishQos1`）。
3. 原方法只保留公共逻辑 + 分发调用。

```java
// Before: 120 行方法
public void publishToLocalOnlineSubscriber(StoredMessage msg, Channel ch) {
    // ... 公共逻辑 ...
    if (msg.getQos() == 0) { /* 40 行 QoS0 逻辑 */ }
    else if (msg.getQos() == 1) { /* 40 行 QoS1 逻辑 */ }
    else if (msg.getQos() == 2) { /* 40 行 QoS2 逻辑 */ }
}

// After: 每个方法 < 40 行
public void publishToLocalOnlineSubscriber(StoredMessage msg, Channel ch) {
    // ... 公共逻辑 ...
    switch (msg.getQos()) {
        case 0: publishQos0(msg, ch, clientId); break;
        case 1: publishQos1(msg, ch, clientId); break;
        case 2: publishQos2(msg, ch, clientId); break;
        default: log.warn("Unknown QoS level: {}", msg.getQos());
    }
}
```

**注意**：提取后检查异常声明是否需要透传（如 `throws NoPacketIdAvailableException`）。

## Pattern 3: 消除冗余 synchronized

当外层已持有 `writeLock` 时，内部的 `synchronized` 块是多余的。

```java
// Before: 双重加锁
LockTemplate.writeRun(readWriteLock, () -> {
    synchronized (children) {   // 冗余！writeLock 已保护
        children.remove(key);
    }
});

// After: 移除冗余 synchronized
LockTemplate.writeRun(readWriteLock, () -> {
    children.remove(key);
});
```

**判断标准**：
- `writeLock` 已保护了整个数据结构 -> `synchronized` 冗余
- 独立的局部变量 -> 不需要 `synchronized`
- 跨锁域的共享状态 -> 保留 `synchronized`

## 重构检查清单

- [ ] 类行数 < 500 行
- [ ] 所有方法行数 < 80 行
- [ ] `@Configuration` 拆分后所有 `@Bean` 仍可正常注入
- [ ] 提取的私有方法 `throws` 声明是否正确
- [ ] 没有冗余的 `synchronized` 块
- [ ] 运行 `mvnd compile` 验证编译通过

## Additional Resources
- 拆分案例：`MetricConfiguration` -> 4 个领域类
- 方法提取案例：`SessionMessageManagerImpl.publishToLocalOnlineSubscriber`
- synchronized 消除案例：`SubscriptionTree.java`
