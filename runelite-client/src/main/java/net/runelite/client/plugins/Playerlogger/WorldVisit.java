package net.runelite.client.plugins.playerlogger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.runelite.client.util.Text;

final class WorldVisit {
    private static final DateTimeFormatter START = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    final String id = UUID.randomUUID().toString();
    final int world;
    final long started;
    final Map<String, PlayerDetails> players = new LinkedHashMap<>();
    long lastDeath;

    WorldVisit(int world, long started) {
        this.world = world;
        this.started = started;
    }

    String heading() {
        return "World " + world + "  ·  " + START.format(Instant.ofEpochMilli(started));
    }

    static boolean record(List<WorldVisit> visits, WorldVisit visit, boolean arriving,
                          List<PlayerDetails> sightings, long death) {
        WorldVisit saved = visits.stream().filter(v -> v.id.equals(visit.id)).findFirst().orElse(null);
        boolean changed = false;
        if (saved == null) {
            if (!arriving) {
                return false; // This visit was cleared or aged out in another client.
            }
            saved = visit;
            visits.add(saved);
            while (visits.size() > 25) {
                visits.remove(0);
            }
            changed = true;
        }
        for (PlayerDetails player : sightings) {
            if (saved.players.putIfAbsent(Text.standardize(player.name), player) == null) {
                changed = true;
            }
        }
        if (death > saved.lastDeath) {
            saved.lastDeath = death;
            changed = true;
        }
        return changed;
    }
}