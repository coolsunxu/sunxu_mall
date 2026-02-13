# Hotspot Scan Reference

## Scope
- `src/main/java/com/example/nettyserver/pipeline/`
- `src/main/java/com/example/nettyserver/mqtt/handler/`
- `src/main/java/com/example/nettyserver/mqtt/processor/`
- `src/main/java/com/example/nettyserver/mqtt/session/`
- `src/main/java/com/example/nettyserver/storage/`
- `src/main/java/com/example/nettyserver/config/`

## Static Checks
1. **Blocking calls in event loop**
   - `Thread.sleep`, `TimeUnit.sleep`, `Future.get`, `CompletableFuture.join`
   - Synchronous Redis/gRPC calls inside handlers/processors/sessions

2. **Lock duplication and long hold**
   - Repeated `tryLock(timeout)` + `finally unlock`
   - Lock acquisition inside event loop or retry loops

3. **Large class**
   - Classes over 500 lines
   - Suggest responsibility split

4. **Long method**
   - Methods over 80 lines
   - Suggest extraction of steps

5. **Allocation hotspots (secondary)**
   - Frequent `new` in publish/subscribe path
   - Redis key string concatenation in hot paths

## Recommended Fix Patterns
- Replace sleep/retry with `eventLoop().schedule(...)` or executor
- Extract lock template: `executeWithLock(clientId, timeout, opName, Supplier<T>)`
- Split monolith classes into focused services
- Enforce method length < 80 lines

## Validation Checklist
- No blocking APIs in event loop packages
- Methods under 80 lines
- Lock acquisition metrics/timeouts in place
