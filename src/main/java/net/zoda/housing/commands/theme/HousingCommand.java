package net.zoda.housing.commands.theme;

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
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Function;

@Command(name = "housing",playerOnly = true)
public class HousingCommand implements ACommand {

    @CommandRunCondition("*")
    private final Function<Player,Boolean> commandCondition = (player -> {
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
        PlayerHouse house = Objects.requireNonNull(LiveHouseInstance.getCurrentHouseOf(player)).getHouse();


    }
}
