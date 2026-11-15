package tech.maze.data.markets.backend.domain.models;

import java.util.List;
import tech.maze.dtos.markets.enums.SyncStatus;

/**
 * Result of a finished SyncMarkets fan-in (completed, timed out, or partial).
 *
 * @param skipped total skipped markets across data providers
 * @param status completion status
 * @param incompleteProviders providers that did not finish (TIMEOUT/PARTIAL)
 */
public record SyncMarketsResult(
    long skipped,
    SyncStatus status,
    List<String> incompleteProviders
) {
  /**
   * Convenience for a fully completed sync.
   *
   * @param skipped total skipped
   * @return completed result
   */
  public static SyncMarketsResult completed(long skipped) {
    return new SyncMarketsResult(skipped, SyncStatus.COMPLETED, List.of());
  }
}
