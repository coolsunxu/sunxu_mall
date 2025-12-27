-- 测试数据库连接和表结构
USE mall;
SELECT 'Current database: ' AS info, DATABASE() AS db_name;
SELECT 'Current table structure: ' AS info;
DESCRIBE sys_menu;

-- 检查是否有其他sys_menu表
SELECT table_schema, table_name, column_name 
FROM information_schema.columns 
WHERE table_name = 'sys_menu' 
ORDER BY table_schema, table_name, ordinal_position;