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
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.carts.container.PlowMenu;
import com.example.horsegenetics.neoforge.carts.container.SeedDrillMenu;
import com.example.horsegenetics.neoforge.carts.container.WagonMenu;
import com.example.horsegenetics.neoforge.carts.entity.AnimalCartEntity;
import com.example.horsegenetics.neoforge.carts.entity.PlowEntity;
import com.example.horsegenetics.neoforge.carts.entity.PostilionEntity;
import com.example.horsegenetics.neoforge.carts.entity.ReaperCartEntity;
import com.example.horsegenetics.neoforge.carts.entity.SeedDrillEntity;
import com.example.horsegenetics.neoforge.carts.entity.SupplyCartEntity;
import com.example.horsegenetics.neoforge.carts.entity.WagonEntity;
import com.example.horsegenetics.neoforge.carts.entity.ai.goal.AvoidCartGoal;
import com.example.horsegenetics.neoforge.carts.entity.ai.goal.PullCartGoal;
import com.example.horsegenetics.neoforge.carts.entity.ai.goal.RideCartGoal;
import com.example.horsegenetics.neoforge.carts.item.CartItem;
import com.example.horsegenetics.neoforge.carts.network.clientbound.UpdateDrawnPayload;
import com.example.horsegenetics.neoforge.carts.network.serverbound.ActionKeyPayload;
import com.example.horsegenetics.neoforge.carts.network.serverbound.CoachmanMovePayload;
import com.example.horsegenetics.neoforge.carts.network.serverbound.OpenSupplyCartPayload;
import com.example.horsegenetics.neoforge.carts.network.serverbound.RequestCartUpdatePayload;
import com.example.horsegenetics.neoforge.carts.util.CartGoalAdder;
import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <b>The cart system's registration and wiring</b>, and the one class that
 * knows a cart exists in every wood rather than in twelve.
 *
 * <h2>Why this is {@code RegisterEvent} and not {@code DeferredRegister}</h2>
 * The rest of this mod registers through {@code DeferredRegister}, and two
 * places - {@code DoubleGates} and {@code ModdedArmour} - go to real trouble to
 * create their per-material entries during class load, because a
 * {@code DeferredRegister} will not accept an entry after it has attached to
 * the bus. That constraint is what makes those two classes awkward.
 *
 * <p>The carts have the same shape of problem and do not need the same
 * awkwardness, because {@link RegisterEvent} fires <i>after</i> mod
 * construction: {@code ModdedMaterials.scan()} has already run, so
 * {@link CartWood#all()} is simply available at the point of registering, and
 * the modded woods need no special handling at all. Upstream already used
 * {@code RegisterEvent}, so keeping it is also the smaller diff against the MIT
 * source we are now maintaining.
 *
 * <p>The cost is that this does not appear in the mod's {@code ModItems} /
 * {@code ModEntities} tables. That is what this javadoc is for.
 *
 * <h2>Entry point</h2>
 * There is no {@code @Mod} annotation here. A jar may hold several {@code @Mod}
 * classes only if each is its own mod id, and the carts are part of Horse
 * Genetics, so {@link #init} is called from the host mod's constructor instead.
 *
 * @see com.example.horsegenetics.common.cart.CartDraft the model that decides how fast any of this moves
 */
public final class HorseCarts {

    public static final String MOD_ID = HorseGenetics.MOD_ID;

    private HorseCarts() {
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    /** The wheel every cart recipe spends. */
    public static Item WHEEL;

    /** kind -> wood id -> item. Filled during {@link Registries#ITEM} registration. */
    private static final Map<CartKind, Map<String, CartItem>> ITEMS = new EnumMap<>(CartKind.class);

    /** The item for this kind of cart in this wood, or null if the wood is unknown. */
    public static CartItem item(final CartKind kind, final CartWood wood) {
        if (wood == null) {
            return null;
        }
        final Map<String, CartItem> byWood = ITEMS.get(kind);
        return byWood == null ? null : byWood.get(wood.id());
    }

    private static void registerItems() {
        WHEEL = register("cart_wheel", Item::new);
        for (final CartKind kind : CartKind.values()) {
            final Map<String, CartItem> byWood = new LinkedHashMap<>();
            for (final CartWood wood : CartWood.all()) {
                if (!wood.has(kind)) {
                    continue;   // no stripped log, no wagon - CartWood.has
                }
                byWood.put(wood.id(), register(wood.itemId(kind.id()),
                        prop -> new CartItem(wood, kind, prop.stacksTo(1))));
            }
            ITEMS.put(kind, byWood);
        }
    }

    // ------------------------------------------------------------------
    // Sounds
    // ------------------------------------------------------------------

    public static final Identifier ATTACH_SOUND_ID = resLoc("entity.cart.attach");
    public static final Identifier DETACH_SOUND_ID = resLoc("entity.cart.detach");
    public static final Identifier PLACE_SOUND_ID = resLoc("entity.cart.place");

    public static SoundEvent ATTACH_SOUND = SoundEvent.createVariableRangeEvent(ATTACH_SOUND_ID);
    public static SoundEvent DETACH_SOUND = SoundEvent.createVariableRangeEvent(DETACH_SOUND_ID);
    public static SoundEvent PLACE_SOUND = SoundEvent.createVariableRangeEvent(PLACE_SOUND_ID);

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    public static EntityType<@NotNull SupplyCartEntity> SUPPLY_CART_ENTITY;
    public static EntityType<@NotNull AnimalCartEntity> ANIMAL_CART_ENTITY;
    public static EntityType<@NotNull PlowEntity> PLOW_ENTITY;
    public static EntityType<@NotNull SeedDrillEntity> SEED_DRILL_ENTITY;
    public static EntityType<@NotNull ReaperCartEntity> REAPER_ENTITY;
    public static EntityType<@NotNull WagonEntity> WAGON_ENTITY;
    public static EntityType<@NotNull PostilionEntity> POSTILION_ENTITY;

    /** Entity type -> the {@link CartKind} it is, so the draught model can find its load. */
    private static final Map<EntityType<?>, CartKind> KINDS = new HashMap<>();

    /**
     * Which vehicle this entity is. Never null for a cart entity; the mapping is
     * filled in the same pass that registers the types, so the two cannot drift.
     */
    public static CartKind kindOf(final EntityType<?> type) {
        return KINDS.get(type);
    }

    private static void registerEntityTypes() {
        SUPPLY_CART_ENTITY = register("supply_cart", EntityType.Builder.of(SupplyCartEntity::new, MobCategory.MISC).sized(1.5f, 1.4f));
        ANIMAL_CART_ENTITY = register("animal_cart", EntityType.Builder.of(AnimalCartEntity::new, MobCategory.MISC).sized(1.3f, 1.4f));
        PLOW_ENTITY = register("plow", EntityType.Builder.of(PlowEntity::new, MobCategory.MISC).sized(1.3f, 1.4f));
        SEED_DRILL_ENTITY = register("seed_drill", EntityType.Builder.of(SeedDrillEntity::new, MobCategory.MISC).sized(1.3f, 1.4f));
        REAPER_ENTITY = register("reaper", EntityType.Builder.of(ReaperCartEntity::new, MobCategory.MISC).sized(1.3f, 1.4f));
        WAGON_ENTITY = register("wagon", EntityType.Builder.of(WagonEntity::new, MobCategory.MISC).sized(2.5f, 3f));
        POSTILION_ENTITY = register("postilion", EntityType.Builder.of(PostilionEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).noSummon().noSave());

        KINDS.clear();
        KINDS.put(SUPPLY_CART_ENTITY, CartKind.SUPPLY_CART);
        KINDS.put(ANIMAL_CART_ENTITY, CartKind.ANIMAL_CART);
        KINDS.put(PLOW_ENTITY, CartKind.PLOW);
        KINDS.put(SEED_DRILL_ENTITY, CartKind.SEED_DRILL);
        KINDS.put(REAPER_ENTITY, CartKind.REAPER);
        KINDS.put(WAGON_ENTITY, CartKind.WAGON);

        CART_PULL_CM.clear();
        CART_PULL_CM.put(SUPPLY_CART_ENTITY, PULL_STATS.get(CartKind.SUPPLY_CART));
        CART_PULL_CM.put(ANIMAL_CART_ENTITY, PULL_STATS.get(CartKind.ANIMAL_CART));
        CART_PULL_CM.put(PLOW_ENTITY, PULL_STATS.get(CartKind.PLOW));
        CART_PULL_CM.put(SEED_DRILL_ENTITY, PULL_STATS.get(CartKind.SEED_DRILL));
        CART_PULL_CM.put(REAPER_ENTITY, PULL_STATS.get(CartKind.REAPER));
        CART_PULL_CM.put(WAGON_ENTITY, PULL_STATS.get(CartKind.WAGON));
    }

    // ------------------------------------------------------------------
    // Menus, goals, tags, stats
    // ------------------------------------------------------------------

    public static final MenuType<@NotNull PlowMenu> PLOW_MENU_TYPE =
            new MenuType<>(PlowMenu::new, FeatureFlags.DEFAULT_FLAGS);
    public static final MenuType<@NotNull SeedDrillMenu> SEED_DRILL_MENU_TYPE =
            new MenuType<>(SeedDrillMenu::new, FeatureFlags.DEFAULT_FLAGS);
    public static final MenuType<@NotNull WagonMenu> WAGON_9x4_MENU_TYPE =
            new MenuType<>((i, inv) -> new WagonMenu(HorseCarts.WAGON_9x4_MENU_TYPE, i, inv, new SimpleContainer(4 * 9), 4), FeatureFlags.DEFAULT_FLAGS);
    public static final MenuType<@NotNull WagonMenu> WAGON_9x8_MENU_TYPE =
            new MenuType<>((i, inv) -> new WagonMenu(HorseCarts.WAGON_9x8_MENU_TYPE, i, inv, new SimpleContainer(8 * 9), 8), FeatureFlags.DEFAULT_FLAGS);
    public static final MenuType<@NotNull WagonMenu> WAGON_12x9_MENU_TYPE =
            new MenuType<>((i, inv) -> new WagonMenu(HorseCarts.WAGON_12x9_MENU_TYPE, i, inv, new SimpleContainer(12 * 9), 12), FeatureFlags.DEFAULT_FLAGS);

    public static final CartGoalAdder<Mob> MOB_GOAL_ADDER = CartGoalAdder.mobGoal(Mob.class)
            .add(1, PullCartGoal::new)
            .add(1, RideCartGoal::new)
            .build();

    public static final CartGoalAdder<PathfinderMob> PATHFINDER_GOAL_ADDER = CartGoalAdder.mobGoal(PathfinderMob.class)
            .add(3, mob -> new AvoidCartGoal<>(mob, SupplyCartEntity.class, 3.0f, 0.5f))
            .add(3, mob -> new AvoidCartGoal<>(mob, PlowEntity.class, 3.0f, 0.5f))
            .add(3, mob -> new AvoidCartGoal<>(mob, SeedDrillEntity.class, 3.0f, 0.5f))
            .add(3, mob -> new AvoidCartGoal<>(mob, ReaperCartEntity.class, 3.0f, 0.5f))
            .build();

    /** Stat ids do not depend on registration order, so they are fixed up front. */
    private static final Map<CartKind, Identifier> PULL_STATS = new EnumMap<>(CartKind.class);

    static {
        for (final CartKind kind : CartKind.values()) {
            PULL_STATS.put(kind, resLoc(kind.id() + "_pull_cm"));
        }
    }

    /** Filled once entity types are registered. */
    public static final Map<EntityType<?>, Identifier> CART_PULL_CM = new HashMap<>();

    public static final Identifier RIDE_CART_CM = resLoc("ride_cart_cm");
    public static final Identifier STEER_ANIMAL_CART_CM = resLoc("steer_animal_cart_cm");
    public static final Identifier STEER_REAPER_CM = resLoc("steer_reaper_cm");
    public static final Identifier STEER_WAGON_CM = resLoc("steer_wagon_cm");

    public static final TagKey<@NotNull Block> PLOW_BREAKABLE_HOE = TagKey.create(Registries.BLOCK, resLoc("plow_breakable/hoe"));
    public static final TagKey<@NotNull Block> PLOW_BREAKABLE_SHOVEL = TagKey.create(Registries.BLOCK, resLoc("plow_breakable/shovel"));
    public static final TagKey<@NotNull Block> PLOW_BREAKABLE_AXE = TagKey.create(Registries.BLOCK, resLoc("plow_breakable/axe"));
    public static final TagKey<@NotNull Block> REAPER_HARVESTABLE = TagKey.create(Registries.BLOCK, resLoc("reaper_harvestable"));
    public static final TagKey<@NotNull Item> SEED_DRILL_PLANTABLE = TagKey.create(Registries.ITEM, resLoc("seed_drill_plantable"));

    public static MinecraftServer server = null;

    // ------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------

    /**
     * Called from {@code HorseGenetics}'s constructor. Common side only - the
     * client half is {@code CartsClient.init}, called from {@code ClientSetup},
     * because everything in it touches renderers and key mappings.
     */
    public static void init(final IEventBus modBus, final ModContainer container) {
        // An explicit file name: the host mod already owns an unnamed COMMON
        // config, and two unnamed configs of one type in one mod collide.
        container.registerConfig(ModConfig.Type.COMMON, CartsConfig.spec(), MOD_ID + "-carts.toml");
        container.registerConfig(ModConfig.Type.CLIENT, CartsConfig.clientSpec(), MOD_ID + "-carts-client.toml");

        modBus.addListener(HorseCarts::onRegister);
        modBus.addListener(HorseCarts::onRegisterPayloads);
        modBus.addListener(HorseCarts::onEntityAttributes);

        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> server = e.getServer());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> server = null);
        NeoForge.EVENT_BUS.addListener(HorseCarts::onServerTick);
        NeoForge.EVENT_BUS.addListener(HorseCarts::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(HorseCarts::onEntityJoin);
    }

    private static void onRegister(final RegisterEvent event) {
        // Vanilla Registry.register is fine here: NeoForge unfreezes registries
        // around this event, which is exactly why the carts use it.
        if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            Registry.register(BuiltInRegistries.SOUND_EVENT, ATTACH_SOUND_ID, ATTACH_SOUND);
            Registry.register(BuiltInRegistries.SOUND_EVENT, DETACH_SOUND_ID, DETACH_SOUND);
            Registry.register(BuiltInRegistries.SOUND_EVENT, PLACE_SOUND_ID, PLACE_SOUND);
        } else if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            registerEntityTypes();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            registerItems();
        } else if (event.getRegistryKey().equals(Registries.MENU)) {
            Registry.register(BuiltInRegistries.MENU, resLoc("plow"), PLOW_MENU_TYPE);
            Registry.register(BuiltInRegistries.MENU, resLoc("seed_drill"), SEED_DRILL_MENU_TYPE);
            Registry.register(BuiltInRegistries.MENU, resLoc("wagon_four_rows"), WAGON_9x4_MENU_TYPE);
            Registry.register(BuiltInRegistries.MENU, resLoc("wagon_eight_rows"), WAGON_9x8_MENU_TYPE);
            Registry.register(BuiltInRegistries.MENU, resLoc("wagon_quad"), WAGON_12x9_MENU_TYPE);
        } else if (event.getRegistryKey().equals(Registries.CUSTOM_STAT)) {
            for (final Identifier stat : PULL_STATS.values()) {
                registerStat(stat);
            }
            registerStat(RIDE_CART_CM);
            registerStat(STEER_ANIMAL_CART_CM);
            registerStat(STEER_REAPER_CM);
            registerStat(STEER_WAGON_CM);
        } else if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, resLoc("carts"),
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.horsegenetics.carts"))
                            .icon(() -> new ItemStack(item(CartKind.WAGON, CartWood.fallback())))
                            // Last of this mod's three, after Horse Genetics and
                            // Horse Breeds - the thing you hitch to the horse you
                            // bred, in the yard you built. See ModCreativeTabs.
                            .withTabsAfter(com.example.horsegenetics.neoforge.item.ModCreativeTabs
                                    .BREEDS.getKey())
                            .displayItems((params, output) -> {
                                output.accept(WHEEL);
                                // Wood-major, so one wood's whole set sits together -
                                // with a wood per mod installed there can be a great
                                // many of these, and grouping by vehicle would scatter
                                // every set across the tab.
                                for (final CartWood wood : CartWood.all()) {
                                    for (final CartKind kind : CartKind.values()) {
                                        final CartItem cart = item(kind, wood);
                                        if (cart != null) {
                                            output.accept(cart);
                                        }
                                    }
                                }
                            })
                            .build());
        }
    }

    private static void registerStat(final Identifier id) {
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
        Stats.CUSTOM.get(id, StatFormatter.DISTANCE);
    }

    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ActionKeyPayload.TYPE, ActionKeyPayload.CODEC,
                (payload, ctx) -> ActionKeyPayload.handle((ServerPlayer) ctx.player()));
        registrar.playToServer(OpenSupplyCartPayload.TYPE, OpenSupplyCartPayload.CODEC,
                (payload, ctx) -> OpenSupplyCartPayload.handle((ServerPlayer) ctx.player()));
        registrar.playToServer(RequestCartUpdatePayload.TYPE, RequestCartUpdatePayload.CODEC,
                (payload, ctx) -> RequestCartUpdatePayload.handle(payload, (ServerPlayer) ctx.player()));
        registrar.playToServer(CoachmanMovePayload.TYPE, CoachmanMovePayload.CODEC,
                (payload, ctx) -> CoachmanMovePayload.handle(payload, (ServerPlayer) ctx.player()));
        // Clientbound: the handler is attached client-side in CartsClient.
        registrar.playToClient(UpdateDrawnPayload.TYPE, UpdateDrawnPayload.CODEC);
    }

    private static void onEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(POSTILION_ENTITY, LivingEntity.createLivingAttributes().build());
    }

    private static void onServerTick(final ServerTickEvent.Post event) {
        final MinecraftServer s = event.getServer();
        for (final ResourceKey<@NotNull Level> levelKey : s.levelKeys()) {
            CartWorld.getServer(s, levelKey).tick(s.getLevel(levelKey));
        }
    }

    private static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        final Entity rider = event.getTarget().getControllingPassenger();
        if (rider instanceof PostilionEntity) {
            rider.stopRiding();
        }
    }

    private static void onEntityJoin(final EntityJoinLevelEvent event) {
        // Fabric's ServerEntityEvents.ENTITY_LOAD is server-only; this fires on both.
        if (event.getLevel().isClientSide()) {
            return;
        }
        MOB_GOAL_ADDER.onEntityJoinWorld(event.getEntity());
        PATHFINDER_GOAL_ADDER.onEntityJoinWorld(event.getEntity());
    }

    // ------------------------------------------------------------------

    public static <T extends Entity> EntityType<@NotNull T> register(final String id, final EntityType.Builder<@NotNull T> builder) {
        final ResourceKey<@NotNull EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, resLoc(id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static <I extends Item> I register(final String id, final Function<Item.Properties, I> function) {
        final ResourceKey<@NotNull Item> key = ResourceKey.create(Registries.ITEM, resLoc(id));
        final I item = function.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static Identifier resLoc(final String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    /** Every cart item, for the generated-pack pass that writes their models. */
    public static List<CartItem> allCartItems() {
        final List<CartItem> out = new java.util.ArrayList<>();
        for (final Map<String, CartItem> byWood : ITEMS.values()) {
            out.addAll(byWood.values());
        }
        return out;
    }
}
