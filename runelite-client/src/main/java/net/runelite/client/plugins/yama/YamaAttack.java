package net.runelite.client.plugins.yama;

public enum YamaAttack
{
    MAGIC(Prayer.PROTECT_FROM_MAGIC),
    RANGE(Prayer.PROTECT_FROM_MISSILES),
    UNKNOWN(null);

    private final Prayer prayer;

    YamaAttack(Prayer prayer)
    {
        this.prayer = prayer;
    }

    public Prayer getPrayer()
    {
        return prayer;
    }
}
