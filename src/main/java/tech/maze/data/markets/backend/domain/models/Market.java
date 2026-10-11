package tech.maze.data.markets.backend.domain.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Market aggregate.
 */
public record Market(
    UUID id,
    MarketType type,
    String exchange,
    String base,
    String quote,
    SpecificData specificData,
    Instant createdAt
) {}
