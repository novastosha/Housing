package net.zoda.housing.house.theme;

import org.bukkit.World;

import java.util.UUID;

public record ThemeEditSession(Theme theme, World world, UUID editor) {}
