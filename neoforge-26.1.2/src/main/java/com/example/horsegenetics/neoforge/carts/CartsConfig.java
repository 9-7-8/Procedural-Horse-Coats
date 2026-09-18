/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts;

import com.example.horsegenetics.common.cart.CartKind;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cart settings.
 *
 * <h2>What is gone from upstream, and why</h2>
 * Upstream gave every cart a {@code pull_speed}: one flat multiplier applied to
 * whatever animal was hitched to it. <b>That option no longer exists</b>, and it
 * could not, because how fast a cart moves is now
 * {@link com.example.horsegenetics.common.cart.CartDraft} reading the horse's
 * pulling ability and its speed. A config value that overrode it would make the
 * genetics decorative, which is the opposite of the point.
 *
 * <p>The per-cart weights live on {@link CartKind#load()} instead - in
 * {@code common/}, beside the model that reads them and the test that pins
 * their ordering.
 *
 * <p>Also gone: {@code slow_speed} and its keybind (the toggle was not ported),
 * and the hand cart's whole section (the vehicle was not ported).
 *
 * <p>Upstream's {@code pull_speed} default also sat outside its own declared
 * range on the wagon - min {@code -1.0}, max {@code -0.2}, default {@code 0.0} -
 * so NeoForge corrected it and warned on every config load. Removing the option
 * removes the bug with it.
 */
public final class CartsConfig {

    public static Common get() {
        return Holder.COMMON;
    }

    public static ModConfigSpec spec() {
        return Holder.COMMON_SPEC;
    }

    public static Client getClient() {
        return Holder.CLIENT;
    }

    public static ModConfigSpec clientSpec() {
        return Holder.CLIENT_SPEC;
    }

    private static final class Holder {
        private static final Common COMMON;
        private static final ModConfigSpec COMMON_SPEC;
        private static final Client CLIENT;
        private static final ModConfigSpec CLIENT_SPEC;

        static {
            final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
            COMMON = specPair.getLeft();
            COMMON_SPEC = specPair.getRight();
            final Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
            CLIENT = clientPair.getLeft();
            CLIENT_SPEC = clientPair.getRight();
        }
    }

    /**
     * What the supply cart draws on its bed.
     *
     * <p>The supply cart renders its actual cargo - tools, flowers, paintings,
     * a spare wheel - as little models sitting in the cart rather than as a
     * closed box. That is the nicest thing in the whole cart system and also
     * the most expensive, so every piece of it can be turned off separately and
     * falls back to a plain floating item.
     */
    public static class Client {
        public final ModConfigSpec.BooleanValue renderSupplies;
        public final ModConfigSpec.BooleanValue renderSupplyGear;
        public final ModConfigSpec.BooleanValue renderSupplyFlowers;
        public final ModConfigSpec.BooleanValue renderSupplyPaintings;
        public final ModConfigSpec.BooleanValue renderSupplyWheel;
        public final ModConfigSpec.ConfigValue<ArrayList<String>> renderBlacklist;

        Client(final ModConfigSpec.Builder builder) {
            builder.comment("Rendering of the cargo visible in a supply cart");
            this.renderSupplies = builder.comment("Draw cargo in the cart at all")
                    .define("render_supplies", true);
            this.renderSupplyGear = builder.comment("Falls back to a plain item if false")
                    .define("render_supply_gear", true);
            this.renderSupplyFlowers = builder.comment("Falls back to a plain item if false")
                    .define("render_supply_flowers", true);
            this.renderSupplyPaintings = builder.comment("Falls back to a plain item if false")
                    .define("render_supply_paintings", true);
            this.renderSupplyWheel = builder.comment("Falls back to a plain item if false")
                    .define("render_supply_wheel", true);
            final ArrayList<String> blacklist = new ArrayList<>();
            blacklist.add("minecraft:trident");
            blacklist.add("minecraft:decorated_pot");
            blacklist.add("#minecraft:buttons");
            blacklist.add("#minecraft:banners");
            this.renderBlacklist = builder.comment("Never draw these blocks and items as cargo")
                    .define("render_item_blacklist", blacklist);
        }
    }

    public static class Common {

        private final Map<CartKind, CartConfig> carts = new EnumMap<>(CartKind.class);

        Common(final ModConfigSpec.Builder builder) {
            builder.comment("Per-vehicle settings. How fast a horse pulls one is genetics,",
                            "not configuration - see the mod's Carts wiki page.")
                   .push("carts");
            for (final CartKind kind : CartKind.values()) {
                this.carts.put(kind, new CartConfig(builder, kind));
            }
            builder.pop();
        }

        public CartConfig of(final CartKind kind) {
            return this.carts.get(kind);
        }
    }

    public static class CartConfig {

        /**
         * Which entities may pull this cart. Empty - the default - means
         * anything that can wear a saddle and is not steered by a held item,
         * which is every horse this mod cares about.
         */
        public final ModConfigSpec.ConfigValue<ArrayList<String>> pullEntities;

        /** Whether an adventure-mode player may interact with the cart. */
        public final ModConfigSpec.BooleanValue adventureModeInteract;

        CartConfig(final ModConfigSpec.Builder builder, final CartKind kind) {
            builder.comment("The " + kind.id().replace('_', ' ')
                            + " (draught load " + kind.load() + ")")
                   .push(kind.id());
            this.pullEntities = builder
                    .comment("Entities able to pull this cart, such as [\"minecraft:horse\"].",
                             "An empty list means anything that can wear a saddle and is not steered by an item.")
                    .define("pull_animals", new ArrayList<>());
            this.adventureModeInteract = builder
                    .comment("Players in adventure mode can interact with this cart")
                    .define("adventure_mode_interact", true);
            builder.pop();
        }
    }
}
