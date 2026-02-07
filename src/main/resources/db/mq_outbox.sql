-- ============================================================
-- MQ Outbox 表（用于 Kafka/Rocket 事务性消息投递）
-- ============================================================
CREATE TABLE IF NOT EXISTS `mq_outbox` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic` VARCHAR(128) NOT NULL COMMENT 'MQ Topic',
    `tag` VARCHAR(64) DEFAULT NULL COMMENT 'MQ Tag（RocketMQ使用）',
    `msg_key` VARCHAR(128) DEFAULT NULL COMMENT '消息业务Key（用于幂等/追踪）',
    `payload_json` TEXT NOT NULL COMMENT '消息体JSON',
    `payload_class` VARCHAR(256) NOT NULL COMMENT '消息体类名（用于反序列化）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=NEW待发送，1=SENDING发送中，2=SENT已发送，3=FAILED失败',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
    `error_msg` VARCHAR(1024) DEFAULT NULL COMMENT '最近一次失败原因（截断存储）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status_retry_time` (`status`, `next_retry_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ事务外盒表';

-- ============================================================
-- common_notify 表扩展（通知推送强一致）
-- ============================================================
ALTER TABLE `common_notify`
    ADD COLUMN `biz_key` VARCHAR(64) NULL COMMENT '业务键（雪花ID）' AFTER `id`,
    ADD COLUMN `push_status` TINYINT NOT NULL DEFAULT 0 COMMENT '推送状态：0=NEW待推送，1=PROCESSING推送中，2=SENT已推送，3=DEAD终态失败' AFTER `is_push`,
    ADD COLUMN `push_retry_count` INT NOT NULL DEFAULT 0 COMMENT '推送重试次数' AFTER `push_status`,
    ADD COLUMN `push_next_retry_time` DATETIME DEFAULT NULL COMMENT '下次允许重试推送时间' AFTER `push_retry_count`,
    ADD COLUMN `push_error_msg` VARCHAR(512) DEFAULT NULL COMMENT '最近一次推送失败原因' AFTER `push_next_retry_time`,
    ADD COLUMN `push_time` DATETIME DEFAULT NULL COMMENT '成功推送时间' AFTER `push_error_msg`;

-- 存量回填 biz_key（可按需调整生成规则）
UPDATE `common_notify`
SET `biz_key` = CONCAT('N', `id`)
WHERE `biz_key` IS NULL;

-- biz_key 调整为非空并加唯一索引
ALTER TABLE `common_notify`
    MODIFY COLUMN `biz_key` VARCHAR(64) NOT NULL COMMENT '业务键（雪花ID）';

ALTER TABLE `common_notify`
    ADD UNIQUE INDEX `uk_common_notify_biz_key` (`biz_key`);

-- 为推送状态机查询添加索引
ALTER TABLE `common_notify`
    ADD INDEX `idx_push_status_retry` (`push_status`, `push_next_retry_time`, `id`);

-- ============================================================
-- common_task 表扩展（业务键 biz_key）
-- ============================================================
ALTER TABLE `common_task`
    ADD COLUMN `biz_key` VARCHAR(64) NULL COMMENT '业务键（雪花ID）' AFTER `id`;

-- 存量回填 biz_key（可按需调整生成规则）
UPDATE `common_task`
SET `biz_key` = CONCAT('T', `id`)
WHERE `biz_key` IS NULL;

-- biz_key 调整为非空并加唯一索引
ALTER TABLE `common_task`
    MODIFY COLUMN `biz_key` VARCHAR(64) NOT NULL COMMENT '业务键（雪花ID）';

ALTER TABLE `common_task`
    ADD UNIQUE INDEX `uk_common_task_biz_key` (`biz_key`);
