# Data sources (server)

> This document is about `plaza-server`, **not** the `plaza-api` artifact.
> Plugin developers do not need it; it is for server operators configuring
> where Plaza stores worlds. The API interface is documented in
> [`api/data-sources.md`](../api/data-sources.md).

Sources are declared in `plaza.yml` under `plaza-worlds.sources`. The `type`
selects the loader; you may define several sources of the same type under
different names.

```yaml
plaza-worlds:
  default-source: file
  sources:
    file:
      type: file
      path: plaza_worlds        # folder for .slime/.anvil files

    sql:
      type: sql
      enabled: false            # database sources are opt-in
      dialect: mysql
      host: 127.0.0.1
      port: 3306
      database: plazamc
      username: plazamc
      password: ""
      use-ssl: false
      table: worlds

    mongodb:
      type: mongodb
      enabled: false
      uri: ""                   # overrides host/port/database/auth-source
      host: 127.0.0.1
      port: 27017
      database: plazamc
      collection: worlds
      auth-source: admin
```

- `file` is the baseline and needs no external service.
- `sql` (MySQL/MariaDB/PostgreSQL via JDBC) and `mongodb` are **disabled by
  default**; set `enabled: true` only after configuring them.
- `plaza-worlds.default-source` picks the source for worlds that do not specify
  one. Per-world source overrides live under `worlds.<name>.source` in
  `worlds.yml` (inside the `file` source folder).
