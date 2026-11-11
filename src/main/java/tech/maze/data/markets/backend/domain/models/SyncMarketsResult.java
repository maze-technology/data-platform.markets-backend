package tech.maze.data.markets.backend.domain.models;

/**
 * Result of a completed SyncMarkets fan-in.
 *
 * @param skipped total skipped markets across data providers
 */
public record SyncMarketsResult(long skipped) {
}
