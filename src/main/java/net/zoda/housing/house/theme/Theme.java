package net.zoda.housing.house.theme;

import com.mongodb.bulk.WriteConcernError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.housing.plugin.HousingPlugin;
import net.zoda.housing.utils.WorldlessLocation;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class Theme {

    public static Theme DEFAULT_THEME;

    @RequiredArgsConstructor
    public enum CoordinateType {
        MAX_X("max-x"),
        MAX_Z("max-z"),

        MAX_DEPTH("max-depth"),
        MAX_HEIGHT("max-height");

        public final String configName;

        public static CoordinateType getByKey(String key) {
            for (CoordinateType coordinateType : values()) {
                if (coordinateType.configName.equals(key)) return coordinateType;
            }

            return null;
        }
    }

    public static final Map<UUID, ThemeEditSession> EDIT_SESSIONS = new HashMap<>();
    private static final FileFilter FILE_FILTER = pathname -> pathname.isFile() && pathname.getName().endsWith(".yml") && !pathname.getName().startsWith("-");
    private static final Logger THEMES_LOGGER = Logger.getLogger("Housing Themes");
    public static final Map<String, Theme> THEMES = new HashMap<>();

    public static final Map<CoordinateType, Integer> MAXIMUM_COORDINATES = new HashMap<>();

    static {
        Arrays.stream(CoordinateType.values()).forEach(coordinateType -> MAXIMUM_COORDINATES.put(coordinateType, 0));
    }

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
    private final byte[] schematic;

    @Getter
    private final File file;

    @Getter
    private final WorldlessLocation pasteLocation;

    @Getter
    private final WorldlessLocation pasteFirst;

    @Getter
    private final WorldlessLocation pasteSecond;

    public static void loadThemes(File themesDir) throws IOException {

        THEMES_LOGGER.info("Loading themes at: " + themesDir.getPath());

        for (File file : themesDir.listFiles(FILE_FILTER)) {
            loadTheme(file);
        }

        if(!THEMES.containsKey("default")) {
            try (InputStream schematicStream = HousingPlugin.getInstance().getResource("default_schematic.schem")) {
                if (DEFAULT_THEME == null) {
                    THEMES_LOGGER.info("Injecting default theme...");
                    if (schematicStream == null) {
                        throw new IOException("Unable to load the default theme schematic! (schematicStream is null!)");
                    }

                    MAXIMUM_COORDINATES.put(CoordinateType.MAX_DEPTH, 15);
                    MAXIMUM_COORDINATES.put(CoordinateType.MAX_HEIGHT, 60);
                    MAXIMUM_COORDINATES.put(CoordinateType.MAX_X, 31);
                    MAXIMUM_COORDINATES.put(CoordinateType.MAX_Z, 31);

                    //FIXME: Fix coordinates
                    //Hardcoded for fallback purposes
                    DEFAULT_THEME = new Theme("default", ChatColor.GREEN + "Default Theme", Material.OAK_SAPLING,
                            schematicStream.readAllBytes(), null, new WorldlessLocation(0, 0, 0),new WorldlessLocation(0,0,0),new WorldlessLocation(0,0,0)).setEnabled(true).setSpawn(new WorldlessLocation(0, 0, 0)).setNPCSpawn(new WorldlessLocation(0, 0, 0));

                    THEMES.put("default", DEFAULT_THEME);
                }
            }
        }else if(DEFAULT_THEME == null){
            Theme supposed = THEMES.get("default");
            if(!supposed.enabled) {
                throw new IOException("Default theme is disabled");
            }

            THEMES_LOGGER.info("The default theme is now: " + supposed.codeName);
            DEFAULT_THEME = supposed;
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

        if (THEMES.containsKey(codeName) && !codeName.equals("default")) {
            THEMES_LOGGER.warning("A theme with this codename already exists! (" + codeName + ")");
            return;
        }

        String displayName = configuration.getString("name");
        Material material = Material.valueOf(configuration.getString("item", "STONE").toUpperCase());

        for (CoordinateType type : CoordinateType.values()) {
            getMax(type, configuration);
        }

        boolean enabled = true;
        File schematic = configuration.getString("schematic-file") != null ? new File(file.getParentFile(), configuration.getString("schematic-file")) : null;
        if (schematic != null && !schematic.exists()) {
            schematic = null;
            enabled = false;
        }

        WorldlessLocation npcSpawn = configuration.getSerializable("npc-spawn", WorldlessLocation.class);
        WorldlessLocation spawn = configuration.getSerializable("spawn", WorldlessLocation.class);
        WorldlessLocation pasteLocation = configuration.getSerializable("house-paste-location",WorldlessLocation.class);

        WorldlessLocation firstPos = configuration.getSerializable("paste.first",WorldlessLocation.class);
        WorldlessLocation secondPos = configuration.getSerializable("paste.second",WorldlessLocation.class);

        if(firstPos == null || secondPos == null) {
            THEMES_LOGGER.severe("Missing pasting positions from theme: "+codeName);
            enabled = false;
        }

        if (npcSpawn == null || spawn == null || pasteLocation == null) {
            enabled = false;
        }

        byte[] arr = new byte[0];
        if (schematic != null) {
            try {
                FileInputStream inputStream = new FileInputStream(schematic);
                arr = new byte[(int) schematic.length()];

                inputStream.read(arr);
                inputStream.close();
            } catch (IOException e) {
                THEMES_LOGGER.severe("There was an error reading the schematic! please try again!");
            }
        }

        for (CoordinateType type : CoordinateType.values()) {
            if (!configuration.isInt("coordinates." + type.configName)) {
                THEMES_LOGGER.severe("Missing " + type.configName + " value from plot settings from theme: " + codeName);
                enabled = false;
            }
        }

        THEMES.put(codeName, new Theme(codeName, displayName, material, arr, file,pasteLocation,firstPos,secondPos).setEnabled(enabled).setSpawn(spawn).setNPCSpawn(npcSpawn));
        if (HousingPlugin.getHousingConfig().getDefaultThemeName().equals(codeName)) {
            THEMES_LOGGER.info("The default theme is now: " + codeName);
            DEFAULT_THEME = THEMES.get(codeName);
        }
    }

    private Theme setSpawn(WorldlessLocation spawn) {
        this.spawn = spawn;
        return this;
    }

    private Theme setNPCSpawn(WorldlessLocation npcSpawn) {
        this.npcSpawn = npcSpawn;
        return this;
    }

    private static void getMax(CoordinateType type, YamlConfiguration configuration) {
        if (configuration.getInt("coordinates." + type.configName) < MAXIMUM_COORDINATES.get(type)) {
            return;
        }
        MAXIMUM_COORDINATES.put(type, configuration.getInt("coordinates." + type.configName));
        THEMES_LOGGER.info(type.name() + " was decreased to: " + MAXIMUM_COORDINATES.get(type));
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

        return true;
    }
}
