// ---------------------------------------------------------------------------
// MineASM native loader - arm64 (AArch64, AAPCS64) entry module (GAS syntax)
//
// Exported symbols (JNI bridge binding, same ABI contract as other arches):
//   Java_com_mineasm_core_NativeBridge_probeArch    -> w0 = 0x613634 ("a64")
//   Java_com_mineasm_core_NativeBridge_probeVersion -> w0 = 0x0100
//   Java_com_mineasm_core_NativeBridge_bootstrap    -> x0 = config pointer;
//       invokes callback table slots 0/1 and merges the return values.
//
// Build (cross): aarch64-linux-gnu-gcc -c -march=armv8-a \
//                 -o loader_entry.o loader_entry.S
// Build (native Apple Silicon / Linux arm64): same command with the system
// gas/clang integrated assembler.
// ---------------------------------------------------------------------------

.arch armv8-a
.text

.global Java_com_mineasm_core_NativeBridge_probeArch
.type   Java_com_mineasm_core_NativeBridge_probeArch, %function
Java_com_mineasm_core_NativeBridge_probeArch:
    mov     w0, #0x3634                  // "a64" = 0x613634
    orr     w0, w0, #0x610000
    ret

.global Java_com_mineasm_core_NativeBridge_probeVersion
.type   Java_com_mineasm_core_NativeBridge_probeVersion, %function
Java_com_mineasm_core_NativeBridge_probeVersion:
    mov     w0, #0x0100
    ret

.global Java_com_mineasm_core_NativeBridge_bootstrap
.type   Java_com_mineasm_core_NativeBridge_bootstrap, %function
Java_com_mineasm_core_NativeBridge_bootstrap:
    stp     x19, x20, [sp, #-16]!
    cbz     x0, .Lnull_config
    mov     x19, x0                    // x19 = config

    // call config[0] = preparePipeline(config)
    ldr     x1, [x19]                  // callback table slot 0
    cbz     x1, .Lskip0
    mov     x0, x19
    blr     x1
.Lskip0:
    mov     x20, x0                    // x20 = preparePipeline result

    // call config[1] = installTransformers(config+8)
    add     x2, x19, #8
    ldr     x1, [x2]                   // callback table slot 1
    cbz     x1, .Lskip1
    mov     x0, x2
    blr     x1
.Lskip1:
    add     x0, x0, x20                // merge results of both phases
    ldp     x19, x20, [sp], #16
    ret

.Lnull_config:
    mov     x0, #0
    ldp     x19, x20, [sp], #16
    ret

// ---- Internal trampoline: void mineasm_invoke_main(void (*fn)(void*), void* arg) ----
.global mineasm_invoke_main
.type   mineasm_invoke_main, %function
mineasm_invoke_main:
    cbz     x0, .Ldone
    br      x0
.Ldone:
    ret

.section .note.GNU-stack,"",%progbits
