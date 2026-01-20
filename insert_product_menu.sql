-- 添加商品管理菜单 SQL
-- 请根据实际情况调整 parent_id (父菜单ID) 和 sort (排序)

INSERT INTO `sys_menu` (
  `pid`, 
  `title`, 
  `name`, 
  `component`, 
  `path`, 
  `icon`, 
  `type`, 
  `is_link`, 
  `sort`, 
  `hidden`, 
  `permission`, 
  `status`, 
  `create_time`, 
  `update_time`
) VALUES (
  0, -- 父菜单ID，0表示顶级菜单，如果需要放在"内容管理"下，请修改为对应ID
  '商品管理', -- 菜单标题
  'Product', -- 路由名称
  'mall/product/index', -- 组件路径 (对应 src/views/mall/product/index.vue)
  'mall/product', -- 路由路径
  'Goods', -- 图标
  1, -- 菜单类型 (0:目录 1:菜单 2:按钮)
  0, -- 是否外链 (0:否 1:是)
  10, -- 排序
  0, -- 是否隐藏 (0:否 1:是)
  'mall:product:list', -- 权限标识
  1, -- 状态 (1:正常 0:停用)
  NOW(), 
  NOW()
);
