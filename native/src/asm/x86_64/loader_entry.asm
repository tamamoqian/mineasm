; ---------------------------------------------------------------------------
; MineASM native loader - x86_64 entry module (NASM syntax)
;
; Exported symbols (bound by the JNI bridge com.mineasm.core.NativeBridge):
;   Java_com_mineasm_core_NativeBridge_probeArch    -> returns architecture magic 0x3634 ("64")
;   Java_com_mineasm_core_NativeBridge_probeVersion -> returns native component ABI version
;   Java_com_mineasm_core_NativeBridge_bootstrap    -> loader bootstrap stub:
;       rdi/rcx = LaunchConfig struct pointer (first field is the Java-side
;       callback table); invokes callback slot 0 (preparePipeline) and slot 1
;       (installTransformers), then returns the combined result to verify the
;       cross-language call chain.
;
; Also provides the internal trampoline mineasm_invoke_main: calls a function
; pointer in a cdecl/SysV-compatible way while preserving registers.
;
; Build (Windows): nasm -f win64 -o loader_entry.o loader_entry.asm
; Build (Linux)  : nasm -f elf64 -o loader_entry.o loader_entry.asm
; ---------------------------------------------------------------------------

default rel

section .text

; ---- Architecture probe: JNI contract returns 0x3634 (the "64" ASCII marker) ----
global Java_com_mineasm_core_NativeBridge_probeArch
Java_com_mineasm_core_NativeBridge_probeArch:
    mov     eax, 0x3634
    ret

; ---- ABI version: major << 8 | minor ----
global Java_com_mineasm_core_NativeBridge_probeVersion
Java_com_mineasm_core_NativeBridge_probeVersion:
    mov     eax, 0x0100
    ret

; ---- Bootstrap stub ----
; Windows x64 calling convention: rcx = config, rdx = reserved
; SysV calling convention:        rdi = config, rsi = reserved
; Under both conventions config lives in the "first integer argument register".
global Java_com_mineasm_core_NativeBridge_bootstrap
Java_com_mineasm_core_NativeBridge_bootstrap:
    push    rbx
    push    r12
    mov     rbx, rdi                ; SysV: config -> rbx
    cmp     rbx, 0
    jne     .have_config
    mov     rbx, rcx                ; Win64: config -> rbx
.have_config:
    test    rbx, rbx
    jz      .null_config

    ; call config[0] = preparePipeline()
    mov     rdi, rbx
    mov     rcx, rbx
    call    [rbx]                   ; callback table slot 0
    mov     r12, rax                ; r12 = preparePipeline result

    ; call config[1] = installTransformers()
    lea     rdi, [rbx + 8]
    lea     rcx, [rbx + 8]
    call    [rbx + 8]               ; callback table slot 1

    add     rax, r12                ; merge results of both phases
    pop     r12
    pop     rbx
    ret

.null_config:
    xor     eax, eax
    pop     r12
    pop     rbx
    ret

; ---- Internal trampoline: void mineasm_invoke_main(void (*fn)(void), void* arg) ----
; SysV: rdi = fn, rsi = arg ; Win64: rcx = fn, rdx = arg
global mineasm_invoke_main
mineasm_invoke_main:
    mov     rax, rdi
    test    rax, rax
    jnz     .sysv
    mov     rax, rcx
    test    rax, rax
    jz      .done
    mov     rdi, rdx                ; Win64 arg -> SysV first argument
.sysv:
    call    rax
.done:
    ret

section .note.GNU-stack noalloc noexec nowrite progbits
