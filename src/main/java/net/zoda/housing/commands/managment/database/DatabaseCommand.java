package net.zoda.housing.commands.managment.database;

import net.zoda.api.command.ACommand;
import net.zoda.api.command.Command;
import net.zoda.api.command.DefaultRun;
import net.zoda.api.command.argument.Argument;
import net.zoda.api.command.argument.ArgumentType;
import net.zoda.api.command.subcommand.Subcommand;
import net.zoda.housing.database.HousingDatabase;
import net.zoda.housing.database.memory.MemoryHousingDatabaseImpl;
import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.plugin.HousingPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@Command(name = "database", permissions = "housing.database")
public class DatabaseCommand implements ACommand {

    private final Class<HousingDatabase.DatabaseType> databaseTypeClass = HousingDatabase.DatabaseType.class;

    @DefaultRun
    public void run(CommandSender sender) {
    }

    /**
     *
     * Writes houses stored in memory into a database
     *
     * @param sender the command sender
     * @param target the target database type
     * @param bool whether or not clear the memory
     */
    @Subcommand(name = "flush", arguments = {
            @Argument(name = "target_database", completer = "databaseTypeClass", type = ArgumentType.ENUM),
            @Argument(name = "clear",type = ArgumentType.BOOLEAN,required = false)
    })
    public void flushMemoryHouses(CommandSender sender, HousingDatabase.DatabaseType target,Boolean bool) {

        if(target.equals(HousingDatabase.DatabaseType.MEMORY)) {
            sender.sendMessage(ChatColor.RED+"You can only flush to databases other than memory!");
            return;
        }

        bool = bool != null && bool;

        if (!(HousingPlugin.getDatabase() instanceof MemoryHousingDatabaseImpl memoryHousingDatabase)) {
            sender.sendMessage(ChatColor.RED + "You can only use this command when the memory database is active!");
            return;
        }

        HousingDatabase newDatabase;
        try {
            newDatabase = target.construct();
        } catch (Exception exception) {
            sender.sendMessage(ChatColor.RED + "There was an error constructing the target database, please try again!");
            exception.printStackTrace();
            return;
        }

        for (PlayerHouse playerHouse : memoryHousingDatabase.getHouses()) {
            try {
                newDatabase.insertHouse(playerHouse);
            } catch (Exception exception) {
                sender.sendMessage(ChatColor.RED+"There was an error inserting house: "+playerHouse.getHouseUUID());
                exception.printStackTrace();
            }
        }

        if(bool) {
            memoryHousingDatabase.clear();
        }

        sender.sendMessage(ChatColor.GREEN+"Successfully flushed all houses into: \""+target.name+"\"");
    }
}
