package net.zoda.housing.house;

import lombok.Getter;
import lombok.Setter;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.house.theme.Theme;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerHouse {

    public static final Logger HOUSES_LOGGER = Logger.getLogger("Houses");

    @Getter
    @Setter
    private Map<Theme.CoordinateType, Integer> coordinates;

    @Getter
    @Setter
    private String themeName;

    @Getter
    @BsonId
    @Setter
    private UUID houseUUID;

    @Getter
    @Setter
    private UUID houseOwner;

    @Getter
    @Setter
    private List<VisitingRule> visitingRules;

    @Getter
    @Setter
    private String encodedHouse;


    public PlayerHouse() {}

    public PlayerHouse(UUID houseUUID, UUID houseOwner, List<VisitingRule> visitingRules, String themeName, Map<Theme.CoordinateType,Integer> createdCoordinates) {
        this.houseUUID = houseUUID;
        this.coordinates = createdCoordinates;
        this.themeName = themeName;
        this.houseOwner = houseOwner;
        this.encodedHouse = "";
        this.visitingRules = visitingRules;

        if(!Theme.THEMES.containsKey(themeName)) {
            HOUSES_LOGGER.severe("House with UUID: "+houseUUID.toString()+" attempted to use nonexistent theme! Defaulting!");
            this.themeName = Theme.DEFAULT_THEME.getCodeName();
        }
    }

    @BsonIgnore
    public boolean isLoaded() {
        return LiveHouseInstance.isLoaded(this);
    }
}
