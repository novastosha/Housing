package net.zoda.housing.database.memory;

import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.plugin.HousingPlugin;

import java.util.*;

public class MemoryHousingDatabaseImpl implements HousingDatabase {

    private final HashMap<UUID, PlayerHouse> houses;

    public MemoryHousingDatabaseImpl() throws IllegalStateException {
        if(!HousingPlugin.isInDebugMode()) {
            throw new IllegalStateException("Instancing "+getClass().getSimpleName()+" when debug is disabled is not allowed!");
        }

        HousingPlugin.getPluginLogger().severe("Using the Memory based database is for debug purposes only, " +
                "houses created while this database is active are going to be deleted in the next reload / restart");

        this.houses = new HashMap<>();
    }

    @Override
    public void insertHouse(PlayerHouse house, PlayerHouse... houses) {
        this.houses.put(house.getHouseUUID(),house);
        Arrays.stream(houses).forEach(loopHouse -> this.houses.put(loopHouse.getHouseUUID(),loopHouse));
    }

    @Override
    public PlayerHouse[] getHousesOf(UUID player, VisitingRule... visitingRules) {
        List<PlayerHouse> houseList = new ArrayList<>();

        for(PlayerHouse playerHouse : houses.values()) {
            if(!playerHouse.getHouseOwner().equals(player)) continue;
            if(!new HashSet<>(playerHouse.getVisitingRules()).containsAll(List.of(visitingRules))) continue;

            houseList.add(playerHouse);
        }

        return houseList.toArray(new PlayerHouse[0]);
    }

    @Override
    public PlayerHouse getHouse(UUID houseUUID) {
        return houses.get(houseUUID);
    }





}
