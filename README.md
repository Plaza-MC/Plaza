# Plaza

Plaza is a [Paper](https://papermc.io/) fork designed for
**plugin-driven Minecraft servers**.

It targets lobbies, hubs, minigames, map-rotation servers, empty worlds, and other
setups where plugins own the gameplay and vanilla simulation is mostly overhead.

## Goal

Paper is a great general-purpose Minecraft server. Plaza narrows that goal:

- keep Paper/Bukkit plugin compatibility;
- make Slime Worlds the default backing world system;
- remove or bypass unnecessary vanilla work from every tick by default;
- stay easy to update from upstream Paper.

The intended result is a server fork for networks that do not need normal vanilla
survival logic, world generation, natural mobs, animal AI, or similar mechanics
unless they explicitly opt into them.

## Core ideas

### Plugin compatibility first

Existing Paper/Bukkit plugins should not need to adapt to Plaza.

For example, a plugin calling `Bukkit.getWorld(...)` should still receive a normal
Bukkit/Paper `World` object. Internally that world may be backed by Plaza's Slime
World layer, but the plugin-facing API should remain familiar.

### Slime Worlds by default

Plaza is planned to replace normal default worlds with Slime Worlds so lobby and
minigame servers can load, reset, and store worlds efficiently.

The Slime World integration should be internal by default, with optional Plaza
APIs/configuration only for advanced control.

### Tick work should match plugin-driven servers

Out of the box, Plaza should prefer disabling or no-oping work that empty,
template, or plugin-controlled worlds do not need, including:

- vanilla world generation;
- natural mob spawning;
- animal and mob AI;
- random ticks and block/entity simulation not required by the configured server;
- other vanilla systems that add baseline cost without helping lobby/minigame
  gameplay.

### Upstream-friendly fork

Plaza should follow Paper updates with minimal friction. The implementation should
use a patch-based workflow similar to projects such as Purpur, Leaf, and
AdvancedSlimePaper:

- track the upstream Paper commit in one Gradle property;
- keep Plaza changes in small, named patches;
- avoid broad rewrites of upstream files;
- preserve Paper API behavior unless Plaza has a deliberate compatibility layer.

The initial target is Minecraft/Paper `1.21.11`.

## Status

Plaza is in early bootstrap stage. The repository now has a Paperweight patcher
layout and basic Plaza branding:

- executable Paperclip distribution builds as `build/libs/plaza-server-<minecraft-version>-<commit>.jar`;
- server manifests use `Brand-Id: plazamc:plaza` and `Brand-Name: Plaza`;
- Bukkit/Paper build info falls back to Plaza branding;
- `/version` reports the server name from the Plaza brand;
- the Minecraft client brand/F3 server brand is backed by Paper's
  `getServerModName()` path, which now resolves to Plaza through build info;
- Paper's existing `/paper` command remains available for compatibility.

## Build workflow

Plaza tracks upstream Paper through the `paperCommit` property in
`gradle.properties`.

Build a distributable Paperclip server jar with:

```bash
./build.sh
```

The script applies patches, runs `createMojmapPaperclipJar`, copies the Paperclip
artifact to a clean distribution name with the current Git commit, and prints the
absolute path:

```text
build/libs/plaza-server-<minecraft-version>-<commit>.jar
```

This is the preferred distribution artifact because it follows Paper's Paperclip
model instead of shipping Mojang `net.minecraft` classes directly.

For development or non-distribution workflows, the normal Gradle tasks remain
available:

```bash
./gradlew applyAllPatches
./gradlew rebuildAllPatches
./gradlew build
```

If you intentionally need Paperweight's Mojmap bundler for local/internal testing,
`./gradlew createMojmapBundlerJar` still exists, but it is not the distribution
artifact.

## Who Plaza is for

Plaza is for server owners and developers running:

- network lobbies and hubs;
- minigame instances;
- map/template reset workflows;
- NPC/decorative entity-heavy worlds where full vanilla AI is not wanted;
- plugin-defined gameplay where vanilla survival mechanics are secondary or
  disabled.

If your server depends on accurate vanilla survival behavior by default, Paper or
another general-purpose fork is likely a better fit.

## Credits

Plaza is planned as a fork of Paper and takes architectural inspiration from
Paper forks and Slime World projects, including Purpur, Leaf, AdvancedSlimePaper,
and AdvancedSlimePurpur.

Licensing and attribution will be preserved as implementation code is added.
