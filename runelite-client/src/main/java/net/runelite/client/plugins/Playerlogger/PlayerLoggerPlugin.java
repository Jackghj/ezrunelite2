package net.runelite.client.plugins.playerlogger;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Player Logger",
    enabledByDefault = false,
    description = "Records nearby players and their first-seen location on your last 25 world visits.",
    tags = {"pvp", "player", "logger", "jack", "tracking"}
)
public class PlayerLoggerPlugin extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(PlayerLoggerPlugin.class);
    private final PlayerHistoryStore history = new PlayerHistoryStore(RuneLite.RUNELITE_DIR.toPath().resolve("player-logger"));
    private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
    private final ArrayDeque<Predicate<List<WorldVisit>>> pendingWrites = new ArrayDeque<>();

    @Inject private Client client;
    @Inject private ScheduledExecutorService worker;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private PlayerLoggerConfig config;

    private PlayerLoggerPanel panel;
    private NavigationButton navButton;
    private WorldVisit visit;
    private boolean arriving;
    private boolean clearedHere;
    private long death;
    private final Set<String> seen = new HashSet<>();
    private final Set<Player> presentAtClear = new HashSet<>();
    private final Map<String, PlayerDetails> pending = new LinkedHashMap<>();

    @Provides
    PlayerLoggerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PlayerLoggerConfig.class);
    }

    @Override
    protected synchronized void startUp() {
        panel = new PlayerLoggerPanel(() -> clientThread.invokeLater(this::clearHistory));
        navButton = NavigationButton.builder()
            .tooltip("Player Logger")
            .icon(ImageUtil.loadImageResource(getClass(), "panel_icon.png"))
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
        updateHistory(visits -> false);
    }

    @Override
    protected synchronized void shutDown() {
        endVisit();
        clientToolbar.removeNavigation(navButton);
        panel = null;
    }

    @Subscribe
    public synchronized void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState()) {
            case LOGGED_IN:
                beginVisit();
                break;
            case LOGIN_SCREEN:
            case HOPPING:
            case CONNECTION_LOST:
                endVisit();
                break;
            default:
                break;
        }
    }

    private void beginVisit() {
        int world = client.getWorld();
        if (world > 0 && (visit == null || visit.world != world)) {
            endVisit();
            visit = new WorldVisit(world, System.currentTimeMillis());
            arriving = true;
        }
    }

    private void endVisit() {
        flush();
        visit = null;
        clearedHere = false;
        seen.clear();
        presentAtClear.clear();
    }

    @Subscribe
    public synchronized void onPlayerSpawned(PlayerSpawned event) {
        if (client.getGameState() == GameState.LOGGED_IN && loggingAllowed()) {
            beginVisit();
            observe(event.getPlayer());
        }
    }

    @Subscribe
    public synchronized void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        beginVisit();
        // Includes players already present when enabling the plugin or entering the Wilderness.
        if (loggingAllowed() && client.getTopLevelWorldView() != null) {
            for (Player player : client.getTopLevelWorldView().players()) {
                observe(player);
            }
        }
        flush();
    }

    private boolean loggingAllowed() {
        return !config.onlyInWilderness() || client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1;
    }

    private void observe(Player player) {
        if (visit == null || player == client.getLocalPlayer() || player.getName() == null || presentAtClear.contains(player)) {
            return;
        }
        String key = Text.standardize(player.getName());
        if (!key.isEmpty() && seen.add(key)) {
            WorldPoint location = player.getWorldView().isInstance()
                ? WorldPoint.fromLocalInstance(client, player.getLocalLocation()) : player.getWorldLocation();
            pending.put(key, new PlayerDetails(player.getName(), player.getCombatLevel(), System.currentTimeMillis(), location));
        }
    }

    @Subscribe
    public synchronized void onPlayerDespawned(PlayerDespawned event) {
        presentAtClear.remove(event.getPlayer());
    }

    @Subscribe
    public synchronized void onActorDeath(ActorDeath event) {
        if (visit != null && event.getActor() == client.getLocalPlayer()) {
            death = System.currentTimeMillis();
        }
    }

    private void flush() {
        if (visit == null || ((!arriving || clearedHere) && pending.isEmpty() && death == 0)) {
            return;
        }
        WorldVisit batchVisit = visit;
        boolean firstBatch = arriving;
        List<PlayerDetails> sightings = new ArrayList<>(pending.values());
        long batchDeath = death;
        updateHistory(visits -> WorldVisit.record(visits, batchVisit, firstBatch, sightings, batchDeath));
        arriving = false;
        clearedHere = false;
        pending.clear();
        death = 0;
    }

    private synchronized void clearHistory() {
        if (panel == null) {
            return;
        }
        visit = null;
        arriving = false;
        pending.clear();
        death = 0;
        seen.clear();
        updateHistory(visits -> {
            visits.clear();
            return true;
        });
        if (client.getGameState() == GameState.LOGGED_IN) {
            beginVisit();
            clearedHere = true;
            // Keep the list clear until another player appears.
            if (client.getTopLevelWorldView() != null) {
                for (Player player : client.getTopLevelWorldView().players()) {
                    presentAtClear.add(player);
                }
            }
        }
    }

    private void updateHistory(Predicate<List<WorldVisit>> edit) {
        PlayerLoggerPanel target = panel;
        writes = writes.thenRunAsync(() -> {
            pendingWrites.addLast(edit);
            try {
                List<WorldVisit> visits = history.update(saved -> {
                    boolean changed = false;
                    for (Predicate<List<WorldVisit>> pendingWrite : pendingWrites) {
                        changed |= pendingWrite.test(saved);
                    }
                    return changed;
                });
                pendingWrites.clear();
                SwingUtilities.invokeLater(() -> target.showVisits(visits));
            } catch (Exception ex) {
                log.warn("Could not access Player Logger history", ex);
                SwingUtilities.invokeLater(target::showStorageError);
            }
        }, worker);
    }
}
