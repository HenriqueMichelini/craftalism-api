package io.github.HenriqueMichelini.craftalism.api.config;

public record MarketSettings(
    boolean enabled,
    long quoteTtlSeconds,
    String trustedMinecraftServerClientId,
    int quoteRateLimitMaxRequests,
    int executeRateLimitMaxRequests,
    long rateLimitWindowSeconds
) {}
