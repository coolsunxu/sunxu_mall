---
name: mq-messaging-dev
description: Provides in-depth guidance for designing, deploying, and troubleshooting messaging systems with RocketMQ and Kafka in Java spring-boot projects. Use when integrating MQ, debugging message delay, duplication, consumerGroup conflicts, or planning performance/capacity.
---

# MQ / Messaging Skill（RocketMQ / Kafka 专项）

## 适用范围
- Spring Boot 2.x/3.x + Java 11+
- RocketMQ 4.x / 5.x（官方 spring-boot-starter）
- Kafka 2.x/3.x（spring-kafka + 事务/批量）
- 场景：任务异步、事件通知、削峰、分布式事务（Outbox）、流式计算

## 设计原则（强约束）

1. **ConsumerGroup 绝对隔离**
   - 一种业务语义 = 一个独立 Group，禁止“通用默认”。
2. **同组内订阅必须一致**
   - topic/tag/selector 完全一致，避免 rebalance 抖动。
3. **必须幂等**
   - 以业务 key 为核心，优先级：唯一约束/状态机 > Redis 去重 > 内存去重。
4. **避免同机多实例互抢**
   - 本地/灰度建议带端口/环境：`${server.port}`, `${spring.profiles.active}`。
5. **生产环境 Group 变更 = 灰度/滚动策略**<br>
   - 先上订阅一致的新实例 → 逐步下线旧实例 → 最终统一 Group 名称。

## 延迟/堆积/重复排障 SOP

按以下证据链定位延迟发生点（按优先级）：

| 步骤 | 判断依据 | 可观测指标 |
|----|--------|----------|
|①延迟消息/重试|RocketMQ：`delayTimeLevel>0`；Kafka：死信/重试 Topic|RocketMQ：`reconsumeTimes`；Kafka：`RetryingBatchErrorHandler` 指标|
|②Broker 入库 vs 接收|比较 `storeTimestamp` 与 `consumeStartTs`|探针：receiveLagMs；RocketMQ：MessageExt.storeTimestamp|
|③consumerGroup 冲突|**如果 connections>1 且订阅不一致或有僵尸实例→分钟级抖动**|探针：`connections`,`consumeStats.diffTotal`；(mqadmin) consumerConnection|
|④“真堆积”|diffTotal 持续增长，消费单条耗时高，线程池阻塞|探针：diffTotal,maxDiff,TPS；Prometheus：kafka_consumer_lag,rocketmq_group_diff|
|⑤客户端停顿|JVM 心跳断档/FULL GC 时间片长|GC 日志、ThreadMXBean、心跳探针|

> 本次根因落在③，因此优先为任务执行消费者设置独立 Group，消除实例互抢；其次清理残留连接。## 配置模板（直接复制使用）

### application.yml（RocketMQ）
```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAMESRV:127.0.0.1:9876}
  producer:
    group: mall-rocket-producer-group
    send-msg-timeout: 3000
  consumer:
    group: mall-rocket-common-group   # 只做通用广播/通知

# --------- ⬇️ 按照业务拆分 ⬇️ ------------
app:
  mq:
    type: rocket       # rocket / kafka / local
    rocket:
      task-topic: mall-task-execute-topic
      task-consumer-group: mall-rocket-consumer-group-task-${server.port}  # 独立
      create-topic: mall-task-create-topic  
      create-consumer-group: mall-rocket-consumer-group-create-${server.port}
      notification-topic: mall-notify-topic
      notification-consumer-group: mall-rocket-consumer-group-notification
```

### application.yml（Kafka）
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: StringSerializer
      value-serializer: JsonSerializer    # 统一 JSON
    consumer:
      key-deserializer: StringDeserializer
      value-deserializer: JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.*,com.alibaba.fastjson2.*"

app:
  mq:
    type: kafka
    kafka:
      task-topic: mall-task-execute-topic
      task-group-id: mall-kafka-consumer-group-task-${server.port}
      notification-topic: mall-notify-topic
      notification-group-id: mall-kafka-consumer-group-notification
```

## Outbox 事务消息（保证业务事务 + 消息可靠投递）

- **原理**：业务表 + `mq_outbox` 同一事务写；后台 Dispatcher 轮询投递。
- **使用**：业务代码只调用 `OutboxMessageProducer.send(topic, tag, key, payload)`。
- **表结构**（已存在）：`mq_outbox`（参考 `db/mq_outbox.sql`）。
- **关键字段**：topic, tag, msg_key, payload_json, payload_class, status, retry_count, next_retry_time。
- **幂等消费**：以 **msg_key** 作为幂等主键，配合 `selectByBizKey`/`upsert` 逻辑。

### Dispatcher 作业要点

- **间隔**：默认 1s（支持 `@Scheduled(fixedDelayString=)`）。
- **失败重试**：指数退避（max 5 次）；成功则 mark `SENT`，失败标记 `FAILED` 并写 `error_msg`。  
- **支持并发抢占**：使用 `try_lock_for_send`（状态 CAS：`NEW→SENDING`）。
- **支持多 MQ**：根据 `mq.type=rocket|kafka` 动态路由。

## 消费者端最佳实践

### 方法模板（直接套）

```java
@Slf4j
@Component
@ConditionalOnProperty(name="app.mq.type",havingValue="rocket")
@RocketMQMessageListener(
   topic = "${app.mq.rocket.task-topic:mall-task-execute-topic}",
   consumerGroup = "${app.mq.rocket.task-consumer-group}",
   selectorExpression = MQConstant.TAG_EXCEL_EXPORT   // tag 过滤
)
public class RocketTaskListener implements RocketMQListener<String> {
    private final TaskConsumerDelegate delegate;
    @Override
    public void onMessage(String message) {
        log.info("Received message: {}", message);
        // --- 幂等校验 ---
        if (ProcessedCache.contains(key)) return;
        // --- 业务委托（异步线程池） ---
        delegate.consumeByBizKey(message);
    }
}
```

### 消费失败处理

- **抛出异常**：消息自动重试（Rocket：maxReconsumeTimes；Kafka：RetryTopic）。
- **达到上限** → 死信队列 / 业务补偿。
- **务必记录重试次数 + 关键日志**（trace/bizKey）。

### 慢任务隔离

```java
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Bean("exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return ex;
    }
}
// 使用：@Async("exportExecutor") 处理导出等慢任务
```

## 监控与告警（生产）

| Item | RocketMQ 指标 | Kafka 指标 | 阈值建议 |
|----|--------------|-----------|---------|
| 连接数异常|consumerGroup connections|consumer group members|突增/突降即告警|
|堆积(diff)|group diff|consumer lag|>10w 或持续增长>5min 告警|
|消费 RT|consume RT|fetch-latency-avg|>2s 时关注|
|重试率|reconsumeTimes|retry-rate|>5% 告警|

**采集方式**：
- RocketMQ：Admin API + 探针（已提供 `cursor-mq-probe.sh`）；集成 Prometheus（exporter）。
- Kafka：JMX MBean、Prometheus exporter（kafka_exporter）。

## 快速排障脚本（参考 scripts 目录）

1. **RocketMQ 探测连接与堆积**  
```bash
# scripts/rocketmq_probe.sh
mqadmin consumerConnection -g <group> -n <namesrv>
mqadmin consumerProgress -g <group> -t <topic> -n <namesrv>
```
2. **Kafka 消费组信息**  
```bash
kafka-consumer-groups.sh --bootstrap-server <broker> --group <group> --describe
```

详见 `reference.md` 与 `examples.md`。