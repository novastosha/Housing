package net.zoda.housing.commands.theme;

import net.zoda.api.command.ACommand;
import net.zoda.api.command.Command;
import net.zoda.api.command.DefaultRun;
import org.bukkit.entity.Player;

@Command(name = "home",playerOnly = true,aliases = "homes")
public class HomeCommand implements ACommand {

    @DefaultRun
    public void run(Player player) {

    }

}
