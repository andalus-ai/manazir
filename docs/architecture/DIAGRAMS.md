# Manazir architecture diagrams

## Layering and dependency direction
```mermaid
flowchart TB
 P[Dalang / Other Products] --> API[Typed Manazir Public API]
 API --> REG[Capability Provider Registry]
 REG --> SPI[Backend SPI]
 SPI --> FFM[Native FFM backend]
 SPI --> PROC[Optional process backend]
 FFM --> GEN[Generated jextract bindings]
 FFM --> LIB[(FFmpeg shared libraries)]
 PROC --> BIN[FFmpeg / FFprobe executables]
```

## Capability-based provider selection
```mermaid
flowchart TD
 R[Explicit provider registrations] --> D[Provider descriptors]
 D --> C{Supports requested capability?}
 C -->|No| X[Reject with diagnostic]
 C -->|Yes| A{Availability}
 A -->|AVAILABLE| S[Eligible candidate]
 A -->|UNKNOWN| U[Do not auto-select]
 A -->|UNAVAILABLE| N[Reject candidate]
 S --> P[Sort by priority, then provider ID]
 P --> Q{Preferred provider selected?}
 Q -->|Yes| O[Return selected provider]
 Q -->|No| F{Fallback explicitly allowed?}
 F -->|No| E[Fail strict selection]
 F -->|Yes| O
```

## Native runtime lifecycle
```mermaid
stateDiagram-v2
 [*] --> NotInitialized
 NotInitialized --> Loading: Explicit initialize(config)
 Loading --> LoadFailed: OS loader failure
 Loading --> LoadedUnverified: Libraries loaded
 LoadedUnverified --> VerificationFailed: Version/symbol mismatch
 LoadedUnverified --> Verified: Required versions and symbols match
 Verified --> Ready: Publish verified capabilities
 LoadFailed --> NotInitialized: Reconfigure/retry
 VerificationFailed --> NotInitialized: Correct runtime and retry
```

## Probe and extraction sequence
```mermaid
sequenceDiagram
 participant Client
 participant API as Typed Media API
 participant Registry as Provider Registry
 participant Backend
 participant FFmpeg
 Client->>API: probe(source, options)
 API->>Registry: select(MEDIA_PROBE, policy)
 Registry-->>API: selected provider + diagnostics
 API->>Backend: typed probe request
 Backend->>FFmpeg: inspect local source
 FFmpeg-->>Backend: container and stream metadata
 Backend-->>API: structured probe report
 API-->>Client: report with stream timebases
 Client->>API: extract(frame request)
 API->>Registry: select(VIDEO_DECODE, policy)
 Registry-->>API: selected provider + diagnostics
 API->>Backend: typed extraction request
 Backend-->>API: frame artifact + requested timestamp
 API-->>Client: extraction result
```

## Ownership and memory lifecycle
```mermaid
flowchart LR
 Call[Public API call] --> Adapter[Backend adapter]
 Adapter --> Arena[Scoped native arena / owner]
 Arena --> Segment[FFM memory and native handles]
 Segment --> Copy[Copy to owned Java data or scoped result]
 Copy --> Result[Public result with explicit lifetime]
 Arena --> Close[Deterministic close]
```
Native segments never escape their owner lifetime. Public APIs expose owned data or explicit closeable resources.
