## 防止重复提交/重复消费方案（结合当前代码）

> 目标：既支持客户端 `Idempotency-Key`，也能在客户端不传 key 时对双击/重试做短窗口去重；并确保 MQ 至少一次投递场景下端到端幂等（允许重复投递，但只执行一次）。

## 现状与问题定位（基于当前代码）

- **Excel 导出链路**：`@ExcelExport` → `CommonTaskAspect` 发送“创建任务”消息 → `*TaskCreateListener` → `CommonTaskService.createTaskFromRequest` 插入 `common_task` 并发送“执行任务”消息 → `TaskConsumerDelegate.consumeByBizKey` → `ExcelExportTask`。
- **重复提交的主要入口**：
  - **HTTP 双击/重试**会让 `CommonTaskAspect` 重复发送“创建任务”消息（当前 key 使用 `IdUtil.getSnowflakeNextIdStr()`，天然无法识别同一请求）。
  - **MQ 至少一次投递**会导致“创建任务/执行任务”消息可能重复。
- **当前缺口**：
  - `mq_outbox` 表 **没有**对 `msg_key` 的唯一约束（`src/main/resources/db/mq_outbox.sql`），producer 也直接 insert，可能重复落库/重复投递。
  - `TaskConsumerDelegate` 仅做 `status==WAITING` 判断（`src/main/java/.../mq/consumer/TaskConsumerDelegate.java`），但没有原子“抢占执行权”，并发重复消费时仍可能重复执行。

## 目标行为

- **客户端可选传入** `Idempotency-Key`：同 key 的重复提交 **不重复执行业务**。
- **客户端不传 key**：服务端按“用户 + URI + 方法 + 请求体 hash”做 **短窗口去重**（例如 5~30 秒），抑制双击。
- **消息链路端到端幂等**：允许重复投递、重复消费，但业务只执行一次。

## 总体方案（分层幂等）

### 1) HTTP 层（所有写接口）

- **落点**：新增通用幂等组件 + 拦截器/注解。
- **关键点**：拦截器要读取请求体，需要先用 `ContentCachingRequestWrapper`（通过 Filter 包装），避免 body 被消费一次后下游拿不到。
- **策略**：
  - **优先使用 Header** `Idempotency-Key`。
  - **无 Header 时 fallback**：`userId + httpMethod + requestURI + bodyHash` 作为短窗口 key。
  - Redis 使用 **setIfAbsent(NX+TTL)** 抢占，失败即判定重复提交：
    - 通用写接口建议返回 **409**（或业务自定义错误码）提示“请勿重复提交”。
    - 某些接口可配置为 **IGNORE**（直接放过/直接返回空成功），按业务决定。

### 2) 任务创建层（Excel 导出/异步任务）

- **DTO 扩展**：在 `CommonTaskRequestDTO` 增加 `idempotencyKey`（或 `dedupKey`）字段；切面构造并写入 DTO。
- **DB 兜底幂等**：在 `common_task` 增加可索引字段（避免对 `request_param` 直接建索引）：
  - 推荐新增 `dedup_key VARCHAR(64)`（或 `request_hash`）
  - 增加唯一索引：`uk_common_task_dedup_key(dedup_key)`（或 `uk(create_user_id,type,biz_type,request_hash)`）
- **服务端逻辑**：`CommonTaskService.createTaskFromRequest` 先基于 `dedup_key` 做“插入或读取已存在任务”，保证 MQ 重复投递时不产生多条任务。

### 3) Outbox 层（防止重复落库/重复投递）

- **表约束**：调整 `mq_outbox.msg_key` 为 NOT NULL，并加唯一索引：
  - 推荐：`uk_mq_outbox_topic_tag_key(topic, tag, msg_key)`
- **写入幂等**：`OutboxMessageProducer.send` 对同一 `(topic,tag,msg_key)` 重复写入时应无害：
  - Mapper SQL 可改为 `INSERT ... ON DUPLICATE KEY UPDATE update_time=NOW()` 或 `INSERT IGNORE`（按你希望的可观测性选择）。

### 4) 消费者执行层（防止重复执行）

- **增加原子抢占**：为 `common_task` 增加 mapper 方法 `tryLockForRunByBizKey`：
  - `UPDATE common_task SET status=RUNNING,... WHERE biz_key=? AND status=WAITING`
  - `rows==1` 才继续执行任务；否则认为已被抢占/已执行，直接 skip。
- **落点**：在 `TaskConsumerDelegate.consumeByBizKey` 调用任务处理前执行抢占。

## 具体实施步骤（按影响面由小到大）

- **Step A：落地通用幂等组件（HTTP 层）**
  - 新增 `IdempotencyProperties`（TTL、header 名称、启用开关、短窗口秒数等）
  - 新增 `IdempotencyService`（基于 Redisson `RBucket.trySet` 实现 setIfAbsent）
  - 新增 `IdempotencyKeyResolver`（Header 优先，fallback bodyHash；bodyHash 使用稳定 hash，如 SHA-256/MD5，注意阿里规约与空值处理）
  - 新增 `IdempotencyFilter`（包装 `ContentCachingRequestWrapper`）
  - 新增 `IdempotencyInterceptor`（对 POST/PUT/DELETE 生效，失败返回 409）
  - 在配置类中注册 Filter/Interceptor（例如新建 `config/WebMvcIdempotencyConfig.java`）
- **Step B：Excel 导出链路接入幂等**
  - 修改 `CommonTaskAspect`：生成 `dedupKey`（Header 或 fallback：userId+bizType+paramJsonHash），重复则不再发送创建消息。
  - 修改 `CommonTaskRequestDTO`：携带 `dedupKey`。
- **Step C：任务创建幂等（DB 兜底）**
  - 增加 `common_task.dedup_key` 字段与唯一索引（提供 `src/main/resources/db/*.sql` 变更脚本）。
  - 修改 `CommonTaskService.createTaskFromRequest`：
    - 生成/读取 `dedupKey`，尝试插入任务；
    - 捕获唯一键冲突时查询已存在任务并直接返回（或记录日志）。
- **Step D：Outbox 幂等（DB + SQL）**
  - 调整 `mq_outbox` 表：`msg_key` NOT NULL + 唯一索引。
  - 修改 `MqOutboxEntityMapper.xml` 的 `insert` 为 upsert/ignore 语义。
- **Step E：消费者端防重执行**
  - 增加 `CommonTaskEntityMapper.tryLockForRunByBizKey` + XML SQL。
  - `TaskConsumerDelegate` 在执行前调用抢占；抢占失败直接 return。
- **Step F：推广到所有写接口**
  - 默认：拦截器对所有 POST/PUT/DELETE 生效。
  - 逐步补齐差异化策略：
    - 上传类/批量类接口可设更短 TTL；
    - 明确可幂等的接口可返回 409 或直接 IGNORE（通过注解/配置白名单控制）。

## 关键文件清单（会修改/新增）

- Excel 任务链路：
  - `src/main/java/com/example/sunxu_mall/aspect/CommonTaskAspect.java`
  - `src/main/java/com/example/sunxu_mall/dto/common/CommonTaskRequestDTO.java`
  - `src/main/java/com/example/sunxu_mall/service/common/CommonTaskService.java`
  - `src/main/java/com/example/sunxu_mall/mq/consumer/TaskConsumerDelegate.java`
  - `src/main/java/com/example/sunxu_mall/mapper/common/CommonTaskEntityMapper.java`
  - `src/main/resources/com/example/sunxu_mall/mapper/common/CommonTaskEntityMapper.xml`
- Outbox：
  - `src/main/resources/db/mq_outbox.sql`
  - `src/main/java/com/example/sunxu_mall/mapper/mq/MqOutboxEntityMapper.java`
  - `src/main/resources/com/example/sunxu_mall/mapper/mq/MqOutboxEntityMapper.xml`
- 通用幂等新增（建议新建包 `idempotency` 或 `interceptor`）：
  - `src/main/java/com/example/sunxu_mall/idempotency/IdempotencyService.java`
  - `src/main/java/com/example/sunxu_mall/idempotency/IdempotencyKeyResolver.java`
  - `src/main/java/com/example/sunxu_mall/idempotency/IdempotencyProperties.java`
  - `src/main/java/com/example/sunxu_mall/interceptor/IdempotencyInterceptor.java`
  - `src/main/java/com/example/sunxu_mall/filter/IdempotencyFilter.java`
  - `src/main/java/com/example/sunxu_mall/config/WebMvcIdempotencyConfig.java`

## 验证与回归

- **本地并发验证（Excel 导出）**：同一用户同参数连续触发 2 次，确保只创建 1 条 `common_task` 且只投递/执行一次。
- **MQ 重复投递验证**：手工重复发送 create/execute 消息，确保任务不重复创建/不重复执行。
- **构建测试**：使用 `mvnd -q test`。

