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

/**
 * The only way to communicate with the database (Save / Load data)
 *
 * @apiNote to properly do database operations, get an instance of {@link HousingDatabase} from {@link HousingPlugin#getDatabase()}
 *
 * @implNote All implementations must insert the values directly meaning there are no flush or close operations
 */
public interface HousingDatabase {

    /**
     * The {@link DatabaseType} lists all the database type implementations as enum constants
     */
    @RequiredArgsConstructor
    enum DatabaseType {

        MEMORY("Memory (Temporary)",MemoryHousingDatabaseImpl.class) {
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
        FILE("File Based",FileBasedHousingDatabaseImpl.class) {
            @Override
            public HousingDatabase construct() {
                return new FileBasedHousingDatabaseImpl(Files.FILE_DATABASE.getFile());
            }
        },
        MONGO("MongoDB",MongoHousingDatabaseImpl.class) {
            @Override
            public HousingDatabase construct() {
                //TODO: Grab the host, port, username and password from the config.
                return new MongoHousingDatabaseImpl("mongodb://localhost:27017");
            }
        };

        private static final Logger databasesLogger = Logger.getLogger("Housing Databases");

        public static DatabaseType defaultType() { return FILE; }

        /**
         *
         * @return the {@link DatabaseType} from the config
         */
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
        public final Class<? extends HousingDatabase> databaseClass;

        /**
         * Constructs a new {@link HousingDatabase} according to the chosen {@link DatabaseType} constant.
         *
         * @return a HousingDatabase instance
         */
        public abstract HousingDatabase construct();
    }

    /**
     * Inserts a house into the database
     *
     * @param house primary house
     * @param houses additional houses
     */
    void insertHouse(PlayerHouse house, PlayerHouse... houses);

    /**
     * Returns the houses owned by a specific player
     *
     * @param player the player
     * @param visitingRules <i>optional array, add visiting rules to filter the player's houses
     *
     * @return the list of houses
     */
    PlayerHouse[] getHousesOf(UUID player, VisitingRule... visitingRules);

    /**
     * Returns the house according to UUID
     *
     * @param houseUUID the house UUID
     * @return the house
     */
    PlayerHouse getHouse(UUID houseUUID);

    /**
     *
     * @return List of houses in the Database
     */
    PlayerHouse[] getHouses();

}
