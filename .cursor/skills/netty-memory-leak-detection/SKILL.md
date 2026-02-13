---
name: netty-memory-leak-detection
description: Detect potential Netty memory leaks related to ByteBuf reference counting and DirectMemory growth. Use when investigating memory growth, leaks, or handler buffer usage.
---

# Netty Memory Leak Detection

## Quick Start
1. Scan for ByteBuf/ReferenceCounted usage and release pairing.
2. Verify handler types and release responsibilities.
3. Enable leak detector for targeted tests and collect stacks.

## Output Template
```
## Netty Memory Leak Report
- High-risk locations: [file/method + pattern]
- Fix guidance: [release ownership, safeRelease, retain/copy rules]
- Verification: [leak detector + metrics]
```

## Additional Resources
- For detailed checks and runtime tips, see [reference.md](reference.md)
