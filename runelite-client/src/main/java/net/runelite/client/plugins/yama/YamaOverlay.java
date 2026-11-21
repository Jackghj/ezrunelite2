package net.runelite.client.plugins.yama;

import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import static net.runelite.client.ui.overlay.OverlayUtil.renderPolygon;

@Singleton
public class YamaOverlay extends Overlay
{
    private final Client client;
    private final YamaPlugin plugin;
    private final YamaConfig config;

    @Inject
    public YamaOverlay(
        Client client,
        YamaPlugin plugin,
        YamaConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        // only while Yama is alive
        if (!plugin.isYamaPresent())
        {
            return null;
        }

        // preview upcoming magic traps
        if (config.highlightMagicTraps())
        {
            for (LocalPoint lp : plugin.getPendingMagic())
            {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly != null)
                {
                    renderPolygon(g, poly, config.magicTrapBorderColor());
                }
            }
        }

        // preview upcoming range traps
        if (config.highlightRangeTraps())
        {
            for (LocalPoint lp : plugin.getPendingRange())
            {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly != null)
                {
                    renderPolygon(g, poly, config.rangeTrapBorderColor());
                }
            }
        }

        // highlight actual spawned magic traps
        if (config.highlightMagicTraps())
        {
            for (LocalPoint lp : plugin.getMagicTraps().keySet())
            {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly != null)
                {
                    renderPolygon(g, poly, config.magicTrapBorderColor());
                }
            }
        }

        // highlight actual spawned range traps
        if (config.highlightRangeTraps())
        {
            for (LocalPoint lp : plugin.getRangeTraps().keySet())
            {
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly != null)
                {
                    renderPolygon(g, poly, config.rangeTrapBorderColor());
                }
            }
        }

        return null;
    }
}
