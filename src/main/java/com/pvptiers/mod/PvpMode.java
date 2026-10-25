package com.pvptiers.mod;

import java.util.Arrays;
import java.util.Locale;

/** One icon in assets/minecraft/font/default.json for each mode. */
public enum PvpMode {
    CPVP("CPvP", '\uE101'),
    SWORD("Sword", '\uE102'),
    AXE("Axe", '\uE103'),
    MACE("Mace", '\uE104'),
    SMP("SMP", '\uE105'),
    UHC("UHC", '\uE106'),
    NETPOT("NetPot", '\uE107'),
    DIAPOT("DiaPot", '\uE108'),
    POT("Pot", '\uE109'),
    GAPPLE("Gapple", '\uE10A'),
    BOW("Bow", '\uE10B'),
    TRIDENT("Trident", '\uE10C'),
    SHIELD("Shield", '\uE10D'),
    BED("Bed", '\uE10E'),
    ANCHOR("Anchor", '\uE10F'),
    CART("Cart", '\uE110'),
    CROSSBOW("Crossbow", '\uE111'),
    ELYTRA("Elytra", '\uE112'),
    TNT("TNT", '\uE113'),
    DIAMOND("Diamond", '\uE114'),
    NETHERITE("Netherite", '\uE115'),
    CARD("Card", '\uE116');

    public final String display;
    public final char icon;

    PvpMode(String display, char icon) {
        this.display = display;
        this.icon = icon;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PvpMode parse(String value) {
        return Arrays.stream(values()).filter(m -> m.id().equalsIgnoreCase(value))
            .findFirst().orElse(null);
    }

    public static String[] ids() {
        return Arrays.stream(values()).map(PvpMode::id).toArray(String[]::new);
    }
}
