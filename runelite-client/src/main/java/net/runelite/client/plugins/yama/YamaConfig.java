package net.runelite.client.plugins.yama;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import java.awt.Color;

@ConfigGroup("yama")
public interface YamaConfig extends Config
{
    @ConfigItem(
        keyName = "highlightMagicTraps",
        name = "Highlight Magic Traps",
        description = "Highlight Yama's magic trap ground objects",
        position = 1
    )
    default boolean highlightMagicTraps()
    {
        return true;
    }

    @ConfigItem(
        keyName = "magicTrapBorderColor",
        name = "Magic trap border color",
        description = "Color of the outline around magic traps",
        position = 2
    )
    default Color magicTrapBorderColor()
    {
        return Color.MAGENTA;
    }

    @ConfigItem(
        keyName = "highlightRangeTraps",
        name = "Highlight Range Traps",
        description = "Highlight Yama's range trap ground objects",
        position = 3
    )
    default boolean highlightRangeTraps()
    {
        return true;
    }

    @ConfigItem(
        keyName = "rangeTrapBorderColor",
        name = "Range trap border color",
        description = "Color of the outline around range traps",
        position = 4
    )
    default Color rangeTrapBorderColor()
    {
        return Color.ORANGE;
    }

    @ConfigItem(
        keyName = "showPrayerOverlay",
        name = "Show Prayer Overlay",
        description = "Outline the correct prayer icon when Yama signals an attack",
        position = 5
    )
    default boolean showPrayerOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showPrayerInfoBox",
        name = "Show Prayer Info Box",
        description = "Display a prayer info box when Yama locks in her next attack",
        position = 6
    )
    default boolean showPrayerInfoBox()
    {
        return true;
    }
}
