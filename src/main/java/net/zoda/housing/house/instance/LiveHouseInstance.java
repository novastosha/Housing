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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.zoda.housing.house.PlayerHouse.HOUSES_LOGGER;

public class LiveHouseInstance implements Listener {

    private static final Map<PlayerHouse,LiveHouseInstance> RUNNING_HOUSES = new HashMap<>();
    private static final SlimePropertyMap PROPERTY_MAP = new SlimePropertyMap();

    
    @Getter private final PlayerHouse house;
    @Getter private final SlimeWorld slimeWorld;
    @Getter private final World bukkitWorld;
    private final int savingTaskID;
    @Getter @Setter
    private Theme theme;

    public void flushUpdates(boolean notify) {

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

            if (!HousingPlugin.getHouseLoader().worldExists(houseName)) {
               HousingPlugin.getSlime().asyncCreateEmptyWorld(HousingPlugin.getHouseLoader(),houseName,false,PROPERTY_MAP).get();
            }

            Optional<SlimeWorld> result = HousingPlugin.getSlime().asyncLoadWorld(HousingPlugin.getHouseLoader(), houseName, true, PROPERTY_MAP).get();

            if (result.isEmpty()) {
                HOUSES_LOGGER.severe("Unable to load house with UUID: " + house.getHouseUUID());
                if (sender != null) {
                    sender.sendMessage(loadHouseError);
                }
                throw new UnableToLoadHouseException("Slime couldn't load world (Result is empty)");
            }

            this.slimeWorld = result.get();
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

                ClipboardFormat houseFormat = ClipboardFormats.findByAlias("SPONGE");
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
    }

    public void shutdown() {
        flushUpdates(false);
        Bukkit.getServer().getScheduler().cancelTask(savingTaskID);
        Bukkit.getServer().unloadWorld(bukkitWorld,false);
        HandlerList.unregisterAll(this);
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
}
