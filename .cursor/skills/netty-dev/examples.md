# Netty 开发示例

## 示例 1：TCP 服务端

```java
public class TcpServer {
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TcpServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                           .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4))
                           .addLast(new SimpleDecoder())
                           .addLast(new SimpleEncoder())
                           .addLast(new EchoServerHandler());
                    }
                });
        bootstrap.bind(port).sync();
    }

    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}
```

## 示例 2：客户端连接池（简化）

```java
public class NettyClientPool {
    private final Bootstrap bootstrap;
    private final String host;
    private final int port;
    private final ArrayBlockingQueue<Channel> pool = new ArrayBlockingQueue<>(10);

    public NettyClientPool(String host, int port) {
        this.host = host;
        this.port = port;
        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new YourDecoder(), new YourEncoder(), new YourHandler());
                    }
                });
    }

    public Channel acquire() throws InterruptedException {
        Channel ch = pool.poll();
        if (ch != null && ch.isActive()) return ch;
        return bootstrap.connect(host, port).sync().channel();
    }

    public void release(Channel ch) {
        if (ch != null && ch.isActive()) pool.offer(ch);
    }
}
```

## 示例 3：自定义协议编解码

协议：4 字节长度（含自身）+ 1 字节类型 + 正文

```java
public class CustomDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) return;
        in.markReaderIndex();
        int len = in.readInt();
        if (in.readableBytes() < len - 4) {
            in.resetReaderIndex();
            return;
        }
        byte type = in.readByte();
        byte[] payload = new byte[len - 5];
        in.readBytes(payload);
        out.add(new CustomMessage(type, payload));
    }
}

public class CustomEncoder extends MessageToByteEncoder<CustomMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, CustomMessage msg, ByteBuf out) {
        int len = 5 + msg.getPayload().length;
        out.writeInt(len);
        out.writeByte(msg.getType());
        out.writeBytes(msg.getPayload());
    }
}
```

## 示例 4：心跳检测

```java
@Sharable
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final ByteBuf HEARTBEAT = Unpooled.unreleasableBuffer(
            Unpooled.copiedBuffer("PING\n", CharsetUtil.UTF_8));

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.writeAndFlush(HEARTBEAT.duplicate()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }
        ctx.fireUserEventTriggered(evt);
    }
}
```

Pipeline：`IdleStateHandler(0, 30, 0)` 表示 30 秒写空闲触发，HeartbeatHandler 发 PING。

## 示例 5：EmbeddedChannel 单元测试

```java
@Test
void decoder_decodesValidFrame() {
    EmbeddedChannel channel = new EmbeddedChannel(
            new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4),
            new SimpleDecoder());
    ByteBuf buf = Unpooled.buffer();
    buf.writeInt(8);
    buf.writeBytes("hello".getBytes(StandardCharsets.UTF_8));
    channel.writeInbound(buf);
    Object msg = channel.readInbound();
    assertThat(msg).isNotNull();
    channel.checkException();
}
```
