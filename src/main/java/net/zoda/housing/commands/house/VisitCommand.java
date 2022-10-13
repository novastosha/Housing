package net.zoda.housing.commands.house;

import net.zoda.api.command.ACommand;
import net.zoda.api.command.Command;
import net.zoda.api.command.DefaultRun;
import net.zoda.api.command.argument.Argument;
import net.zoda.api.command.argument.ArgumentType;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.rules.VisitingRule;
import net.zoda.housing.plugin.HousingPlugin;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Command(name = "visit",playerOnly = true)
public class VisitCommand implements ACommand {

    private final HousingDatabase database = HousingPlugin.getDatabase();

    @DefaultRun(arguments = @Argument(type = ArgumentType.ANY_PLAYER,name = "target"))
    public void visitPlayer(Player player, OfflinePlayer target) {
        PlayerHouse[] houses = database.getHousesOf(target.getUniqueId(), VisitingRule.PUBLIC);

        if(houses.length == 0) {
            player.sendMessage(ChatColor.RED+"This player has no houses!");
            return;
        }
    }

}
