package net.zoda.housing.house.instance;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.theme.Theme;
import net.zoda.housing.plugin.HousingPlugin;
import net.zoda.housing.utils.WorldlessLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.*;

import static net.zoda.housing.house.PlayerHouse.HOUSES_LOGGER;

public class LiveHouseInstance implements Listener {

    private static final Map<PlayerHouse,LiveHouseInstance> RUNNING_HOUSES = new HashMap<>();

    public static boolean isPlayerInHouse(Player player){
        return getCurrentHouseOf(player) != null;
    }

    public static LiveHouseInstance getCurrentHouseOf(Player player) {
        for(LiveHouseInstance houseInstance : RUNNING_HOUSES.values()) {
            if(houseInstance.playerInHouse(player)) return houseInstance;
        }
        return null;
    }

    private static final SlimePropertyMap PROPERTY_MAP = new SlimePropertyMap();

    @Getter private final List<Player> players = new ArrayList<>();
    @Getter private final PlayerHouse house;
    @Getter private final SlimeWorld slimeWorld;
    @Getter private final World bukkitWorld;
    private final int savingTaskID;
    private final int syncingTaskID;
    @Getter @Setter
    private Theme theme;

    @Getter @Setter
    private WorldlessLocation spawnLocation;

    public void flushUpdates(boolean notify) {

    }

    public void syncPlayers() {

        ArrayList<Player> playerArrayList = new ArrayList<>();
        for(Player worldPlayer : players) {
            if(worldPlayer.getWorld().equals(bukkitWorld)) continue;

            worldPlayer.kickPlayer(ChatColor.RED+"You left house: "+house.getHouseUUID()+" illegally!");
            playerArrayList.add(worldPlayer);
        }
        players.removeAll(playerArrayList);

        for(Player worldPlayer : bukkitWorld.getPlayers()) {
            if(players.contains(worldPlayer)) continue;

            join(worldPlayer);
        }
    }

    private void join(Player player) {

    }

    private void leave(Player player) {

    }

    public boolean playerInHouse(Player player) {
        syncPlayers();
        return players.contains(player);
    }

    public static final class UnableToLoadHouseException extends Exception{
        public UnableToLoadHouseException(String message) {
            super(message);
        }

        public UnableToLoadHouseException(Exception e) {
            super(e);
        }
    }

    private LiveHouseInstance(PlayerHouse house, CommandSender sender) throws UnableToLoadHouseException{
        this.house = house;
        this.theme = Theme.THEMES.get(house.getThemeName());

        Component loadHouseError = Component.text("Unable to load this house, please notify an admin about it!").color(NamedTextColor.RED);
        try {
            final String houseName = "housing-" + house.getHouseUUID().toString();

            if (!HousingPlugin.getTemporaryWorldLoader().worldExists(houseName)) {
               HousingPlugin.getSlime().createEmptyWorld(HousingPlugin.getTemporaryWorldLoader(),houseName,false,PROPERTY_MAP);
            }

            this.slimeWorld = HousingPlugin.getSlime().loadWorld(HousingPlugin.getTemporaryWorldLoader(), houseName, true, PROPERTY_MAP);
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
            ClipboardFormat format = ClipboardFormats.findByFile(theme.getSchematic());
            if(format == null) {
                throw new UnableToLoadHouseException("Unable to detect schematic type of file: "+theme.getSchematic().getName());
            }
            try (ClipboardReader reader = format.getReader(new FileInputStream(theme.getSchematic()))) {
                Clipboard clipboard = reader.read();

                com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(bukkitWorld);

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(0,clipboard.getMaxY()+10,0))
                            .copyEntities(false)
                            .copyBiomes(false)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(operation);
                }
            }

            if(!getHouse().getEncodedHouse().equals("")) {
                byte[] decoded = Base64.getDecoder().decode(getHouse().getEncodedHouse());

                ClipboardFormat houseFormat = ClipboardFormats.findByAlias("FAST_SCHEMATIC");
                try (ClipboardReader reader = houseFormat.getReader(new ByteArrayInputStream(decoded))) {
                    Clipboard clipboard = reader.read();

                    com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(bukkitWorld);
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(0,clipboard.getMaxY()+10,0))
                                .copyEntities(false)
                                .copyBiomes(false)
                                .ignoreAirBlocks(false)
                                .build();
                        Operations.complete(operation);
                    }
                }
            }
        }catch (Exception e) {
            if (sender != null) {
                sender.sendMessage(loadHouseError);
            }
            throw new UnableToLoadHouseException(e);
        }

        HousingPlugin housingPlugin = HousingPlugin.getInstance();
        housingPlugin.getServer().getPluginManager().registerEvents(this,housingPlugin);

        this.savingTaskID = housingPlugin.getServer().getScheduler()
                .scheduleSyncRepeatingTask(housingPlugin, () -> flushUpdates(true),0L,(20L*60L)*3L);
        this.syncingTaskID = housingPlugin.getServer().getScheduler()
                .scheduleSyncRepeatingTask(housingPlugin, this::syncPlayers,0L,20L*30L);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        flushUpdates(false);
        Bukkit.getServer().getScheduler().cancelTask(savingTaskID);
        Bukkit.getServer().getScheduler().cancelTask(syncingTaskID);
        for(Player player : bukkitWorld.getPlayers()) {
            player.kickPlayer(ChatColor.RED+"House closed!");
        }
        Bukkit.getServer().unloadWorld(bukkitWorld,false);
    }

    public static boolean isLoaded(PlayerHouse house) {
        return RUNNING_HOUSES.containsKey(house);
    }

    public static LiveHouseInstance getHouseInstance(PlayerHouse house,CommandSender player) throws UnableToLoadHouseException {
        if(!RUNNING_HOUSES.containsKey(house)) {
            RUNNING_HOUSES.put(house,new LiveHouseInstance(house,player));
        }

        return RUNNING_HOUSES.get(house);
    }

    public static LiveHouseInstance getHouseInstance(PlayerHouse house) throws UnableToLoadHouseException {
        return getHouseInstance(house,null);
    }

    @EventHandler
    public void onTeleport(PlayerChangedWorldEvent event) { if(event.getPlayer().getWorld().equals(bukkitWorld)) syncPlayers(); }
}
