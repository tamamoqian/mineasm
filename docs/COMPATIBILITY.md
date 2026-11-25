# MineASM Forge / NeoForge Version Compatibility List

The loader core `:core` plus 11 adapter modules cover the full 1.7.x ~ 1.21.x
series. The table below is verified automatically by `./gradlew verifyAll`
(each version generation runs through the full loading pipeline using a
synthetic Forge/NeoForge mod Jar in the generation-correct format).

| MC Version | Platform | Adapter (module) | Metadata | Java | Launch pipeline | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1.7.2 | Forge | forge-1.7 (adapter-1_7) | mcmod.info | 6+ | Launchwrapper (FMLTweaker) | :verifyAll PASS |
| 1.7.10 | Forge | forge-1.7 (adapter-1_7) | mcmod.info | 6+ | Launchwrapper (FMLTweaker) | :verifyAll PASS |
| 1.12.2 | Forge | forge-1.12 (adapter-1_12) | mcmod.info | 8+ | Launchwrapper (FMLTweaker) | :verifyAll PASS |
| 1.13.2 | Forge | forge-1.13 (adapter-1_13) | META-INF/mods.toml | 8+ | ModLauncher | :verifyAll PASS |
| 1.14.4 | Forge | forge-1.14 (adapter-1_14) | META-INF/mods.toml | 8+ | ModLauncher | :verifyAll PASS |
| 1.15.2 | Forge | forge-1.15 (adapter-1_15) | META-INF/mods.toml | 8+ | ModLauncher | :verifyAll PASS |
| 1.16.5 | Forge | forge-1.16 (adapter-1_16) | META-INF/mods.toml | 8+ | ModLauncher | :verifyAll PASS |
| 1.17.1 | Forge | forge-1.17 (adapter-1_17) | META-INF/mods.toml | 16+ | ModLauncher | :verifyAll PASS |
| 1.18.2 | Forge | forge-1.18 (adapter-1_18) | META-INF/mods.toml | 17+ | ModLauncher | :verifyAll PASS |
| 1.19.2 | Forge | forge-1.19 (adapter-1_19) | META-INF/mods.toml | 17+ | ModLauncher | :verifyAll PASS |
| 1.20.1 | Forge | forge-1.20 (adapter-1_20) | META-INF/mods.toml | 17+ | ModLauncher | :verifyAll PASS |
| 1.20.1 | NeoForge | neoforge-1.20 (adapter-1_20) | META-INF/mods.toml | 17+ | ModLauncher | :verifyAll PASS |
| 1.21.1 | NeoForge | neoforge-1.21 (adapter-1_21) | META-INF/neoforge.mods.toml | 21+ | ModLauncher | :verifyAll PASS |
| 1.21.1 | Forge | forge-1.21 (adapter-1_21) | META-INF/mods.toml | 21+ | ModLauncher | :verifyAll PASS |

## Metadata detection priority

`ModScanner` recognizes mod Jar metadata entries in this order:

1. `META-INF/neoforge.mods.toml` -> NeoForge (1.20.4+ / 1.21+)
2. `META-INF/mods.toml` -> Forge 1.13 ~ 1.20 / NeoForge 20.1 ~ 20.2
3. `mcmod.info` -> Forge 1.7 ~ 1.12
4. `fabric.mod.json` -> Fabric (recognized only; Forge-family adapters reject it)

## Conflict protection

- Platform mismatch (e.g. a NeoForge mod entering a Forge modpack) is rejected
  at the adapter `accepts()` stage with an explicit reason;
- A generation whose Java major requirement is not met throws
  `IllegalStateException` in `prepareLaunch()`, preventing wrong-version mixing;
- When a single Jar contains metadata of multiple generations, the highest
  priority one wins, avoiding double-loading of dual-platform mods.
