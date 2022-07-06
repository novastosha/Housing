package net.zoda.housing.database;

import com.mongodb.client.MongoDatabase;
import net.zoda.housing.house.PlayerHouse;

import java.util.UUID;

public interface HousingDatabase {

    String getConnectionString();
    MongoDatabase getHousingDatabase();

    void insertHouse(PlayerHouse house,PlayerHouse... houses);
    PlayerHouse[] getHousesOf(UUID player);
    PlayerHouse getHouse(UUID houseUUID);

}
