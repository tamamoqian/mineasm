package com.mineasm.core;

/**
 * MineASM loader entry point.
 *
 * Usage:
 *   java -jar mineasm-loader.jar [--mods-dir <dir>] [--mc-version <ver>]
 *        [--arch <x86_64|arm32|arm64>] [--verify] [--list-adapters]
 *
 * Flow:
 *   1. Detect host/target architecture and report native component status;
 *   2. Scan all Jar mods in the mods directory;
 *   3. Select a version adapter based on --mc-version and the mod platform;
 *   4. Run each adapter's prepareLaunch to assemble the launch context.
 */
public final class MineasmLoader {

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println("[mineasm] Load failed: " + e.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        String modsDir = "mods";
        String mcVersion = null;
        String archArg = null;
        boolean verify = false;
        boolean listAdapters = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--mods-dir".equals(a) && i + 1 < args.length) {
                modsDir = args[++i];
            } else if ("--mc-version".equals(a) && i + 1 < args.length) {
                mcVersion = args[++i];
            } else if ("--arch".equals(a) && i + 1 < args.length) {
                archArg = args[++i];
            } else if ("--verify".equals(a)) {
                verify = true;
            } else if ("--list-adapters".equals(a)) {
                listAdapters = true;
            }
        }

        Arch targetArch = archArg != null ? Arch.fromOsArch(archArg) : Arch.detectHost();
        AdapterRegistry registry = new AdapterRegistry();

        System.out.println("[mineasm] MineASM cross-architecture native mod loader v" + version());
        System.out.println("[mineasm] Target architecture: " + targetArch.id());
        if (NativeBridge.isAvailable()) {
            Arch nativeArch = NativeBridge.probeArchSafe();
            int abi = NativeBridge.probeVersion();
            System.out.println("[mineasm] Native component: loaded (arch=" + (nativeArch == null ? "unknown" : nativeArch.id())
                    + ", abi=0x" + Integer.toHexString(abi) + ")");
            if (nativeArch != null && nativeArch != targetArch) {
                System.err.println("[mineasm] Warning: native component architecture " + nativeArch.id()
                        + " does not match target architecture " + targetArch.id());
            }
        } else {
            System.out.println("[mineasm] Native component: not loaded ("
                    + NativeBridge.loadFailureReason() + "); falling back to pure Java pipeline");
        }

        if (listAdapters) {
            System.out.println("[mineasm] Registered adapters:");
            for (VersionAdapter a : registry.all()) {
                System.out.println("  - " + a.id() + " (" + a.platform().displayName()
                        + " " + a.mcFamily() + ", Java " + a.requiredJavaMajor() + "+, "
                        + a.expectedMetadata().entryPath() + ")");
            }
            return;
        }

        if (verify) {
            System.out.println("[mineasm] --verify self-test mode: see com.mineasm.core.CompatibilityVerifier");
            return;
        }

        java.io.File dir = new java.io.File(modsDir);
        java.util.List<ModCandidate> candidates = new java.util.ArrayList<ModCandidate>();
        if (dir.isDirectory()) {
            java.io.File[] jars = dir.listFiles();
            if (jars != null) {
                java.util.Arrays.sort(jars);
                for (java.io.File f : jars) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        ModCandidate c = ModScanner.scan(f);
                        if (c != null) {
                            candidates.add(c);
                        }
                    }
                }
            }
        } else {
            System.out.println("[mineasm] mods directory does not exist: " + dir.getAbsolutePath()
                    + " (continuing with an empty mod set)");
        }

        if (mcVersion == null) {
            mcVersion = guessMcVersion(registry);
            System.out.println("[mineasm] --mc-version not specified; defaulting to " + mcVersion);
        }

        System.out.println("[mineasm] Scanned " + candidates.size() + " mod candidates (directory: " + modsDir + ")");
        LaunchContext ctx = new LaunchContext(mcVersion, registry.resolve(mcVersion) != null
                ? registry.resolve(mcVersion).platform() : ModPlatform.UNKNOWN,
                targetArch, candidates);

        for (ModCandidate c : candidates) {
            VersionAdapter adapter = registry.match(c, mcVersion);
            if (adapter == null) {
                System.err.println("[mineasm] No adapter can load: " + c
                        + " (target MC " + mcVersion + ")");
                continue;
            }
            System.out.println("[mineasm] " + c.modId() + " -> adapter " + adapter.id());
            adapter.prepareLaunch(ctx);
        }

        System.out.println("[mineasm] Launch context assembled: mc=" + ctx.mcVersion()
                + ", arch=" + ctx.targetArch().id()
                + ", injected properties=" + ctx.jvmProperties().size());
    }

    private static String guessMcVersion(AdapterRegistry registry) {
        java.util.Set<String> versions = registry.allSupportedVersions();
        // Pick the newest generation in the set (lexicographic order approximates newest)
        String latest = "1.7.10";
        for (String v : versions) {
            if (v.compareTo(latest) > 0) {
                latest = v;
            }
        }
        return latest;
    }

    private static String version() {
        Package p = MineasmLoader.class.getPackage();
        return p != null && p.getImplementationVersion() != null
                ? p.getImplementationVersion() : "1.0.0";
    }

    private MineasmLoader() {
    }
}
