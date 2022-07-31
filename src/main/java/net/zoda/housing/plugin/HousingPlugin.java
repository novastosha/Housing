package net.zoda.housing.plugin;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import lombok.Getter;
import net.zoda.api.command.manager.CommandManager;
import net.zoda.housing.commands.theme.HomeCommand;
import net.zoda.housing.commands.theme.HousingCommand;
import net.zoda.housing.commands.theme.ThemeCommand;
import net.zoda.housing.commands.theme.VisitCommand;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.house.theme.Theme;
import net.zoda.housing.house.theme.ThemeEditSession;
import net.zoda.housing.utils.Files;
import net.zoda.housing.utils.loaders.TemporaryLoader;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class HousingPlugin extends JavaPlugin {

    @Getter
    private static HousingPlugin instance;

    @Getter private static Logger pluginLogger;
    @Getter private static SlimePlugin slime;
    @Getter private static HousingDatabase database;
    @Getter private static SlimeLoader temporaryWorldLoader;
    private static boolean debugMode = false;

    public static boolean isInDebugMode() {
        return debugMode;
    }

    @Override
    public void onEnable() {
        pluginLogger = getLogger();
        instance = this;

        if(!getDataFolder().mkdirs() && !getDataFolder().exists()) {
            pluginLogger.severe("Couldn't create root plugin directory: "+getDataFolder().getPath());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        for(Files file : Files.values()) {
            if(!file.isToCreate()) continue;
            if(!createFile(file)) {
                pluginLogger.severe("Couldn't create "+(file.isFile() ? "file":"directory")+": "+file.getFile().getPath());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            try {
                file.onCreate();
            } catch (IOException e) {
                pluginLogger.severe("There was an error creating file: "+file.getFile().getPath());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        debugMode = getConfig().getBoolean("debug",false);

        if(!checkPlugin("SlimeWorldManager")) return;
        if(!checkPlugin("FastAsyncWorldEdit")) return;

        slime = (SlimePlugin) getServer().getPluginManager().getPlugin("SlimeWorldManager");

        deleteTemporaryWorlds();
        temporaryWorldLoader = new TemporaryLoader();

        HousingDatabase.DatabaseType type = HousingDatabase.DatabaseType.get();

        pluginLogger.info("Using: \""+type.name+"\" as the database!");
        database = type.construct();

        if(database == null) {
            pluginLogger.severe("Couldn't connect to database!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CommandManager.getInstance().registerCommands(this,
                new ThemeCommand(),
                new HousingCommand(),
                new VisitCommand(),
                new HomeCommand()
        );

        Theme.loadThemes(Files.THEMES_DIRECTORY.getFile());
    }

    private void deleteTemporaryWorlds() {
        File[] files = Files.TEMPORARY_THEME_EDIT_WORLDS.getFile().listFiles();

        if(files == null) return;
        Arrays.stream(files).forEach(File::delete);
    }

    private void testFakeHouses() {

        UUID playerUUID = UUID.fromString("b876ec32-e396-476b-a115-8438d83c67d4");

        UUID houseUUID = UUID.fromString("9df21dfd-b6ad-4cca-94a4-64df402e2b82");
        UUID houseUUID2 = UUID.fromString("0ab20ef4-1828-4654-b06d-a9796124aff9");

        PlayerHouse playerHouse = new PlayerHouse(houseUUID,playerUUID,new ArrayList<>(List.of(VisitingRule.PUBLIC)),"default_theme");
        PlayerHouse playerHouse2 = new PlayerHouse(houseUUID2,playerUUID,new ArrayList<>(List.of(VisitingRule.PRIVATE)),"default_theme");

        database.insertHouse(playerHouse,playerHouse2);

        for(PlayerHouse house : database.getHousesOf(playerUUID)) {
            System.out.println(house.getHouseUUID().toString());
        }

        System.out.println(database.getHouse(houseUUID).getVisitingRules());
        System.out.println(database.getHouse(houseUUID2).getVisitingRules());
    }

    private boolean checkPlugin(String name) {
        if(getServer().getPluginManager().getPlugin(name) == null) {
            pluginLogger.severe(name+" is required but missing, please install it to run this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    private boolean createFile(Files file) {
        if(file.getFile().exists()) {
            return true;
        }
        try {
            return file.isFile() ? file.getFile().createNewFile() : file.getFile().mkdirs();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Used {@link org.bukkit.entity.Player#kickPlayer(String)} (Which is deprecated in Paper) to evade usage of Kyori Adventure Text
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onDisable() {
        pluginLogger.info("Ending theme editing sessions!");

        for(UUID uuid : Theme.EDIT_SESSIONS.keySet()) {

            ThemeEditSession themeEditSession = Theme.EDIT_SESSIONS.get(uuid);
            Player player = getServer().getPlayer(uuid);

            if(player != null && player.isOnline()
                    && themeEditSession.world().equals(player.getWorld())) {
                player.kickPlayer(ChatColor.RED+"Housing Plugin has been disabled!");
            }

            ThemeCommand.endSession(uuid,false);
        }
        Theme.EDIT_SESSIONS.clear();
        deleteTemporaryWorlds();
    }
}
