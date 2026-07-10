<div align="center">
  <img src="https://raw.githubusercontent.com/Plaza-MC/.github/refs/heads/main/profile/assets/plaza.png" alt="Plaza" width="420">
  <p>A lightweight Paper fork optimized for non vanilla servers.</p>
  <p>
    <a href="https://plazamc.org">Plaza</a> ·
    <a href="https://blueva.net">Blueva (central org)</a> ·
    <a href="https://discord.com/invite/CRFJ32NdcK">Discord (Blueva)</a> ·
    <a href="https://github.com/Plaza-MC">Organization</a>
  </p>
  <p>
    <a href="https://github.com/Plaza-MC/Plaza/stargazers"><img src="https://img.shields.io/github/stars/Plaza-MC/Plaza?logo=github&label=Stars&style=for-the-badge" alt="Stars"></a>
    <a href="https://github.com/Plaza-MC/Plaza/issues"><img src="https://img.shields.io/github/issues/Plaza-MC/Plaza?logo=github&label=Issues&style=for-the-badge" alt="Issues"></a>
    <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&style=for-the-badge" alt="Java 21">
    <img src="https://img.shields.io/badge/Build-Gradle-02303A?logo=gradle&style=for-the-badge" alt="Gradle">
  </p>
  <p>
    <a href="https://plazamc.org/downloads"><img src="https://img.shields.io/badge/Download-2ea44f?style=for-the-badge" alt="Download Plaza" height="40"></a>
  </p>
</div>

---

Plaza is a lightweight Paper fork optimized for non vanilla servers.

Plaza is tuned for servers where plugins own the game. It cuts the vanilla
simulation you don't use so your plugins keep the whole tick.

### ⚠️ Warning ⚠️
> The project targets lobbies, hubs, minigames, template worlds, and other
> server setups where plugins own most gameplay logic and vanilla simulation is
> not the main goal. If your server relies on vanilla systems such as SMP,
> SkyBlock, Towny, Factions, Lifesteal, etc., use
> [Paper](https://github.com/papermc/paper) instead.

## Features

- **Slime Worlds by default.** Worlds load as fast, resettable Slime Worlds
  instead of vanilla's ANVIL region files, with lighter storage and quicker loads
  for lobby and minigame maps, with ANVIL still available when you need it.
- **Lower CPU usage.** Empty worlds skip their tick, and natural spawning, mob
  AI, random ticks, weather and the daylight cycle are off by default.
- **More performance headroom.** Less vanilla work per tick leaves more room for
  your plugins and players on the same hardware.
- **Instant map resets.** Clone a read-only template per arena and reset a world
  in a single call, with no files copied on disk.
- **Drop-in for your plugins.** Bukkit/Paper plugins work unchanged and already
  run on fast, resettable Slime Worlds.
- **Lightweight by design.** No Nether/End unless you enable them, no vanilla
  terrain generation, and nothing extra bundled into your plugin jar.

Planned: additional optimized world formats (`LINEAR`, `POLAR`).

## Using the API (for plugin developers)

Plaza is a drop-in for Bukkit/Paper plugins. A plugin that never touches the
Plaza API still runs on Slime Worlds: Plaza patches `CraftServer` so that
`Bukkit.createWorld(...)` / `new WorldCreator(name).createWorld()` respect the
server's default world format (`world.default-format: SLIME`) and install a void
generator, as long as the plugin does not supply its own `ChunkGenerator`. So
`Bukkit.getWorld(...)`, the `World` API, and Bukkit/Paper world events all keep
working unchanged, and most plugins need no changes at all.

You only depend on `plaza-api` when you want explicit control that Bukkit does
not offer: read-only template worlds cloned per arena for instant map reset,
reading a world without loading it, choosing the data source, or reacting to
Plaza world lifecycle events.

The API is published on [JitPack](https://jitpack.io). Add the repository and
the `plaza-api` dependency. Use `compileOnly`/`provided` scope: the server
supplies the API at runtime, so it must not be bundled into your plugin jar.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Plaza-MC:plaza-api:<version>")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Plaza-MC:plaza-api:<version>'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Plaza-MC</groupId>
    <artifactId>plaza-api</artifactId>
    <version>TAG</version>
    <scope>provided</scope>
</dependency>
```

`<version>` can be a release tag (recommended), a short commit hash, or a
branch snapshot such as `main-SNAPSHOT`. See the available builds at
`https://jitpack.io/#Plaza-MC/Plaza`.

### Why use the API at all

In Plaza, `Bukkit.createWorld(creator)` already gives you a Slime World, so the
plain Bukkit API is all you need for ordinary worlds. Reach for `plaza-api` when
you want what Bukkit cannot do, like resetting an arena instantly by cloning a
read-only template instead of copying files on disk:

```java
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldProperties;

public final class ArenaReset {

    private final PlazaAPI api = PlazaAPI.instance();
    private final PlazaWorld template; // read once, reused for every arena

    public ArenaReset(PlazaWorldLoader templateLoader) {
        this.template = api.readWorld(
                templateLoader, "arena-template",
                /* readOnly */ true, PlazaWorldProperties.defaults());
    }

    /** Fresh playable copy of the template, without copying files on disk. */
    public PlazaWorldInstance openArena(String name) {
        PlazaWorld copy = template.clone(name);            // in-memory, read-only
        return api.loadWorld(copy, /* callWorldLoadEvent */ true); // server thread
    }
}
```

Reacting to Plaza world lifecycle is just a Bukkit listener:

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.plazamc.api.events.PlazaWorldLoadEvent;

public final class WorldListener implements Listener {

    @EventHandler
    public void onPlazaWorldLoad(PlazaWorldLoadEvent event) {
        event.getInstance().getBukkitWorld().getName();
    }
}
```

See [`docs/api/`](docs/api/) for the `plaza-api` documentation (compatibility,
world management, data sources, and events). Server-operator topics
(`plaza.yml`, concrete loaders, and the performance defaults) live in
[`docs/server/`](docs/server/) and are not part of the `plaza-api` artifact.

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
