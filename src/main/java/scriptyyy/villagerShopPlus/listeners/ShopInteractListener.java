package scriptyyy.villagerShopPlus.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import scriptyyy.villagerShopPlus.VillagerShopPlus;
import scriptyyy.villagerShopPlus.listeners.gui.ShopEditorGUI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopInteractListener implements Listener {

    private final VillagerShopPlus plugin;
    private static final Map<UUID, Villager> namingSession = new HashMap<>();

    public ShopInteractListener(VillagerShopPlus plugin) {
        this.plugin = plugin;
    }

    public static void startNaming(Player player, Villager villager) {
        namingSession.put(player.getUniqueId(), villager);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        if (!plugin.isShop(villager)) return;

        Player player = event.getPlayer();

        // пкм в присяде: открываем меню настроек
        if (player.isSneaking()) {
            event.setCancelled(true);
            if (player.hasPermission("villagershop.edit")) {
                ShopEditorGUI.openHubGUI(player, villager);
            } else {
                player.sendMessage(plugin.getMsg("no-permission"));
            }
            return;
        }

        // обычный пкм: открываем виртуального торговца (позволяет покупать толпе игроков одновременно)
        event.setCancelled(true);
        Component title = villager.customName() != null ? villager.customName() : Component.text("Shop");
        Merchant merchant = Bukkit.createMerchant(title);
        merchant.setRecipes(villager.getRecipes());
        player.openMerchant(merchant, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrade(PlayerTradeEvent event) {
        if (event.getVillager() instanceof Villager villager && plugin.isShop(villager)) {
            event.getTrade().setUses(0);
            event.getTrade().setDemand(0);
            event.getTrade().setSpecialPrice(0);
            event.setRewardExp(false);

            for (MerchantRecipe r : villager.getRecipes()) {
                r.setUses(0);
                r.setDemand(0);
                r.setSpecialPrice(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Villager villager = namingSession.remove(player.getUniqueId());
        if (villager == null) return;

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (text.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getMsg("name-cancelled"));
            Bukkit.getScheduler().runTask(plugin, () -> ShopEditorGUI.openHubGUI(player, villager));
            return;
        }

        if (text.equalsIgnoreCase("reset")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                villager.customName(null);
                villager.setCustomNameVisible(false);
                player.sendMessage(plugin.getMsg("name-reset"));
                ShopEditorGUI.openHubGUI(player, villager);
            });
            return;
        }

        Component nameComp = VillagerShopPlus.parse(text);
        Bukkit.getScheduler().runTask(plugin, () -> {
            villager.customName(nameComp);
            villager.setCustomNameVisible(true);
            player.sendMessage(plugin.getMsg("name-set"));
            ShopEditorGUI.openHubGUI(player, villager);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        namingSession.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component title = event.getView().title();

        if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.hub-title")))) {
            ShopEditorGUI.handleHubClick(event, plugin);
        } else if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.main-title")))) {
            ShopEditorGUI.handleMainClick(event, plugin);
        } else if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.creator-title"))) ||
                title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.editor-title")))) {
            ShopEditorGUI.handleCreatorClick(event, plugin);
        } else if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.types-title")))) {
            ShopEditorGUI.handleTypeClick(event, plugin);
        } else if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.profs-title")))) {
            ShopEditorGUI.handleProfClick(event, plugin);
        }
    }

    @EventHandler
    public void onCreatorDrag(InventoryDragEvent event) {
        Component title = event.getView().title();
        if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.creator-title"))) ||
                title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.editor-title")))) {
            // полностью запрещаем драг предметов в верхнее меню
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < 27) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onCreatorClose(InventoryCloseEvent event) {
        Component title = event.getView().title();
        if (title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.creator-title"))) ||
                title.equals(VillagerShopPlus.parse(plugin.getRaw("gui.editor-title")))) {
            ShopEditorGUI.handleCreatorClose(event);
        }
    }
}