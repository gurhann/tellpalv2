# TellPal Mobile v1 PRD Addendum

## Downstream Architecture Decisions

### Mobile technology choice

Flutter is not a fixed requirement for the rewrite. Flutter and native iOS/Android development may be compared during architecture work against the product requirements, migration risk, delivery time, crash reduction, media/playback reliability, maintainability, testing, and operational cost.

The PRD must define externally observable capabilities and parity expectations without prematurely selecting the implementation technology.

### Image memory and stability investigation

The user reports an approximately 85% crash-free rate and suspects high-resolution images cause excessive RAM usage. Neither the metric definition nor the root cause has been verified. Inspect crash and termination evidence alongside device memory measurements before attributing failures to image resolution or selecting a framework as the remedy.

Evaluate image rendition sizing, decoded-image dimensions, concurrently resident pages, bounded prefetch, cache budgets, and resource release during repeated navigation. These are investigation candidates, not selected implementation mechanisms. Include low-memory supported devices and the heaviest content samples; preserve visual readability. Numerical memory, latency, and crash-free acceptance thresholds remain open.
