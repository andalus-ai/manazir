# Manazir architecture diagrams

## Layering and dependency direction
```mermaid
flowchart TB
 P[Dalang / Other Products] --> API[Typed Manazir Public API]
 API --> SPI[Backend SPI]
 SPI --> FFM[Native FFM backend]
 SPI --> PROC[Optional process backend]
 FFM --> GEN[Generated jextract bindings]
 FFM --> LIB[(FFmpeg shared libraries)]
 PROC --> BIN[FFmpeg executable]
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
 participant Runtime as Runtime/Capabilities
 participant Backend
 participant FFmpeg
 Client->>API: probe(source, options)
 API->>Runtime: require(PROBE)
 Runtime-->>API: verified capability
 API->>Backend: typed probe request
 Backend->>FFmpeg: open input / inspect streams
 FFmpeg-->>Backend: container and stream metadata
 Backend-->>API: structured probe report
 API-->>Client: report with stream timebases
 Client->>API: extract(range, options)
 API->>Runtime: require(DECODE/EXTRACT)
 API->>Backend: timestamp-preserving extraction
 Backend-->>API: artifacts + source PTS/timebases
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