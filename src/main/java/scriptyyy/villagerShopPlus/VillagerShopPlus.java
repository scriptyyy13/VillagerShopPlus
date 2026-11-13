package scriptyyy.villagerShopPlus;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import scriptyyy.villagerShopPlus.listeners.ShopInteractListener;
import scriptyyy.villagerShopPlus.listeners.ShopProtectionListener;

import java.io.File;
import java.util.List;

public final class VillagerShopPlus extends JavaPlugin {

    private static VillagerShopPlus instance;
    private NamespacedKey shopKey;
    private NamespacedKey locXKey;
    private NamespacedKey locYKey;
    private NamespacedKey locZKey;
    private NamespacedKey locYawKey;
    private NamespacedKey locPitchKey;
    private Team shopTeam;

    private FileConfiguration langConfig;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadLanguage();

        this.shopKey = new NamespacedKey(this, "is_custom_shop");
        this.locXKey = new NamespacedKey(this, "loc_x");
        this.locYKey = new NamespacedKey(this, "loc_y");
        this.locZKey = new NamespacedKey(this, "loc_z");
        this.locYawKey = new NamespacedKey(this, "loc_yaw");
        this.locPitchKey = new NamespacedKey(this, "loc_pitch");

        setupTeam();

        getServer().getPluginManager().registerEvents(new ShopProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopInteractListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    "vshop",
                    "Управление магазинами",
                    List.of("villagershop", "vshopplus"),
                    new ShopCommand(this)
            );
        });

        // таймер проверки и возврата жителей на точку (каждые 4 секунды)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Villager v : world.getEntitiesByClass(Villager.class)) {
                    if (isShop(v)) {
                        fixShop(v);
                    }
                }
            }
        }, 80L, 80L);

        getComponentLogger().info(parse("<green>VillagerShopPlus запущен!</green>"));
    }

    // загрузка языкового файла из папки lang
    public void loadLanguage() {
        saveResource("lang/ru_ru.yml", false);
        saveResource("lang/en_us.yml", false);

        String langName = getConfig().getString("language", "ru_ru");
        File langFile = new File(getDataFolder(), "lang/" + langName + ".yml");
        if (!langFile.exists()) {
            langFile = new File(getDataFolder(), "lang/ru_ru.yml");
        }
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void setupTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("vshop_collision");
        if (team == null) {
            team = scoreboard.registerNewTeam("vshop_collision");
        }
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        this.shopTeam = team;
    }

    // фиксирует моба на точке и не дает провалиться под блок
    public void fixShop(Villager v) {
        v.setCollidable(false);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistent(true);
        v.setGravity(false);

        AttributeInstance speed = v.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.0);
        AttributeInstance knockback = v.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.setBaseValue(1.0);

        if (shopTeam != null && !shopTeam.hasEntity(v)) {
            shopTeam.addEntity(v);
        }

        PersistentDataContainer pdc = v.getPersistentDataContainer();
        if (pdc.has(locXKey, PersistentDataType.DOUBLE)) {
            double x = pdc.get(locXKey, PersistentDataType.DOUBLE);
            double y = pdc.get(locYKey, PersistentDataType.DOUBLE);
            double z = pdc.get(locZKey, PersistentDataType.DOUBLE);
            float yaw = pdc.getOrDefault(locYawKey, PersistentDataType.FLOAT, v.getYaw());
            float pitch = pdc.getOrDefault(locPitchKey, PersistentDataType.FLOAT, v.getPitch());

            Location target = new Location(v.getWorld(), x, y, z, yaw, pitch);
            // возвращаем если сместился
            if (v.getLocation().distanceSquared(target) > 0.001) {
                v.teleport(target);
            }
        }
    }

    // запись точной отцентрированной локации в память жителя
    public void setShopLocation(Villager v, Location loc) {
        double x = loc.getBlockX() + 0.5;
        double z = loc.getBlockZ() + 0.5;
        double y = Math.round(loc.getY() * 1000.0) / 1000.0;

        Location centered = new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch());
        v.teleport(centered);

        PersistentDataContainer pdc = v.getPersistentDataContainer();
        pdc.set(shopKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(locXKey, PersistentDataType.DOUBLE, x);
        pdc.set(locYKey, PersistentDataType.DOUBLE, y);
        pdc.set(locZKey, PersistentDataType.DOUBLE, z);
        pdc.set(locYawKey, PersistentDataType.FLOAT, loc.getYaw());
        pdc.set(locPitchKey, PersistentDataType.FLOAT, loc.getPitch());

        fixShop(v);
    }

    public boolean isShop(Villager v) {
        return v.getPersistentDataContainer().has(shopKey, PersistentDataType.BYTE);
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (text.contains("&") || text.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
        return MINI_MESSAGE.deserialize(text);
    }

    public Component getMsg(String path) {
        String raw = langConfig.getString("messages." + path, "<red>Missing string: " + path + "</red>");
        return parse(raw);
    }

    public String getRaw(String path) {
        return langConfig.getString(path, path);
    }

    public List<String> getRawList(String path) {
        return langConfig.getStringList(path);
    }

    public static VillagerShopPlus getInstance() {
        return instance;
    }

    public NamespacedKey getShopKey() { return shopKey; }
    public FileConfiguration getLang() { return langConfig; }
}