package net.zoda.housing.house;

import lombok.Getter;
import lombok.Setter;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.house.theme.Theme;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerHouse {

    public static final Logger HOUSES_LOGGER = Logger.getLogger("Houses");

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


   /* @BsonIgnore
    @Getter
    private SlimeWorld loadedWorld;
*/

    public PlayerHouse() {

    }

    public PlayerHouse(UUID houseUUID, UUID houseOwner, List<VisitingRule> visitingRules,String themeName) {
        this.houseUUID = houseUUID;
        this.themeName = themeName;
        this.houseOwner = houseOwner;
        this.encodedHouse = "";
        this.visitingRules = visitingRules;

        if(!Theme.THEMES.containsKey(themeName)) {
            HOUSES_LOGGER.severe("House with UUID: "+houseUUID.toString()+" attempted to use nonexistent theme! Defaulting!");
            this.themeName = "default_theme";
        }
    }

    @BsonIgnore
    public boolean isLoaded() {
        return LiveHouseInstance.isLoaded(this);
    }

 /*   public boolean load(CommandSender sender) {
        if (isLoaded()) {
            return true;
        }


        Component loadHouseError = Component.text("Unable to load this house, please notify an admin about it!").color(NamedTextColor.RED);
        try {
            final String houseName = "housing-" + houseUUID.toString();

            if (!HousingPlugin.getHouseLoader().worldExists(houseName)) {
                if(sender instanceof Player player && player.getUniqueId().equals(houseOwner)) {
                    HOUSES_LOGGER.info("Creating new house of player: "+player.getUniqueId()+" with UUID: "+houseUUID.toString());
                    player.sendMessage(Component.text("Creating a new house...").color(NamedTextColor.GREEN));
                }
                HousingPlugin.getSlime().asyncCreateEmptyWorld(HousingPlugin.getHouseLoader(),houseName,false,PROPERTY_MAP);
            }

            Optional<SlimeWorld> result = HousingPlugin.getSlime().asyncLoadWorld(HousingPlugin.getHouseLoader(), houseName, false, PROPERTY_MAP).get();

            if (result.isEmpty()) {
                HOUSES_LOGGER.severe("Unable to load house with UUID: " + houseUUID);
                if (sender != null) {
                    sender.sendMessage(loadHouseError);
                }
                return false;
            }

            loadedWorld = result.get();
            return true;
        } catch (Exception e) {
            HOUSES_LOGGER.severe("Unable to load house with UUID: " + houseUUID);
            e.printStackTrace();
            if (sender != null) {
                sender.sendMessage(loadHouseError);
            }
            return false;
        }
    }

    public boolean load() {
        return load(null);
    }*/
}
