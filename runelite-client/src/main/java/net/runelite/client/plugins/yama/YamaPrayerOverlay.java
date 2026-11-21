package net.runelite.client.plugins.yama;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import static net.runelite.client.ui.overlay.OverlayUtil.renderPolygon;

@Singleton
public class YamaPrayerOverlay extends Overlay
{
    private final Client client;
    private final YamaPlugin plugin;
    private final YamaConfig config;

    @Inject
    public YamaPrayerOverlay(
        Client client,
        YamaPlugin plugin,
        YamaConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showPrayerOverlay())
        {
            return null;
        }

        YamaAttack next = plugin.getNextAttack();
        if (next == YamaAttack.UNKNOWN || plugin.getTicksUntilAttack() > 1)
        {
            return null;
        }

        Prayer prayer = next.getPrayer();
        if (prayer == null)
        {
            return null;
        }

        // green if already active, red if you need to switch
        Color color = client.getVarbitValue(prayer.getVarbit()) == 1
            ? Color.GREEN
            : Color.RED;

        // unpack your WidgetInfoExt.getId()
        int packed = prayer.getWidgetInfo().getId();
        int group  = packed >>> 16;
        int child  = packed & 0xFFFF;

        Widget widget = client.getWidget(group, child);
        if (widget == null)
        {
            return null;
        }

        Rectangle b = widget.getBounds();
        Polygon poly = new Polygon(
            new int[] {b.x, b.x + b.width, b.x + b.width, b.x},
            new int[] {b.y, b.y, b.y + b.height, b.y + b.height},
            4
        );

        renderPolygon(graphics, poly, color);
        return null;
    }
}
