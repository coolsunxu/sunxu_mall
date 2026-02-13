-- ============================================================
-- 幂等/防重复提交相关表结构变更
-- ============================================================

-- 1. common_task 表增加 dedup_key（任务创建去重）
ALTER TABLE `common_task`
    ADD COLUMN `dedup_key` VARCHAR(128) NULL COMMENT '幂等去重Key（md5摘要，同一dedupKey只创建一次任务）' AFTER `biz_key`;

-- 存量回填 dedup_key（用 biz_key 作为默认值，避免唯一索引冲突）
UPDATE `common_task`
SET `dedup_key` = `biz_key`
WHERE `dedup_key` IS NULL;

-- dedup_key 加唯一索引（允许 NULL，但非 NULL 值必须唯一）
ALTER TABLE `common_task`
    ADD UNIQUE INDEX `uk_common_task_dedup_key` (`dedup_key`);

-- 2. mq_outbox 表调整 msg_key 约束（防止重复落库/重复投递）
ALTER TABLE `mq_outbox`
    MODIFY COLUMN `msg_key` VARCHAR(128) NOT NULL COMMENT '消息业务Key（用于幂等/追踪，NOT NULL）';

ALTER TABLE `mq_outbox`
    ADD UNIQUE INDEX `uk_mq_outbox_topic_tag_key` (`topic`, `tag`, `msg_key`);
