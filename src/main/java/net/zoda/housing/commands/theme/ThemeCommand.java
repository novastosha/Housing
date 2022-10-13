package net.zoda.housing.commands.theme;

import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.zoda.api.command.ACommand;
import net.zoda.api.command.Command;
import net.zoda.api.command.CommandRunCondition;
import net.zoda.api.command.DefaultRun;
import net.zoda.api.command.argument.Argument;
import net.zoda.api.command.argument.ArgumentType;
import net.zoda.api.command.subcommand.Subcommand;
import net.zoda.api.command.subcommand.group.SubcommandGroup;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.house.theme.Theme;
import net.zoda.housing.house.theme.ThemeEditSession;
import net.zoda.housing.plugin.HousingPlugin;
import net.zoda.housing.utils.*;
import net.zoda.housing.utils.loaders.TemporaryLoader;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.util.*;
import java.util.function.Function;

import static net.zoda.housing.utils.Utils.apply;
import static net.zoda.housing.utils.Utils.displayVisitingRules;

@Command(name = "theme", permissions = "housing.themes", playerOnly = true)
public class ThemeCommand implements ACommand, Listener {

    public ThemeCommand() {
        HousingPlugin.getInstance().getServer().getPluginManager().registerEvents(this, HousingPlugin.getInstance());
    }

    private static final NamespacedKey wandKey = new NamespacedKey(HousingPlugin.getInstance(), "wand");

    /**
     * Used {@link ItemMeta#setDisplayName(String)} (Which is deprecated in Paper) to evade usage of Kyori Adventure Text
     */
    @SuppressWarnings("deprecation")
    private static final ItemStack wandItem = apply(new ItemStack(Material.STICK), wand -> {
        wand.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        ItemMeta meta = wand.getItemMeta();
        meta.setUnbreakable(true);
        meta.setDisplayName(ChatColor.GREEN + "Selection wand");
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);

        wand.setItemMeta(meta);
    });
    private static final Map<UUID, Pair<Location, Location>> schematicPositions = new HashMap<>();
    private static final Map<UUID, Pair<Location, Location>> spawnLocations = new HashMap<>();
    private static final Map<UUID, Location> housePasteLocations = new HashMap<>();
    private static final Map<UUID, Map<Theme.CoordinateType, Integer>> coordinates = new HashMap<>();

    public static Map<Theme.CoordinateType, Integer> coordsFromConfigurationSection(ConfigurationSection coordinates) {
        Map<Theme.CoordinateType, Integer> map = new HashMap<>();

        for (Theme.CoordinateType type1 : Theme.CoordinateType.values()) {
            if (coordinates.contains(type1.configName)) {
                map.put(type1, coordinates.getInt(type1.configName));
            }
        }

        return map;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent interactEvent) {
        if (interactEvent.getClickedBlock() == null || !Theme.EDIT_SESSIONS.containsKey(interactEvent.getPlayer().getUniqueId()))
            return;

        if (interactEvent.getItem() == null) return;
        if (!interactEvent.getItem().getItemMeta().getPersistentDataContainer().has(wandKey)) return;

        PositionType positionType = interactEvent.getAction().isLeftClick() ? PositionType.FIRST : PositionType.SECOND;
        final Location location = interactEvent.getClickedBlock().getLocation();

        setPosition(interactEvent.getPlayer(), positionType, location);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!Theme.EDIT_SESSIONS.containsKey(event.getPlayer().getUniqueId())) return;
        if (!event.getFrom().equals(Theme.EDIT_SESSIONS.get(event.getPlayer().getUniqueId()).world())) return;

        endSession(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        endSession(event.getPlayer().getUniqueId(), true);
    }


    @SuppressWarnings("deprecation")
    public static void endSession(UUID uuid, boolean remove) {
        schematicPositions.remove(uuid);
        if (Theme.EDIT_SESSIONS.containsKey(uuid)) {
            ThemeEditSession editSession = Theme.EDIT_SESSIONS.get(uuid);

            for (Player player : editSession.world().getPlayers()) {
                player.kickPlayer(ChatColor.RED + "Session ended!");
            }

            Bukkit.getServer().unloadWorld(editSession.world(), false);

            schematicPositions.remove(uuid);
            housePasteLocations.remove(uuid);
            spawnLocations.remove(uuid);
            coordinates.remove(uuid);

            if (remove) Theme.EDIT_SESSIONS.remove(uuid);
        }
    }

    @CommandRunCondition({"list", "edit name", "edit item", "edit schematic", "delete"})
    private final Function<Player, Boolean> themeMapEmpty = (player -> {
        if (Theme.THEMES.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no themes! Maybe create a new one?");
            return false;
        }
        return true;
    });

    @CommandRunCondition("edit schematic")
    private final Function<Player, Boolean> notEditingCheck = (player -> {
        if (Theme.EDIT_SESSIONS.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already editing an other theme!");
            return false;
        }
        return true;
    });

    @CommandRunCondition({"save", "edit wand", "edit set", "exit"})
    private final Function<Player, Boolean> editingCheck = (player -> {
        if (!Theme.EDIT_SESSIONS.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not editing any theme!");
            return false;
        }
        return true;
    });

    private final Class<Material> item = Material.class;
    private final Class<Theme.CoordinateType> coordinateTypeClass = Theme.CoordinateType.class;

    private final Function<Player, List<String>> schematic_file_name = player -> {
        List<String> list = new ArrayList<>();
        for (File file : Files.THEMES_DIRECTORY.getFile().listFiles()) {
            if (!file.isFile() || !file.getName().endsWith(".schem")) continue;

            list.add(file.getName());
        }
        return list;
    };

    private final Function<Player, List<String>> theme = player -> List.of(Theme.THEMES.keySet().toArray(new String[0]));

    private final Class<PositionType> type = PositionType.class;

    @DefaultRun
    public void run(Player player) {
    }

    /**
     * Displays list of loaded themes
     */
    @Subcommand(name = "list")
    public void list(Player player) {
    }

    @Subcommand(name = "create", arguments = {
            @Argument(name = "name", type = ArgumentType.STRING),
            @Argument(name = "display_name", type = ArgumentType.STRING),
            @Argument(name = "item", type = ArgumentType.ENUM),
    })
    public void createTheme(Player player, String name, String displayName, Material item) {
        File themeFile = new File(Files.THEMES_DIRECTORY.getFile(), name + ".yml");
        if (Theme.THEMES.containsKey(name) || themeFile.exists()) {
            player.sendMessage(Component.text("A theme with this name already exists!").color(NamedTextColor.RED));
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("name", displayName);
        configuration.set("code-name", name);
        configuration.set("item", item.name().toLowerCase(Locale.ROOT));

        try {
            configuration.save(themeFile);
            Theme.THEMES.put(name, new Theme(name, ChatColor.translateAlternateColorCodes('&', displayName), item, new byte[0], themeFile, null, null, null).setEnabled(false));

            player.sendMessage(Component.text("Successfully created this theme!").color(NamedTextColor.GREEN));
        } catch (IOException e) {
            player.sendMessage(Component.text("Couldn't save this theme, please try again!").color(NamedTextColor.RED));
        }
    }


    @SubcommandGroup("edit")
    @Subcommand(name = "name", arguments = {
            @Argument(name = "theme", type = ArgumentType.STRING),
            @Argument(name = "new_name", type = ArgumentType.STRING)
    })
    public void renameTheme(Player player, String themeName, String newDisplayName) {

    }


    @SubcommandGroup("edit")
    @Subcommand(name = "item", arguments = {
            @Argument(name = "theme", type = ArgumentType.STRING),
            @Argument(name = "item", type = ArgumentType.ENUM)
    })
    public void changeThemeMaterial(Player player, String themeName, Material item) {

    }


    @SubcommandGroup("edit")
    @Subcommand(name = "schematic", arguments = {
            @Argument(name = "theme", type = ArgumentType.STRING),
    })
    public void editThemeSchematic(Player player, String themeName) {
        Theme theme = Theme.THEMES.get(themeName);
        String worldName = "theme-" + UUID.randomUUID().toString().split("-")[1];

        player.sendMessage(ChatColor.GREEN + "Creating a temporary world... please wait!");
        try {
            final SlimeWorld worldSlime = HousingPlugin.getSlime().createEmptyWorld(HousingPlugin.getTemporaryWorldLoader(), worldName, false, TemporaryLoader.BASIC_SLIME_PROPERTY_MAP);
            HousingPlugin.getSlime().generateWorld(worldSlime);

            World world = Bukkit.getServer().getWorld(worldName);
            Location center = new Location(world, 0, 90, 0);

            coordinates.put(player.getUniqueId(), Theme.MAXIMUM_COORDINATES);
            spawnLocations.put(player.getUniqueId(), new Pair<>(
                    theme.getNpcSpawn() == null ? null : theme.getNpcSpawn().toBukkitLocation(world),
                    theme.getSpawn() == null ? null : theme.getSpawn().toBukkitLocation(world)));
            if (theme.getPasteLocation() != null) {
                housePasteLocations.put(player.getUniqueId(), theme.getPasteLocation().toBukkitLocation(world));
            }
            if (theme.getSchematic() == null || theme.getSchematic().length == 0) {
                world.getBlockAt(center).setType(Material.STONE);
            } else {
                ClipboardFormat format = BuiltInClipboardFormat.FAST;
                try (ClipboardReader reader = format.getReader(new ByteArrayInputStream(theme.getSchematic()))) {
                    Clipboard clipboard = reader.read();

                    center = new Location(world, 0, clipboard.getMinY(), 0);
                    com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(center.getX(), center.getY(), center.getZ()))
                                .copyEntities(false)
                                .copyBiomes(false)
                                .ignoreAirBlocks(false)
                                .build();
                        Operations.complete(operation);
                    }
                }
                spawnLocations.put(player.getUniqueId(), new Pair<>(theme.getNpcSpawn().toBukkitLocation(world), theme.getSpawn().toBukkitLocation(world)));
            }

            Location spawnLocation = center.toCenterLocation().add(0, 1, 0);

            //Find a suitable spawn location
            while (!isEligibleAsSpawn(spawnLocation)) {
                spawnLocation = spawnLocation.add(0, 1, 0);
            }

            Utils.resetPlayer(player, GameMode.CREATIVE, Utils.ResetAttribute.ALL);
            Theme.EDIT_SESSIONS.put(player.getUniqueId(), new ThemeEditSession(theme, world, player.getUniqueId()));

            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Heads up! ALL houses will be pasted on (X+ y: Y± z: Z+)");
            player.teleport(spawnLocation);
        } catch (WorldAlreadyExistsException e) {
            player.sendMessage(ChatColor.GREEN + "Couldn't create the temporary world, please try again!");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            player.sendMessage(ChatColor.GREEN + "There was an error loading the schematic! please try again!");

            final World world = player.getServer().getWorld(worldName);
            if (world != null) player.getServer().unloadWorld(world, false);
        }
    }

    @Subcommand(name = "save")
    public void saveEdit(Player player) {
        Pair<Location, Location> positions = schematicPositions.getOrDefault(player.getUniqueId(), new Pair<>());
        ThemeEditSession session = Theme.EDIT_SESSIONS.get(player.getUniqueId());

        if (positions.getA() == null || positions.getB() == null) {
            player.sendMessage(ChatColor.RED + "Missing positions!");
            return;
        }

        Pair<Location, Location> spawns = spawnLocations.getOrDefault(player.getUniqueId(), new Pair<>());

        if (spawns.getA() == null || spawns.getB() == null) {
            player.sendMessage(ChatColor.RED + "Missing spawn locations!");
            return;
        }

        if (!housePasteLocations.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Missing house paste location!");
            return;
        }

        for (Theme.CoordinateType coordinateType : Theme.CoordinateType.values()) {
            if (coordinates.getOrDefault(player.getUniqueId(), new HashMap<>()).containsKey(coordinateType)) continue;
            player.sendMessage(ChatColor.RED + "Missing coordinates!");
            return;
        }

        File themeFile = session.theme().getFile();

        try {

            if (themeFile == null) {
                themeFile = new File(Files.THEMES_DIRECTORY.getFile(), session.theme().getCodeName() + ".schem");
                if (!themeFile.exists()) {
                    themeFile.createNewFile();
                }
            }

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(themeFile);

            configuration.set("npc-spawn", WorldlessLocation.fromBukkitLocation(spawns.getA()));
            configuration.set("spawn", WorldlessLocation.fromBukkitLocation(spawns.getB()));
            configuration.set("coordinates", coordsConfigurationSection(coordinates.get(player.getUniqueId()), configuration.createSection("coordinates")));
            configuration.set("house-paste-location", WorldlessLocation.fromBukkitLocation(housePasteLocations.get(player.getUniqueId())));
            configuration.set("schematic-file", session.theme().getCodeName() + ".schem");
            configuration.set("paste.first", WorldlessLocation.fromBukkitLocation(schematicPositions.get(player.getUniqueId()).getA()));
            configuration.set("paste.second", WorldlessLocation.fromBukkitLocation(schematicPositions.get(player.getUniqueId()).getB()));

            configuration.save(themeFile);
        } catch (IOException e) {
            HousingPlugin.getPluginLogger().severe("Couldn't save theme config: " + session.theme().getCodeName());
            return;
        }


        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(player.getWorld());

        BlockVector3 firstWE = new LocationToBlockVector3(positions.getA());
        BlockVector3 secondWE = new LocationToBlockVector3(positions.getB());

        CuboidRegion region = new CuboidRegion(world, firstWE, secondWE);
        try (BlockArrayClipboard clipboard = new BlockArrayClipboard(region)) {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());

                forwardExtentCopy.setCopyingBiomes(false);
                forwardExtentCopy.setCopyingEntities(false);

                Operations.complete(forwardExtentCopy);

                try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(new FileOutputStream(new File(Files.THEMES_DIRECTORY.getFile(), session.theme().getCodeName() + ".schem")))) {
                    writer.write(clipboard);
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Couldn't save theme!");
                    return;
                }
            }
        }
        endSession(player.getUniqueId(), true);
    }

    public static ConfigurationSection coordsConfigurationSection(Map<Theme.CoordinateType, Integer> coordinateTypeIntegerMap, ConfigurationSection section) {
        for (Map.Entry<Theme.CoordinateType, Integer> entry : coordinateTypeIntegerMap.entrySet()) {
            section.set(entry.getKey().configName, entry.getValue());

        }
        return section;
    }

    @Subcommand(name = "exit")
    public void exitSession(Player player) {
        player.sendMessage(ChatColor.GREEN + "Ending your session!");
        endSession(player.getUniqueId(), true);
    }

    @SubcommandGroup("edit")
    @Subcommand(name = "wand")
    public void giveWand(Player player) {
        if (player.getInventory().contains(wandItem)) {
            player.sendMessage(ChatColor.RED + "You already have the selection wand!");
            return;
        }

        player.getInventory().addItem(wandItem);
    }

    public enum PositionType {
        FIRST,
        SECOND
    }

    @SubcommandGroup("edit")
    @SubcommandGroup("set")
    @Subcommand(name = "coordinate", arguments = {
            @Argument(name = "type", type = ArgumentType.ENUM, completer = "coordinateTypeClass"),
            @Argument(name = "value", type = ArgumentType.INTEGER)
    })
    public void setCoordinate(Player player, Theme.CoordinateType coordinateType, Integer value) {
        Map<Theme.CoordinateType, Integer> coordinateMap = coordinates.getOrDefault(player.getUniqueId(), new HashMap<>());
        coordinateMap.put(coordinateType, value);

        coordinates.put(player.getUniqueId(), coordinateMap);
        player.sendMessage(ChatColor.GREEN + "Successfully set the coordinate: " + coordinateType.name() + " to: " + value);
    }

    @SubcommandGroup("edit")
    @SubcommandGroup("set")
    @Subcommand(name = "paste-location", arguments = @Argument(name = "location", type = ArgumentType.LOCATION, required = false))
    public void setHousePasteLocation(Player player, Location location) {
        location = (location == null ? player.getLocation().toBlockLocation() : location.toBlockLocation());

        housePasteLocations.put(player.getUniqueId(), location);
        player.sendMessage(ChatColor.GREEN + "Successfully set paste location to: " + ArgumentType.LOCATION.stringify(player, location));
    }

    @SubcommandGroup("edit")
    @SubcommandGroup("set")
    @Subcommand(name = "spawn", arguments = @Argument(name = "location", type = ArgumentType.LOCATION, required = false))
    public void setPlayerSpawn(Player player, Location location) {
        if (location == null) location = player.getLocation();

        Pair<Location, Location> pair = spawnLocations.getOrDefault(player.getUniqueId(), new Pair<>());

        pair.setB(location);

        spawnLocations.put(player.getUniqueId(), pair);
        player.sendMessage(ChatColor.GREEN + "Successfully set the Player spawn location!");
    }

    @SubcommandGroup("edit")
    @SubcommandGroup("set")
    @Subcommand(name = "npc-spawn", arguments = @Argument(name = "location", type = ArgumentType.LOCATION, required = false))
    public void setNPCSpawn(Player player, Location location) {
        if (location == null) location = player.getLocation();

        Pair<Location, Location> pair = spawnLocations.getOrDefault(player.getUniqueId(), new Pair<>());

        pair.setA(location);

        spawnLocations.put(player.getUniqueId(), pair);
        player.sendMessage(ChatColor.GREEN + "Successfully set the NPC spawn location!");
    }

    @SubcommandGroup("edit")
    @SubcommandGroup("set")
    @Subcommand(name = "position", arguments = {@Argument(name = "type", type = ArgumentType.ENUM), @Argument(name = "position", type = ArgumentType.LOCATION)})
    public void setPosition(Player player, PositionType positionType, Location location) {
        Pair<Location, Location> pair = schematicPositions.getOrDefault(player.getUniqueId(), new Pair<>());

        switch (positionType) {
            case FIRST -> pair.setA(location.toBlockLocation());
            case SECOND -> pair.setB(location.toBlockLocation());
        }

        schematicPositions.put(player.getUniqueId(), pair);

        player.sendMessage(ChatColor.GREEN + "Set the " + positionType.name().toLowerCase() + " position to: (" + ArgumentType.LOCATION.stringify(player, location) + ")");
    }

    private boolean isEligibleAsSpawn(Location spawnLocation) {
        return spawnLocation.getBlock().getType().equals(Material.AIR) && spawnLocation.add(0, 1, 0).getBlock().getType().equals(Material.AIR);
    }

    @Subcommand(name = "delete", arguments = @Argument(name = "theme", type = ArgumentType.STRING))
    public void deleteTheme(Player player, String themeName) {
    }

}
