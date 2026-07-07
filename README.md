<div align="center">
  <img src="https://raw.githubusercontent.com/Plaza-MC/.github/refs/heads/main/profile/assets/plaza.png" alt="Plaza" width="420">
  <p>A Paper fork focused on plugin-driven Minecraft servers.</p>
  <p>
    <a href="https://plazamc.org">Plaza</a> ·
    <a href="https://blueva.net">Blueva (central org)</a> ·
    <a href="https://discord.com/invite/CRFJ32NdcK">Discord (Blueva)</a> ·
    <a href="https://github.com/Plaza-MC">Organization</a>
  </p>
  <p>
    <a href="https://discord.com/invite/CRFJ32NdcK"><img src="https://img.shields.io/badge/Discord-Blueva-5865F2?logo=discord&logoColor=white" alt="Discord"></a>
    <a href="https://github.com/Plaza-MC/Plaza/stargazers"><img src="https://img.shields.io/github/stars/Plaza-MC/Plaza?logo=github&label=Stars" alt="Stars"></a>
    <a href="https://github.com/Plaza-MC/Plaza/issues"><img src="https://img.shields.io/github/issues/Plaza-MC/Plaza?logo=github&label=Issues" alt="Issues"></a>
    <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
    <img src="https://img.shields.io/badge/Build-Gradle-02303A?logo=gradle" alt="Gradle">
  </p>
</div>

---

Plaza is a Paper fork focused on plugin-driven Minecraft servers.

The project targets lobbies, hubs, minigames, template worlds, and other server
setups where plugins own most gameplay logic and vanilla simulation is not the
main goal.

## Status

Plaza is currently in early development.

## Building

```bash
./build.sh
```

The distributable server jar is written to `build/libs/`.

For development workflows, the normal Gradle tasks are also available:

```bash
./gradlew applyAllPatches
./gradlew rebuildAllPatches
./gradlew build
```

## License

Plaza is based on Paper. Upstream licenses and attributions apply.
