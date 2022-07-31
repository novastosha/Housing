package net.zoda.housing.utils;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.function.Consumer;

public final class Utils {
    public static final ChatColor PREFIX_COLOR = ChatColor.GRAY;
    public static final ChatColor DATA_COLOR = ChatColor.GREEN;

    private Utils() {
        throw new AssertionError("Utils cannot be instanced!");
    }

    public static <T> T apply(T val, Consumer<T> consumer) {
        if (val == null) return null;
        if (consumer == null) return val;

        consumer.accept(val);
        return val;
    }

    public static String prefixData(String prefix, Object data) {
        if (data == null || prefix == null) return "";


        return PREFIX_COLOR + prefix + ": " + DATA_COLOR + data.toString();
    }

    public enum ResetAttribute {

        HEALTH {
            @Override
            protected void act(Player player) {
                player.setHealth(player.getHealthScale());
                player.setRemainingAir(player.getMaximumAir());
                player.setFreezeTicks(0);
                player.setFireTicks(0);
                player.setFallDistance(0F);
                player.setArrowsInBody(0);
                player.setArrowsStuck(0);
            }
        },
        FLYING {
            @Override
            protected void act(Player player) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
        },
        HUNGER {
            @Override
            protected void act(Player player) {
                player.setFoodLevel(20);
                player.setSaturation(20F);
            }
        };

        public static final ResetAttribute[] ALL = values();

        protected abstract void act(Player player);
    }

    public static void resetPlayer(Player player, GameMode gameMode, ResetAttribute... resetAttributes) {
        player.setGameMode(gameMode);
        Arrays.stream(resetAttributes).forEach(resetAttribute -> resetAttribute.act(player));
    }
}
