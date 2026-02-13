---
name: session-lifecycle-refactor
description: Refactor large MQTT session lifecycle logic into smaller services, deduplicate locks, and remove event-loop blocking calls. Use when working on SessionLifecycleManager or related session flow changes.
---

# Session Lifecycle Refactor

## Quick Start
1. Split session lifecycle responsibilities into focused services.
2. Extract a lock template to remove duplicate tryLock/unlock blocks.
3. Replace blocking retries with eventLoop scheduling or executor.

## Output Template
```
## Session Lifecycle Refactor Plan
- Split plan: [new classes + moved methods]
- Lock template: [methods to migrate]
- Non-blocking changes: [locations + approach]
```

## Additional Resources
- For detailed split and checks, see [reference.md](reference.md)
