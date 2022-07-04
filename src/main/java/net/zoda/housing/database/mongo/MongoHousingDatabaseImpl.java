package net.zoda.housing.database.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.zoda.housing.database.HousingDatabase;

@RequiredArgsConstructor
public class MongoHousingDatabaseImpl implements HousingDatabase {
    private final String connectionString;
    @Getter
    private final MongoClient client = new MongoClient(connectionString);

    //Database
    private final MongoDatabase housingDatabase = client.getDatabase("housing");

    //Collections
    //private final MongoCollection<PlayerIsland> playerIslands = skyblockDatabase.getCollection("houses", PlayerIsland.class);

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public MongoDatabase getHousingDatabase() {
        return skyblockDatabase;
    }

}
