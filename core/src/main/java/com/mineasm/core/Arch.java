package com.mineasm.core;

/**
 * Target CPU architecture enumeration.
 *
 * The code values match the probe constants implemented in the native assembly:
 *   x86_64 (NASM) : 0x3634  ("64")
 *   arm32  (GAS)  : 0x613332 ("a32")
 *   arm64  (GAS)  : 0x613634 ("a64")
 */
public enum Arch {
    X86_64("x86_64", 0x3634, new String[] { "amd64", "x86_64" }),
    ARM32("arm32", 0x613332, new String[] { "arm", "armv7" }),
    ARM64("arm64", 0x613634, new String[] { "aarch64", "arm64" });

    private final String id;
    private final int nativeCode;
    private final String[] osArchFragments;

    Arch(String id, int nativeCode, String[] osArchFragments) {
        this.id = id;
        this.nativeCode = nativeCode;
        this.osArchFragments = osArchFragments;
    }

    public String id() {
        return id;
    }

    public int nativeCode() {
        return nativeCode;
    }

    /** Detects the host architecture from the JVM's os.arch; defaults to x86_64 when unknown. */
    public static Arch detectHost() {
        return fromOsArch(System.getProperty("os.arch", ""));
    }

    public static Arch fromOsArch(String osArch) {
        String a = osArch.toLowerCase();
        for (Arch arch : values()) {
            for (String frag : arch.osArchFragments) {
                if (a.contains(frag)) {
                    return arch;
                }
            }
        }
        return X86_64;
    }

    /** Maps a magic number returned by a native component back to an architecture; null if unknown. */
    public static Arch fromNativeCode(int code) {
        for (Arch arch : values()) {
            if (arch.nativeCode == code) {
                return arch;
            }
        }
        return null;
    }
}
