package tech.maze.data.markets.backend.domain.models;

/**
 * Result of a completed SyncMarkets fan-in.
 *
 * @param skipped total skipped markets across data providers
 * @param replyTo destination for SyncMarketsResponse
 */
public record SyncMarketsResult(long skipped, String replyTo) {
  /**
   * Creates a result without a reply destination.
   *
   * @param skipped skipped count
   */
  public SyncMarketsResult(long skipped) {
    this(skipped, null);
  }
}
