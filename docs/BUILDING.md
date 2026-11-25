# MineASM Build Guide

## Requirements

| Component | Requirement |
| --- | --- |
| JDK | 8 ~ 24 (to run Gradle; project bytecode targets Java 8) |
| Gradle | 8.x (wrapper included, no manual install needed) |
| NASM | x86_64 native component assembly (optional) |
| GAS / cross gcc | arm32: `arm-linux-gnueabihf-gcc`; arm64: `aarch64-linux-gnu-gcc` (optional) |
| Host OS | Windows / Linux / macOS / FreeBSD / OpenBSD (NetBSD follows the BSD model) |

## Quick build

```bash
# Full pipeline: native components (all architectures) + Java loader + three-architecture modpacks
./gradlew buildAll

# Java part only
./gradlew build

# Modpacks only
./gradlew :dist:packageAll

# Full-version compatibility verification
./gradlew verifyAll
```

On Windows use `gradlew.bat`, e.g. `gradlew.bat buildAll`.

## Native toolchain configuration

The build script resolves toolchains in this order:

1. Explicit paths in `gradle.properties`:
   - `mineasm.native.x86_64.assembler` / `mineasm.native.x86_64.linker`
   - `mineasm.native.arm32.assembler` / `mineasm.native.arm32.linker`
   - `mineasm.native.arm64.assembler` / `mineasm.native.arm64.linker`
2. PATH probing (x86_64: `nasm` + gcc/clang; arm32/arm64: cross gcc candidate lists).
3. Nothing found:
   - `mineasm.native.strict=false` (default): skip native compilation for that
     architecture; modpacks still build (pure Java pipeline).
   - `mineasm.native.strict=true`: fail the build, forcing toolchain installation.

### Assembler flags and link rules per architecture

| Architecture | Assembler | Flags | Object | Link |
| --- | --- | --- | --- | --- |
| x86_64 | NASM | `-f win64` (Windows) / `-f elf64` (Linux / FreeBSD / OpenBSD) / `-f macho64` (macOS), `-g [-F dwarf]` | loader_entry.o | `-shared [-fPIC]` -> `mineasm_native.dll` (Win) / `libmineasm_native.so` (Linux/BSD) / `libmineasm_native.dylib` (macOS) |
| arm32 | GAS (arm-linux-gnueabihf-gcc; `cc`/`clang`/`as` on native armhf hosts) | `-c -marm -mfpu=neon -g` (ARMv7-A / AAPCS) | loader_entry.o | `-shared` -> `libmineasm_native.so` |
| arm64 | GAS (aarch64-linux-gnu-gcc; `cc`/`clang` on native AArch64 hosts) | `-c -march=armv8-a -g` (ARMv8-A / AAPCS64) | loader_entry.o | `-shared` -> `libmineasm_native.so` |

Linker candidates by host OS: Windows -> `x86_64-w64-mingw32-gcc`/`gcc`/`clang`;
macOS -> `clang`; FreeBSD / OpenBSD -> `cc`/`clang`/`gcc`; Linux -> `gcc`/`clang`.

## Build output locations

```
native/build/native/<arch>/        Native shared libraries and object files per architecture
native/build/native/native-manifest.json   Native component manifest
core/build/libs/                   Loader core Jar
dist/build/libs/                   mineasm-loader fat Jar (core + all adapters)
dist/build/distributions/          mineasm-modpack-<arch>-<version>.zip x 3
```

## Modpack structure

```
mineasm-modpack-<arch>-<version>.zip
├── loader/mineasm-loader-<version>.jar
├── native/<arch>/mineasm_native.dll | libmineasm_native.so   (when toolchain available)
├── native/native-manifest.json
├── mods/                          Mod placement directory
├── launch-<arch>.sh / .bat
├── README.txt
├── COMPATIBILITY.md
└── ARCHITECTURES.md
```

## CI / cross-compilation tips

- In an x86_64 Linux container, `apt install nasm gcc` produces all x86_64 components.
- On FreeBSD / OpenBSD, `pkg install nasm` (or `pkg_add nasm` on OpenBSD) plus the
  base `cc` produces all x86_64 components; both systems are ELF64, matching the
  Linux build rules.
- On macOS (Apple Silicon), the system clang assembles and links the arm64 component
  natively.
- For arm32/arm64, build on the matching host or install the `gcc-arm-linux-gnueabihf`
  / `gcc-aarch64-linux-gnu` cross toolchains.
- A missing toolchain on any architecture never blocks the other architectures or
  the modpack build (unless strict mode is enabled).
