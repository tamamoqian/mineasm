# MineASM - Cross-architecture Native Mod Loader

Gradle multi-project build. Native loader components for the x86_64 / arm32 /
arm64 architectures are implemented in assembly language (NASM/GAS), with
Forge / NeoForge 1.7.x ~ 1.21.x Jar mod loading support across the full series.
Supported host OSes: Windows / Linux / macOS / FreeBSD / OpenBSD.

## Module layout

```
mineasm-loader/
├── native/          Native compilation module (NASM x86_64, GAS arm32/arm64,
│                    toolchain probing and link rules)
├── core/            Loader core (arch detection, JNI bridge, Jar scanning,
│                    adapter registry, verifier)
├── adapters/        Version adapter modules (11 subprojects, 14 adapters, 1.7 ~ 1.21)
│   ├── adapter-1_7    Forge 1.7.2 / 1.7.10
│   ├── adapter-1_12   Forge 1.12.2
│   ├── adapter-1_13   Forge 1.13.2
│   ├── adapter-1_14   Forge 1.14.4
│   ├── adapter-1_15   Forge 1.15.2
│   ├── adapter-1_16   Forge 1.16.5
│   ├── adapter-1_17   Forge 1.17.1
│   ├── adapter-1_18   Forge 1.18.2
│   ├── adapter-1_19   Forge 1.19.2
│   ├── adapter-1_20   Forge 1.20.1 + NeoForge 1.20.1
│   └── adapter-1_21   Forge 1.21.1 + NeoForge 1.21.1
├── dist/            Packaging module (fat Jar, three-architecture modpacks,
│                    compatibility verification)
└── docs/            Documentation (BUILDING / ARCHITECTURES / COMPATIBILITY)
```

## Quick start

```bash
./gradlew buildAll     # Native components for all architectures + three modpacks
./gradlew verifyAll    # Full-version compatibility verification (14 cases, 1.7.2 ~ 1.21.1)
./gradlew :dist:packageModpack_x86_64   # Single-architecture modpack
```

Artifacts: `dist/build/distributions/` (modpacks) and `native/build/native/`
(native components).

## Running a modpack

```bash
./launch-x86_64.sh --mc-version 1.20.1   # place Forge mods in the mods/ directory
launch-arm64.bat --mc-version 1.21.1
```

Further reading: [docs/BUILDING.md](docs/BUILDING.md) ·
[docs/ARCHITECTURES.md](docs/ARCHITECTURES.md) ·
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) ·
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)
