# Java 后端开发参考

## 错误码约定

使用 `ErrorCode` 枚举，与 `BusinessException` 配合：

```java
throw new BusinessException(ErrorCode.PRODUCT_NOT_EXIST, "商品不存在");
throw new BusinessException(ErrorCode.PARAMETER_VALIDATION_ERROR, "参数不合法");
throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "版本冲突，请刷新后重试");
```

## Controller 模板

```java
@Tag(name = "模块名", description = "模块描述")
@RestController
@RequestMapping("/v1/资源路径")
public class XxxController {

    private final XxxService xxxService;
    private final XxxStructMapper xxxStructMapper;

    public XxxController(XxxService xxxService, XxxStructMapper xxxStructMapper) {
        this.xxxService = xxxService;
        this.xxxStructMapper = xxxStructMapper;
    }

    @Operation(summary = "接口说明", description = "详细描述")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在")
    })
    @PostMapping
    public ResponseEntity<XxxVO> create(@Valid @RequestBody CreateXxxDTO request) {
        XxxEntity created = xxxService.create(request);
        return ResponseEntity.ok(xxxStructMapper.toVO(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<XxxVO> findById(@PathVariable Long id) {
        XxxEntity entity = xxxService.findById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        return ResponseEntity.ok(xxxStructMapper.toVO(entity));
    }
}
```

## Service 事务边界

- 写操作（insert/update/delete）：`@Transactional(rollbackFor = Exception.class)`
- 多表写：同一 Service 方法内完成，保证原子性
- 只读：不加 `@Transactional`

## 游标分页实现

继承 `BaseService<Entity, QueryDTO>` 并实现：

```java
@Override
protected List<Entity> selectListWithLimit(QueryDTO query, int limit) {
    // SQL: ... WHERE ... LIMIT #{limit}
}

@Override
protected List<Entity> selectListByCursorWithLimit(QueryDTO query, Long cursorId, int limit) {
    // SQL: ... WHERE ... AND id < #{cursorId} ORDER BY id DESC LIMIT #{limit}
}

@Override
protected Long extractEntityId(Entity entity) {
    return entity.getId();
}
```

调用：`searchByBidirectionalCursor(query)`，返回 `ResponseCursorEntity`。

## 缓存策略

- **本地**：Caffeine，TTL 按业务设置（如 1h）
- **分布式**：Redisson，适用于多实例共享
- 更新/删除时主动失效，避免脏读

## 鉴权要点

- 需登录接口：不配置到 `permit-urls`，由 Spring Security 拦截
- 放行接口：在 `mall.security.permit-urls` 或 `NoLoginMap` 中配置
- 当前用户：`SecurityUtil.getCurrentUserId()` / `getCurrentUsername()`

## 日志与监控

- 关键业务：`log.info` 记录 traceId、userId、关键参数
- 异常：由 `GlobalExceptionHandler` 统一打日志
- 慢接口：`mall.performance.enabled` 配合 AOP 记录超过阈值的请求
