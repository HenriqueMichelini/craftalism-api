package io.github.HenriqueMichelini.craftalism.api.service;

import java.util.List;
import java.util.Set;
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
    private static final long COMMON_FARMING_PRICE = 8_000L;
    private static final long UNCOMMON_FARMING_PRICE = 12_000L;
    private static final long HARD_FARMING_PRICE = 30_000L;
    private static final long BASIC_NATURAL_BLOCK_PRICE = 4_000L;
    private static final long BASIC_DECORATIVE_BLOCK_PRICE = 20_000L;
    private static final Set<String> RETIRED_ITEM_IDS = Set.of(
        "shulker_shell",
        "wither_skeleton_skull",
        "totem_of_undying",
        "trident"
    );

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
        farmingItem("apple", "Apple", "APPLE", 20_000L),
        farmingItem(
            "melon_slice",
            "Melon Slice",
            "MELON_SLICE",
            COMMON_FARMING_PRICE
        ),
        farmingItem(
            "sweet_berries",
            "Sweet Berries",
            "SWEET_BERRIES",
            10_000L
        ),
        farmingItem(
            "glow_berries",
            "Glow Berries",
            "GLOW_BERRIES",
            HARD_FARMING_PRICE
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
        farmingItem("beetroot", "Beetroot", "BEETROOT", 14_000L),
        farmingItem("pumpkin", "Pumpkin", "PUMPKIN", 12_000L),
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
        naturalBlock("dirt", "Dirt", "DIRT", BASIC_NATURAL_BLOCK_PRICE),
        item(
            "cobblestone",
            "natural_blocks",
            "Natural Blocks",
            "Cobblestone",
            "COBBLESTONE",
            "-0.8",
            BASIC_NATURAL_BLOCK_PRICE
        ),
        naturalBlock("sand", "Sand", "SAND", BASIC_NATURAL_BLOCK_PRICE),
        naturalBlock("gravel", "Gravel", "GRAVEL", BASIC_NATURAL_BLOCK_PRICE),
        naturalBlock(
            "andesite",
            "Andesite",
            "ANDESITE",
            BASIC_NATURAL_BLOCK_PRICE
        ),
        naturalBlock("diorite", "Diorite", "DIORITE", BASIC_NATURAL_BLOCK_PRICE),
        naturalBlock("granite", "Granite", "GRANITE", BASIC_NATURAL_BLOCK_PRICE),
        naturalBlock("deepslate", "Deepslate", "DEEPSLATE", 6_000L),
        mineral("coal", "Coal", "COAL", "0.9", CHEAP_ORE_PRICE),
        mineral("iron_ingot", "Iron Ingot", "IRON_INGOT", "1.1", 140_000L),
        mineral("gold_ingot", "Gold Ingot", "GOLD_INGOT", "2.8", 220_000L),
        item(
            "redstone_dust",
            "minerals",
            "Minerals",
            "Redstone Dust",
            "REDSTONE",
            CHEAP_ORE_PRICE
        ),
        item(
            "lapis_lazuli",
            "minerals",
            "Minerals",
            "Lapis Lazuli",
            "LAPIS_LAZULI",
            CHEAP_ORE_PRICE
        ),
        mineral("emerald", "Emerald", "EMERALD", EXPENSIVE_ORE_PRICE),
        item(
            "diamond",
            "minerals",
            "Minerals",
            "Diamond",
            "DIAMOND",
            "4.5",
            900_000L
        ),
        decorativeBlock(
            "wool",
            "Wool",
            "WHITE_WOOL",
            BASIC_DECORATIVE_BLOCK_PRICE
        ),
        decorativeBlock("terracotta", "Terracotta", "TERRACOTTA", 25_000L),
        decorativeBlock("glowstone", "Glowstone", "GLOWSTONE", 35_000L),
        decorativeBlock("sea_lantern", "Sea Lantern", "SEA_LANTERN", 45_000L),
        animalProduct("beef", "Raw Beef", "BEEF", UNCOMMON_FARMING_PRICE),
        animalProduct(
            "porkchop",
            "Raw Porkchop",
            "PORKCHOP",
            UNCOMMON_FARMING_PRICE
        ),
        animalProduct("chicken", "Raw Chicken", "CHICKEN", COMMON_FARMING_PRICE),
        animalProduct("rabbit", "Raw Rabbit", "RABBIT", 14_000L),
        animalProduct("cod", "Raw Cod", "COD", 6_000L),
        animalProduct("salmon", "Raw Salmon", "SALMON", COMMON_FARMING_PRICE),
        animalProduct("mutton", "Raw Mutton", "MUTTON", 10_000L),
        animalProduct("leather", "Leather", "LEATHER", 12_000L),
        animalProduct("feather", "Feather", "FEATHER", 6_000L),
        animalProduct("egg", "Egg", "EGG", 6_000L),
        animalProduct("rabbit_hide", "Rabbit Hide", "RABBIT_HIDE", 8_000L),
        animalProduct("rabbit_foot", "Rabbit Foot", "RABBIT_FOOT", 35_000L),
        animalProduct("ink_sac", "Ink Sac", "INK_SAC", 10_000L),
        animalProduct("glow_ink_sac", "Glow Ink Sac", "GLOW_INK_SAC", 18_000L),
        animalProduct("turtle_scute", "Turtle Scute", "TURTLE_SCUTE", 60_000L),
        animalProduct(
            "armadillo_scute",
            "Armadillo Scute",
            "ARMADILLO_SCUTE",
            30_000L
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
        mobDrop("breeze_rod", "Breeze Rod", "BREEZE_ROD", 75_000L)
    );

    public List<MarketSeedItem> items() {
        return ITEMS;
    }

    Set<String> retiredItemIds() {
        return RETIRED_ITEM_IDS;
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

    private static MarketSeedItem naturalBlock(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "natural_blocks",
            "Natural Blocks",
            displayName,
            iconKey,
            baseUnitPrice
        );
    }

    private static MarketSeedItem mineral(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "minerals",
            "Minerals",
            displayName,
            iconKey,
            baseUnitPrice
        );
    }

    private static MarketSeedItem mineral(
        String itemId,
        String displayName,
        String iconKey,
        String variationPercent,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "minerals",
            "Minerals",
            displayName,
            iconKey,
            variationPercent,
            baseUnitPrice
        );
    }

    private static MarketSeedItem decorativeBlock(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "decorative_blocks",
            "Decorative Blocks",
            displayName,
            iconKey,
            baseUnitPrice
        );
    }

    private static MarketSeedItem animalProduct(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "animal_products",
            "Animal Products",
            displayName,
            iconKey,
            baseUnitPrice
        );
    }

    private static MarketSeedItem farmingItem(
        String itemId,
        String displayName,
        String iconKey,
        long baseUnitPrice
    ) {
        return item(
            itemId,
            "farming",
            "Farming",
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
