package net.zoda.housing.house.theme;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.housing.utils.WorldlessLocation;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class Theme {

    public static final Map<UUID,ThemeEditSession> EDIT_SESSIONS = new HashMap<>();
    private static final FileFilter FILE_FILTER = pathname -> pathname.isFile() && pathname.getName().endsWith(".yml") && !pathname.getName().startsWith("-");
    private static final Logger THEMES_LOGGER = Logger.getLogger("Housing Themes");
    public static final Map<String, Theme> THEMES = new HashMap<>();

    @Getter
    private boolean enabled = true;

    @Getter
    private WorldlessLocation npcSpawn;

    @Getter
    private WorldlessLocation spawn;

    public Theme setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

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
        getMaxX(configuration);
        getMaxZ(configuration);

        boolean enabled = true;
        File schematic = new File(file.getParentFile(), configuration.getString("schematic-file",""));
        if (!schematic.exists()) {
            schematic = null;
            enabled = false;
        }

        WorldlessLocation npcSpawn =  configuration.getSerializable("npc-spawn",WorldlessLocation.class);
        WorldlessLocation spawn =  configuration.getSerializable("spawn",WorldlessLocation.class);

        THEMES.put(codeName,new Theme(codeName,displayName,material,schematic).setEnabled(enabled).setSpawn(spawn).setNPCSpawn(npcSpawn));
    }

    private Theme setSpawn(WorldlessLocation spawn) {
        this.spawn =spawn;
        return this;
    }

    private Theme setNPCSpawn(WorldlessLocation npcSpawn) {
        this.npcSpawn = npcSpawn;
        return this;
    }

    private static void getMaxX(YamlConfiguration configuration) {
        if (configuration.getInt("plot.max-x") < maxX) {
            return;
        }
        maxX = configuration.getInt("plot.max-x");
        THEMES_LOGGER.info("Maximum plot X was decreased to: "+maxX);
    }

    private static void getMaxZ(YamlConfiguration configuration) {
        if (configuration.getInt("plot.max-z") < maxZ) {
            return;
        }
        maxZ = configuration.getInt("plot.max-z");
        THEMES_LOGGER.info("Maximum plot Z was decreased to: "+maxZ);
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

        return true;
    }
}
