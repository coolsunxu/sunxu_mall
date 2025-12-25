## 实现计划

1. **了解 Redis String 操作需求**
   - 基本的设置和获取操作
   - 带过期时间的设置
   - 数值递增/递减
   - 字符串追加
   - 其他常用操作

2. **设计方法签名**
   - 使用 RedissonClient 获取 RString 对象
   - 实现以下核心方法：
     - `set(String key, String value)` - 设置字符串值
     - `get(String key)` - 获取字符串值
     - `set(String key, String value, long expireTime, TimeUnit timeUnit)` - 设置带过期时间的值
     - `incr(String key)` - 递增1
     - `incrBy(String key, long delta)` - 递增指定值
     - `decr(String key)` - 递减1
     - `decrBy(String key, long delta)` - 递减指定值
     - `append(String key, String suffix)` - 追加字符串
     - `strlen(String key)` - 获取字符串长度
     - `exists(String key)` - 检查键是否存在
     - `delete(String key)` - 删除键

3. **实现方法**
   - 在 RedisUtil.java 中添加上述方法
   - 使用 redissonClient.getString(key) 获取 RString 对象
   - 调用 RString 对象的相应方法实现功能
   - 处理可能的异常

4. **添加必要的导入**
   - 导入 java.util.concurrent.TimeUnit
   - 导入 org.redisson.api.RString

5. **确保代码风格一致**
   - 保持与现有代码相同的缩进和命名风格
   - 添加适当的注释

## 预期结果

完成后，RedisUtil 类将具备完整的 Redis String 操作功能，包括设置、获取、过期时间管理、数值增减、字符串追加等常用操作，方便其他组件使用。