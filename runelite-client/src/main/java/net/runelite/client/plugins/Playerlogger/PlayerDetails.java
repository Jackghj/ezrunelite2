package net.runelite.client.plugins.playerlogger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.runelite.api.coords.WorldPoint;

final class PlayerDetails {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    final String name;
    final int combatLevel;
    final long firstSeen;
    final int x;
    final int y;
    final int plane;

    PlayerDetails(String name, int combatLevel, long firstSeen, WorldPoint location) {
        this.name = name;
        this.combatLevel = combatLevel;
        this.firstSeen = firstSeen;
        x = location.getX();
        y = location.getY();
        plane = location.getPlane();
    }

    String location() {
        return x + ", " + y + " (plane " + plane + ")";
    }

    String time() {
        return TIME.format(Instant.ofEpochMilli(firstSeen));
    }

    String summary(int world) {
        return String.format("%s | Level %d | World %d | %s | %s", name, combatLevel, world, time(), location());
    }
}