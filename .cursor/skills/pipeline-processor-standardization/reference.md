# Pipeline And Processor Standardization Reference

## Scope
- `src/main/java/com/example/nettyserver/pipeline/`
- `src/main/java/com/example/nettyserver/mqtt/processor/impl/`

---

## Pipeline Deduplication（已实现）

### 问题
四个 Pipeline 类（TCP、TCP-SSL、WS、WS-SSL）中 80%+ handler 重复添加。

### 解决方案：基类提取公共方法

在 `MqttPipelineInitializer` 基类中新增两个 `protected` 方法：

```java
// 前置 handler：IP 黑名单、代理协议、流量指标
protected void addCommonFrontHandlers(ChannelPipeline pipeline,
        FlowMetricsHandler flowMetricsHandler,
        ProxyProtocolDecoder proxyProtocolDecoder,
        IpBlacklistHandler ipBlacklistHandler) {
    if (ipBlacklistHandler != null) {
        pipeline.addFirst("ipBlacklistHandler", ipBlacklistHandler);
    }
    pipeline.addFirst("proxyDecoder", proxyProtocolDecoder);
    pipeline.addFirst("flowMetricHandler", flowMetricsHandler);
}

// MQTT 编解码和业务 handler
protected void addCommonMqttHandlers(ChannelPipeline pipeline,
        MessageMetricsHandler messageMetricsHandler,
        MqttHandler mqttHandler,
        ExceptionHandler exceptionHandler) {
    pipeline.addLast("decoder", new MqttDecoder(serverConfig.getMaxBytesInMessage()));
    pipeline.addLast("encoder", MqttEncoder.INSTANCE);
    pipeline.addLast("messageMetricHandler", messageMetricsHandler);
    pipeline.addLast("mqttBrokerHandler", mqttHandler);
    pipeline.addLast("exceptionHandler", exceptionHandler);
}
```

### 子类调用模式

```java
// TcpPipeline（最简形式）
@Override
public void configureCustomPipeline(SocketChannel channel, ChannelPipeline pipeline) {
    addCommonFrontHandlers(pipeline, flowMetricsHandler, proxyProtocolDecoder, ipBlacklistHandler);
    addCommonMqttHandlers(pipeline, messageMetricsHandler, mqttHandler, exceptionHandler);
}

// TcpSslPipeline（中间插入 SSL handler）
@Override
public void configureCustomPipeline(SocketChannel channel, ChannelPipeline pipeline) {
    addCommonFrontHandlers(pipeline, flowMetricsHandler, proxyProtocolDecoder, ipBlacklistHandler);
    pipeline.addLast("ssl", sslContext.newHandler(channel.alloc()));
    addCommonMqttHandlers(pipeline, messageMetricsHandler, mqttHandler, exceptionHandler);
}

// WebsocketPipeline（中间插入 HTTP + WS handler）
@Override
public void configureCustomPipeline(SocketChannel channel, ChannelPipeline pipeline) {
    addCommonFrontHandlers(pipeline, flowMetricsHandler, proxyProtocolDecoder, ipBlacklistHandler);
    pipeline.addLast("httpServerCodec", new HttpServerCodec());
    pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(65536));
    pipeline.addLast("webSocketServerProtocolHandler",
            new WebSocketServerProtocolHandler(serverConfig.getWebsocketPath(), "mqtt", true));
    pipeline.addLast("mqttWebSocketCodec", new MqttWebSocketCodec());
    addCommonMqttHandlers(pipeline, messageMetricsHandler, mqttHandler, exceptionHandler);
}
```

### 扩展点
新增 Pipeline 类型时，只需：
1. 继承 `MqttPipelineInitializer`
2. 调用 `addCommonFrontHandlers()` + 插入自定义 handler + 调用 `addCommonMqttHandlers()`

---

## Processor Dependency Aggregation（已实现）

### 问题
Processor 构造器参数过多（6+），不利于维护和测试。

### 解决方案：`XxxProcessorDependencies` 聚合对象

```java
@Getter
@Component
public class PingReqProcessorDependencies {
    private final CustomMqttMessageFactory messageFactory;
    private final SessionManager sessionManager;
    private final SubscriptionStoreService subscriptionStoreService;
    private final MetricService metricService;

    public PingReqProcessorDependencies(
            CustomMqttMessageFactory messageFactory,
            SessionManager sessionManager,
            SubscriptionStoreService subscriptionStoreService,
            MetricService metricService) {
        this.messageFactory = messageFactory;
        this.sessionManager = sessionManager;
        this.subscriptionStoreService = subscriptionStoreService;
        this.metricService = metricService;
    }
}
```

### Processor 使用方式

```java
public class PingReqProcessor implements MqttProcessor {
    private final CustomMqttMessageFactory messageFactory;
    private final SessionManager sessionManager;
    // ...

    public PingReqProcessor(PingReqProcessorDependencies dependencies) {
        this.messageFactory = dependencies.getMessageFactory();
        this.sessionManager = dependencies.getSessionManager();
        // ...
    }
}
```

### 命名规范
- 依赖类：`{ProcessorName}Dependencies`
- 包路径：与对应 Processor 同包（`mqtt.processor.impl`）
- 注解：`@Getter @Component`（由 Spring 自动注入）

---

## Processor Exception Handling
- 统一 catch 块中的断连/ack 逻辑
- 引入 base handler 或 exception mapper（待后续实现）

## Validation Checklist
- [ ] 四个 Pipeline 子类无重复 handler 链代码
- [ ] 新增 Pipeline 类型时只需 override `configureCustomPipeline`
- [ ] Processor 构造器参数 <= 2（使用 Dependencies 聚合）
- [ ] Exception 行为跨 Processor 保持一致
