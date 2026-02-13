# Storage, Metrics, Config, Build Reference

## Scope
- `src/main/java/com/example/nettyserver/storage/redisImpl/`
- `src/main/java/com/example/nettyserver/config/beans/*MetricConfiguration.java`
- `src/main/java/com/example/nettyserver/config/props/ServerConfig.java`
- `src/main/java/com/example/nettyserver/constant/RedisKeyPrefix.java`

---

## Redis Key Prefix 集中管理（已实现）

### 问题
Redis key 前缀散落在各 `*StoreServiceImpl` 中，容易拼写错误（如 `messsagequeue` 三个 s）。

### 解决方案：`RedisKeyPrefix` 常量类

```java
public final class RedisKeyPrefix {
    private RedisKeyPrefix() {}

    public static final String SESSION_META = "mqtt:sessionmeta:";
    public static final String CLIENT_BROKER_INFO = "mqtt:clientbrokerinfo:";
    public static final String CLIENT_SUBSCRIPTION = "mqtt:clientsubs:";
    public static final String WILL_MESSAGE = "mqtt:willmessage:";
    public static final String WILL_MESSAGE_TOPIC = "mqtt:willmessageinfo:";
    public static final String MESSAGE_QUEUE = "mqtt:messagequeue:";
    public static final String INFLIGHT_WINDOW = "mqtt:inflightwindow:";
    public static final String QOS2 = "mqtt:qos2:";
    public static final String BIND_SESSION = "mqtt:bindsession:";
    public static final String CLEANUP_INDEX = "mqtt:cleanup:offline:";
}
```

### 使用方式
每个 Redis 存储服务中：
```java
// Before
private static final String PREFIX = "mqtt:messsagequeue:"; // 拼写错误！

// After
private static final String PREFIX = RedisKeyPrefix.MESSAGE_QUEUE;
```

### 新增 Key 规范
1. 在 `RedisKeyPrefix` 中添加常量
2. 命名格式：`mqtt:{domain}:`（全小写，冒号分隔）
3. 在存储服务中通过 `RedisKeyPrefix.XXX` 引用

### 已迁移的服务

| 服务类 | 使用的常量 |
|---|---|
| `RedisSessionMetaStoreServiceImpl` | `SESSION_META` |
| `RedisClientInfoStoreServiceImpl` | `CLIENT_BROKER_INFO` |
| `RedisSubscriptionStoreServiceImpl` | `CLIENT_SUBSCRIPTION` |
| `RedisWillMessageStoreServiceImpl` | `WILL_MESSAGE` |
| `RedisWillMessageTopicStoreServiceImpl` | `WILL_MESSAGE_TOPIC` |
| `RedisMessageQueueStoreServiceImpl` | `MESSAGE_QUEUE` |
| `RedisInflightWindowStoreServiceImpl` | `INFLIGHT_WINDOW` |
| `RedisBindSessionStoreServiceImpl` | `BIND_SESSION` |

### 类型一致性检查
注意 `RMap` 的泛型参数与 `put()`/`get()` 一致。例如：
```java
// Bug: remove() 使用了错误的泛型类型
RMap<String, DeviceCommandEnum> rmap = ...; // 错误！实际存的是 String

// Fix: 与 put()/get() 保持一致
RMap<String, String> rmap = ...;
```

---

## Metrics @Configuration 按领域拆分（已实现）

### 问题
`MetricConfiguration` 超过 1056 行，包含所有领域的指标 Bean。

### 解决方案：按领域拆分为 4 个配置类

| 配置类 | 职责 | Bean 数量 |
|---|---|---|
| `ConnectionMetricConfiguration` | 连接/断连/会话 Gauge + Counter | ~18 |
| `MqttPacketMetricConfiguration` | MQTT 报文/流量/Timer/权限/异常 | ~25 |
| `KafkaMetricConfiguration` | Kafka 发送/消费 Counter | ~4 |
| `NettyMetricConfiguration` | Netty DirectMemory/HeapMemory Gauge | ~2 |

原 `MetricConfiguration` 保留为文档索引：
```java
public final class MetricConfiguration {
    private MetricConfiguration() {
        // @see ConnectionMetricConfiguration
        // @see MqttPacketMetricConfiguration
        // @see KafkaMetricConfiguration
        // @see NettyMetricConfiguration
    }
}
```

### 拆分判断标准
- 按 `MetricConst` 中的指标名前缀分组
- 同一个领域的 Gauge 和 Counter 放在同一个类中
- 每个类职责单一，命名明确

---

## Config Validation（已实现）

### `ServerConfig` 增强

```java
@Data @Slf4j @Component @Validated
@ConfigurationProperties(prefix = "netty-server")
public class ServerConfig {

    @Min(value = 1, message = "tcpPort 必须大于 0")
    @Max(value = 65535, message = "tcpPort 不能超过 65535")
    private Integer tcpPort;

    @NotNull(message = "channelTimeout 不能为空")
    @Min(value = 1, message = "channelTimeout 必须大于 0")
    private Integer channelTimeout;

    // ... 其他字段同理 ...

    @PostConstruct
    public void init() throws UnknownHostException {
        // ... 初始化逻辑 ...
        logStartupSummary();
    }

    private void logStartupSummary() {
        log.info("========== ServerConfig Startup Summary ==========");
        log.info("TCP: port={}, SSL={}", tcpPort, startTcpSslServer);
        log.info("WebSocket: port={}, SSL={}", wsPort, startWsSslServer);
        log.info("Connections: max={}, channelTimeout={}s", maxConnection, channelTimeout);
        // ...
        log.info("===================================================");
    }
}
```

### 验证注解使用规范
- 端口类：`@Min(1) @Max(65535)`
- 超时/数量类：`@NotNull @Min(1)`
- 布尔开关类：`@NotNull`
- 所有 `message` 使用中文描述

---

## mvnd Build Acceleration

### 常用命令
```bash
# 编译
mvnd compile -T 1C

# 跳过测试编译
mvnd compile -DskipTests

# 运行测试
mvnd test

# 完整打包
mvnd package -DskipTests
```

### 依赖检查
```bash
# 依赖树
mvnd dependency:tree

# 依赖冲突
mvnd enforcer:enforce
```
