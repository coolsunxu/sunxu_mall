---
name: hotspot-scan
description: Identify Netty hotspots such as blocking calls on event loop, duplicated lock patterns, large classes, and long methods. Use when optimizing performance or refactoring Netty handlers, processors, or session managers.
---

# Hotspot Scan

## Quick Start
1. Scan Netty-facing packages for blocking calls and lock patterns.
2. Flag classes > 500 lines and methods > 80 lines.
3. Produce a ranked risk list with fix guidance.

## Output Template
Use this format:
```
## Hotspot Scan Report
- Files: [list with tags: BLOCKING, LOCK_DUP, LARGE_CLASS, LONG_METHOD]
- Top risks: [top 5 with short fix guidance]
- Refactor targets: [new class/method extraction suggestions]
```

## Additional Resources
- For detailed checks and patterns, see [reference.md](reference.md)
