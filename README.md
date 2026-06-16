# NewTerraForged

Unofficial continuation of TerraForged, maintained by **LiGaSt**. Not affiliated with the original TerraForged authors (dags, Won-Ton).

NewTerraForged is a Minecraft 1.18.2 world generation mod — a continuation of the TerraForged terrain engine with custom mega/giga caves, regional cave biomes, and extended mod integration (Terralith, TerraBlender, Dynamic Trees, and others).

This branch targets **Minecraft 1.18.2 / Forge 40.x**. It is under active recovery from the reference release and may still be unstable.

## Build

```powershell
python _recovery/sync_jar_resources.py   # optional: refresh png/json/toml from reference jar
.\gradlew.bat jar reobfJar -x test
```

Output: `build/libs/NewTerraForged-1.18.2-0.4.3.jar`

Release notes: [CHANGELOG.md](CHANGELOG.md)

## License

MIT — see [LICENSE](LICENSE). Based on the original [TerraForged](https://github.com/TerraForged/TerraForged) project by dags and Won-Ton.
