package net.zoda.housing.database.file;

import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.plugin.HousingPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class FileBasedHousingDatabaseImpl implements HousingDatabase {
    private final File workDir;
    private final Logger logger = HousingPlugin.getPluginLogger();
    private static final FileFilter HOUSE_FILTER = pathname -> pathname.isFile() && pathname.getName().endsWith(".yml") && !pathname.getName().startsWith("-");

    public FileBasedHousingDatabaseImpl(File file) {
        this.workDir = file;
        HousingPlugin.getPluginLogger().warning("Using File Based databases is not recommended!");
    }

    @Override
    public void insertHouse(PlayerHouse house, PlayerHouse... houses) {
        insertHouse0(house);
        Arrays.stream(houses).forEach(this::insertHouse0);
    }

    private void insertHouse0(PlayerHouse house) {
        try {

            File file = getHouseFile(house.getHouseUUID());

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

            configuration.set("data", null);
            configuration.set("data", serializeHouse(house));

            configuration.save(file);
        } catch (IOException ignored) {
            logger.severe("Unable to save house: " + house.getHouseUUID() + " into config!");
        }
    }


    @Override
    public PlayerHouse[] getHousesOf(UUID player, VisitingRule... visitingRules) {
        List<PlayerHouse> houses = new ArrayList<>();

        for (File file : workDir.listFiles(HOUSE_FILTER)) {
            ConfigurationSection configuration = YamlConfiguration.loadConfiguration(file);
            configuration = configuration.getConfigurationSection("data");

            if(configuration == null) continue;
            if (!configuration.contains("owner")) continue;

            UUID uuid = UUID.fromString(Objects.requireNonNull(configuration.getString("owner")));
            if (uuid.equals(player) &&
                    new HashSet<>(List.of(visitingRules)).containsAll(resolveVisitingRules(configuration.getStringList("visiting-rules")))) {
                houses.add(deserializeHouse(configuration));
            }
        }

        return houses.toArray(new PlayerHouse[0]);
    }

    @Override
    public PlayerHouse getHouse(UUID houseUUID) {
        File file = new File(workDir,houseUUID.toString()+".yml");
        if(!file.exists()) {
            return null;
        }

        return deserializeHouse(YamlConfiguration.loadConfiguration(file).getConfigurationSection("data"));
    }

    public static ConfigurationSection serializeHouse(PlayerHouse house) {
        ConfigurationSection section = new MemoryConfiguration();

        section.set("uuid", house.getHouseUUID().toString());
        section.set("owner", house.getHouseOwner().toString());
        section.set("visiting-rules", house.getVisitingRules());
        section.set("current-theme", house.getThemeName());
        section.set("encoded-house", house.getEncodedHouse());

        return section;
    }

    public static PlayerHouse deserializeHouse(ConfigurationSection section) {

        if(section == null) return null;
        if (!section.contains("uuid") || !section.contains("owner")) {
            return null;
        }

        return new PlayerHouse(UUID.fromString(section.getString("uuid"))
                , UUID.fromString(section.getString("owner"))
                , resolveVisitingRules(section.getStringList("visiting-rules"))
                , section.getString("theme-name", "default"));
    }

    private File getHouseFile(UUID houseUUID) throws IOException {
        File file = new File(workDir, houseUUID.toString() + ".yml");

        if (!file.exists()) {
            file.createNewFile();
        }

        return file;
    }

    private static List<VisitingRule> resolveVisitingRules(List<String> list) {
        List<VisitingRule> visitingRules = new ArrayList<>();

        for (String string : list) {
            try {
                visitingRules.add(VisitingRule.valueOf(string.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return Collections.singletonList(VisitingRule.PUBLIC);
            }
        }
        return visitingRules;
    }
}