# World management

The Plaza API separates an *in-memory* world (`PlazaWorld`) from a *live* level
(`PlazaWorldInstance`). You read/create a `PlazaWorld` first, then load it as a
live level when you want players and ticks.

## Two ways to load a world

### Convenience: Bukkit `WorldCreator`

Best for plugins that already use the Bukkit world API. It respects the
server's default format and void generator, with no loader handling:

```java
World world = PlazaAPI.instance().createBukkitWorld(new WorldCreator("arena"));
```

### Explicit: `readWorld` + `loadWorld`

When you need to choose the data source, properties, or read-only mode:

```java
PlazaAPI api = PlazaAPI.instance();

PlazaWorldPropertyMap props = PlazaWorldProperties.defaults();
props.setString(PlazaWorldProperties.ENVIRONMENT, "normal");
props.setBoolean(PlazaWorldProperties.ALLOW_MONSTERS, false);

PlazaWorldLoader loader = /* a configured source, see data-sources.md */ null;
PlazaWorld data = api.readWorld(loader, "arena", /* readOnly */ true, props);

// Must run on the server thread.
PlazaWorldInstance instance = api.loadWorld(data, /* callWorldLoadEvent */ true);
World live = instance.getBukkitWorld();
```

> `loadWorld` **must be called synchronously on the server thread**. Schedule
> with `Bukkit.getScheduler().runTask(plugin, ...)` if you are async.

`readWorld` only reads into memory; nothing is ticked until `loadWorld`.

## Loaded worlds

```java
PlazaWorldInstance instance = api.getLoadedWorld("arena");   // null if not loaded
List<PlazaWorldInstance> all = api.getLoadedWorlds();

if (instance != null) {
    World live = instance.getBukkitWorld();
    PlazaWorld data = instance.getWorldData();
    boolean stillThere = instance.isLoaded();
}
```

## Saving and unloading

```java
api.saveWorld(data);                 // writes to the world's loader (IOException)
boolean loaded = api.worldLoaded(data);
```

`PlazaWorld` extends `PersistentDataHolder`, so you can attach plugin data to
the world with its `PersistentDataContainer`.

## Creating and cloning

```java
// Empty, in-memory world. loader == null -> temporary (not persisted).
PlazaWorld empty = api.createEmptyWorld("tmp", false, PlazaWorldProperties.defaults(), null);

// Read-only clone with a new name (not saved).
PlazaWorld copy = data.clone("arena-copy-1");

// Clone persisted into a loader (throws if the name already exists).
PlazaWorld saved = data.clone("arena-copy-2", loader);
```

Cloning is the fast path for minigame map reset: keep one read-only template
and `clone` it per arena instead of copying files on disk.

## Import, export, migrate

```java
// Vanilla Anvil folder -> Plaza data source.
PlazaWorld imported = api.importVanillaWorld(new File("world"), "arena", loader);

// Plaza world -> vanilla Anvil folder.
api.exportWorld(data, new File("arena-export"));

// Move a world between data sources.
api.migrateWorld("arena", currentLoader, newLoader);
```

## Formats

`PlazaWorldFormat` identifies the storage format:

- `SLIME`: default. In-memory, resettable, plugin-friendly.
- `ANVIL`: vanilla region files. Slower; must be enabled in the server
  configuration (see [`server/configuration.md`](../server/configuration.md)).
- `LINEAR`, `POLAR`: reserved for future optimized formats.

```java
PlazaWorldFormat fmt = data.getFormat();
String id = fmt.getId();                       // "slime"
PlazaWorldFormat parsed = PlazaWorldFormat.fromId("slime");
```

## Exceptions

World operations throw typed exceptions you can handle specifically:

- `UnknownWorldException`: world not found in the source.
- `WorldAlreadyExistsException`: target name is taken (clone/migrate/import).
- `WorldLoadedException`: the vanilla world is currently loaded (import).
- `CorruptedWorldException`: the world data could not be read.
- `InvalidWorldException`: the input is not a valid world (import).
- `IOException`: underlying storage failure.

All extend `PlazaWorldException`.
