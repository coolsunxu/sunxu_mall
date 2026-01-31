# Netty 开发参考

## 服务端模板（ServerBootstrap）

```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();
try {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                       .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4))
                       .addLast(new YourDecoder())
                       .addLast(new YourEncoder())
                       .addLast(new YourBusinessHandler());
                }
            });
    ChannelFuture future = bootstrap.bind(port).sync();
    future.channel().closeFuture().sync();
} finally {
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
}
```

## 客户端模板（Bootstrap）

```java
EventLoopGroup group = new NioEventLoopGroup();
try {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                       .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4))
                       .addLast(new YourDecoder())
                       .addLast(new YourEncoder())
                       .addLast(new IdleStateHandler(0, 30, 0))
                       .addLast(new YourHeartbeatHandler())
                       .addLast(new YourBusinessHandler());
                }
            });
    ChannelFuture future = bootstrap.connect(host, port).sync();
    future.channel().closeFuture().sync();
} finally {
    group.shutdownGracefully();
}
```

## ChannelHandler 分层

| 位置 | 职责 |
|------|------|
| 最前（靠近 Channel） | SSL、粘包拆包 |
| 中间 | 编解码（Decoder/Encoder） |
| 其后 | 心跳（IdleStateHandler + 业务心跳） |
| 最后 | 业务 Handler |

入站顺序：Channel → 解码 → 业务；出站顺序：业务 → 编码 → Channel。

## 编解码器选型

- **定长头+长度域**：`LengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip)`
- **分隔符**：`DelimiterBasedFrameDecoder(maxFrameLength, Delimiters.lineDelimiter())`
- **固定长度**：`FixedLengthFrameDecoder(frameLength)`
- **自定义**：继承 `ByteToMessageDecoder`，注意 `out.add(msg)` 后不 release，由后续 Handler 负责

## 粘包拆包示例（LengthFieldBasedFrameDecoder）

协议格式：4 字节长度（含自身）+ 正文

```java
// maxFrameLength=65536, lengthFieldOffset=0, lengthFieldLength=4,
// lengthAdjustment=0（长度域含自身）, initialBytesToStrip=4（跳过长度域）
.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4))
```

## 心跳与 IdleStateHandler

```java
// 读空闲 30 秒触发
.addLast(new IdleStateHandler(30, 0, 0))
.addLast(new HeartbeatHandler())
```

在 `HeartbeatHandler.userEventTriggered` 中处理 `IdleStateEvent`，写心跳包；若多次无响应则关闭连接。

## 重连机制（客户端）

- 连接失败：`ChannelFutureListener` 中 schedule 重连
- 读空闲超时：关闭连接后重新 connect
- 使用指数退避：1s、2s、4s... 限制最大间隔

## 性能参数

- **SO_BACKLOG**：服务端 accept 队列，建议 128~1024
- **WRITE_BUFFER_WATER_MARK**：低水位 32KB，高水位 64KB，防止写堆积
- **ALLOCATOR**：`PooledByteBufAllocator.DEFAULT`
- **EventLoopGroup 线程数**：默认 CPU*2，IO 密集可适当增大

## SSL/TLS 配置

```java
SslContext sslContext = SslContextBuilder.forServer(certChain, key)
        .build();
// Pipeline 最前面
ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()));
```

客户端需 `SslContextBuilder.forClient().trustManager(caCert).build()`，并启用 hostname 校验。
