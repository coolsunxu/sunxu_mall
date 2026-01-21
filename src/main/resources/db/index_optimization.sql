-- ============================================
-- 索引优化建议 SQL
-- 执行前请在测试环境验证，并根据实际数据量和查询模式调整
-- ============================================

-- 1. 用户表 (sys_user) 索引优化
-- 游标分页查询优化：id < cursorId order by id desc
-- 主键索引已天然支持此查询模式

-- 常用筛选条件联合索引
CREATE INDEX IF NOT EXISTS idx_user_is_del_id 
ON sys_user(is_del, id DESC);

-- 用户名模糊查询（如果是前缀匹配可以利用索引）
CREATE INDEX IF NOT EXISTS idx_user_username 
ON sys_user(user_name);

-- 部门筛选 + 排序
CREATE INDEX IF NOT EXISTS idx_user_dept_id 
ON sys_user(dept_id, id DESC);


-- 2. 商品表 (mall_product) 索引优化
-- 游标分页查询优化
CREATE INDEX IF NOT EXISTS idx_product_is_del_id 
ON mall_product(is_del, id DESC);

-- 分类筛选 + 排序
CREATE INDEX IF NOT EXISTS idx_product_category_id 
ON mall_product(category_id, id DESC);

-- 品牌筛选 + 排序
CREATE INDEX IF NOT EXISTS idx_product_brand_id 
ON mall_product(brand_id, id DESC);

-- 商品组筛选 + 排序
CREATE INDEX IF NOT EXISTS idx_product_group_id 
ON mall_product(product_group_id, id DESC);


-- 3. 任务表 (common_task) 索引优化
-- 定时任务查询：按状态和创建时间
CREATE INDEX IF NOT EXISTS idx_task_status_create_time 
ON common_task(status, create_time);

-- 用户任务查询
CREATE INDEX IF NOT EXISTS idx_task_user_id 
ON common_task(create_user_id, id DESC);


-- 4. 通知表 (common_notify) 索引优化
-- 用户通知查询
CREATE INDEX IF NOT EXISTS idx_notify_to_user_read 
ON common_notify(to_user_id, read_status, id DESC);


-- ============================================
-- 验证索引使用情况
-- 使用 EXPLAIN 分析慢查询，确认索引是否被正确使用
-- ============================================
-- 示例:
-- EXPLAIN SELECT * FROM sys_user WHERE is_del = 0 AND id < 100 ORDER BY id DESC LIMIT 10;
-- EXPLAIN SELECT * FROM mall_product WHERE is_del = 0 AND category_id = 1 ORDER BY id DESC LIMIT 10;
