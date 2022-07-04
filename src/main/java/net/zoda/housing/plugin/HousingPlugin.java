package net.zoda.housing.plugin;

import com.grinderwolf.swm.api.SlimePlugin;
import lombok.Getter;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.database.mongo.MongoHousingDatabaseImpl;
import net.zoda.housing.utils.Files;
import net.zoda.housing.utils.loader.MongoLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Logger;

public class HousingPlugin extends JavaPlugin {

    @Getter
    private static HousingPlugin instance;

    @Getter private static Logger pluginLogger;
    @Getter private static SlimePlugin slime;
    @Getter private static MongoLoader mongoLoader;
    @Getter private static HousingDatabase database;

    @Override
    public void onEnable() {
        pluginLogger = getLogger();
        instance = this;

        if(!getDataFolder().mkdirs()) {
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
        }

        if(getServer().getPluginManager().getPlugin("SlimeWorldManager") == null) {
            pluginLogger.severe("SlimeWorldManager is required but missing, please install it to run this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        slime = (SlimePlugin) getServer().getPluginManager().getPlugin("SlimeWorldManager");
        database = new MongoHousingDatabaseImpl("mongodb://localhost:27017");

        if(database instanceof MongoHousingDatabaseImpl mongoHousingDatabase) {
            mongoLoader = new MongoLoader(mongoHousingDatabase);
        }
    }

    private boolean createFile(Files file) {
        try {
            return file.isFile() ? file.getFile().createNewFile() : file.getFile().mkdirs();
        } catch (IOException e) {
            return false;
        }
    }
}
