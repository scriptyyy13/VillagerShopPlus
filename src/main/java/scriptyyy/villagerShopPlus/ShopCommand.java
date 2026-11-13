package scriptyyy.villagerShopPlus;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import scriptyyy.villagerShopPlus.listeners.gui.ShopEditorGUI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ShopCommand implements BasicCommand {

    private final VillagerShopPlus plugin;

    public ShopCommand(VillagerShopPlus plugin) {
        this.plugin = plugin;
    }

    private String getAllTypesString() {
        return Registry.VILLAGER_TYPE.stream()
                .map(t -> t.key().value())
                .collect(Collectors.joining(", "));
    }

    private String getAllProfessionsString() {
        return Registry.VILLAGER_PROFESSION.stream()
                .map(p -> p.key().value())
                .collect(Collectors.joining(", "));
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            for (String line : plugin.getRawList("messages.help")) {
                sender.sendMessage(VillagerShopPlus.parse(line));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("villagershop.reload") && !sender.hasPermission("villagershop.admin")) {
                sender.sendMessage(plugin.getMsg("no-permission"));
                return;
            }
            plugin.reloadConfig();
            plugin.loadLanguage();
            sender.sendMessage(plugin.getMsg("config-reloaded"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMsg("only-players"));
            return;
        }

        // спавн жителя
        if (args[0].equalsIgnoreCase("spawn")) {
            if (!player.hasPermission("villagershop.spawn")) {
                player.sendMessage(plugin.getMsg("no-permission"));
                return;
            }

            Villager.Type type = Villager.Type.SWAMP;
            Villager.Profession prof = Villager.Profession.TOOLSMITH;

            if (args.length > 1) {
                NamespacedKey key = NamespacedKey.fromString(args[1].toLowerCase());
                Villager.Type parsedType = key != null ? Registry.VILLAGER_TYPE.get(key) : null;
                if (parsedType == null) {
                    String msg = plugin.getRaw("messages.invalid-type")
                            .replace("{types}", getAllTypesString());
                    player.sendMessage(VillagerShopPlus.parse(msg));
                    return;
                }
                type = parsedType;
            }

            if (args.length > 2) {
                NamespacedKey key = NamespacedKey.fromString(args[2].toLowerCase());
                Villager.Profession parsedProf = key != null ? Registry.VILLAGER_PROFESSION.get(key) : null;
                if (parsedProf == null) {
                    String msg = plugin.getRaw("messages.invalid-prof")
                            .replace("{profs}", getAllProfessionsString());
                    player.sendMessage(VillagerShopPlus.parse(msg));
                    return;
                }
                prof = parsedProf;
            }

            Location loc = player.getLocation();
            Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);

            v.setVillagerType(type);
            v.setProfession(prof);
            v.setVillagerLevel(5);

            // центрируем и сохраняем координаты
            plugin.setShopLocation(v, loc);

            String msg = plugin.getRaw("messages.shop-created")
                    .replace("{type}", type.key().value())
                    .replace("{prof}", prof.key().value());
            player.sendMessage(VillagerShopPlus.parse(msg));
            return;
        }

        // команда перемещения ближайшего жителя к игроку
        if (args[0].equalsIgnoreCase("move")) {
            if (!player.hasPermission("villagershop.edit")) {
                player.sendMessage(plugin.getMsg("no-permission"));
                return;
            }

            Villager target = getNearbyShop(player);
            if (target == null) {
                player.sendMessage(plugin.getMsg("no-nearby-shop"));
                return;
            }

            plugin.setShopLocation(target, player.getLocation());
            player.sendMessage(plugin.getMsg("shop-moved"));
            return;
        }

        // открыть меню ближайшего жителя
        if (args[0].equalsIgnoreCase("edit")) {
            if (!player.hasPermission("villagershop.edit")) {
                player.sendMessage(plugin.getMsg("no-permission"));
                return;
            }

            Villager target = getNearbyShop(player);
            if (target == null) {
                player.sendMessage(plugin.getMsg("no-nearby-shop"));
                return;
            }

            ShopEditorGUI.openHubGUI(player, target);
            return;
        }

        for (String line : plugin.getRawList("messages.help")) {
            player.sendMessage(VillagerShopPlus.parse(line));
        }
    }

    private Villager getNearbyShop(Player player) {
        for (Entity e : player.getNearbyEntities(5, 5, 5)) {
            if (e instanceof Villager v && plugin.isShop(v)) {
                return v;
            }
        }
        return null;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (args.length <= 1) {
            List<String> list = new ArrayList<>();
            String current = args.length == 1 ? args[0].toLowerCase() : "";
            if (sender.hasPermission("villagershop.spawn") && "spawn".startsWith(current)) list.add("spawn");
            if (sender.hasPermission("villagershop.edit")) {
                if ("move".startsWith(current)) list.add("move");
                if ("edit".startsWith(current)) list.add("edit");
            }
            if ((sender.hasPermission("villagershop.reload") || sender.hasPermission("villagershop.admin")) && "reload".startsWith(current)) list.add("reload");
            if ("help".startsWith(current)) list.add("help");
            return list;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            String current = args[1].toLowerCase();
            return Registry.VILLAGER_TYPE.stream()
                    .map(t -> t.key().value())
                    .filter(s -> s.startsWith(current))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            String current = args[2].toLowerCase();
            return Registry.VILLAGER_PROFESSION.stream()
                    .map(p -> p.key().value())
                    .filter(s -> s.startsWith(current))
                    .toList();
        }

        return List.of();
    }
}