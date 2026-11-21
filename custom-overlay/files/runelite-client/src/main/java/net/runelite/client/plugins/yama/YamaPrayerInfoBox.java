package net.runelite.client.plugins.yama;

import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.yama.YamaConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

@Singleton
public class YamaPrayerInfoBox extends Overlay
{
    private static final int BOX_SIZE = 36;
    private static final java.awt.Color NOT_ACTIVE_BG = new java.awt.Color(150, 0, 0, 150);

    private final Client client;
    private final YamaPlugin plugin;
    private final YamaConfig config;
    private final SpriteManager spriteManager;
    private final PanelComponent panel = new PanelComponent();

    private YamaAttack attack = YamaAttack.UNKNOWN;

    @Inject
    public YamaPrayerInfoBox(
        Client client,
        YamaPlugin plugin,
        YamaConfig config,
        SpriteManager spriteManager)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.spriteManager = spriteManager;

        setPosition(OverlayPosition.BOTTOM_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);

        panel.setPreferredSize(new Dimension(BOX_SIZE, BOX_SIZE));
    }

    /** Persist the infobox until the attack lands or Yama despawns */
    void updateAttack(YamaAttack next)
    {
        this.attack = next;
    }

    /** Clear as soon as the attack resolves or Yama leaves */
    void clearAttack()
    {
        this.attack = YamaAttack.UNKNOWN;
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!config.showPrayerInfoBox()
            || attack == YamaAttack.UNKNOWN
            || !plugin.isYamaPresent())
        {
            return null;
        }

        // Only while waiting for that queued attack
        if (plugin.getTicksUntilAttack() > 0)
        {
            Prayer p = attack.getPrayer();
            int sprite = p == Prayer.PROTECT_FROM_MAGIC
                ? SpriteID.PRAYER_PROTECT_FROM_MAGIC
                : SpriteID.PRAYER_PROTECT_FROM_MISSILES;
            BufferedImage icon = spriteManager.getSprite(sprite, 0);
            boolean active = client.getVarbitValue(p.getVarbit()) == 1;

            panel.getChildren().clear();
            panel.setBackgroundColor(active
                ? ComponentConstants.STANDARD_BACKGROUND_COLOR
                : NOT_ACTIVE_BG);
            panel.getChildren().add(new ImageComponent(icon));
            return panel.render(g);
        }

        return null;
    }
}
