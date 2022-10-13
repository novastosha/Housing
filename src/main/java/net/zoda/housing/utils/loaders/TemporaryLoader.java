package net.zoda.housing.utils.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;

import java.util.*;

import static net.zoda.housing.utils.Utils.apply;

public class TemporaryLoader implements SlimeLoader {

    private final Map<String, byte[]> worlds = new HashMap<>();


    public static final SlimePropertyMap BASIC_SLIME_PROPERTY_MAP = apply(new SlimePropertyMap(), slimePropertyMap ->  {
        slimePropertyMap.setValue(SlimeProperties.DIFFICULTY,"normal");
        slimePropertyMap.setValue(SlimeProperties.PVP,false);
        slimePropertyMap.setValue(SlimeProperties.ALLOW_ANIMALS,false);
        slimePropertyMap.setValue(SlimeProperties.ALLOW_MONSTERS,false);
    });


    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException {  return worlds.getOrDefault(worldName,new byte[0]); }

    @Override
    public boolean worldExists(String s) { return worlds.containsKey(s); }

    @Override
    public List<String> listWorlds() { return worlds.keySet().stream().toList(); }

    @Override
    public void saveWorld(String s, byte[] bytes, boolean b) {
        worlds.put(s,bytes);
    }

    @Override
    public void unlockWorld(String s) {}

    @Override
    public boolean isWorldLocked(String s) { return false; }

    @Override
    public void deleteWorld(String s) {
        worlds.remove(s);
    }
}
