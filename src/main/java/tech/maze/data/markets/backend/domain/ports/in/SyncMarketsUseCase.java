package tech.maze.data.markets.backend.domain.ports.in;

import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;

/**
 * Orchestrates SyncMarkets: fan-out FetchMarketsRequest and correlate responses.
 */
public interface SyncMarketsUseCase {
  /**
   * Starts a sync session and fans out FetchMarketsRequest on the fixed topic.
   *
   * @param correlationId correlation id shared by fetch responses
   * @param replyTo destination for the eventual SyncMarketsResponse
   * @return true when the fetch request was published
   */
  boolean startSync(String correlationId, String replyTo);

  /**
   * Records a FetchMarketsResponse chunk for an in-flight sync.
   *
   * @param correlationId correlation id
   * @param dataProvider data provider identifier
   * @param skipped skipped count from this chunk
   * @param last whether this is the last chunk from the provider
   * @return completed result when fan-in is done, otherwise empty
   */
  java.util.Optional<SyncMarketsResult> onFetchResponse(
      String correlationId,
      String dataProvider,
      long skipped,
      boolean last
  );
}
