package tech.maze.data.markets.backend.infrastructure.sync;

import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.maze.commons.eventstream.EventSender;
import tech.maze.data.markets.backend.AppProperties;
import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;
import tech.maze.dtos.markets.events.EventTypes;
import tech.maze.dtos.markets.payloads.SyncMarketsResponse;

/**
 * Publishes SyncMarketsResponse on the fixed ENV topic.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class SyncMarketsResponsePublisher {
  EventSender eventSender;
  AppProperties appProperties;

  /**
   * Publishes a sync finish event.
   *
   * @param correlationId correlation id
   * @param result finish payload
   * @return true when published
   */
  public boolean publish(String correlationId, SyncMarketsResult result) {
    final SyncMarketsResponse.Builder builder = SyncMarketsResponse.newBuilder()
        .setSkipped(result.skipped())
        .setStatus(result.status());
    if (result.incompleteProviders() != null && !result.incompleteProviders().isEmpty()) {
      builder.addAllIncompleteProviders(result.incompleteProviders());
    }
    final boolean sent = eventSender.send(
        appProperties.getSync().getResponseTopic(),
        builder.build(),
        EventTypes.SYNC_MARKETS_RESPONSE,
        correlationId == null || correlationId.isBlank()
            ? Map.of()
            : Map.of(EventSender.CORRELATION_ID_EXTENSION, correlationId)
    );
    if (!sent) {
      log.error(
          "Failed to dispatch SyncMarketsResponse ({}) for correlation {}",
          result.status(),
          correlationId
      );
    }
    return sent;
  }
}
