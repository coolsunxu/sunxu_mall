---
name: validation-benchmark
description: Define MQTT broker load-test scenarios and performance regression baselines. Use when evaluating performance changes or creating test plans.
---

# Validation And Benchmark

## Quick Start
1. Define standard load scenarios (connect storm, QoS mix, subscription stress).
2. Capture baseline metrics (latency, throughput, error rate).
3. Set regression thresholds and document results.

## Output Template
```
## Benchmark Plan
- Scenarios: [matrix of clients/rate/duration/QoS]
- Baseline metrics: [p50/p95/p99/throughput/error]
- Regression thresholds: [rules]
```

## Additional Resources
- For detailed checks and scenarios, see [reference.md](reference.md)
