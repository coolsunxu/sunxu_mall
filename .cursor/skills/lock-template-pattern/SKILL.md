---
name: lock-template-pattern
description: Standardize ReentrantReadWriteLock and ReentrantLock usage via LockTemplate utility and session-lock template methods. Use when refactoring duplicated lock/unlock boilerplate, replacing Thread.sleep with non-blocking alternatives, or adding concurrency control to new code.
---

# Lock Template Pattern

## Quick Start

1. 检查目标类中是否存在重复的 `lock()/unlock()` try-finally 块。
2. 对 `ReentrantReadWriteLock`：使用 `LockTemplate` 工具类替换。
3. 对 `ReentrantLock.tryLock()`：提取 `executeWithXxxLock()` 模板方法。
4. 将 `Thread.sleep()` 替换为 `LockSupport.parkNanos()`（尤其是 EventLoop 线程中）。

## Pattern 1: LockTemplate 工具类

位置：`com.example.nettyserver.util.LockTemplate`

提供四个静态方法，封装 `ReentrantReadWriteLock` 的加锁/解锁：

| 方法 | 锁类型 | 返回值 | 适用场景 |
|---|---|---|---|
| `readOp(lock, supplier)` | 读锁 | `T` | 查询操作，如 `getClients()` |
| `readRun(lock, runnable)` | 读锁 | void | 只读遍历，如 `isSubscribed()` |
| `writeOp(lock, supplier)` | 写锁 | `T` | 写入并返回结果 |
| `writeRun(lock, runnable)` | 写锁 | void | 纯写入，如 `add()`、`remove()` |

**重构示例（Before/After）**：

```java
// Before: 手动 lock/unlock，重复出现 10+ 次
readWriteLock.readLock().lock();
try {
    return doMatch(topicFilter);
} finally {
    readWriteLock.readLock().unlock();
}

// After: 一行搞定
return LockTemplate.readOp(readWriteLock, () -> doMatch(topicFilter));
```

## Pattern 2: executeWithSessionLock 模板方法

适用于 `ReentrantLock.tryLock()` + 固定清理逻辑的场景：

```java
private void executeWithSessionLock(String clientId, Channel channel,
        String action, Consumer<Boolean> operation) {
    ReentrantLock sessionLock = storageManager.getOrCreateSessionLock(clientId);
    boolean locked = false;
    try {
        locked = sessionLock.tryLock(WAITING_FOR_SESSION_MS, TimeUnit.MILLISECONDS);
        if (!locked) { log.warn("acquire lock timeout"); }
        operation.accept(locked);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        cutdownConnection(channel);
        storageManager.removeSessionLock(clientId);
        if (locked) { sessionLock.unlock(); }
    }
}
```

**要点**：
- `finally` 块中统一执行清理（断连、移除锁、释放锁）
- 调用方只需关注业务逻辑：`executeWithSessionLock(clientId, ch, "bind", locked -> doBind(...))`

## Pattern 3: 非阻塞等待替代 Thread.sleep

```java
// Before: 阻塞 EventLoop 线程
Thread.sleep(RETRY_DELAY_MS * attempt);

// After: 非阻塞等待
LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(RETRY_DELAY_MS * attempt));
if (Thread.interrupted()) { break; }
```

## 应用检查清单

- [ ] `readWriteLock.readLock().lock()` / `writeLock().lock()` 全部替换为 `LockTemplate`
- [ ] 写锁内的 `synchronized` 块是否冗余（写锁已保证互斥）
- [ ] `ReentrantLock.tryLock()` 是否有重复的 try-finally 模式可提取
- [ ] `Thread.sleep()` 是否出现在 Netty EventLoop 线程中
- [ ] 所有 `finally` 块是否按正确顺序执行（先业务清理、再释放锁）

## Additional Resources
- 实现代码：`src/main/java/com/example/nettyserver/util/LockTemplate.java`
- 应用案例：`SubscriptionTree.java`、`SessionLifecycleManagerImpl.java`
