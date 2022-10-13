package net.zoda.housing.house.instance;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import lombok.Setter;
import net.zoda.api.command.argument.ArgumentType;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.theme.Theme;
import net.zoda.housing.plugin.HousingPlugin;
import net.zoda.housing.utils.Cuboid;
import net.zoda.housing.utils.WorldlessLocation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.io.ByteArrayInputStream;
import java.util.*;

import static net.zoda.housing.house.PlayerHouse.HOUSES_LOGGER;
import static net.zoda.housing.utils.Utils.*;

/**
 * This class represents an open running house.
 */
public class LiveHouseInstance implements Listener {

    public static final Map<PlayerHouse, LiveHouseInstance> RUNNING_HOUSES = new HashMap<>();
    private final Cuboid houseCuboid;

    public static boolean isPlayerInHouse(Player player) {
        return getCurrentHouseOf(player) != null;
    }

    public static LiveHouseInstance getCurrentHouseOf(Player player) {
        for (LiveHouseInstance houseInstance : RUNNING_HOUSES.values()) {
            if (houseInstance.playerInHouse(player)) return houseInstance;
        }
        return null;
    }

    private static final SlimePropertyMap PROPERTY_MAP = apply(new SlimePropertyMap(), slimePropertyMap -> {
        slimePropertyMap.setValue(SlimeProperties.DIFFICULTY, "normal");
        slimePropertyMap.setValue(SlimeProperties.PVP, true);
        slimePropertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, false);
        slimePropertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, false);
    });

    @Getter
    private final List<Player> players = new ArrayList<>();
    @Getter
    private final PlayerHouse house;
    @Getter
    private final SlimeWorld slimeWorld;
    @Getter
    private final World bukkitWorld;
    private final int savingTaskID;
    private final int syncingTaskID;
    @Getter
    @Setter
    private Theme theme;

    @Getter
    @Setter
    private WorldlessLocation spawnLocation;

    public void flushUpdates(boolean notify) {

    }

    public void syncPlayers() {

        ArrayList<Player> playerArrayList = new ArrayList<>();
        for (Player worldPlayer : players) {
            if (worldPlayer.getWorld().equals(bukkitWorld)) continue;

            worldPlayer.kickPlayer(ChatColor.RED + "You left house: " + house.getHouseUUID() + " illegally!");
            playerArrayList.add(worldPlayer);
        }
        players.removeAll(playerArrayList);

        for (Player worldPlayer : bukkitWorld.getPlayers()) {
            if (players.contains(worldPlayer)) continue;

            join(worldPlayer);
        }
    }

    public void join(Player player) {
        if (!players.contains(player)) {

            players.add(player);
            resetPlayer(player, GameMode.CREATIVE, ResetAttribute.ALL);

            teleportToSpawn(player,false);
            syncPlayers();
        }
    }

    public void leave(Player player) {

    }

    public boolean playerInHouse(Player player) {
        syncPlayers();
        return players.contains(player);
    }

    public void teleportToSpawn(Player player, boolean message) {
        player.teleport(theme.getSpawn().toBukkitLocation(bukkitWorld));

        if (message) {
            player.sendMessage(ChatColor.GREEN + "You have been sent back to spawn");
        }
    }

    public static final class UnableToLoadHouseException extends Exception {
        public UnableToLoadHouseException(String message) {
            super(message);
        }

        public UnableToLoadHouseException(Exception e) {
            super(e);
        }
    }


    private LiveHouseInstance(PlayerHouse house, CommandSender sender) throws UnableToLoadHouseException {
        this.house = house;

        for (Map.Entry<Theme.CoordinateType, Integer> coordinate : house.getCoordinates().entrySet()) {
            if (Theme.MAXIMUM_COORDINATES.get(coordinate.getKey()) < coordinate.getValue()) {

                HOUSES_LOGGER.severe("Attempted to load house (" + house.getHouseUUID().toString() + ") with bigger maximum coordinates than the current ones, printing requirements!");

                //It is necessary to nest loops here
                for (Map.Entry<Theme.CoordinateType, Integer> coord : house.getCoordinates().entrySet()) {
                    HOUSES_LOGGER.warning(coord.getKey().name() + " (" + coord.getKey().configName + ") (got: " + coord.getValue() + ", maximum: " + Theme.MAXIMUM_COORDINATES.get(coord.getKey()) + ")");
                }

                throw new UnableToLoadHouseException("This house was created with bigger maximum coordinates than the current ones, " +
                        "which means it cannot be loaded until a theme with the stats above is available.");
            }
        }

        this.theme = Theme.THEMES.get(house.getThemeName());

        String loadHouseError = ChatColor.RED + "Unable to load this house, please notify an admin about it!";
        try {

            final String houseName = "housing-" + house.getHouseUUID().toString();

            this.slimeWorld = HousingPlugin.getSlime().createEmptyWorld(HousingPlugin.getTemporaryWorldLoader(), houseName, false, PROPERTY_MAP);
            HousingPlugin.getSlime().generateWorld(slimeWorld);
            this.bukkitWorld = HousingPlugin.getInstance().getServer().getWorld(houseName);
        } catch (Exception e) {
            HOUSES_LOGGER.severe("Unable to load house with UUID: " + house.getHouseUUID());
            e.printStackTrace();


            if (sender != null) {
                sender.sendMessage(loadHouseError);
            }
            throw new UnableToLoadHouseException(e);
        }

        try {
            pasteHouseAndTheme();
        } catch (Exception e) {
            if (sender != null) {
                sender.sendMessage(loadHouseError);
            }
            throw new UnableToLoadHouseException(e);
        }

        if (sender instanceof Player player) {
            join(player);
        }

        this.houseCuboid = getCuboid();


        HousingPlugin housingPlugin = HousingPlugin.getInstance();
        housingPlugin.getServer().getPluginManager().registerEvents(this, housingPlugin);

        this.savingTaskID = housingPlugin.getServer().getScheduler()
                .scheduleSyncRepeatingTask(housingPlugin, () -> flushUpdates(true), 0L, (20L * 60L) * 3L);
        this.syncingTaskID = housingPlugin.getServer().getScheduler()
                .scheduleSyncRepeatingTask(housingPlugin, this::syncPlayers, 0L, 20L * 30L);
    }

    private Cuboid getCuboid() {
        WorldlessLocation secondCorner = new WorldlessLocation(
                (theme.getPasteLocation().x() + Theme.MAXIMUM_COORDINATES.get(Theme.CoordinateType.MAX_X))
                , Theme.MAXIMUM_COORDINATES.get(Theme.CoordinateType.MAX_HEIGHT) + theme.getPasteLocation().y()
                , (theme.getPasteLocation().z() + Theme.MAXIMUM_COORDINATES.get(Theme.CoordinateType.MAX_Z))
        );

        int y = (int) (theme.getPasteLocation().y() - Theme.MAXIMUM_COORDINATES.get(Theme.CoordinateType.MAX_DEPTH));
        Location firstCorner = new Location(bukkitWorld, theme.getPasteLocation().x(), y, theme.getPasteLocation().z());

        System.out.println(secondCorner);
        System.out.println(ArgumentType.LOCATION.stringify(null, firstCorner));


        return new Cuboid(firstCorner, secondCorner.toBukkitLocation(bukkitWorld));
    }

    private void pasteHouseAndTheme() throws Exception {
        ClipboardFormat format = BuiltInClipboardFormat.FAST;
        try (ClipboardReader reader = format.getReader(new ByteArrayInputStream(theme.getSchematic()))) {
            Clipboard clipboard = reader.read();

            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(bukkitWorld);

            int calculatedY = (int) (Math.min(theme.getPasteSecond().y(), theme.getPasteFirst().y()));
            int calculatedX = (int) (Math.min(theme.getPasteFirst().x(), theme.getPasteSecond().x()));
            int calculatedZ = (int) (Math.min(theme.getPasteFirst().z(), theme.getPasteSecond().z()));

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(calculatedX, calculatedY, calculatedZ))
                        .copyEntities(false)
                        .copyBiomes(false)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
        }

        if (!getHouse().getEncodedHouse().equals("")) {
            byte[] decoded = Base64.getDecoder().decode(getHouse().getEncodedHouse());

            int y = (int) (theme.getPasteLocation().y() - Theme.MAXIMUM_COORDINATES.get(Theme.CoordinateType.MAX_DEPTH));

            ClipboardFormat houseFormat = BuiltInClipboardFormat.FAST;
            try (ClipboardReader reader = houseFormat.getReader(new ByteArrayInputStream(decoded))) {
                Clipboard clipboard = reader.read();

                com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(bukkitWorld);
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(theme.getPasteLocation().x(), y, theme.getPasteLocation().z()))
                            .copyEntities(false)
                            .copyBiomes(false)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(operation);
                }
            }
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        flushUpdates(false);
        Bukkit.getServer().getScheduler().cancelTask(savingTaskID);
        Bukkit.getServer().getScheduler().cancelTask(syncingTaskID);
        for (Player player : bukkitWorld.getPlayers()) {
            player.kickPlayer(ChatColor.RED + "House closed!");
        }
        Bukkit.getServer().unloadWorld(bukkitWorld, false);

        try {
            RUNNING_HOUSES.remove(house);
        } catch (ConcurrentModificationException ignored) {
        }
    }

    public static boolean isLoaded(PlayerHouse house) {
        return RUNNING_HOUSES.containsKey(house);
    }

    public static LiveHouseInstance getHouseInstance(PlayerHouse house, CommandSender player) throws UnableToLoadHouseException {
        if (!RUNNING_HOUSES.containsKey(house)) {
            RUNNING_HOUSES.put(house, new LiveHouseInstance(house, player));
        }

        return RUNNING_HOUSES.get(house);
    }

    public static LiveHouseInstance getHouseInstance(PlayerHouse house) throws UnableToLoadHouseException {
        return getHouseInstance(house, null);
    }

    @EventHandler
    public void onTeleport(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld().equals(bukkitWorld)) syncPlayers();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        event.setCancelled(checkIfOutOfBounds(event.getPlayer(), event.getBlock()));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        event.setCancelled(checkIfOutOfBounds(event.getPlayer(), event.getBlock()));
    }

    private boolean checkIfOutOfBounds(Player player, Block block) {
        if (!block.getWorld().equals(bukkitWorld)) return false;
        if (!houseCuboid.contains(block)) {
            player.sendMessage(ChatColor.RED + "You can only break block within your house's region");
            return true;
        }

        return false;
    }
}
