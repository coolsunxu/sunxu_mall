# Java 后端开发示例

## 示例 1：新增分页查询接口

**需求**：为商品列表提供游标分页接口。

**步骤**：

1. DTO：已有 `ProductQueryDTO` 继承 `BasePageQuery`（含 `pageNum`、`pageSize`、`cursorToken`）
2. Service：`ProductService` 继承 `BaseService<ProductEntity, ProductQueryDTO>`，实现 `selectListWithLimit`、`selectListByCursorWithLimit`、`extractEntityId`
3. Controller：
```java
@PostMapping("/searchByBidirectionalCursor")
public ResponseCursorEntity<ProductVO> searchByBidirectionalCursor(@RequestBody ProductQueryDTO query) {
    ResponseCursorEntity<ProductEntity> cursorEntity = productService.searchByBidirectionalCursor(query);
    return ResponseCursorEntity.convert(cursorEntity, productStructMapper::toVOList);
}
```
4. Mapper XML：`where ... limit #{limit}` 与 `where ... and id < #{cursorId} order by id desc limit #{limit}`

## 示例 2：添加幂等控制

**需求**：创建商品时防止重复提交。

**方案**：业务唯一键（如 `name+categoryId`）+ Redis 分布式锁（Redisson）或数据库唯一索引。

```java
String lockKey = "product:create:" + DigestUtil.md5Hex(name + categoryId);
RLock lock = redissonClient.getLock(lockKey);
if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
    throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "请勿重复提交");
}
try {
    // 执行创建逻辑
} finally {
    lock.unlock();
}
```

## 示例 3：加缓存与失效策略

**需求**：商品详情读多写少，加本地缓存。

```java
@Cacheable(value = "product:detail", key = "#id", unless = "#result == null")
public ProductDetailDTO findById(Long id) {
    return doFindById(id);
}

// 更新/删除时失效
@CacheEvict(value = "product:detail", key = "#id")
public void updateProduct(Long id, UpdateProductDTO dto) { ... }
```

配置 Caffeine：`mall.task.ip-city.cache.caffeine-ttl-seconds` 参考。

## 示例 4：编写集成测试

```java
@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void findById_whenExists_returnsDetail() {
        ProductDetailDTO dto = productService.findById(1L);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_returnsNull() {
        ProductDetailDTO dto = productService.findById(999999L);
        assertThat(dto).isNull();
    }
}
```

运行：`mvnd -q test`
