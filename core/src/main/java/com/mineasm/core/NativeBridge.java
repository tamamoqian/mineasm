package com.mineasm.core;

import java.io.File;

/**
 * JNI bridge to the native component.
 *
 * The native methods are implemented in assembly for three architectures
 * (see native/src/asm/...):
 *   Java_com_mineasm_core_NativeBridge_probeArch
 *   Java_com_mineasm_core_NativeBridge_probeVersion
 *   Java_com_mineasm_core_NativeBridge_bootstrap
 *
 * When the native library is missing or the platform does not match,
 * isAvailable() is false and the loader degrades to a pure Java pipeline.
 */
public final class NativeBridge {
    public static final int ABI_VERSION = 0x0100;

    private static volatile boolean loadAttempted = false;
    private static volatile boolean available = false;
    private static volatile String loadFailure = null;

    private NativeBridge() {
    }

    static {
        tryLoad();
    }

    private static synchronized void tryLoad() {
        if (loadAttempted) {
            return;
        }
        loadAttempted = true;
        Arch host = Arch.detectHost();
        String base = "mineasm_native";
        String libName;
        if (isWindows()) {
            libName = base + ".dll";
        } else if (isMac()) {
            libName = "lib" + base + ".dylib";
        } else {
            libName = "lib" + base + ".so";
        }

        // 1) Relative loading. Modpack layout: <root>/native/<arch>/<lib>;
        //    development layout: <project>/native/build/native/<arch>/<lib>
        String[] relativeDirs = new String[] {
                "native/" + host.id() + "/",
                "../native/" + host.id() + "/",
                "native/build/native/" + host.id() + "/",
        };
        for (String dir : relativeDirs) {
            File candidate = new File(dir, libName);
            if (candidate.isFile()) {
                try {
                    System.load(candidate.getAbsolutePath());
                    available = true;
                    return;
                } catch (Throwable t) {
                    loadFailure = "load " + candidate + ": " + t.getMessage();
                }
            }
        }

        // 2) Regular resolution via java.library.path
        try {
            System.loadLibrary(base);
            available = true;
        } catch (Throwable t) {
            loadFailure = loadFailure == null ? "loadLibrary: " + t.getMessage() : loadFailure;
        }
    }

    public static boolean isAvailable() {
        tryLoad();
        return available;
    }

    public static String loadFailureReason() {
        tryLoad();
        return loadFailure;
    }

    public static native int probeArch();

    public static native int probeVersion();

    /**
     * Native bootstrap stub. config is a LaunchConfig function-table pointer
     * (constructed by the caller on the native side); the return value is the
     * sum of the preparePipeline and installTransformers phase results.
     */
    public static native long bootstrap(long configPtr);

    /** Safely probes the host native component architecture; returns null when unavailable. */
    public static Arch probeArchSafe() {
        if (!isAvailable()) {
            return null;
        }
        try {
            return Arch.fromNativeCode(probeArch());
        } catch (Throwable t) {
            loadFailure = "probeArch: " + t.getMessage();
            return null;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
