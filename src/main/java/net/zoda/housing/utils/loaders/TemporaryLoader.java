package net.zoda.housing.utils.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;

import java.util.ArrayList;
import java.util.List;

import static net.zoda.housing.utils.Utils.apply;

public class TemporaryLoader implements SlimeLoader {

    public static final SlimePropertyMap BASIC_SLIME_PROPERTY_MAP = apply(new SlimePropertyMap(), slimePropertyMap ->  {
        slimePropertyMap.setValue(SlimeProperties.DIFFICULTY,"normal");
        slimePropertyMap.setValue(SlimeProperties.PVP,false);
        slimePropertyMap.setValue(SlimeProperties.ALLOW_ANIMALS,false);
        slimePropertyMap.setValue(SlimeProperties.ALLOW_MONSTERS,false);
    });


    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException {
        throw new UnknownWorldException("Temporary loader cannot load worlds!");
    }

    @Override
    public boolean worldExists(String s) { return false; }

    @Override
    public List<String> listWorlds() { return new ArrayList<>(); }

    @Override
    public void saveWorld(String s, byte[] bytes, boolean b) {}

    @Override
    public void unlockWorld(String s) {}

    @Override
    public boolean isWorldLocked(String s) { return true; }

    @Override
    public void deleteWorld(String s) {}
}
