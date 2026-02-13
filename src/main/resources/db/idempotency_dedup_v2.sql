-- ============================================================
-- 幂等/防重复提交 v2 迁移
-- 目标：导出任务由“历史唯一”改为“进行中唯一”
-- ============================================================

-- 1) common_task 新增 fingerprint（同参数任务指纹）
ALTER TABLE `common_task`
    ADD COLUMN `fingerprint` VARCHAR(64) NULL COMMENT '任务参数指纹（用于进行中任务判重）' AFTER `dedup_key`;

-- 2) 回填 fingerprint：优先使用 dedup_key，为空则回退 biz_key（避免空值）
UPDATE `common_task`
SET `fingerprint` = CASE
    WHEN `dedup_key` IS NOT NULL AND `dedup_key` <> '' THEN `dedup_key`
    ELSE `biz_key`
END
WHERE `fingerprint` IS NULL;

-- 3) 取消 dedup_key 的历史唯一约束（允许任务完成后再次创建）
ALTER TABLE `common_task`
    DROP INDEX `uk_common_task_dedup_key`;

-- 4) 新增进行中判重索引（fingerprint + status + is_del）
ALTER TABLE `common_task`
    ADD INDEX `idx_task_fingerprint_status_create_time` (`fingerprint`, `status`, `is_del`, `create_time`);
