# MQ 技术参考 & 配置模板库

## 目录
- 配置 YAML 模板（独立 Group + Outbox）
- 监控指标&阈值
- 性能/容量参数速查
- 工具脚本

---

## YAML 模板

### Outbox 模式统一入口
```java
@Service
public class OrderService {
    @Resource private OutboxMessageProducer producer;
    @Transactional
    public void createOrder(CreateOrderDTO dto){
        Order order = saveOrder(dto);          // ① 业务表
        producer.send("order-event-topic","order_create",order.getOrderNo(),order);
    }
}
```

### RocketMQ 消费者（独立 Group + tag 过滤幂等）

```yaml
app:
  mq:
    rocket:
      task-topic: mall-task-execute-topic
      task-consumer-group: mall-rocket-consumer-group-task-${server.port}
      dispatch-interval: 1000   # Outbox dispatcher ms
```

```java
@RocketMQMessageListener(
    topic = "${app.mq.rocket.task-topic}",
    consumerGroup = "${app.mq.rocket.task-consumer-group}",
    selectorExpression = MQConstant.TAG_EXCEL_EXPORT,
    maxReconsumeTimes = 3,        // 默认 16，调小防止风暴
    consumeThreadMax = 20)        // 默认 64，可调
public class RocketTaskListener implements RocketMQListener<String> {
    ...
}
```

### Kafka 消费者（独立 Group + JSON 反序列化 + 重试 Topic）

```yaml
spring:
  kafka:
    bootstrap-servers: your-broker:9092
    producer:
      key-serializer: StringSerializer
      value-serializer: JsonSerializer
    consumer:
      key-deserializer: StringDeserializer
      value-deserializer: JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.*"
        isolation.level: read_committed      # 仅事务消息
      enable-auto-commit: false   // 手工 commit
      auto-offset-reset: earliest
    listener:
      type: single            // 单条模式/批量 batch
      ack-mode: manual_immediate
```

```java
@Component
@KafkaListener(topics = "${app.mq.kafka.task-topic}", groupId = "${app.mq.kafka.task-group-id}")
public class KafkaTaskListener {
    @KafkaHandler
    public void handle(String message, Acknowledgment ack) {
        try {
            TaskEvent e = JsonUtil.parseObject(message, TaskEvent.class);
            delegate.consume(e.getBizKey());
        } finally { ack.acknowledge(); }
    }
}
```

---

## 监控指标 & 阈值

| 关注项 | RocketMQ (AdminAPI) | Kafka(JMX) | 推荐阈值 | 备注 |
|--------|-------------------|----------|--------|------|
| 连接数异常 | consumerConnection.connectionSet.size | consumer_group.members | 突增/突降均告警 | 说明有僵尸或重复部署 |
| 堆积 | examineConsumeStats.diffTotal/queueDiff | consumer_lag_sum | 持续>10 万或 >5 min 增加 | 区分 rebalance 与消费能力问题 |
| 消费能力 | consumeTps,avgRT | consumer_fetch_rate,records-lag-max | RT>2s 或 TPS<预期 50% | 排除慢任务，考虑扩容 |
| 重试/死信 | reconsumeTimes | retry_count,dlq_rate | >5% 开始关注 | 关注重试原因是否异常累积 |

---

## 性能 & 容量速查

### RocketMQ

| 参数 | 功能 | 建议值 |
|-----|-----|--------|
|`consumeThreadMin`|初始化线程|机器核数一半|
|`consumeThreadMax`|最大线程|CPU 利用率 70% 以内，默认 20（可调 64）|
|`pullBatchSize`|每批消息条数|默认 32，< 100|
|`pullInterval`|空闲拉取等待|默认 0，堆积可配合长轮询 100-200ms|
|`consumeTimeout`|单条超时|默认 15 min，调小能快速重试|

### Kafka

| 参数 | 功能 | 建议 |
|-----|-----|------|
|`fetch.min.bytes`|Broker 最小返回数据|1KB->16KB，降低网络轮询|
|`fetch.max.wait.ms`|最长等待打包|0->100~500ms，提高吞吐|
|`max.poll.records`|单次 poll 条数|默认 500；处理慢时调小|
|`session.timeout.ms`|会话超时|默认 10s；网络抖动可 30s|
|`heartbeat.interval.ms`|心跳|1/3 session，一般 3~10s|

---

## CLI 工具速查

### RocketMQ
```bash
cd $ROCKETMQ_HOME/bin
mqadmin topicList -n localhost:9876
mqadmin topicRoute -t <topic> -n localhost:9876
mqadmin consumerConnection -g <group> -n localhost:9876
mqadmin consumerProgress -g <group> -t <topic> -n localhost:9876
```

### Kafka
```bash
# 消费组列表
kafka-consumer-groups.sh --bootstrap-server <broker> --list
# 消费进度/滞后
kafka-consumer-groups.sh --bootstrap-server <broker> --group <group> --describe
# 重置 offset (到最早/最新/特定时间)
kafka-consumer-groups.sh --bootstrap-server <broker> --group <group> --topic <topic> --reset-offsets --to-earliest --execute
```

---

## 常用 SQL（MySQL Outbox）

```sql
-- 积压量查询
SELECT status,COUNT(*) FROM mq_outbox WHERE next_retry_time<=now();
-- 清理已发送（运维维护）
DELETE FROM mq_outbox WHERE status=2 AND create_time<now()-INTERVAL 30 DAY LIMIT 1000;
```

## 其他
- **网络带宽评估**：每 1w qps 1KB 消息 ≈ 80 Mbit/s，双机房冗余 * 2
- **JVM 内存**（消费者）：建议 1~2G / 实例，大量批量拉取可 4G
- **磁盘**（消息留存）：单条 1KB * 10w/min * 2d * 2（副本）≈ 550 GB，按 70% 使用率预留