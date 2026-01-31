---
name: netty-dev
description: Guides Netty development for TCP/UDP server and client, codec, heartbeat, WebSocket, SSL/TLS, performance tuning. Enforces Alibaba Java conventions, single-method ≤80 lines, mvnd for build/test. Use when building Netty servers, clients, custom protocols, codec,粘包拆包, heartbeat, IdleStateHandler, EmbeddedChannel tests.
---

# Netty 开发 Skill

## 适用范围
- Netty 4.x
- TCP/UDP 服务端与客户端、自定义协议、WebSocket、SSL/TLS
- 构建/测试：使用 `mvnd`（非 `mvn`）

## 默认工作流

1. **需求澄清**：协议类型、连接模式（长连/短连）、序列化方式、安全要求
2. **影响面扫描**：Handler 链、编解码器、配置、现有测试
3. **分步实现**：Pipeline 组装 → 业务 Handler → 异常处理
4. **自检清单**：见下节
5. **运行测试**：`mvnd -q test`（含 EmbeddedChannel 单测）
6. **总结变更**：列出改动文件与手工测试点

## 强制约束

| 约束 | 说明 |
|------|------|
| 单方法行数 | 不超过 80 行，超则拆方法或抽取类 |
| 代码规范 | 遵循阿里巴巴 Java 开发规约 |
| 构建/测试 | 使用 `mvnd`，不使用 `mvn` |

## 自检清单

### 服务端 / 客户端
- [ ] EventLoopGroup 分离：服务端 bossGroup + workerGroup；客户端单 group
- [ ] 优雅关闭：`group.shutdownGracefully()`，设置合理的超时
- [ ] Option：`SO_BACKLOG`、`SO_KEEPALIVE`、`TCP_NODELAY` 按需配置

### Pipeline
- [ ] Handler 顺序：入站（解码→业务）→出站（编码），顺序不可颠倒
- [ ] 共享 Handler：无状态且线程安全时加 `@Sharable`，否则每次新建
- [ ] 异常传播：`ChannelHandlerContext.fireExceptionCaught` 或自定义异常 Handler

### 编解码
- [ ] 粘包拆包：使用 `LengthFieldBasedFrameDecoder`、`DelimiterBasedFrameDecoder` 等
- [ ] ByteBuf 释放：业务 Handler 中 `ReferenceCountUtil.release(msg)` 或 `try-finally`
- [ ] 解码器：继承 `ByteToMessageDecoder`/`ReplayingDecoder`，不吞异常

### 心跳与重连
- [ ] 心跳：`IdleStateHandler` + 读空闲时发心跳包，写空闲时关闭连接或重连
- [ ] 客户端重连：`ChannelFutureListener.CLOSE_ON_FAILURE` 或定时重连任务
- [ ] Pong 响应：WebSocket 需响应 Ping 帧

### 性能
- [ ] 内存池：使用 `PooledByteBufAllocator.DEFAULT`
- [ ] 线程数：`NioEventLoopGroup` 默认 `Runtime.getRuntime().availableProcessors() * 2`，按负载调整
- [ ] 水位线：`ChannelOption.WRITE_BUFFER_WATER_MARK` 防止写堆积

### 安全（SSL/TLS）
- [ ] SslContext：使用 `SslContextBuilder` 配置证书、密钥
- [ ] Pipeline 位置：SSL Handler 应放在 Pipeline 最前面（靠近 Channel）
- [ ] 证书校验：生产环境启用服务端/客户端证书校验

### 测试
- [ ] EmbeddedChannel：验证编解码、Handler 逻辑，`checkException()` 检查异常
- [ ] 执行：`mvnd -q test`

## 常用技术选型

| 场景 | 选型 |
|------|------|
| 粘包拆包 | `LengthFieldBasedFrameDecoder`（定长头+长度域）、`DelimiterBasedFrameDecoder`（分隔符） |
| 序列化 | Protobuf、JSON、自定义二进制 |
| 心跳 | `IdleStateHandler` + 自定义 HeartbeatHandler |
| WebSocket | `WebSocketServerProtocolHandler`、`WebSocketFrameAggregator` |
| SSL | `SslHandler`（`SslContext.newHandler`） |
| 内存 | `PooledByteBufAllocator`，避免 `Unpooled` 大量分配 |

## 参考文档

- 规范与模板：[reference.md](reference.md)
- 示例场景：[examples.md](examples.md)
