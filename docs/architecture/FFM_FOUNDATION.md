# FFM and native runtime foundation

## Layers
1. Generated jextract declarations: ABI-specific implementation only.
2. Native runtime: explicit loading, platform/version/symbol verification, capabilities and lifecycle.
3. Typed media API: stable Java records and service interfaces.
4. Backend adapters: FFM or process-based implementation.
5. Product integrations: Dalang consumes typed APIs only.

## Explicit initialization
Use the explicit NativeRuntime initialization API described in BUILDING.md. Constructing API values, timestamps or reports must not load native code. The current default library order is a load attempt strategy, not an ABI guarantee. In non-fail-fast mode return all load diagnostics; fail-fast mode stops and retains partial report.

## Verification states
OS System.loadLibrary success proves only that a library was loaded by the platform loader. It does not prove generated bindings match the ABI or required symbols exist. Represent loaded-but-unverified separately from verified. Query FFmpeg library versions and required symbols before advertising probe/decode/encode capabilities.

## Memory safety
Never load native code in static initializers. Never expose generated structs, native pointers or FFM segments in public contracts. Scope arenas/handles and callbacks explicitly. Never return memory after its owner closes. Do not claim native libraries can be unloaded safely.

## Backend policy
Keep FFM implementation internal. A process backend may be added for isolation/portability, using argument vectors and typed filter plans. Backend selection must be explicit and capability-aware.