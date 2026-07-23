# Configuration (server)

> This document is about `plaza-server`, **not** the `plaza-api` artifact. It
> is for server operators configuring Plaza; plugin developers do not need it.

Plaza world behavior is configured in `plaza.yml` under the `plaza-worlds` key.
This page covers the world-related settings. See the generated `plaza.yml` for
the full, commented set.

## Default format and source

```yaml
plaza-worlds:
  default-format: SLIME     # SLIME (default), ANVIL or LINEAR
  default-source: file      # must match an entry in plaza-worlds.sources
```

Worlds created/loaded through Plaza (including `createBukkitWorld`) use these
defaults unless overridden per world.

Choosing a format:

- **SLIME** worlds are RAM-first template worlds. They keep chunks in memory
  and do **not** persist game rules, level.dat settings, player data, stats or
  advancements. They are cheap, reset instantly and are the right choice for
  lobbies and minigame maps.
- **LINEAR** worlds keep a real world folder with full vanilla persistence
  (level.dat, game rules, settings, player data), but store region, entity and
  POI data as zstd-compressed `.linear` files (Linear region format v2)
  instead of `.mca`. Faster and smaller than ANVIL with the same fidelity, so
  they fit persistent creative worlds (PlotSquared, housing, etc.).
- **ANVIL** is the vanilla storage (`.mca`); use it only when something
  external needs to read the world folder with vanilla tooling.

## Format options

```yaml
plaza-worlds:
  formats:
    slime:
      default-biome: minecraft:plains
      read-only: false
      save-poi: true
      save-block-ticks: false
      save-fluid-ticks: false
    anvil:
      enabled: false          # set true to allow loading vanilla worlds
    linear:
      compression-level: 6    # zstd level for .linear files (1-22)
      io-thread-count: 6      # max concurrent region file flushes
      io-flush-delay-ms: 100  # delay between flush checks
      use-virtual-threads: true
```

The SLIME defaults are lobby-friendly: scheduled block/fluid ticks are not
saved (`save-block-ticks: false`, `save-fluid-ticks: false`), which keeps
template worlds small and resettable.

A storage folder that already contains `.linear` files is always opened as
LINEAR, regardless of the configured format, so worlds copied from another
server keep working without editing `worlds.yml`.

## Per-world entries

The world list lives in its own file: `worlds.yml` inside the folder of the
`file` source (`plaza_worlds/worlds.yml` by default), so `plaza.yml` stays
small.

```yaml
# plaza_worlds/worlds.yml
worlds:
  world:
    format: SLIME
    # source: file
    # load-on-startup: true
```

Each entry selects the format and (optionally) the data source for a named
world, and whether it loads on startup.

## Performance defaults

Plaza's out-of-box profile is optimized for lobby/minigame/template worlds,
where vanilla simulation is usually wasted work. By default Plaza avoids or
disables:

- Vanilla terrain/world generation (void/empty worlds).
- Natural mob spawning and animal/mob AI for decorative entities.
- Random ticks and block/fluid scheduled ticks (see `save-block-ticks`,
  `save-fluid-ticks`).
- Ticking worlds with no players (`plaza-worlds.disable-tick-when-empty: true`).

Two helpers keep void worlds usable:

```yaml
plaza-worlds:
  spawn-platform:
    enabled: true             # safe block under players in void worlds
  dynamic-world-border:
    enabled: true             # border hugs the loaded map bounds
    margin-blocks: 8
```

Operators can opt back into specific mechanics in `plaza.yml` when a world
needs them (for example, a survival-like map). Prefer enabling only what a given
world actually uses, to preserve the baseline savings.
