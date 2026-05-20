package io.github.HenriqueMichelini.craftalism.api.service;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultMarketCatalog {

    private static final String DEFAULT_VARIATION_PERCENT = "0.0";
    private static final long DEFAULT_SEGMENT_SIZE = 50L;
    private static final String DEFAULT_PRICE_SENSITIVITY = "0.0800";
    private static final long DEFAULT_BASE_REGEN_QUANTITY = 1L;
    private static final long DEFAULT_REGEN_INTERVAL_SECONDS = 60L;

    private static final long CHEAP_LOG_PRICE = 25_000L;
    private static final long MEDIUM_LOG_PRICE = 45_000L;
    private static final long EXPENSIVE_LOG_PRICE = 75_000L;
    private static final long CHEAP_ORE_PRICE = 35_000L;
    private static final long EXPENSIVE_ORE_PRICE = 750_000L;

    private static final List<MarketSeedItem> ITEMS = List.of(
        item(
            "wheat",
            "farming",
            "Farming",
            "Wheat",
            "WHEAT",
            "2.3",
            50_000L
        ),
        item(
            "carrot",
            "farming",
            "Farming",
            "Carrot",
            "CARROT",
            "-1.4",
            10_000L
        ),
        item(
            "potato",
            "farming",
            "Farming",
            "Potato",
            "POTATO",
            "0.8",
            12_000L
        ),
        item(
            "sugar_cane",
            "farming",
            "Farming",
            "Sugar Cane",
            "SUGAR_CANE",
            "1.7",
            18_000L
        ),
        item(
            "spruce_log",
            "forestry",
            "Forestry",
            "Spruce Log",
            "SPRUCE_LOG",
            "0.4",
            CHEAP_LOG_PRICE
        ),
        item(
            "oak_log",
            "forestry",
            "Forestry",
            "Oak Log",
            "OAK_LOG",
            "-0.2",
            CHEAP_LOG_PRICE
        ),
        forestryLog("birch_log", "Birch Log", "BIRCH_LOG", CHEAP_LOG_PRICE),
        forestryLog("jungle_log", "Jungle Log", "JUNGLE_LOG", CHEAP_LOG_PRICE),
        forestryLog(
            "acacia_log",
            "Acacia Log",
            "ACACIA_LOG",
            MEDIUM_LOG_PRICE
        ),
        forestryLog(
            "dark_oak_log",
            "Dark Oak Log",
            "DARK_OAK_LOG",
            MEDIUM_LOG_PRICE
        ),
        forestryLog(
            "mangrove_log",
            "Mangrove Log",
            "MANGROVE_LOG",
            EXPENSIVE_LOG_PRICE
        ),
        forestryLog(
            "cherry_log",
            "Cherry Log",
            "CHERRY_LOG",
            EXPENSIVE_LOG_PRICE
        ),
        forestryLog(
            "pale_oak_log",
            "Pale Oak Log",
            "PALE_OAK_LOG",
            EXPENSIVE_LOG_PRICE
        ),
        item(
            "cobblestone",
            "mining",
            "Mining",
            "Cobblestone",
            "COBBLESTONE",
            "-0.8",
            4_000L
        ),
        item("coal", "mining", "Mining", "Coal", "COAL", "0.9", 35_000L),
        item(
            "redstone_dust",
            "mining",
            "Mining",
            "Redstone Dust",
            "REDSTONE",
            CHEAP_ORE_PRICE
        ),
        item(
            "lapis_lazuli",
            "mining",
            "Mining",
            "Lapis Lazuli",
            "LAPIS_LAZULI",
            CHEAP_ORE_PRICE
        ),
        item(
            "iron_ingot",
            "mining",
            "Mining",
            "Iron Ingot",
            "IRON_INGOT",
            "1.1",
            140_000L
        ),
        item(
            "gold_ingot",
            "mining",
            "Mining",
            "Gold Ingot",
            "GOLD_INGOT",
            "2.8",
            220_000L
        ),
        item(
            "emerald",
            "mining",
            "Mining",
            "Emerald",
            "EMERALD",
            EXPENSIVE_ORE_PRICE
        ),
        item(
            "diamond",
            "mining",
            "Mining",
            "Diamond",
            "DIAMOND",
            "4.5",
            900_000L
        ),
        mobDrop("rotten_flesh", "Rotten Flesh", "ROTTEN_FLESH", 2_000L),
        mobDrop("bone", "Bone", "BONE", 8_000L),
        mobDrop("string", "String", "STRING", 8_000L),
        mobDrop("spider_eye", "Spider Eye", "SPIDER_EYE", 10_000L),
        mobDrop("gunpowder", "Gunpowder", "GUNPOWDER", 18_000L),
        mobDrop("ender_pearl", "Ender Pearl", "ENDER_PEARL", 45_000L),
        mobDrop("slime_ball", "Slime Ball", "SLIME_BALL", 25_000L),
        mobDrop("blaze_rod", "Blaze Rod", "BLAZE_ROD", 60_000L),
        mobDrop("ghast_tear", "Ghast Tear", "GHAST_TEAR", 90_000L),
        mobDrop("magma_cream", "Magma Cream", "MAGMA_CREAM", 35_000L),
        mobDrop(
            "phantom_membrane",
            "Phantom Membrane",
            "PHANTOM_MEMBRANE",
            45_000L
        ),
        mobDrop("shulker_shell", "Shulker Shell", "SHULKER_SHELL", 125_000L),
        mobDrop(
            "prismarine_shard",
            "Prismarine Shard",
            "PRISMARINE_SHARD",
            20_000L
        ),
        mobDrop(
            "prismarine_crystals",
            "Prismarine Crystals",
            "PRISMARINE_CRYSTALS",
            35_000L
        ),
        mobDrop(
            "nautilus_shell",
            "Nautilus Shell",
            "NAUTILUS_SHELL",
            80_000L
        ),
        mobDrop("leather", "Leather", "LEATHER", 12_000L),
        mobDrop("feather", "Feather", "FEATHER", 6_000L),
        mobDrop("egg", "Egg", "EGG", 6_000L),
        mobDrop("rabbit_hide", "Rabbit Hide", "RABBIT_HIDE", 8_000L),
        mobDrop("rabbit_foot", "Rabbit Foot", "RABBIT_FOOT", 35_000L),
        mobDrop("ink_sac", "Ink Sac", "INK_SAC", 10_000L),
        mobDrop("glow_ink_sac", "Glow Ink Sac", "GLOW_INK_SAC", 18_000L),
        mobDrop("turtle_scute", "Turtle Scute", "TURTLE_SCUTE", 60_000L),
        mobDrop(
            "armadillo_scute",
            "Armadillo Scute",
            "ARMADILLO_SCUTE",
            30_000L
        ),
        mobDrop("beef", "Beef", "BEEF", 10_000L),
        mobDrop("porkchop", "Porkchop", "PORKCHOP", 10_000L),
        mobDrop("chicken", "Chicken", "CHICKEN", 8_000L),
        mobDrop("mutton", "Mutton", "MUTTON", 8_000L),
        mobDrop("rabbit", "Rabbit", "RABBIT", 10_000L),
        mobDrop("cod", "Cod", "COD", 6_000L),
        mobDrop("salmon", "Salmon", "SALMON", 8_000L),
        mobDrop("breeze_rod", "Breeze Rod", "BREEZE_ROD", 75_000L),
        mobDrop(
            "wither_skeleton_skull",
            "Wither Skeleton Skull",
            "WITHER_SKELETON_SKULL",
            250_000L
        ),
        mobDrop(
            "totem_of_undying",
            "Totem of Undying",
            "TOTEM_OF_UNDYING",
            300_000L
        ),
        mobDrop("trident", "Trident", "TRIDENT", 350_000L)
    );

    public List<MarketSeedItem> items() {
        return ITEMS;
    }

    private static MarketSeedItem forestryLog(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "forestry",
            "Forestry",
            displayName,
            iconKey,
            baseUnitPrice
        );
    }

    private static MarketSeedItem mobDrop(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "mob_drops",
            "Mob Drops",
            displayName,
            iconKey,
            baseUnitPrice
        );
    }

    private static MarketSeedItem item(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            categoryId,
            categoryDisplayName,
            displayName,
            iconKey,
            DEFAULT_VARIATION_PERCENT,
            baseUnitPrice
        );
    }

    private static MarketSeedItem item(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        String displayName,
        String iconKey,
        String variationPercent,
        long baseUnitPrice
    ) {
        return new MarketSeedItemBuilder()
            .itemId(itemId)
            .categoryId(categoryId)
            .categoryDisplayName(categoryDisplayName)
            .displayName(displayName)
            .iconKey(iconKey)
            .variationPercent(variationPercent)
            .baseUnitPrice(baseUnitPrice)
            .minUnitPrice(baseUnitPrice / 2L)
            .maxUnitPrice(baseUnitPrice * 3L)
            .segmentSize(DEFAULT_SEGMENT_SIZE)
            .priceSensitivity(DEFAULT_PRICE_SENSITIVITY)
            .baseRegenQuantity(DEFAULT_BASE_REGEN_QUANTITY)
            .regenIntervalSeconds(DEFAULT_REGEN_INTERVAL_SECONDS)
            .build();
    }
}
