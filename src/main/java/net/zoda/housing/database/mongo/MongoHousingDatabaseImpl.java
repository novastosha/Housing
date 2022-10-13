package net.zoda.housing.database.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * The {@link HousingDatabase} implementation for MongoDB
 */
public class MongoHousingDatabaseImpl implements HousingDatabase {

    public MongoHousingDatabaseImpl(String connectionString) {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));

        MongoClientSettings settings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(new ConnectionString(connectionString))
                .build();

        this.client = MongoClients.create(settings);
        this.housingDatabase = client.getDatabase("housing").withCodecRegistry(pojoCodecRegistry);
        this.playerHouses = housingDatabase.getCollection("houses", PlayerHouse.class);
    }

    @Getter
    private final MongoClient client;

    //Database
    private final MongoDatabase housingDatabase;

    //Collections
    private final MongoCollection<PlayerHouse> playerHouses;

    @Override
    public void insertHouse(PlayerHouse house,PlayerHouse... houses) {
        playerHouses.insertOne(house);

        if(houses.length != 0) {
            playerHouses.insertMany(List.of(houses));
        }
    }

    @Override
    public PlayerHouse[] getHousesOf(UUID player, VisitingRule... visitingRules) {
        List<PlayerHouse> houses = new ArrayList<>();

        try(MongoCursor<PlayerHouse> iterator = playerHouses.find(eq("houseOwner",player)).iterator()) {
            while(iterator.hasNext()){
                PlayerHouse house = iterator.next();

                if(!new HashSet<>(house.getVisitingRules()).containsAll(List.of(visitingRules))) {
                    continue;
                }

                houses.add(house);
            }
        }

        return houses.toArray(new PlayerHouse[0]);
    }

    @Override
    public PlayerHouse getHouse(UUID houseUUID) {
        return playerHouses.find(eq("_id",houseUUID)).first();
    }

    @Override
    public PlayerHouse[] getHouses() {
        List<PlayerHouse> houses = new ArrayList<>();

        for(PlayerHouse house : playerHouses.find()) {
            houses.add(house);
        }

        return houses.toArray(new PlayerHouse[0]);
    }

    public MongoDatabase getHousingDatabase() {
        return housingDatabase;
    }
}
