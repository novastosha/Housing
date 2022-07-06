package net.zoda.housing.plugin;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import lombok.Getter;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.database.mongo.MongoHousingDatabaseImpl;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.utils.Files;
import net.zoda.housing.utils.loader.MongoLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class HousingPlugin extends JavaPlugin {

    @Getter
    private static HousingPlugin instance;

    @Getter private static Logger pluginLogger;
    @Getter private static SlimePlugin slime;
    @Getter private static SlimeLoader houseLoader;
    @Getter private static HousingDatabase database;

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
            file.onCreate();
        }

        if(!checkPlugin("SlimeWorldManager")) return;
        if(!checkPlugin("FastAsyncWorldEdit")) return;

        slime = (SlimePlugin) getServer().getPluginManager().getPlugin("SlimeWorldManager");
        database = new MongoHousingDatabaseImpl("mongodb://localhost:27017");

        if(database instanceof MongoHousingDatabaseImpl mongoHousingDatabase) {
            houseLoader = new MongoLoader(mongoHousingDatabase);

        }

        testFakeHouses();
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
}
