package net.zoda.housing.ui.api;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class SelectionGUI<T> implements Listener {

    @Getter private final T[] elements;
    @Getter private final int maximumRows;
    private final Pages<T> pagedElements;

    public SelectionGUI(T[] elements, int maximumRows) {
        this.elements = elements;
        this.maximumRows = maximumRows;

        this.pagedElements = new Pages<T>(List.of(elements),(maximumRows*9)-2);
    }

    public SelectionGUI(T[] elements) {
        this(elements,3);
    }

    public abstract ItemStack convert(T t);

    protected void open(Player player, int page) {
        List<T> pageElements = pagedElements.getSorted().get(page);

        if(pageElements == null) {
            player.sendMessage(Component.text("Nonexistent page!").color(NamedTextColor.RED));
            return;
        }
    }

    public void open(Player player) {
        open(player,0);
    }

}
