package scriptyyy.villagerShopPlus.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import scriptyyy.villagerShopPlus.VillagerShopPlus;

public class ShopProtectionListener implements Listener {

    private final VillagerShopPlus plugin;

    public ShopProtectionListener(VillagerShopPlus plugin) {
        this.plugin = plugin;
    }

    private boolean isShop(Entity entity) {
        if (!(entity instanceof Villager villager)) return false;
        return plugin.isShop(villager);
    }

    // откладываем починку на 1 тик чтобы чанк успел зарегистрировать моба
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Entity e : event.getEntities()) {
                if (e.isValid() && e instanceof Villager v && plugin.isShop(v)) {
                    plugin.fixShop(v);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isShop(event.getEntity())) {
            event.setCancelled(true);
            plugin.fixShop((Villager) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombust(EntityCombustEvent event) {
        if (isShop(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTransform(EntityTransformEvent event) {
        if (isShop(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnterVehicle(VehicleEnterEvent event) {
        if (isShop(event.getEntered())) {
            event.setCancelled(true);
        }
    }
}