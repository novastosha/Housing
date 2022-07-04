package net.zoda.housing.database;

import com.mongodb.client.MongoDatabase;

public interface HousingDatabase {

    String getConnectionString();
    MongoDatabase getHousingDatabase();

}
