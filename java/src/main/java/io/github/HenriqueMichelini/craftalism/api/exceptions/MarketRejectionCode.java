package io.github.HenriqueMichelini.craftalism.api.exceptions;

public enum MarketRejectionCode {
    STALE_QUOTE,
    ITEM_BLOCKED,
    ITEM_NOT_OPERATING,
    INSUFFICIENT_STOCK,
    INSUFFICIENT_FUNDS,
    MARKET_CLOSED,
    INVALID_QUANTITY,
    RATE_LIMITED,
    QUOTE_EXPIRED,
    API_UNAVAILABLE,
    UNKNOWN_ITEM,
}
