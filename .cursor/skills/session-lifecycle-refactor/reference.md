# Session Lifecycle Refactor Reference

## Primary Target
- `src/main/java/com/example/nettyserver/mqtt/session/impl/SessionLifecycleManagerImpl.java`

## Responsibility Split (Suggested)
- `SessionCreationService` (create, shallow create, reactivate)
- `SessionBindingService` (bind old/new, clean old connection)
- `SessionCleanupService` (deep/shallow remove, cleanup steps)
- `SessionDisconnectService` (normal/abnormal disconnect)
- `WillMessageService` (send will message)
- `MqttPropertiesBuilder` (build mqtt properties)

## Lock Template Extraction
Consolidate `tryLock(timeout)` + `finally unlock` into:
```
<T> T executeWithLock(String clientId, long timeoutMs, String opName, Supplier<T> action)
```
Centralize timeout logging and metrics.

## Event Loop Non-Blocking
- Replace `Thread.sleep` with `eventLoop().schedule(...)`
- Offload blocking IO (Redis/gRPC) to executor

## Static Checks
- Methods over 80 lines
- Blocking calls in event loop
- Lock acquisition inside loop

## Validation Checklist
- Methods < 80 lines
- No blocking APIs in event loop paths
- Lock usage consistent and deduplicated
