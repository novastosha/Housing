package net.zoda.housing.plugin;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.api.command.manager.CommandManager;
import net.zoda.housing.commands.house.HomeCommand;
import net.zoda.housing.commands.managment.HousingCommand;
import net.zoda.housing.commands.theme.ThemeCommand;
import net.zoda.housing.commands.house.VisitCommand;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.house.theme.Theme;
import net.zoda.housing.house.theme.ThemeEditSession;
import net.zoda.housing.utils.Files;
import net.zoda.housing.utils.WorldlessLocation;
import net.zoda.housing.utils.loaders.TemporaryLoader;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

public class HousingPlugin extends JavaPlugin {

    @Getter
    private static HousingConfig housingConfig;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HousingConfig {

        @Getter
        private final String defaultThemeName;
        @Getter
        private final int maximumPlayerHouses;
        @Getter
        private final int maximumHouseGuests;

    }

    @Getter
    private static HousingPlugin instance;

    @Getter
    private static Logger pluginLogger;
    @Getter
    private static SlimePlugin slime;
    @Getter
    private static HousingDatabase database;
    @Getter
    private static SlimeLoader temporaryWorldLoader;
    private static boolean debugMode = false;

    public static boolean isInDebugMode() {
        return debugMode;
    }

    @Override
    public void onEnable() {
        pluginLogger = getLogger();
        instance = this;

        ConfigurationSerialization.registerClass(WorldlessLocation.class, "WorldlessLocation");

        if (!getDataFolder().mkdirs() && !getDataFolder().exists()) {
            pluginLogger.severe("Couldn't create root plugin directory: " + getDataFolder().getPath());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        for (Files file : Files.values()) {
            if (!file.isToCreate()) continue;
            if(file.getFile().exists()) continue;
            if (!createFile(file)) {
                pluginLogger.severe("Couldn't create " + (file.isFile() ? "file" : "directory") + ": " + file.getFile().getPath());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }else{
                try {
                    file.onCreate();
                } catch (IOException e) {
                    pluginLogger.severe("There was an error creating file: " + file.getFile().getPath());
                    e.printStackTrace();
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }
        }

        debugMode = getConfig().getBoolean("debug", false);
        housingConfig = new HousingConfig(
                getConfig().getString("default-theme", "default"),
                getConfig().getInt("houses.maximum-player-houses", 20),
                getConfig().getInt("maximum-house-guests", 100)
        );

        if (!checkPlugin("SlimeWorldManager")) return;
        if (!checkPlugin("FastAsyncWorldEdit")) return;

        slime = (SlimePlugin) getServer().getPluginManager().getPlugin("SlimeWorldManager");

        deleteTemporaryWorlds();
        temporaryWorldLoader = new TemporaryLoader();

        HousingDatabase.DatabaseType type = HousingDatabase.DatabaseType.get();

        pluginLogger.info("Using: \"" + type.name + "\" as the database!");
        database = type.construct();

        if (database == null) {
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

        try {
            Theme.loadThemes(Files.THEMES_DIRECTORY.getFile());
            if (Theme.THEMES.size() == 0) {
                pluginLogger.severe("There are no themes loaded, the plugin cannot work!");
            }
        } catch (IOException e) {
            pluginLogger.severe("An error occurred while loading themes!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void deleteTemporaryWorlds() {
        File[] files = Files.TEMPORARY_THEME_EDIT_WORLDS.getFile().listFiles();
        if (files == null) return;
        Arrays.stream(files).forEach(File::delete);
    }

    private boolean checkPlugin(String name) {
        if (getServer().getPluginManager().getPlugin(name) == null) {
            pluginLogger.severe(name + " is required but missing, please install it to run this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    private boolean createFile(Files file) {
        if (file.getFile().exists()) {
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

        for (UUID uuid : Theme.EDIT_SESSIONS.keySet()) {

            ThemeEditSession themeEditSession = Theme.EDIT_SESSIONS.get(uuid);
            Player player = getServer().getPlayer(uuid);

            if (player != null && player.isOnline()
                    && themeEditSession.world().equals(player.getWorld())) {
                player.kickPlayer(ChatColor.RED + "Housing Plugin has been disabled!");
            }

            ThemeCommand.endSession(uuid, false);
        }
        Theme.EDIT_SESSIONS.clear();

        pluginLogger.info("Closing running houses...");

        LiveHouseInstance.RUNNING_HOUSES.values().forEach(LiveHouseInstance::shutdown);
        LiveHouseInstance.RUNNING_HOUSES.clear();

        deleteTemporaryWorlds();
    }
}
