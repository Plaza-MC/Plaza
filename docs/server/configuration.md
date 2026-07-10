# Configuration (server)

> This document is about `plaza-server`, **not** the `plaza-api` artifact. It
> is for server operators configuring Plaza; plugin developers do not need it.

Plaza world behavior is configured in `plaza.yml` under the `plaza-worlds` key.
This page covers the world-related settings. See the generated `plaza.yml` for
the full, commented set.

## Default format and source

```yaml
plaza-worlds:
  default-format: SLIME     # SLIME (default) or ANVIL
  default-source: file      # must match an entry in plaza-worlds.sources
```

Worlds created/loaded through Plaza (including `createBukkitWorld`) use these
defaults unless overridden per world.

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
```

The SLIME defaults are lobby-friendly: scheduled block/fluid ticks are not
saved (`save-block-ticks: false`, `save-fluid-ticks: false`), which keeps
template worlds small and resettable.

## Per-world entries

```yaml
plaza-worlds:
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
