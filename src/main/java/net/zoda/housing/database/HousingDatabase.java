package net.zoda.housing.database;

import lombok.RequiredArgsConstructor;
import net.zoda.housing.database.file.FileBasedHousingDatabaseImpl;
import net.zoda.housing.database.memory.MemoryHousingDatabaseImpl;
import net.zoda.housing.database.mongo.MongoHousingDatabaseImpl;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.plugin.HousingPlugin;
import net.zoda.housing.utils.Files;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

public interface HousingDatabase {

    @RequiredArgsConstructor
    enum DatabaseType {


        MEMORY("Memory (Temporary)") {
            @Override
            public HousingDatabase construct() {
                try {
                    return new MemoryHousingDatabaseImpl();
                }catch (IllegalStateException exception) {
                    databasesLogger.severe(exception.getMessage());

                    databasesLogger.warning("Defaulting database type to: "+defaultType().name);
                    return defaultType().construct();
                }
            }
        },
        FILE("File Based") {
            @Override
            public HousingDatabase construct() {
                return new FileBasedHousingDatabaseImpl(Files.FILE_DATABASE.getFile());
            }
        },
        MONGO("MongoDB") {
            @Override
            public HousingDatabase construct() {
                return new MongoHousingDatabaseImpl("mongodb://localhost:27017");
            }
        };

        private static final Logger databasesLogger = Logger.getLogger("Housing Databases");

        public static DatabaseType defaultType() { return FILE; }

        public static DatabaseType get() {
            ConfigurationSection configuration = HousingPlugin.getInstance().getConfig();

            if (!configuration.contains("database.type")) {
                return defaultType();
            }

            String value = configuration.getString("database.type").toUpperCase(Locale.ROOT);
            try {
                return valueOf(value);
            } catch (IllegalArgumentException ignored) {
                databasesLogger.severe("Unknown database type: " + value + ", defaulting to: " + defaultType().name);
                return defaultType();
            }
        }

        public final String name;

        public abstract HousingDatabase construct();
    }

    void insertHouse(PlayerHouse house, PlayerHouse... houses);

    PlayerHouse[] getHousesOf(UUID player, VisitingRule... visitingRules);

    PlayerHouse getHouse(UUID houseUUID);

}
