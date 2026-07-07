# Plaza

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
