package net.zoda.housing.utils;

import com.sk89q.worldedit.math.BlockVector3;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

@RequiredArgsConstructor
public class LocationToBlockVector3 extends BlockVector3 {
    private final Location location;

    @Override
    public int getX() {
        return location.getBlockX();
    }

    @Override
    public int getY() {
        return location.getBlockY();
    }

    @Override
    public int getZ() {
        return location.getBlockZ();
    }
}
