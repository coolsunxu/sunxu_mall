# Netty Memory Leak Detection Reference

## Static Checks
- `ByteBuf`, `CompositeByteBuf`, `ReferenceCounted` usage
- `retain()`/`release()` pairing
- try/finally release on error paths
- `SimpleChannelInboundHandler` vs `ChannelInboundHandlerAdapter`
- Cross-thread handoff requires `retain()` or `copy()`

## Leak-Prone Patterns
- Storing ByteBuf in maps/queues without release policy
- Encoder/decoder failures skipping release
- Composite buffers without releasing components

## Runtime Detection
- Enable leak detector in test/stage:
  - `-Dio.netty.leakDetection.level=advanced`
  - `-Dio.netty.leakDetection.targetRecords=8`
- Use `paranoid` only for short tests

## Direct Memory Observation
- Set `-XX:MaxDirectMemorySize=<size>`
- Monitor allocator metrics and RSS trend

## Validation Checklist
- No leak detector warnings under baseline load
- DirectMemory stabilizes at steady state
