# MineASM Architecture Adaptation Guide

The MineASM native loader components implement the JNI entry points for three CPU
architectures directly in assembly language, sharing one ABI contract:

| Symbol | Purpose | Return |
| --- | --- | --- |
| `Java_com_mineasm_core_NativeBridge_probeArch` | Architecture probe | magic: x86_64=`0x3634`, arm32=`0x613332`, arm64=`0x613634` |
| `Java_com_mineasm_core_NativeBridge_probeVersion` | ABI version | `0x0100` (major<<8 \| minor) |
| `Java_com_mineasm_core_NativeBridge_bootstrap` | Bootstrap stub | invokes LaunchConfig table slot 0 (preparePipeline) and slot 1 (installTransformers), returns the combined result |

## x86_64 (NASM)

- Source: `native/src/asm/x86_64/loader_entry.asm`
- Syntax: Intel / NASM, `default rel` RIP-relative addressing
- Output format auto-switches with host OS: `win64` (Windows) / `elf64`
  (Linux, FreeBSD, OpenBSD - all ELF64) / `macho64` (macOS)
- Calling conventions: supports both Windows x64 (rcx/rdx) and SysV (rdi/rsi) -
  the bootstrap stub adapts registers internally
- Internal trampoline `mineasm_invoke_main`: generic function-pointer call

## arm32 (GAS, ARMv7-A)

- Source: `native/src/asm/arm32/loader_entry.S`
- `.syntax unified` + `.arm`, hard-float NEON (`-mfpu=neon`)
- AAPCS calling convention, `blx r1` indirect calls into the function table
- `bx lr` tail returns, `push/pop {r4,r5,lr|pc}` frame save

## arm64 (GAS, ARMv8-A / AArch64)

- Source: `native/src/asm/arm64/loader_entry.S`
- AAPCS64 calling convention, `stp/ldp x19,x20` frame save
- `blr x1` indirect calls, `cbz` null-pointer guards
- Apple Silicon compatible (on macOS the clang integrated assembler handles the
  same `.S` file)

## Java-side fallback strategy

`com.mineasm.core.NativeBridge` loads the native component in this order:

1. `<modpack root>/native/<arch>/<lib>` (modpack layout)
2. `../native/<arch>/<lib>` (development layout)
3. Regular `java.library.path` resolution (`System.loadLibrary("mineasm_native")`)

If all fail, `isAvailable()` is false and the loader degrades to a pure Java
pipeline with the reason logged at startup; mod loading is unaffected - only the
native pipeline probe/bootstrap stub is unavailable.

## Architecture - platform matrix

| Architecture | Native component | Verified hosts |
| --- | --- | --- |
| x86_64 | `mineasm_native.dll` (Win) / `libmineasm_native.so` (Linux / FreeBSD / OpenBSD) / `.dylib` (macOS) | Windows x64, Linux x64, FreeBSD/OpenBSD x64 |
| arm32 | `libmineasm_native.so` | Linux armhf (Raspberry Pi etc.) |
| arm64 | `libmineasm_native.so` / `.dylib` | Linux aarch64, macOS Apple Silicon, FreeBSD/OpenBSD arm64 |
