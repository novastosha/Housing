package net.zoda.housing.commands.house;

import net.zoda.api.command.ACommand;
import net.zoda.api.command.Command;
import net.zoda.api.command.CommandRunCondition;
import net.zoda.api.command.DefaultRun;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.utils.Utils;
import org.bukkit.entity.Player;

import java.util.function.Function;

@Command(name="stuck",playerOnly = true)
public class StuckCommand implements ACommand {

    @CommandRunCondition("*")
    private final Function<Player,Boolean> genericCondition = Utils.IN_HOUSE_CONDITION.get();

    @DefaultRun
    public void execute(Player player) {
        LiveHouseInstance instance = LiveHouseInstance.getCurrentHouseOf(player);

        instance.teleportToSpawn(player, true);
    }
}
