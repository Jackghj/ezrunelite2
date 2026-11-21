package net.runelite.client.plugins.yama;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@PluginDescriptor(
    name = "Yama Boss",
    description = "Highlights Yama's traps, prayer hints, and falling-boulder AoE timer",
    tags = {"boss", "pve", "yama", "trap", "prayer", "aoe", "overlay"}
)
public class YamaPlugin extends Plugin
{
    // --- NPC, projectile, trap, graphic IDs ---
    private static final int YAMA_NPC_ID      = 14176;
    private static final int PROJECTILE_MAGIC = 3254;
    private static final int PROJECTILE_RANGE = 3257;
    private static final int TRAP_MAGIC_OBJ   = 56336;
    private static final int TRAP_RANGE_OBJ   = 56335;
    private static final int SPOT_MAGIC       = 3246;
    private static final int SPOT_RANGE       = 3243;
    private static final int AOE_GRAPHIC      = 3262;

    @Inject private Client client;
    @Inject private YamaConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private YamaOverlay yamaOverlay;
    @Inject private YamaPrayerOverlay prayerOverlay;
    @Inject private YamaPrayerInfoBox prayerInfoBox;
    @Inject private YamaAoeWarningOverlay aoeOverlay;

    // --- state for traps ---
    private final Set<LocalPoint> pendingMagic = new HashSet<>();
    private final Set<LocalPoint> pendingRange = new HashSet<>();
    private final Map<LocalPoint, GameObject> magicTraps = new HashMap<>();
    private final Map<LocalPoint, GameObject> rangeTraps = new HashMap<>();

    // --- state for AoE bombs ---
    private final Set<AoeBomb> aoeBombs = new HashSet<>();

    // --- state for prayer helper ---
    private YamaAttack nextAttack = YamaAttack.UNKNOWN;
    private int ticksUntilAttack = 0;

    @Provides
    YamaConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(YamaConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(yamaOverlay);
        if (config.showPrayerOverlay())
        {
            overlayManager.add(prayerOverlay);
        }
        if (config.showPrayerInfoBox())
        {
            overlayManager.add(prayerInfoBox);
        }
        overlayManager.add(aoeOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(yamaOverlay);
        overlayManager.remove(prayerOverlay);
        overlayManager.remove(prayerInfoBox);
        overlayManager.remove(aoeOverlay);

        pendingMagic.clear();
        pendingRange.clear();
        magicTraps.clear();
        rangeTraps.clear();
        aoeBombs.clear();
        nextAttack = YamaAttack.UNKNOWN;
        ticksUntilAttack = 0;
    }

    /** Returns true if Yama is in the scene. */
    public boolean isYamaPresent()
    {
        return client.getNpcs().stream().anyMatch(npc -> npc.getId() == YAMA_NPC_ID);
    }

    /** Preview Yama’s magic/range projectiles landing tiles. */
    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        Projectile proj = event.getProjectile();
        if (!(proj.getInteracting() instanceof NPC)) return;
        NPC caster = (NPC) proj.getInteracting();
        if (caster.getId() != YAMA_NPC_ID) return;

        LocalPoint lp = proj.getTarget();
        if (proj.getId() == PROJECTILE_MAGIC)
        {
            pendingMagic.add(lp);
        }
        else if (proj.getId() == PROJECTILE_RANGE)
        {
            pendingRange.add(lp);
        }
    }

    /** Track when the trap objects actually spawn. */
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();
        LocalPoint lp = obj.getLocalLocation();

        if (pendingMagic.remove(lp) && obj.getId() == TRAP_MAGIC_OBJ)
        {
            magicTraps.put(lp, obj);
        }
        else if (pendingRange.remove(lp) && obj.getId() == TRAP_RANGE_OBJ)
        {
            rangeTraps.put(lp, obj);
        }
    }

    /** Clean up when traps despawn. */
    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        GameObject obj = event.getGameObject();
        magicTraps.remove(obj.getLocalLocation());
        rangeTraps.remove(obj.getLocalLocation());
    }

    /** Record each falling boulder (AoE) when its graphic appears. */
    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        int gfx = event.getGraphicsObject().getId();
        if (gfx == AOE_GRAPHIC)
        {
            LocalPoint lp = event.getGraphicsObject().getLocation();
            aoeBombs.add(new AoeBomb(lp, client.getTickCount()));
        }
    }

    /** Detect Yama’s spot-animation for prayer hints. */
    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        if (!(event.getActor() instanceof NPC)) return;
        NPC n = (NPC) event.getActor();
        if (n.getId() != YAMA_NPC_ID) return;

        int gfx = n.getGraphic();
        if (gfx == SPOT_MAGIC || gfx == SPOT_RANGE)
        {
            nextAttack = (gfx == SPOT_MAGIC) ? YamaAttack.MAGIC : YamaAttack.RANGE;
            ticksUntilAttack = 5;
            // show the infobox immediately
            prayerInfoBox.updateAttack(nextAttack);
        }
    }

    /** Tick down prayer timers and clear once the attack lands. */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (ticksUntilAttack > 0)
        {
            ticksUntilAttack--;
        }
        if (ticksUntilAttack == 0)
        {
            nextAttack = YamaAttack.UNKNOWN;
            prayerInfoBox.clearAttack();
        }
    }

    /* Exposed for the overlays below */
    Set<LocalPoint> getPendingMagic() { return pendingMagic; }
    Set<LocalPoint> getPendingRange() { return pendingRange; }
    Map<LocalPoint, GameObject> getMagicTraps() { return magicTraps; }
    Map<LocalPoint, GameObject> getRangeTraps() { return rangeTraps; }
    Set<AoeBomb> getAoeBombs() { return aoeBombs; }
    YamaAttack getNextAttack() { return nextAttack; }
    int getTicksUntilAttack() { return ticksUntilAttack; }

    /** Simple record of one falling-boulder AoE. */
    static class AoeBomb
    {
        private final LocalPoint loc;
        private final int startTick;

        AoeBomb(LocalPoint loc, int startTick)
        {
            this.loc = loc;
            this.startTick = startTick;
        }

        LocalPoint getLoc()
        {
            return loc;
        }

        int getStartTick()
        {
            return startTick;
        }
    }
}
