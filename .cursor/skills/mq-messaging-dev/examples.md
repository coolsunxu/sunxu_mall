# MQ 消息延迟/重复排查与修复实战示例

> 基于 **RocketMQ** 与 **Kafka** 在真实生产环境的经验整理

---

## 🍵 场景 ①：开发环境“偶发 2~5 分钟”延迟 —— Group 冲突 / 同机多实例

**现象**
- 日志：发送成功 (`SEND_OK`)，storeTs 正常，但 `ConsumeMessageThread` 5 分钟后才触发。
- 未触发重试/延迟消息：reconsumeTimes=0，delayTimeLevel=0。

**排查**
```bash
mqadmin consumerConnection -g mall-rocket-consumer-group
connectionSet size = 2
- 26.26.26.1:32460
- 26.26.26.1:32512
```
> **根因**：同一 Group 被两个本地实例抢占（端口不同）导致 rebalance，队列被另一实例先占，本实例等待重分配。

**修复**
1. 为“任务执行”消费者独立 Group：
```yaml
app:
  mq:
    rocket:
      task-consumer-group: mall-rocket-consumer-group-task-${server.port}
```
2. 清理/停止残留实例：
```bash
kill $(ps -ef | grep 'java.*sunxu.*8012' | awk '{print $2}')
```
3. 验证连接数恢复 1：
```bash
mqadmin consumerConnection -g <task-group>
```
4. 持续触发任务，确认 receiveLagMs 保持<500ms。

---

## 🐳 场景 ②：上线后堆积 10w+ / 消费 RT 3s —— Kafka fetch+业务双慢

**监控告警**
```
kafka_consumer_lag_sum{group="payment-group"} 128000
kafka_consumer_fetch_latency_avg{group="payment-group"} 3.1s
```

**排查**
1. 应用侧日志：单条处理耗时统计 —— `AverageTaskTime = 2.8s`（网络+数据库调用慢）。
2. 网络抓包：Broker RTT <5ms，排除网络。
3. Broker 日志：无 fetch error。

**根因**
业务操作（外部 API+大 SQL）阻塞消费线程；fetch 等待聚合超时 3s（batch 未攒够）。

**修复**
1. 业务线程池隔离：
```java
    @Async("bizExecutor")   // core=4 max=8 q=200
    public void doPayment(PaymentEvent e){ ... }
```
2. 调小 fetch 聚合：
```properties
fetch.min.bytes=512→1024
fetch.max.wait.ms=500→100
max.poll.records=500→100
```
3. 提高消费线程并发：
```properties
spring.kafka.consumer.properties.max.poll.interval.ms=300000→600000
```

**效果**
Lag 由 10w→2w（30min 内）；RT 下降 50%。继续扩容消费者实例（副本+分区）至 0 Lag。

---

## 🔥 场景 ③：双机房灰度，消息重复率 15% —— 订阅不一致

**现象**
- 用户收到多条重复 SMS；log 显示同一 msgId 被两个实例成功消费。

**排查**
```bash
mqadmin consumerProgress -g mall-notification-group
QueueID #2：
  BrokerOffset: 80012
  ConsumerOffset: (A) 80010  26.26.26.3
                  (B) 80011  26.26.26.4
```
- 机房 A selectorExpression = "TAG_SMS"（旧版本 tag）
- 机房 B selectorExpression = "TAG_SMS_V2"（新版本）
> 订阅不一致→broker 为同队列推送两次（tag 过滤逻辑）。

**修复**
1. 回滚/统一 tag，两机房保持一致再灰度上线。
2. 升级策略：
   - 新版本实例加入，订阅一致后下线旧实例。
   - **禁止同时存在不同 selectorExpression！**
3. 业务幂等补齐：SMS 按 phoneNo+templateId 写入 10min TTL 缓存。

---

## ⚙️ 场景 ④：定时任务兜底后依然“部分失败” —— Outbox + 幂等未闭环

**现象**
Outbox Dispatcher 日志：
```
Send failed,Outbox id=12345,error=Duplicate entry 'ORD-20260208-0001' for key 'task.UNIQ_biz_key'
```

**根因**
- 业务表用唯一键约束做幂等，但 Outbox 记录因前面失败再次投递成功后，业务插入违反唯一键→抛异常回滚→Dispatcher 标记 `FAILED`，不再重试，出现“丢消息”假象。

**修复**
1. 业务 Service 改为 `insertOrUpdateByKey()` 语义：
```java
@Transactional
public void createTaskFromRequest(TaskRequestDTO dto){
    Task t = selectByBizKey(dto.getBizKey());   // 优先查询
    if (t==null) save(dto);
    else updateIfNecessary(dto);
}
```
2. Dispatcher 侧捕获特定异常，标记成功：
```java
} catch (DuplicateKeyException ex) {
    outboxMapper.markSent(outboxId);          // 幂等成功
    log.warn("DuplicateKey ignored, outbox={}", outboxId);
    return true;
}
```
3. 为 task 表 biz_key 添加 **业务索引** 并校验逻辑幂等（非仅 DB 约束）。

---

## 🧪 场景 ⑤：压力测试 → 消费者频繁触 FULL GC —— 拉取批过大

**压测环境**
qps 1w→2w 时，RT 抖动变大；JVM 观察到 FULL GC 每 3min；Kafkagc 指标正常。

**原因**
- 默认 `max.poll.records=500` → 瞬时 100MB 大对象。
- 业务反序列化后缓存计算，年轻代无法及时回收。

**解决**
1. 调小批量：
```properties
max.poll.records=500→50
fetch.min.bytes=1KB→2KB
fetch.max.wait.ms=0→50ms     // 稍聚合即可
```
2. 并发调大：
```properties
spring.kafka.listener.concurrency=6   // 分区副本充足
```
3. JVM：-Xms=-Xmx=4G，G1GC + InitiatingHeapOccupancyPercent=25。

---

## ✅ 排查 checklist（推荐复制到 PRD）

1. **消息是否延迟/重试**？
   - RocketMQ：msg.reconsumeTimes / msg.delayTimeLevel。
   - Kafka：是否 RetryTopic/DeadLetter。

2. **Broker 入库时间 vs 消费时间**？
   - lag = consumerTs - storeTs。
   - >2s 记录堆积/慢任务；>2min 高度怀疑 Group 冲突或僵尸。

3. **消费者连接数**？
   - 每个 Group connections = 1（灰度期可接受 2-3 但必须订阅一致）。
   - >1 且延迟 → 优先清理/独立 Group。

4. **diffTotal / maxDiff**？
   - RocketMQ：`mqadmin consumerProgress`。
   - Kafka：`kafka-consumer-groups --describe`。
   - 持续增长：并发能力 or 单条 RT 问题。

5. **订阅一致性比对**？
   - `mqadmin consumerConnection -g <group>` 打印 subscriptionDataSet。
   - Kafka group-coordinator 日志查看 assignment 差异。

6. **消费线程/线程池/RT 指标**？
   - JVM：ThreadMXBean、RT 直方；线程池队列长度。
   - 业务：单条耗时百分位 P99；慢日志 >2s。

7. **GC/JVM 停顿**？
   - FULL GC 频率；GC 停顿 >500ms 影响拉取心跳。

8. **业务幂等验证**？
   - duplicateKey 处理；重放脚本压测后数据一致。

> 按 1→8 顺序检查，基本可覆盖 90% 线上消息延迟/重复/堆积场景。