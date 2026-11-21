package net.runelite.client.plugins.yama;

import net.runelite.api.Varbits;

/**
 * Minimal Prayer enum for Yama plugin; copy full from Nightmare plugin if needed.
 */
public enum Prayer
{
    PROTECT_FROM_MAGIC(Varbits.PRAYER_PROTECT_FROM_MAGIC, WidgetInfoExt.PRAYER_PROTECT_FROM_MAGIC),
    PROTECT_FROM_MISSILES(Varbits.PRAYER_PROTECT_FROM_MISSILES, WidgetInfoExt.PRAYER_PROTECT_FROM_MISSILES);

    private final int varbit;
    private final WidgetInfoExt widgetInfo;

    Prayer(int varbit, WidgetInfoExt widgetInfo)
    {
        this.varbit = varbit;
        this.widgetInfo = widgetInfo;
    }

    public int getVarbit()
    {
        return varbit;
    }

    public int getPrayerSpriteId()
    {
        return widgetInfo.getId();
    }

    // ← NEW: expose the WidgetInfoExt so we can fetch the widget
    public WidgetInfoExt getWidgetInfo()
    {
        return widgetInfo;
    }
}
