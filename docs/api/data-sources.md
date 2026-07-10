# Data sources (API)

A `PlazaWorldLoader` reads and writes worlds from a storage backend. This page
covers the **API interface** shipped in `plaza-api`. The concrete loaders
(`file`, `sql`, `mongodb`) and their configuration live in `plaza-server` and
are documented under [`server/data-sources.md`](../server/data-sources.md).

## Loader interface

```java
PlazaWorld readWorld(String name, boolean readOnly, PlazaWorldPropertyMap props);
boolean worldExists(String name);
List<String> listWorlds();
void saveWorld(PlazaWorld world);
void deleteWorld(String name);
void lockWorld(String name);    // prevent concurrent loads across servers
void unlockWorld(String name);
boolean isWorldLocked(String name);
String getName();               // source name, e.g. "file"
```

## Getting a loader in a plugin

Plugins do not construct loaders (the implementations are server-side). You get
one from a world that was read with it, or you let Plaza use the default source
through `createBukkitWorld` and never handle a loader directly:

```java
PlazaWorld world = ...;
PlazaWorldLoader loader = world.getLoader();   // null only for temporary worlds

if (loader != null) {
    loader.listWorlds();
    loader.lockWorld("arena");
    try {
        // read/save ...
    } finally {
        loader.unlockWorld("arena");
    }
}
```

Use `lockWorld`/`unlockWorld` when the same source is shared by several servers
to prevent two instances from loading the same world at once.

For the available source types and how to enable them, see
[`server/data-sources.md`](../server/data-sources.md).
