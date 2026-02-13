---
name: storage-metrics-config-build
description: Standardize Redis storage services, simplify metrics registration, strengthen config validation, and document mvnd build acceleration. Use when touching storage, metrics, configuration, or build performance.
---

# Storage, Metrics, Config, Build Optimization

## Quick Start
1. 新增 Redis 存储服务时，key 前缀必须使用 `RedisKeyPrefix` 常量（禁止硬编码字符串）。
2. 新增指标时，按领域放入对应的 `*MetricConfiguration` 类。
3. 新增配置项时，加上 `@NotNull`/`@Min`/`@Max` 验证注解。
4. 编译使用 `mvnd compile -T 1C` 加速。

## 已实现的模式
- **`RedisKeyPrefix` 常量类**：集中管理所有 Redis key 前缀，防止拼写错误
- **`RMap` 泛型一致性**：`put()`/`get()`/`remove()` 的泛型参数必须一致
- **Metric 领域拆分**：`ConnectionMetricConfiguration`、`MqttPacketMetricConfiguration`、`KafkaMetricConfiguration`、`NettyMetricConfiguration`
- **`@Validated` + `@PostConstruct` 启动自检**：`ServerConfig` 启动时打印配置摘要

## Output Template
```
## Storage/Metrics/Config/Build Recommendations
- Redis template: [base methods + prefix catalog]
- Metrics simplification: [registry/builder proposal]
- Config validation: [constraints + self-check list]
- mvnd build: [commands + audit steps]
```

## Additional Resources
- 实现细节和代码模板，see [reference.md](reference.md)
