package net.zoda.housing.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.housing.plugin.HousingPlugin;

import java.awt.*;
import java.io.*;

@RequiredArgsConstructor
public enum Files {

    FILE_DATABASE(new File(HousingPlugin.getInstance().getDataFolder(), "file_database"), false, true),
    CONFIG_FILE(new File(HousingPlugin.getInstance().getDataFolder(), "config.yml"), true, true) {

        @Override
        public void onCreate() throws IOException {

            FileWriter fileWriter = new FileWriter(getFile());

            InputStream resource = HousingPlugin.getInstance().getResource("config.yml");
            if (resource == null) {
                throw new IOException("Couldn't find config.yml resource!");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(resource));

            reader.lines().forEach(s -> {
                try {
                    fileWriter.write(s + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            reader.close();
            fileWriter.flush();
            fileWriter.close();
        }
    },
    DATABASE_FILE(new File(HousingPlugin.getInstance().getDataFolder(), "database.yml"), true, true),
    THEMES_DIRECTORY(new File(HousingPlugin.getInstance().getDataFolder(), "themes"), false, true),
    TEMPORARY_THEME_EDIT_WORLDS(new File(HousingPlugin.getInstance().getDataFolder(), "temp_themes"), false, true);
    @Getter
    private final File file;
    private final boolean isFile;

    public boolean isFile() {
        return isFile;
    }

    private final boolean create;

    public void onCreate() throws IOException {
    }

    public boolean isToCreate() {
        return create;
    }

}
