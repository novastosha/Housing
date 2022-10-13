package net.zoda.housing.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


@SerializableAs("WorldlessLocation")
public record WorldlessLocation(double x, double y, double z) implements ConfigurationSerializable {


    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("x",x);
        map.put("y",y);
        map.put("z",z);

        return map;
    }

    public static WorldlessLocation deserialize(Map<String, Object> map) {
        return new WorldlessLocation((double) map.get("x"),(double) map.get("y"),(double) map.get("z"));
    }

    public Location toBukkitLocation(World world) {
        return new Location(world,x,y,z);
    }
    public static WorldlessLocation fromBukkitLocation(Location location) {
        return new WorldlessLocation(location.getX(),location.getY(),location.getZ());
    }
}
