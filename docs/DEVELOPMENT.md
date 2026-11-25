# MineASM Development Guide

This document walks new contributors through the project structure, core concepts,
and step-by-step recipes for the most common extension tasks.

For build commands and toolchain setup see [BUILDING.md](BUILDING.md); for the
supported version matrix see [COMPATIBILITY.md](COMPATIBILITY.md); for the native
ABI contract see [ARCHITECTURES.md](ARCHITECTURES.md).

## 1. What this project is

MineASM is a cross-architecture native mod loader:

- **Native layer** (`:native`): hand-written assembly (NASM for x86_64, GAS for
  arm32/arm64) exports three JNI symbols that form a common ABI contract across
  all architectures, compiled into `mineasm_native.dll/.so/.dylib`.
- **Core layer** (`:core`): pure-Java, zero third-party dependencies, bytecode
  target Java 8 so it can run inside both legacy (Java 6/8) and modern (Java 17/21)
  MC runtimes. It detects the host architecture, loads the native component with
  graceful fallback, scans mod Jars, and dispatches each mod to a version adapter.
- **Adapter layer** (`:adapters`): one subproject per MC version generation
  (11 modules, 14 adapters) covering Forge 1.7.x ~ 1.21.x and NeoForge 1.20.1/1.21.1.
  Adapters register via `META-INF/services` and are discovered with ServiceLoader.
- **Packaging layer** (`:dist`): merges core + adapters into an executable fat Jar
  and packages one runnable modpack zip per architecture.

## 2. Repository layout

```
mineasm-loader/
├── settings.gradle                  Includes :native :core :dist :adapters:adapter-*
├── build.gradle                     Global conventions + adapter subproject wiring + buildAll/verifyAll
├── gradle.properties                Toolchain paths, strict mode, JVM config
├── native/
│   ├── build.gradle                 Toolchain probing, NASM/GAS flags, link rules, manifest
│   └── src/asm/{x86_64,arm32,arm64}/loader_entry.*   Assembly sources
├── core/src/main/java/com/mineasm/core/
│   ├── Arch.java                    Arch enum + os.arch detection + magic codes
│   ├── NativeBridge.java            JNI bridge, 3-step library load with fallback
│   ├── ModScanner.java              Jar metadata detection (mcmod.info / mods.toml / neoforge.mods.toml)
│   ├── ModCandidate.java            Scan result value object
│   ├── MetadataType.java            Metadata format enum
│   ├── ModPlatform.java             FORGE / NEOFORGE / FABRIC / UNKNOWN
│   ├── VersionAdapter.java          Adapter contract (interface)
│   ├── AbstractVersionAdapter.java  accepts()/prepareLaunch() shared logic
│   ├── AdapterRegistry.java         ServiceLoader registry + match() selection
│   ├── LaunchContext.java           Version/arch/mods + JVM property injection
│   ├── MineasmLoader.java           CLI entry point
│   └── CompatibilityVerifier.java   Synthetic-jar verification for all generations
├── adapters/adapter-1_7 ... adapter-1_21/         One module per MC generation
├── dist/build.gradle                fatJar, per-arch modpack zips, verifyCompatibility
└── docs/                            BUILDING / ARCHITECTURES / COMPATIBILITY / DEVELOPMENT
```

## 3. Environment setup

1. Install a JDK 8 ~ 24 (the build sets `org.gradle.java.home`; change it in
   `gradle.properties` if your default JVM is newer).
2. Optional native toolchains (missing tools simply skip the native target):
   - x86_64: `nasm` + a linker (`gcc`/`clang`/`x86_64-w64-mingw32-gcc`)
   - arm32: `arm-linux-gnueabihf-gcc` (or native `cc`/`clang`/`as` on armhf hosts)
   - arm64: `aarch64-linux-gnu-gcc` (or native `cc`/`clang` on AArch64 hosts)
3. If tools are not on PATH, set them explicitly:

```properties
mineasm.native.x86_64.assembler=C:\\Program Files\\NASM\\nasm.exe
mineasm.native.x86_64.linker=C:\\Program Files\\mingw64\\bin\\x86_64-w64-mingw32-gcc.exe
```

4. Sanity check:

```bash
./gradlew buildAll    # native components + modpacks
./gradlew verifyAll   # must print 12 passed, 0 failed
```

## 4. Core concepts you must know before changing code

### 4.1 Native ABI contract

All three assembly sources export the same symbols (see ARCHITECTURES.md):

| Symbol | Returns |
| --- | --- |
| `Java_com_mineasm_core_NativeBridge_probeArch` | magic: x86_64=`0x3634`, arm32=`0x613332`, arm64=`0x613634` |
| `Java_com_mineasm_core_NativeBridge_probeVersion` | `0x0100` (major<<8 \| minor) |
| `Java_com_mineasm_core_NativeBridge_bootstrap` | sum of LaunchConfig table slot 0 + slot 1 results |

The magic codes are duplicated in `Arch` (Java) and the assembly (native) - keep
them in sync when touching either side.

### 4.2 Native library load order (`NativeBridge.tryLoad`)

1. `<cwd>/native/<arch>/<lib>` (modpack layout)
2. `<cwd>/../native/<arch>/<lib>` (modpack subdirectory)
3. `<cwd>/native/build/native/<arch>/<lib>` (development layout)
4. `System.loadLibrary("mineasm_native")` via `java.library.path`

All failures degrade to the pure-Java pipeline with the reason logged; never let
native unavailability throw.

### 4.3 Metadata detection priority (`ModScanner`)

1. `META-INF/neoforge.mods.toml` -> NEOFORGE
2. `META-INF/mods.toml` -> FORGE
3. `mcmod.info` -> FORGE (legacy)
4. `fabric.mod.json` -> FABRIC (recognized, rejected by Forge-family adapters)
5. none -> UNKNOWN (adapters decide)

Adapters must not accept a candidate whose `metadataType()` differs from their
`expectedMetadata()` - `AbstractVersionAdapter.accepts()` already enforces this.

### 4.4 Adapter selection (`AdapterRegistry.match`)

First pass: platform match + exact MC version match + accepts().
Second pass: platform match + accepts() (in-family fallback, e.g. a 1.7.2 mod in
a 1.7.10 pack).

## 5. Recipe: add a new MC version adapter

Example: adding Forge 1.19.4 support to an existing `adapter-1_19` module, or
creating a brand-new generation module.

1. **Create/reuse the module** (new generation: add it to `settings.gradle`):

```
adapters/adapter-1_22/build.gradle                    # header comment + java-library plugin
adapters/adapter-1_22/src/main/java/com/mineasm/adapter/v122/Forge122Adapter.java
adapters/adapter-1_22/src/main/resources/META-INF/services/com.mineasm.core.VersionAdapter
```

The root `build.gradle` automatically applies `java-library` and the `:core`
dependency to every project named `adapter-*`.

2. **Implement the adapter** by extending `AbstractVersionAdapter`:

```java
public class Forge122Adapter extends AbstractVersionAdapter {
    private static final Set<String> VERSIONS = new LinkedHashSet<>(Arrays.asList("1.22"));

    @Override public String id() { return "forge-1.22"; }
    @Override public String mcFamily() { return "1.22.x"; }
    @Override public Set<String> supportedMcVersions() { return VERSIONS; }
    @Override public ModPlatform platform() { return ModPlatform.FORGE; }
    @Override public int requiredJavaMajor() { return 21; }
    @Override public MetadataType expectedMetadata() { return MetadataType.MODS_TOML; }

    @Override
    protected void applyLaunchProperties(LaunchContext context) {
        context.setJvmProperty("mineasm.adapter.family", "1.22");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
        // generation-specific FML/NeoForge loading flags go here
    }
}
```

3. **Register the service** - `META-INF/services/com.mineasm.core.VersionAdapter`
   must contain the fully-qualified class name, one per line:

```
com.mineasm.adapter.v122.Forge122Adapter
```

4. **Add a verify case** in `CompatibilityVerifier.verifyAll()`:

```java
{ "1.22", "META-INF/mods.toml" },
```

5. **Update docs**: append a row to the table in `docs/COMPATIBILITY.md`, bump the
   `Supported-Versions` Jar manifest attribute in `dist/build.gradle`, and add the
   version to the module list in `README.md` if it is a new generation.

6. **Verify**: `./gradlew verifyAll` must report the new case PASS.

## 6. Recipe: add a new target architecture

1. **Write the assembly source** in `native/src/asm/<arch>/loader_entry.<ext>`,
   implementing the three JNI symbols plus the `mineasm_invoke_main` trampoline.
   Pick a fresh architecture magic (avoid `0x3634`/`0x613332`/`0x613634`).
2. **Register it** in `archConfigs` in `native/build.gradle`: assembler candidates,
   flags, `srcDir`/`srcInclude`, link candidates, `libName`, `linkerFlags`.
3. **Add the tasks** to the aggregate: `buildAllArchs.dependsOn` gains
   `link<Arch>Lib` (the `archConfigs.each` loop already wires compile/link per arch).
4. **Add the Java enum value** in `Arch` with the matching magic code and
   `os.arch` fragments, plus the loading directory in `NativeBridge` if needed.
5. **Add the arch to `dist/build.gradle`** (`def arches = [...]`) so a modpack zip
   is produced.
6. **Update docs** (`BUILDING.md`, `ARCHITECTURES.md`).

## 7. Recipe: extend metadata support (e.g. new platform)

1. Add an enum value to `MetadataType` with the Jar entry path.
2. Add the platform to `ModPlatform` if needed.
3. Teach `ModScanner.scanZip` the entry path and parsing (regex-only parsing - the
   core stays dependency-free).
4. Update `AbstractVersionAdapter.accepts()` rules if the format interacts with
   existing ones.
5. Add a verify case + doc row as in section 5.

## 8. Verification and release workflow

```bash
./gradlew verifyAll          # 12 generation cases must pass
./gradlew buildAll           # native components + three modpack zips
./gradlew :dist:fatJar run   # manual smoke test (run is not wired; use java -jar)
java -jar dist/build/libs/mineasm-loader-1.0.0.jar --list-adapters
java -jar dist/build/libs/mineasm-loader-1.0.0.jar --mc-version 1.20.1 --arch x86_64
```

Artifacts:

- `dist/build/distributions/mineasm-modpack-<arch>-<version>.zip`
- `native/build/native/<arch>/` + `native-manifest.json`
- `native/build/native/native-manifest.json` records per-arch `built` status -
  check it when a native target unexpectedly did not build.

## 9. Coding conventions

- **Language**: all comments, log messages and docs in English.
- **Bytecode**: Java 8 target everywhere (no records, no `var`, no `List.of`).
  Use explicit generics and `LinkedHashSet`/`Collections.singleton` for version sets.
- **Dependencies**: `:core` and adapters stay zero-dependency (JDK only). The
  metadata parsers use plain regex on purpose.
- **Failure policy**: never throw from optional paths - native absence degrades
  to pure-Java; platform/version mismatches fail fast in `accepts()`/
  `prepareLaunch()` with actionable messages.
- **Property keys** injected by adapters are namespaced `mineasm.*`; FML/Forge
  official keys (`fml.*`, tweak class names) are injected verbatim.
- **Windows paths in gradle.properties** use escaped backslashes (`\\`).

## 10. Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| `toolchain missing: ...` but skipping build | Expected when tools are absent; install or set paths in `gradle.properties`. Set `mineasm.native.strict=true` to make it fatal. |
| `Native component: not loaded (loadLibrary: ...)` at runtime | DLL/SO not next to the loader in any of the layouts listed in 4.2; copy it into `native/<arch>/` or extend the layout. |
| `no adapter can load: ...` | Mod platform/metadata does not match any adapter; check the detection priority in 4.3 and the mod's actual entry paths. |
| `requires Java N+` | The generation's Java requirement is not met; run a newer JVM (or set `mineasm.simulated.java.major` in verification contexts only). |
| NASM error `unknown debug format` | win64 output has no named debug format; keep `nasmFlags: ['-g']` for Windows, `-F dwarf` for ELF/Mach-O. |
| Build on JDK 25 fails to start Gradle | Gradle 8.x runs on Java 8~24; point `org.gradle.java.home` at a supported JDK. |
