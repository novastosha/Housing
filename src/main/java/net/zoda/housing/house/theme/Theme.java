package net.zoda.housing.house.theme;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Theme {

    private static final FileFilter FILE_FILTER = pathname -> pathname.isFile() && pathname.getName().endsWith(".yml") && !pathname.getName().startsWith("-");
    private static final Logger THEMES_LOGGER = Logger.getLogger("Housing Themes");
    public static final Map<String, Theme> THEMES = new HashMap<>();

    @Getter
    private final String codeName;

    @Getter
    private final String displayName;

    @Getter
    private final Material displayItem;

    @Getter
    private final File schematic;

    @Getter
    private static int maxX, maxZ;

    public static void loadThemes(File themesDir) {
        THEMES_LOGGER.info("Loading themes at: " + themesDir.getPath());

        for (File file : themesDir.listFiles(FILE_FILTER)) {
            loadTheme(file);
        }
    }

    private static void loadTheme(File file) {
        THEMES_LOGGER.info("Loading theme: " + file.getName());

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.load(file);
        } catch (InvalidConfigurationException | IOException e) {
            THEMES_LOGGER.severe("Unable to load theme file: " + file.getName());
            e.printStackTrace();
            return;
        }

        if (!checkMissingData(configuration, file.getName())) return;

        String codeName = configuration.getString("code-name");
        String displayName = configuration.getString("name");
        Material material = Material.valueOf(configuration.getString("item", "STONE").toUpperCase());
        int maxX = getMaxX(configuration);
        int maxZ = getMaxZ(configuration);

        File schematic = new File(file.getParentFile(), configuration.getString("schematic-file") + ".schem");
        if (!schematic.exists()) {
            THEMES_LOGGER.severe("Theme schematic file: " + schematic.getName()+" does not exist!");
            return;
        }

        THEMES.put(codeName,new Theme(codeName,displayName,material,schematic));
    }

    private static int getMaxX(YamlConfiguration configuration) {
        if (configuration.getInt("plot.max-x") < maxX) {
            return maxX;
        }
        maxX = configuration.getInt("plot.max-x");
        THEMES_LOGGER.severe("Maximum plot X was decreased to: "+maxX);
        return maxX;
    }

    private static int getMaxZ(YamlConfiguration configuration) {
        if (configuration.getInt("plot.max-z") < maxZ) {
            return maxZ;
        }
        maxZ = configuration.getInt("plot.max-z");
        THEMES_LOGGER.severe("Maximum plot Z was decreased to: "+maxZ);
        return maxZ;
    }

    private static boolean checkMissingData(YamlConfiguration configuration, String name) {
        if (!configuration.isString("name")) {
            THEMES_LOGGER.severe("Missing name value from theme: " + name);
            return false;
        }
        if (!configuration.isString("code-name")) {
            THEMES_LOGGER.severe("Missing code-name value from theme: " + name);
            return false;
        }

        if (!configuration.isString("item")) {
            THEMES_LOGGER.severe("Missing item value from theme: " + name);
            return false;
        }

        try {
            Material.valueOf(configuration.getString("item").toUpperCase());
        } catch (IllegalArgumentException e) {
            THEMES_LOGGER.severe("Unknown display item: " + configuration.getString("item").toUpperCase());
            return false;
        }

        if (!configuration.isInt("plot.max-x")) {
            THEMES_LOGGER.severe("Missing max-x value from plot settings from theme: " + name);
            return false;
        }

        if (!configuration.isInt("plot.max-z")) {
            THEMES_LOGGER.severe("Missing max-z value from plot settings from theme: " + name);
            return false;
        }

        if (!configuration.isString("schematic-file")) {
            THEMES_LOGGER.severe("Missing schematic-file from theme: " + name);
            return false;
        }

        return true;
    }
}
