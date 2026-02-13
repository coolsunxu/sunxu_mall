---
name: null-check-convention
description: Enforce type-specific null/empty checking in Java code. Replaces raw == null / != null with Objects.isNull, StringUtils.isBlank, CollectionUtils.isEmpty, Optional, etc. Use when writing or reviewing Java null checks, guard clauses, parameter validation, or any conditional involving null.
---

# Java Null 判断规范

禁止在业务代码中直接使用 `== null` / `!= null`，必须根据变量类型选择对应的判空工具方法。

> **例外**：MyBatis Generator 自动生成的 Entity `equals` / `hashCode` 方法允许保留 `== null`，因为这些方法由代码生成器产出，不属于手写业务代码。

## 类型 → 判空方法速查表

| 变量类型 | 判空（null / 空） | 判非空（非 null / 非空） | 所属包 |
|---|---|---|---|
| **通用对象** | `Objects.isNull(obj)` | `Objects.nonNull(obj)` | `java.util.Objects` |
| **String** | `StringUtils.isBlank(str)` | `StringUtils.isNotBlank(str)` | `org.apache.commons.lang3.StringUtils` |
| **Collection / List / Set** | `CollectionUtils.isEmpty(coll)` | `CollectionUtils.isNotEmpty(coll)` | `org.apache.commons.collections4.CollectionUtils` |
| **Map** | `MapUtils.isEmpty(map)` | `MapUtils.isNotEmpty(map)` | `org.apache.commons.collections4.MapUtils` |
| **数组** | `ArrayUtils.isEmpty(arr)` | `ArrayUtils.isNotEmpty(arr)` | `org.apache.commons.lang3.ArrayUtils` |
| **Optional 包装** | `optional.isEmpty()` | `optional.isPresent()` / `optional.ifPresent(...)` | `java.util.Optional` |

### 重要说明

- **String 判空优先使用 `isBlank`**（同时处理 null、空串、空白字符），只有明确需要区分空白字符串和空串时才用 `isEmpty`。
- **统一使用 Apache Commons**：不要混用 `org.springframework.util.StringUtils` 或 `org.springframework.util.CollectionUtils`，统一使用 `org.apache.commons.lang3` 和 `org.apache.commons.collections4`。
- **禁止 `!Objects.isNull(x)`**：直接使用 `Objects.nonNull(x)`。
- **禁止 `!CollectionUtils.isEmpty(x)`**：直接使用 `CollectionUtils.isNotEmpty(x)`。

## 正确示例

```java
// 通用对象判空
if (Objects.isNull(dto)) {
    throw new BusinessException(ErrorCode.PARAM_ERROR);
}

// String 判空 — 使用 StringUtils.isBlank
if (StringUtils.isBlank(token)) {
    return Result.fail("token 不能为空");
}

// 集合判空 — 使用 CollectionUtils
if (CollectionUtils.isEmpty(orderList)) {
    return Collections.emptyList();
}

// Map 判空
if (MapUtils.isEmpty(paramMap)) {
    return;
}

// Optional 链式调用
Optional.ofNullable(userEntity)
        .map(UserEntity::getRoleId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

// 组合条件：对象非空 + 字符串非空
if (Objects.nonNull(dto.getUserId()) && StringUtils.isNotBlank(dto.getUserName())) {
    fillUser(entity, dto);
}
```

## 错误示例

```java
// BAD: 直接 == null
if (dto == null) { ... }

// GOOD:
if (Objects.isNull(dto)) { ... }

// BAD: 直接 != null 判断字符串
if (token != null && !token.isEmpty()) { ... }

// GOOD:
if (StringUtils.isNotBlank(token)) { ... }

// BAD: 使用 Spring 的 CollectionUtils
if (!org.springframework.util.CollectionUtils.isEmpty(list)) { ... }

// GOOD: 使用 Apache Commons
if (CollectionUtils.isNotEmpty(list)) { ... }

// BAD: 双重否定
if (!Objects.isNull(user)) { ... }

// GOOD:
if (Objects.nonNull(user)) { ... }
```

## 应用场景

1. **方法入参校验**：方法开头的 guard clause 统一使用上述工具方法。
2. **条件分支**：if / 三元表达式中的 null 判断。
3. **Stream 操作**：`filter(Objects::nonNull)` 过滤空元素。
4. **返回值处理**：优先使用 `Optional` 包装可能为 null 的返回值，避免调用方做 null 判断。
