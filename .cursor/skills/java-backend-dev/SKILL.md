---
name: java-backend-dev
description: Guides Java backend development with Spring Boot, MyBatis, Spring Security, Redisson/Caffeine. Enforces Alibaba Java conventions, single-method ≤80 lines, mvnd for build/test. Use when adding APIs, fixing bugs, refactoring, performance tuning, adding cache/auth, or writing tests.
---

# Java 后端开发 Skill

## 适用范围
- Java 11 + Spring Boot 2.6.x
- MyBatis、Druid、Spring Security、JJWT、Redisson、Caffeine
- 构建/测试：使用 `mvnd`（非 `mvn`）

## 默认工作流

1. **需求澄清**：确认接口语义、权限、分页/游标、幂等、缓存需求
2. **影响面扫描**：Controller、Service、Mapper、DTO、配置、现有测试
3. **方案选择**：事务边界、分页方式（游标/页码）、缓存层、鉴权策略
4. **分步实现**：每步小改动，优先复用已有 DTO/Entity/VO
5. **自检清单**：见下节
6. **运行测试**：`mvnd -q test`（或按模块）
7. **总结变更**：列出改动文件与手工测试点

## 强制约束

| 约束 | 说明 |
|------|------|
| 单方法行数 | 不超过 80 行，超则拆方法或抽取类 |
| 代码规范 | 遵循阿里巴巴 Java 开发规约 |
| 构建/测试 | 使用 `mvnd`，不使用 `mvn` |

## 自检清单

### 代码规范（阿里规约）
- [ ] 命名：驼峰、常量全大写下划线、包小写
- [ ] 空值：`Objects.isNull`/`Objects.nonNull`，禁止裸 `== null`
- [ ] 集合：使用 `CollectionUtils.isEmpty/isNotEmpty` 判断
- [ ] 魔法值：抽取为常量或枚举
- [ ] 注释：类、public 方法有 Javadoc

### Controller 层
- [ ] 入参：`@Valid` + DTO 校验注解（`@NotBlank`、`@NotNull` 等）
- [ ] 出参：使用 `ResponseEntity` 或统一 `ApiResult`/VO
- [ ] 异常：交由 `GlobalExceptionHandler`，抛出 `BusinessException(ErrorCode, msg)`
- [ ] API 文档：`@Tag`、`@Operation`、`@ApiResponses`

### Service 层
- [ ] 事务：写操作加 `@Transactional`，只读不加
- [ ] 业务校验：参数不合法抛 `BusinessException`
- [ ] 分页：优先游标分页（`ResponseCursorEntity`），继承 `BaseService` 时实现抽象方法
- [ ] 单方法 ≤80 行

### Mapper / SQL
- [ ] 参数：使用 `#{}`，禁止 `${}` 拼接（防 SQL 注入）
- [ ] 分页：显式 `limit #{limit}`，游标用 `where id < #{cursorId} order by id desc`
- [ ] 大表：避免 `SELECT *`，按需列；关注慢 SQL（Druid 已配置慢查询日志）

### 安全
- [ ] 鉴权：需登录接口配置到 Spring Security，放行列表参考 `mall.security.permit-urls`
- [ ] 越权：按 userId/tenantId 过滤，禁止跨用户查改
- [ ] 敏感信息：日志不输出密码、token；生产环境 token/密钥走环境变量

### 性能
- [ ] 批量：避免循环单条 insert/update，使用 batch
- [ ] 缓存：读多写少用 Caffeine/Redisson，注明 TTL 与失效策略
- [ ] 接口慢阈值：`mall.performance.slow-api-threshold-ms`（默认 1000ms）

### 测试
- [ ] 单元测试：JUnit5 + Mockito，Mock 外部依赖
- [ ] 集成测试：`@SpringBootTest` + `@Transactional` 回滚
- [ ] 执行：`mvnd -q test`

## 常用技术选型

| 场景 | 选型 |
|------|------|
| 分页 | 游标分页 `ResponseCursorEntity`（大表/深分页）；传统页码用 `ResponsePageEntity` |
| 参数校验 | `@Valid` + `spring-boot-starter-validation` |
| 异常 | `BusinessException(ErrorCode.XXX, "message")` |
| 对象转换 | MapStruct 或 BeanCopyUtils |
| 缓存 | Caffeine（本地）、Redisson（分布式） |
| 幂等 | 业务键 + Redis 分布式锁或数据库唯一约束 |
| 重试 | `@Retryable`（spring-retry） |

## 项目约定（sunxu_mall）

- 包结构：`controller.{module}`、`service.{module}`、`mapper.{module}`、`dto`、`entity`、`vo`
- 分页查询：继承 `BaseService`，实现 `selectListWithLimit`、`selectListByCursorWithLimit`、`extractEntityId`
- 错误码：使用 `ErrorCode` 枚举，统一在 `GlobalExceptionHandler` 处理
- API 文档：SpringDoc OpenAPI，路径 `/swagger-ui.html`

## 参考文档

- 规范与模板：[reference.md](reference.md)
- 示例场景：[examples.md](examples.md)
