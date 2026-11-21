package net.runelite.client.plugins.yama;

import net.runelite.client.plugins.yama.WidgetIDExt;

/**
 * Minimal WidgetInfoExt for prayer icons; copy additional entries from Nightmare if needed.
 */
public enum WidgetInfoExt
{
    PRAYER_PROTECT_FROM_MAGIC(WidgetIDExt.PRAYER_GROUP_ID, WidgetIDExt.Prayer.PROTECT_FROM_MAGIC),
    PRAYER_PROTECT_FROM_MISSILES(WidgetIDExt.PRAYER_GROUP_ID, WidgetIDExt.Prayer.PROTECT_FROM_MISSILES);

    private final int id;

    WidgetInfoExt(int group, int child)
    {
        this.id = (group << 16) | child;
    }

    public int getId()
    {
        return id;
    }
}