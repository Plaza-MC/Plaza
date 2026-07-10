# Events

Plaza fires three lifecycle events in addition to the standard Bukkit world
events (which keep firing as usual).

| Event | When | Payload |
| --- | --- | --- |
| `PlazaWorldLoadEvent` | after a Plaza world is loaded as a live level | `getInstance()` → `PlazaWorldInstance` |
| `PlazaWorldSaveEvent` | after a Plaza world is saved to its source | `getWorld()` → `PlazaWorld` |
| `PlazaWorldUnloadEvent` | before a Plaza world is unloaded | `getWorld()` → `PlazaWorld` |

They are plain Bukkit events:

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.plazamc.api.events.*;

public final class PlazaWorldListener implements Listener {

    @EventHandler
    public void onLoad(PlazaWorldLoadEvent event) {
        event.getInstance().getBukkitWorld().getName();
        event.getInstance().getWorldData().getFormat();
    }

    @EventHandler
    public void onSave(PlazaWorldSaveEvent event) {
        event.getWorld().getName();
    }

    @EventHandler
    public void onUnload(PlazaWorldUnloadEvent event) {
        event.getWorld().getName();
    }
}
```

`PlazaAPI.loadWorld(world, callWorldLoadEvent)` controls whether the Bukkit
`WorldLoadEvent` is fired when loading; pass `false` if you already react to
`PlazaWorldLoadEvent` and want to avoid double handling.
