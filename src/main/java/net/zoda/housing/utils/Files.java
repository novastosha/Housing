package net.zoda.housing.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.housing.plugin.HousingPlugin;

import java.io.File;

@RequiredArgsConstructor
public enum Files {

    CONFIG_FILE(new File(HousingPlugin.getInstance().getDataFolder(),"config.yml"),true,true),
    DATABASE_FILE(new File(HousingPlugin.getInstance().getDataFolder(), "database.yml"),true,true)
    ;

    @Getter private final File file;
    @Getter private final boolean isFile;
    private final boolean create;

    public void onCreate() {}
    public boolean isToCreate() {
        return create;
    }

}
