# Plaza docs

Plaza is a Paper fork for plugin-driven servers. These docs cover the server
(`plaza-server`) first, since that is the product you run, and the `plaza-api`
artifact second, for plugin developers who want explicit control.

## Server (`plaza-server`)

- [Configuration](server/configuration.md): `plaza.yml` world settings and the
  performance defaults (void generator, disabled vanilla tick work, etc.).
- [Data sources](server/data-sources.md): the `file`, `sql` and `mongodb`
  loaders and how to enable them in `plaza.yml`.

## API (`plaza-api`)

Public documentation for developing plugins against the `plaza-api` artifact
published on JitPack. Most plugins do **not** need this: in Plaza,
`Bukkit.createWorld(...)` / `WorldCreator` already produce Slime Worlds by
default, so unadapted plugins run on Slime Worlds unchanged. See
[`docs/api/`](api/) for:

- [Compatibility](api/compatibility.md): drop-in behavior and when the API is
  actually needed.
- [World management](api/world-management.md): reading, loading, saving,
  cloning, migrating, importing and exporting worlds.
- [Data sources](api/data-sources.md): the `PlazaWorldLoader` interface and how
  a plugin obtains a loader.
- [Events](api/events.md): Plaza world lifecycle events.

## Add the dependency

The API is published on [JitPack](https://jitpack.io). Coordinates are
`com.github.Plaza-MC:plaza-api:<version>` in `compileOnly`/`provided` scope (the
server supplies it at runtime). See the
[README](../README.md#using-the-api-for-plugin-developers) for copy-paste
Gradle/Maven snippets.
