---
name: pipeline-processor-standardization
description: Deduplicate Netty pipeline setup and standardize MQTT processor dependencies and exception handling. Use when modifying pipeline initializers or processor implementations.
---

# Pipeline And Processor Standardization

## Quick Start
1. 检查 Pipeline 子类中是否有重复的 handler 链代码。
2. 使用基类的 `addCommonFrontHandlers()` 和 `addCommonMqttHandlers()` 消除重复。
3. 检查 Processor 构造器参数数量，超过 4 个则创建 `XxxProcessorDependencies` 聚合。
4. 统一 Processor 异常处理（断连/ack 规则）。

## 已实现的模式
- **Pipeline 公共 handler 提取**：`MqttPipelineInitializer` 基类提供 `addCommonFrontHandlers` + `addCommonMqttHandlers`
- **Processor 依赖聚合**：`PingReqProcessorDependencies` 示范了 `@Getter @Component` 聚合模式
- **子类扩展点**：新增 Pipeline 只需 override `configureCustomPipeline()`，在公共方法之间插入自定义 handler

## Output Template
```
## Pipeline/Processor Standardization
- Pipeline merge targets: [classes]
- Shared initializer proposal: [signature + extension points]
- Processor deps refactor: [processors + deps object fields]
- Exception handling: [template + affected processors]
```

## Additional Resources
- 实现细节和代码模板，see [reference.md](reference.md)
