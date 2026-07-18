# Official TerraForged 1.18.2 reference (decompiled)

Source jar: `C:\curseforge\minecraft\Instances\TerraforgedTest\mods\TerraForged-1.18.2-0.3.1-alpha-2.jar.disabled`

Decompiled with CFR into this folder for port audits against NewTerraForged.

## Confirmed NewTF drift vs this jar (2026-07-18)

| Area | Official 1.18.2 TF | Broken NewTF (before fix) |
|------|--------------------|---------------------------|
| `NoiseChunk` router | `NoopNoise.ROUTER` (all-zero) | vanilla `NoiseBasedChunkGenerator` router via reflection |
| Surface-cache halo | fill with chunk **min** height | clamped edge heights |
| `VanillaGen` | no `noiseRouter` field; `SurfaceSystem` uses settings random | forced XOROSHIRO + resolveRouter |
| `BuiltinHook` | `overrideRegistryFromResources` | no-op / debug-only |
| `getWaterLevel` | same formula | same (sea-level tunnels ⇒ river detection / noise, not API) |

Do not compare river behaviour to TF 0.3.x (1.19) for this bug.
