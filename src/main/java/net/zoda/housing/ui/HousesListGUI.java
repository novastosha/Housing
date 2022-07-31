package net.zoda.housing.ui;

import net.zoda.housing.house.PlayerHouse;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.ui.api.SelectionGUI;
import net.zoda.housing.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static net.zoda.housing.utils.Utils.*;

public final class HousesListGUI extends SelectionGUI<PlayerHouse> {

    public HousesListGUI(PlayerHouse[] elements) {
        super(elements, 3);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack convert(PlayerHouse house) {
        return apply(new ItemStack(Material.GRASS),
                item -> item.setItemMeta(apply(item.getItemMeta(),
                                itemMeta -> {
                                    itemMeta.setDisplayName(ChatColor.GREEN+house.getHouseUUID().toString());

                                    itemMeta.setLore(makeLore(house));
                                }
                        )
                ));
    }

    private List<String> makeLore(PlayerHouse house) {
        List<String> list = new ArrayList<>();

        list.add(" ");
        list.add(prefixData("Owner",house.getHouseOwner()));
        list.add(" ");

        if(house.isLoaded()) {

            //House will not load anyway
            try {
                list.add(prefixData("Guests", LiveHouseInstance.getHouseInstance(house).getPlayers().size()));
            } catch (LiveHouseInstance.UnableToLoadHouseException ignored) {}
        }

        return list;
    }

    public void open(Player player) {
    }
}
