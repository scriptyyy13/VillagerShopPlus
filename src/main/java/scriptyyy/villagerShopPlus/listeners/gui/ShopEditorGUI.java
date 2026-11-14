package scriptyyy.villagerShopPlus.listeners.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import scriptyyy.villagerShopPlus.VillagerShopPlus;
import scriptyyy.villagerShopPlus.listeners.ShopInteractListener;

import java.util.*;

public class ShopEditorGUI {

    private static final Map<Player, Villager> activeVillagers = new HashMap<>();
    private static final Map<Player, Integer> editingTrades = new HashMap<>();

    public static void openHubGUI(Player player, Villager villager) {
        activeVillagers.put(player, villager);
        VillagerShopPlus pl = VillagerShopPlus.getInstance();

        Inventory inv = Bukkit.createInventory(null, 27, VillagerShopPlus.parse(pl.getRaw("gui.hub-title")));

        ItemStack tradesBtn = createBtn(Material.EMERALD, pl.getRaw("gui.hub.trades.name"), pl.getRawList("gui.hub.trades.lore"));
        inv.setItem(10, tradesBtn);

        List<String> typeLore = new ArrayList<>();
        for (String l : pl.getRawList("gui.hub.type.lore")) {
            typeLore.add(l.replace("{type}", villager.getVillagerType().key().value()));
        }
        ItemStack typeBtn = createBtn(Material.GRASS_BLOCK, pl.getRaw("gui.hub.type.name"), typeLore);
        inv.setItem(12, typeBtn);

        List<String> profLore = new ArrayList<>();
        for (String l : pl.getRawList("gui.hub.profession.lore")) {
            profLore.add(l.replace("{prof}", villager.getProfession().key().value()));
        }
        ItemStack profBtn = createBtn(Material.WRITABLE_BOOK, pl.getRaw("gui.hub.profession.name"), profLore);
        inv.setItem(14, profBtn);

        ItemStack moveBtn = createBtn(Material.COMPASS, pl.getRaw("gui.hub.move.name"), pl.getRawList("gui.hub.move.lore"));
        inv.setItem(16, moveBtn);

        Component curName = villager.customName() != null ? villager.customName() : VillagerShopPlus.parse(pl.getRaw("gui.name-status.none"));
        String visStr = villager.isCustomNameVisible() ? pl.getRaw("gui.name-status.visible") : pl.getRaw("gui.name-status.hidden");

        List<Component> nameLore = new ArrayList<>();
        for (String line : pl.getRawList("gui.hub.name.lore")) {
            if (line.contains("{name}")) {
                assert curName != null;
                nameLore.add(Component.text("").append(VillagerShopPlus.parse(line.replace("{name}", ""))).append(curName));
            } else {
                nameLore.add(VillagerShopPlus.parse(line.replace("{visible}", visStr)));
            }
        }
        ItemStack nameBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta nMeta = nameBtn.getItemMeta();
        if (nMeta != null) {
            nMeta.displayName(VillagerShopPlus.parse(pl.getRaw("gui.hub.name.name")));
            nMeta.lore(nameLore);
            nameBtn.setItemMeta(nMeta);
        }
        inv.setItem(21, nameBtn);

        ItemStack delBtn = createBtn(Material.BARRIER, pl.getRaw("gui.hub.delete.name"), pl.getRawList("gui.hub.delete.lore"));
        inv.setItem(23, delBtn);

        player.openInventory(inv);
    }

    public static void handleHubClick(InventoryClickEvent event, VillagerShopPlus pl) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Villager villager = activeVillagers.get(player);
        if (villager == null || !villager.isValid()) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 10) openMainGUI(player, villager);
        if (slot == 12) openTypeSelectorGUI(player, villager);
        if (slot == 14) openProfSelectorGUI(player, villager);

        if (slot == 16) {
            pl.setShopLocation(villager, player.getLocation());
            player.sendMessage(pl.getMsg("shop-moved"));
            openHubGUI(player, villager);
        }

        if (slot == 21) {
            if (event.isShiftClick() && event.isRightClick()) {
                villager.customName(null);
                villager.setCustomNameVisible(false);
                player.sendMessage(pl.getMsg("name-reset"));
                openHubGUI(player, villager);
                return;
            }
            if (event.isRightClick()) {
                boolean next = !villager.isCustomNameVisible();
                villager.setCustomNameVisible(next);
                player.sendMessage(next ? pl.getMsg("name-shown") : pl.getMsg("name-hidden"));
                openHubGUI(player, villager);
                return;
            }
            if (event.isLeftClick()) {
                player.closeInventory();
                ShopInteractListener.startNaming(player, villager);
                for (String line : pl.getRawList("messages.name-prompt")) {
                    player.sendMessage(VillagerShopPlus.parse(line));
                }
            }
        }

        if (slot == 23) {
            pl.unregisterShop(villager.getUniqueId());
            villager.remove();
            player.closeInventory();
            player.sendMessage(pl.getMsg("shop-deleted"));
        }
    }

    public static void openTypeSelectorGUI(Player player, Villager villager) {
        activeVillagers.put(player, villager);
        VillagerShopPlus pl = VillagerShopPlus.getInstance();
        Inventory inv = Bukkit.createInventory(null, 27, VillagerShopPlus.parse(pl.getRaw("gui.types-title")));

        int slot = 10;
        for (Villager.Type type : Registry.VILLAGER_TYPE) {
            if (slot > 16) break;
            ItemStack item = new ItemStack(Material.OAK_SAPLING);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(VillagerShopPlus.parse("<yellow>" + type.key().value() + "</yellow>"));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        ItemStack back = createBtn(Material.RED_CONCRETE, pl.getRaw("gui.back-button"), List.of());
        inv.setItem(22, back);
        player.openInventory(inv);
    }

    public static void handleTypeClick(InventoryClickEvent event, VillagerShopPlus pl) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Villager villager = activeVillagers.get(player);
        if (villager == null || !villager.isValid()) return;

        int slot = event.getRawSlot();
        if (slot == 22) {
            openHubGUI(player, villager);
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String plainName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(item.getItemMeta().displayName())).replace("[", "").replace("]", "").trim().toLowerCase();
            for (Villager.Type type : Registry.VILLAGER_TYPE) {
                if (type.key().value().equalsIgnoreCase(plainName)) {
                    List<MerchantRecipe> savedRecipes = new ArrayList<>(villager.getRecipes());
                    villager.setVillagerType(type);
                    villager.setRecipes(savedRecipes);
                    Bukkit.getScheduler().runTask(pl, () -> villager.setRecipes(savedRecipes));

                    player.sendMessage(VillagerShopPlus.parse(pl.getRaw("messages.type-changed").replace("{type}", type.key().value())));
                    openHubGUI(player, villager);
                    return;
                }
            }
        }
    }

    public static void openProfSelectorGUI(Player player, Villager villager) {
        activeVillagers.put(player, villager);
        VillagerShopPlus pl = VillagerShopPlus.getInstance();
        Inventory inv = Bukkit.createInventory(null, 27, VillagerShopPlus.parse(pl.getRaw("gui.profs-title")));

        int slot = 0;
        for (Villager.Profession prof : Registry.VILLAGER_PROFESSION) {
            if (slot >= 22) break;
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(VillagerShopPlus.parse("<gold>" + prof.key().value() + "</gold>"));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        ItemStack back = createBtn(Material.RED_CONCRETE, pl.getRaw("gui.back-button"), List.of());
        inv.setItem(22, back);
        player.openInventory(inv);
    }

    public static void handleProfClick(InventoryClickEvent event, VillagerShopPlus pl) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Villager villager = activeVillagers.get(player);
        if (villager == null || !villager.isValid()) return;

        int slot = event.getRawSlot();
        if (slot == 22) {
            openHubGUI(player, villager);
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String plainName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(item.getItemMeta().displayName())).replace("[", "").replace("]", "").trim().toLowerCase();
            for (Villager.Profession prof : Registry.VILLAGER_PROFESSION) {
                if (prof.key().value().equalsIgnoreCase(plainName)) {
                    List<MerchantRecipe> savedRecipes = new ArrayList<>(villager.getRecipes());
                    villager.setProfession(prof);
                    villager.setVillagerLevel(5);
                    villager.setRecipes(savedRecipes);
                    Bukkit.getScheduler().runTask(pl, () -> villager.setRecipes(savedRecipes));

                    player.sendMessage(VillagerShopPlus.parse(pl.getRaw("messages.prof-changed").replace("{prof}", prof.key().value())));
                    openHubGUI(player, villager);
                    return;
                }
            }
        }
    }

    public static void openMainGUI(Player player, Villager villager) {
        activeVillagers.put(player, villager);
        editingTrades.remove(player);
        VillagerShopPlus pl = VillagerShopPlus.getInstance();

        Inventory inv = Bukkit.createInventory(null, 54, VillagerShopPlus.parse(pl.getRaw("gui.main-title")));

        String p1Template = pl.getRaw("gui.trade-item.price1");
        String p2Template = pl.getRaw("gui.trade-item.price2");
        List<String> actionLore = pl.getRawList("gui.trade-item.lore");

        List<MerchantRecipe> recipes = villager.getRecipes();
        for (int i = 0; i < Math.min(recipes.size(), 45); i++) {
            MerchantRecipe recipe = recipes.get(i);
            ItemStack display = recipe.getResult().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();
                lore.add(Component.empty());

                ItemStack in1 = recipe.getIngredients().get(0);
                lore.add(VillagerShopPlus.parse(p1Template.replace("{type}", in1.getType().name()).replace("{amount}", String.valueOf(in1.getAmount()))));

                if (recipe.getIngredients().size() > 1) {
                    ItemStack in2 = recipe.getIngredients().get(1);
                    lore.add(VillagerShopPlus.parse(p2Template.replace("{type}", in2.getType().name()).replace("{amount}", String.valueOf(in2.getAmount()))));
                }

                for (String line : actionLore) {
                    lore.add(VillagerShopPlus.parse(line));
                }

                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        ItemStack addBtn = createBtn(Material.EMERALD, pl.getRaw("gui.add-button.name"), pl.getRawList("gui.add-button.lore"));
        inv.setItem(48, addBtn);

        ItemStack backBtn = createBtn(Material.BARRIER, pl.getRaw("gui.delete-button.name"), pl.getRawList("gui.delete-button.lore"));
        inv.setItem(50, backBtn);

        player.openInventory(inv);
    }

    public static void handleMainClick(InventoryClickEvent event, VillagerShopPlus pl) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Villager villager = activeVillagers.get(player);
        if (villager == null || !villager.isValid()) return;

        int slot = event.getRawSlot();

        if (slot == 48) {
            openTradeBuilderGUI(player, villager, -1);
            return;
        }

        if (slot == 50) {
            openHubGUI(player, villager);
            return;
        }

        if (slot >= 0 && slot < 45 && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
            List<MerchantRecipe> list = new ArrayList<>(villager.getRecipes());
            if (slot >= list.size()) return;

            if (event.isShiftClick() && event.isLeftClick()) {
                if (slot > 0) {
                    Collections.swap(list, slot, slot - 1);
                    villager.setRecipes(list);
                    openMainGUI(player, villager);
                }
                return;
            }

            if (event.isShiftClick() && event.isRightClick()) {
                if (slot < list.size() - 1) {
                    Collections.swap(list, slot, slot + 1);
                    villager.setRecipes(list);
                    openMainGUI(player, villager);
                }
                return;
            }

            if (event.isRightClick()) {
                list.remove(slot);
                villager.setRecipes(list);
                player.sendMessage(pl.getMsg("trade-deleted"));
                openMainGUI(player, villager);
                return;
            }

            if (event.isLeftClick()) {
                openTradeBuilderGUI(player, villager, slot);
            }
        }
    }

    public static void openTradeBuilderGUI(Player player, Villager villager, int tradeIndex) {
        activeVillagers.put(player, villager);
        editingTrades.put(player, tradeIndex);
        VillagerShopPlus pl = VillagerShopPlus.getInstance();

        boolean isEdit = tradeIndex >= 0;
        String titleKey = isEdit ? "gui.editor-title" : "gui.creator-title";
        Inventory inv = Bukkit.createInventory(null, 27, VillagerShopPlus.parse(pl.getRaw(titleKey)));

        ItemStack filler = createBtn(Material.GRAY_STAINED_GLASS_PANE, "", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, filler.clone());

        if (isEdit && tradeIndex < villager.getRecipes().size()) {
            MerchantRecipe recipe = villager.getRecipes().get(tradeIndex);
            inv.setItem(11, recipe.getIngredients().get(0).clone());
            if (recipe.getIngredients().size() > 1) {
                inv.setItem(13, recipe.getIngredients().get(1).clone());
            } else {
                inv.setItem(13, null);
            }
            inv.setItem(15, recipe.getResult().clone());
        } else {
            inv.setItem(11, null);
            inv.setItem(13, null);
            inv.setItem(15, null);
        }

        inv.setItem(12, createBtn(Material.ORANGE_STAINED_GLASS_PANE, pl.getRaw("gui.creator.plus-sign"), List.of()));
        inv.setItem(14, createBtn(Material.ARROW, pl.getRaw("gui.creator.result-sign"), List.of()));
        inv.setItem(18, createBtn(Material.RED_CONCRETE, pl.getRaw("gui.creator.back-button.name"), List.of()));

        String confirmNameKey = isEdit ? "gui.editor.confirm-button.name" : "gui.creator.confirm-button.name";
        String confirmLoreKey = isEdit ? "gui.editor.confirm-button.lore" : "gui.creator.confirm-button.lore";
        inv.setItem(26, createBtn(Material.LIME_CONCRETE, pl.getRaw(confirmNameKey), pl.getRawList(confirmLoreKey)));

        player.openInventory(inv);
    }

    public static void handleCreatorClick(InventoryClickEvent event, VillagerShopPlus pl) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getClick().isKeyboardClick() || event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();

        if (rawSlot >= 27) {
            return;
        }

        event.setCancelled(true);

        Villager villager = activeVillagers.get(player);
        if (villager == null || !villager.isValid()) return;

        Inventory top = event.getView().getTopInventory();

        if (rawSlot == 11 || rawSlot == 13 || rawSlot == 15) {
            ItemStack cursor = event.getCursor();
            if (!cursor.getType().isAir()) {
                top.setItem(rawSlot, cursor.clone());
            } else {
                top.setItem(rawSlot, null);
            }
            return;
        }

        if (rawSlot == 18) {
            editingTrades.remove(player);
            openMainGUI(player, villager);
            return;
        }

        if (rawSlot == 26) {
            ItemStack buy1 = top.getItem(11);
            ItemStack buy2 = top.getItem(13);
            ItemStack sell = top.getItem(15);

            if (buy1 == null || buy1.getType().isAir() || sell == null || sell.getType().isAir()) {
                player.sendMessage(pl.getMsg("creator-need-items"));
                return;
            }

            MerchantRecipe recipe = new MerchantRecipe(sell.clone(), Integer.MAX_VALUE);
            recipe.setExperienceReward(false);
            recipe.setPriceMultiplier(0.0f);
            recipe.setDemand(0);
            recipe.setUses(0);
            recipe.addIngredient(buy1.clone());

            if (buy2 != null && !buy2.getType().isAir()) {
                recipe.addIngredient(buy2.clone());
            }

            List<MerchantRecipe> list = new ArrayList<>(villager.getRecipes());
            int editIdx = editingTrades.getOrDefault(player, -1);

            if (editIdx >= 0 && editIdx < list.size()) {
                list.set(editIdx, recipe);
                player.sendMessage(pl.getMsg("trade-edited"));
            } else {
                list.add(recipe);
                player.sendMessage(pl.getMsg("trade-added"));
            }

            villager.setRecipes(list);

            editingTrades.remove(player);
            openMainGUI(player, villager);
        }
    }

    public static void handleCreatorClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            editingTrades.remove(player);
        }
    }

    private static ItemStack createBtn(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!name.isEmpty()) meta.displayName(VillagerShopPlus.parse(name));
            List<Component> cLore = new ArrayList<>();
            for (String l : lore) cLore.add(VillagerShopPlus.parse(l));
            meta.lore(cLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}