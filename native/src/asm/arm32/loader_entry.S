@ ---------------------------------------------------------------------------
@ MineASM native loader - arm32 (ARMv7-A, AAPCS) entry module (GAS syntax)
@
@ Exported symbols (JNI bridge binding, same ABI contract as x86_64):
@   Java_com_mineasm_core_NativeBridge_probeArch    -> r0 = 0x613332 ("a32")
@   Java_com_mineasm_core_NativeBridge_probeVersion -> r0 = 0x0100
@   Java_com_mineasm_core_NativeBridge_bootstrap    -> config pointer (r0);
@       invokes callback table slots 0/1 and merges the return values.
@
@ Build (cross): arm-linux-gnueabihf-gcc -c -marm -mfpu=neon -mfloat-abi=hard \
@                 -o loader_entry.o loader_entry.S
@ Build (native Linux armhf): same command with the system gas.
@ ---------------------------------------------------------------------------

.syntax unified
.arch armv7-a
.arm

.text

.global Java_com_mineasm_core_NativeBridge_probeArch
.type   Java_com_mineasm_core_NativeBridge_probeArch, %function
Java_com_mineasm_core_NativeBridge_probeArch:
    ldr     r0, =0x613332
    bx      lr

.global Java_com_mineasm_core_NativeBridge_probeVersion
.type   Java_com_mineasm_core_NativeBridge_probeVersion, %function
Java_com_mineasm_core_NativeBridge_probeVersion:
    ldr     r0, =0x0100
    bx      lr

.global Java_com_mineasm_core_NativeBridge_bootstrap
.type   Java_com_mineasm_core_NativeBridge_bootstrap, %function
Java_com_mineasm_core_NativeBridge_bootstrap:
    push    {r4, r5, lr}
    cmp     r0, #0
    beq     .Lnull_config
    mov     r4, r0                  @ r4 = config

    @ call config[0] = preparePipeline(config)
    ldr     r1, [r4]                @ callback table slot 0
    cmp     r1, #0
    beq     .Lskip0
    mov     r0, r4
    blx     r1
.Lskip0:
    mov     r5, r0                  @ r5 = preparePipeline result

    @ call config[1] = installTransformers(config+4)
    add     r2, r4, #4
    ldr     r1, [r2]                @ callback table slot 1
    cmp     r1, #0
    beq     .Lskip1
    mov     r0, r2
    blx     r1
.Lskip1:
    add     r0, r0, r5              @ merge results of both phases
    pop     {r4, r5, pc}

.Lnull_config:
    mov     r0, #0
    pop     {r4, r5, pc}

@ ---- Internal trampoline: void mineasm_invoke_main(void (*fn)(void*), void* arg) ----
.global mineasm_invoke_main
.type   mineasm_invoke_main, %function
mineasm_invoke_main:
    cmp     r0, #0
    bxeq    lr
    bx      r0

.section .note.GNU-stack,"",%progbits
