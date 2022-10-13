package net.zoda.housing.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.zoda.api.command.CommandRunCondition;
import net.zoda.housing.house.instance.LiveHouseInstance;
import net.zoda.housing.house.rules.VisitingRule;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public static String displayVisitingRules(List<VisitingRule> visitingRules) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < visitingRules.size(); i++) {
            builder.append(i == 0 ? "" : " ").append(visitingRules.get(i).color).append(visitingRules.get(i).name());
        }

        return builder.toString();
    }

    public static String prefixData(String prefix, Object data) {
        if (data == null || prefix == null) return "";


        return PREFIX_COLOR + prefix + ": " + DATA_COLOR + data.toString();
    }

    @SneakyThrows
    public static PrintWriter getPlayerPrintStream(Player player) {
        return new PrintWriter(new Writer() {

            private final ArrayList<Pair<String,Pair<Integer,Integer>>> strings = new ArrayList<>();

            @Override
            public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                String str = new String(cbuf);

                if(off < 0 || off > cbuf.length-1) {
                    throw new IOException("offset is out of bounds!");
                }

                if(len < 0 || len > cbuf.length-1) {
                    throw new IOException("length is out of bounds!");
                }

                strings.add(new Pair<>(str,new Pair<>(off,len)));
            }

            @Override
            public void flush() throws IOException {
                for(Pair<String,Pair<Integer,Integer>> pair : strings) {
                    String newString = pair.getA().substring(pair.getB().getA())
                            .substring(pair.getB().getB())
                            .replaceAll("\n","");

                    player.sendMessage(newString);
                }
            }

            @Override
            public void close() {
                strings.clear();
            }
        },true);
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
    
    @RequiredArgsConstructor
    public static final class Condition<T extends CommandSender> {
        
        private final Function<T,Boolean> function;
        
        public Condition<T> merge(Condition<T> condition) {
            return new Condition<>(t -> function.apply(t) && condition.get().apply(t));
        }
        
        public Function<T,Boolean> get() {
            return function;
        }
    }
    
    public static final Condition<Player> IN_HOUSE_CONDITION = new Condition<>(player -> {
        if (!LiveHouseInstance.isPlayerInHouse(player)) {
            player.sendMessage(Component.text("You must be in a house to execute this command!").color(NamedTextColor.RED));
            return false;
        }
        return true;
    });
}
