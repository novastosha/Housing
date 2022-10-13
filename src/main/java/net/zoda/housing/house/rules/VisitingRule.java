package net.zoda.housing.house.rules;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;

@RequiredArgsConstructor
public enum VisitingRule {

    PUBLIC(true,ChatColor.GREEN),
    PRIVATE(true,ChatColor.RED)
    ;

    public final boolean single;
    public final ChatColor color;

}
