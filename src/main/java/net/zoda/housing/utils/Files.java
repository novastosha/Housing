package net.zoda.housing.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.housing.plugin.HousingPlugin;

import java.io.File;

@RequiredArgsConstructor
public enum Files {

    CONFIG_FILE(new File(HousingPlugin.getInstance().getDataFolder(),"config.yml"),true,true),
    DATABASE_FILE(new File(HousingPlugin.getInstance().getDataFolder(), "database.yml"),true,true),
    THEMES_DIRECTORY(new File(HousingPlugin.getInstance().getDataFolder(),"themes"),false,true)
    ;

    @Getter private final File file;
    private final boolean isFile;

    public boolean isFile() {
        return isFile;
    }

    private final boolean create;

    public void onCreate() {}
    public boolean isToCreate() {
        return create;
    }

}
