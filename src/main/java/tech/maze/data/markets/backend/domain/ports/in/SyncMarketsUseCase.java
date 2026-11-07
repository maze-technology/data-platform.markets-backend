package tech.maze.data.markets.backend.domain.ports.in;

import java.util.Optional;
import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;

/**
 * Orchestrates SyncMarkets: fan-out FetchMarketsRequest and correlate responses.
 */
public interface SyncMarketsUseCase {
  /**
   * Starts a sync session and fans out FetchMarketsRequest on the fixed topic.
   *
   * @param correlationId correlation id shared by fetch responses
   * @return true when the fan-out was published
   */
  boolean startSync(String correlationId);

  /**
   * Records a FetchMarketsResponse chunk for an in-flight sync.
   *
   * @param correlationId correlation id from the CloudEvent
   * @param dataProvider provider identifier that produced the chunk
   * @param skipped skipped count from the chunk
   * @param last whether this chunk completes the provider's stream
   * @return completed result when all expected providers are done
   */
  Optional<SyncMarketsResult> onFetchResponse(
      String correlationId,
      String dataProvider,
      long skipped,
      boolean last
  );
}
