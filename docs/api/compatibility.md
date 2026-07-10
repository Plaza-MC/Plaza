# Compatibility

Plaza is designed as a drop-in for Bukkit/Paper plugins. Existing plugins do
not have to adapt to Plaza to work — and an unadapted plugin still runs on
Slime Worlds.

## What keeps working unchanged

- `Bukkit.getWorld(name)`, `Bukkit.getWorlds()`, and `org.bukkit.World` objects.
- `new WorldCreator(name).createWorld()` and `Bukkit.createWorld(...)`.
- Bukkit/Paper world events (`WorldLoadEvent`, `WorldUnloadEvent`,
  `WorldSaveEvent`, `WorldInitEvent`).
- Persistent data containers, `NamespacedKey`, and the rest of the Bukkit API.

Internally, default worlds are Slime Worlds, but externally they look like
normal Bukkit worlds.

## Non-adapted plugins already get Slime Worlds

Plaza patches world creation so that the standard Bukkit API respects the
server's default world format. When a plugin calls `Bukkit.createWorld(...)` /
`new WorldCreator(name).createWorld()` **without supplying its own
`ChunkGenerator`**, Plaza installs a void generator and, with the default
`world.default-format: SLIME`, returns a Slime World. So a plugin that does not
even know Plaza exists still creates and loads Slime Worlds.

The only case that falls back to vanilla behavior is a plugin that passes its
own `ChunkGenerator`/`BiomeProvider` to the `WorldCreator`: Plaza then stays out
of the way and the world is created normally.

## When you actually need the Plaza API

Depend on `plaza-api` only when you want explicit Plaza behavior that Bukkit
cannot provide:

- Read-only template worlds you `clone` per arena for instant map reset.
- Reading a world into memory without loading it as a live level.
- Choosing where worlds are stored through pluggable data sources.
- Reacting to Plaza world lifecycle events.

For everything else, the Bukkit API is enough and preferred.

## The convenience path

`PlazaAPI.createBukkitWorld(WorldCreator)` follows the same path as
`Bukkit.createWorld(...)` under the default Slime format, while making the Plaza
intent explicit at the call site:

```java
World world = PlazaAPI.instance().createBukkitWorld(new WorldCreator("arena"));
```

## Dependency scope

Always declare `plaza-api` as `compileOnly` (Gradle) or `provided` (Maven).
The server supplies the API at runtime; bundling it into your plugin jar would
duplicate the whole Paper/Plaza API and cause class conflicts.
