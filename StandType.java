package com.steelballrun;

import net.minecraft.text.Text;

public enum StandType {
    // id, daño por golpe, cooldown (ticks), alcance (bloques), empuje
    CRIMSON_VANGUARD("crimson_vanguard", 8.0f, 10, 4.5, 0.9),
    TIDAL_ECHO("tidal_echo", 4.0f, 14, 4.0, 0.3),
    ASHEN_WARDEN("ashen_warden", 5.0f, 12, 4.0, 0.5),
    GATE_WARDEN("gate_warden", 6.0f, 12, 4.0, 0.4),
    THORN_SPIRAL("thorn_spiral", 4.0f, 10, 4.0, 0.2),
    WILD_HUNT("wild_hunt", 6.0f, 10, 4.5, 0.6);

    public final String id;
    public final float damage;
    public final int cooldown;
    public final double reach;
    public final double knockback;

    StandType(String id, float damage, int cooldown, double reach, double knockback) {
        this.id = id;
        this.damage = damage;
        this.cooldown = cooldown;
        this.reach = reach;
        this.knockback = knockback;
    }

    public Text displayName() { return Text.translatable("stand.steelballrun." + id); }
    public Text abilityName() { return Text.translatable("ability.steelballrun." + id); }

    public static StandType byId(String id) {
        for (StandType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
