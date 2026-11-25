# Building Manazir

## Requirements

- JDK 22 or newer. The source baseline is Java 22 because the generated bindings use the finalized Foreign Function & Memory API.
- Maven 3.9 or newer.

The build is verified in CI on Temurin JDK 22 (the minimum source baseline) and JDK 25 (a current LTS runtime). This is a Java build compatibility check, not yet a supported native deployment matrix.

## Build and test

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

The default build compiles the checked-in generated FFM declarations and runs unit tests without requiring FFmpeg native libraries. Native integration tests must use a separate opt-in profile after the FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix are pinned.

CI runs the same Maven verification command for pull requests and pushes to `main`. It uses a JDK matrix (22 and 25), Maven dependency caching, and read-only repository permissions.
The generated FFM binding snapshot targets the Java Foreign Function & Memory API. The Maven build declares Java 22 as its compiler release. This remains a provisional source/build baseline, not a supported native deployment matrix.

## Build and test

~~~bash
mvn test
~~~

The timestamp, runtime, and inspector contract tests use injected dependencies and do not require FFmpeg native libraries.

## Native runtime initialization and verification

Native loading is explicit:

~~~java
var loaded = NativeRuntime.system()
    .initialize(NativeRuntimeConfig.ffmpegDefaults());

var verification = new NativeRuntimeVerifier(new FfmpegNativeLibraryInspector())
    .verify(loaded);
~~~

The concrete inspector uses Java 22 FFM SymbolLookup.loaderLookup() to resolve FFmpeg symbols from libraries already loaded into the process. It queries each component's version function and checks selected stable API symbol groups. It must run after NativeRuntime.initialize.

A VERIFIED decode/encode/resample/scale/filter/probe/mux assessment means the relevant component API symbols resolved. It does not mean a particular codec, container, pixel format, hardware device, or media operation has been exercised successfully. Hardware acceleration and per-codec availability remain UNKNOWN and require runtime enumeration or an execution probe.

## Opt-in native integration test

The FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix remain to be pinned. Native integration tests remain out of the default CI path until that matrix is established.
On a machine with FFmpeg shared libraries installed:

~~~bash
mvn -Pnative-integration test
~~~

The profile enables native access for the test JVM and activates the native integration test. If libavutil is not installed, the test is skipped by an assumption. The normal mvn test path does not activate native integration.

## Generated binding status

The committed generated Java source currently contains the tech.kayys.manazir.avutil package only. It does not expose the FFmpeg component version entry points used by the inspector. For that reason, the inspector uses a narrow FFM symbol-access adapter rather than pretending that the existing generated classes provide FFmpeg API coverage.

The FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix remain to be pinned. Generated source provenance and reproducibility must be resolved before treating those bindings as the supported FFmpeg ABI surface.
