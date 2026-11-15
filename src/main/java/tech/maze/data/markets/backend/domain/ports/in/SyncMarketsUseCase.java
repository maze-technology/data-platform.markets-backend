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
   * @param pageIndex zero-based page index within the provider stream
   * @param skipped skipped count from the chunk
   * @param last whether this chunk is the provider's last page
   * @return finished result when the sync completes or times out on this call
   */
  Optional<SyncMarketsResult> onFetchResponse(
      String correlationId,
      String dataProvider,
      int pageIndex,
      long skipped,
      boolean last
  );
}
