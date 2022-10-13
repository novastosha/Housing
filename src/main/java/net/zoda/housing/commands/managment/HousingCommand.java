package net.zoda.housing.commands.managment;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.zoda.api.command.ACommand;
import net.zoda.api.command.Command;
import net.zoda.api.command.CommandRunCondition;
import net.zoda.api.command.DefaultRun;
import net.zoda.api.command.argument.Argument;
import net.zoda.api.command.argument.ArgumentType;
import net.zoda.api.command.subcommand.Subcommand;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.house.theme.Theme;
import net.zoda.housing.plugin.HousingPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Command(name = "housing",playerOnly = true)
public class HousingCommand implements ACommand {

    @CommandRunCondition("test")
    private final Function<Player,Boolean> adminCondition = (player -> {
        if(player.hasPermission("housing.test")) {
            return true;
        }

        player.sendMessage(ChatColor.RED+"Not enough permissions!");
        return false;
    });

    @CommandRunCondition({"name","save"})
    private final Function<Player,Boolean> genericCondition = (player -> {
        boolean playerInHouse = LiveHouseInstance.isPlayerInHouse(player);

        if(!playerInHouse) {
            player.sendMessage(Component.text("You must be in a house to execute this command!").color(NamedTextColor.RED));
            return false;
        }

        //TODO: Check permissions

        return true;
    });

    @DefaultRun
    public void run(Player player) {}

    @Subcommand(name = "name",arguments = @Argument(name = "new_name",type = ArgumentType.STRING))
    public void renameHouse(Player player,String newName) {
        PlayerHouse house = LiveHouseInstance.getCurrentHouseOf(player).getHouse();


    }

    @Subcommand(name = "save")
    public void save(Player player) {
        LiveHouseInstance houseInstance = LiveHouseInstance.getCurrentHouseOf(player);

        houseInstance.flushUpdates(true);
    }

    @Subcommand(name = "test")
    public void test(Player player){
        PlayerHouse house = new PlayerHouse(UUID.fromString("1e3529cd-885f-44cb-91aa-b226d1788f07"),player.getUniqueId(), List.of(VisitingRule.PUBLIC), Theme.DEFAULT_THEME.getCodeName(), Theme.MAXIMUM_COORDINATES);

        HousingPlugin.getDatabase().insertHouse(house);
        try {
            LiveHouseInstance.getHouseInstance(house,player);
        } catch (LiveHouseInstance.UnableToLoadHouseException e) {
            e.printStackTrace();
        }
    }
}
