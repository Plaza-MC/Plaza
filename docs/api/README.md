# `plaza-api` docs

Documentation for developing plugins against the `plaza-api` artifact published
on JitPack.

You probably do **not** need this. Plaza is a drop-in: an unadapted plugin that
creates worlds through `Bukkit.createWorld(...)` / `new WorldCreator(name)` (with
no custom `ChunkGenerator`) already gets Slime Worlds, because the server makes
the Bukkit world API respect `world.default-format: SLIME`. Reach for `plaza-api`
only when you want control Bukkit cannot give you.

- [Compatibility](compatibility.md): drop-in behavior, what stays the same, and
  when the API is worth adding.
- [World management](world-management.md): reading, loading, saving, cloning,
  migrating, importing and exporting worlds.
- [Data sources](data-sources.md): the `PlazaWorldLoader` interface and how a
  plugin obtains a loader.
- [Events](events.md): Plaza world lifecycle events.

## Add the dependency

Coordinates are `com.github.Plaza-MC:plaza-api:<version>` in
`compileOnly`/`provided` scope. See the
[README](../../README.md#using-the-api-for-plugin-developers) for Gradle/Maven
snippets.

## Entry point

```java
PlazaAPI api = PlazaAPI.instance();
```

The API's value over plain Bukkit is explicit control: read-only template worlds
you `clone` per arena for instant map reset, reading a world without loading it
(`readWorld`), choosing the data source, and the Plaza lifecycle events.
