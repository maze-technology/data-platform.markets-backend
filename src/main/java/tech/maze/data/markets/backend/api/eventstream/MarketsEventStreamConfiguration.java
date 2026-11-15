package tech.maze.data.markets.backend.api.eventstream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.maze.commons.eventstream.EventSender;
import tech.maze.commons.eventstream.MazeEventProperties;
import tech.maze.data.markets.backend.AppProperties;
import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;
import tech.maze.data.markets.backend.domain.ports.in.SyncMarketsUseCase;
import tech.maze.data.markets.backend.infrastructure.sync.SyncMarketsResponsePublisher;
import tech.maze.dtos.markets.events.EventTypes;
import tech.maze.dtos.markets.payloads.FetchMarketsResponse;

/**
 * Event stream configuration for markets Sync/Fetch orchestration.
 *
 * <p>Flow: SyncMarketsRequest → fan-out FetchMarketsRequest on a fixed topic →
 * upsert MarketMetaDatas from each FetchMarketsResponse → correlate by
 * {@code correlationid} → SyncMarketsResponse on a fixed topic.
 */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EnableConfigurationProperties({MazeEventProperties.class, AppProperties.class})
@Slf4j
public class MarketsEventStreamConfiguration {
  EventSender eventSender;
  ObjectProvider<MeterRegistry> meterRegistryProvider;
  SyncMarketsUseCase syncMarketsUseCase;
  MarketMetaDatasEventService marketMetaDatasEventService;
  AppProperties appProperties;
  SyncMarketsResponsePublisher syncMarketsResponsePublisher;

  /**
   * Handles SyncMarketsRequest: starts correlation and fans out FetchMarketsRequest.
   *
   * @return a consumer for CloudEvents
   */
  @Bean
  public Consumer<CloudEvent> syncMarketsRequestConsumer() {
    return event -> {
      if (!EventTypes.SYNC_MARKETS_REQUEST.equals(event.getType())) {
        log.warn(
            "Skipping event type {} (expected {})",
            event.getType(),
            EventTypes.SYNC_MARKETS_REQUEST
        );
        return;
      }

      final String correlationId = Optional.ofNullable(eventSender.resolveCorrelationId(event))
          .filter(id -> !id.isBlank())
          .orElseGet(() -> event.getId() == null ? UUID.randomUUID().toString() : event.getId());

      final boolean started = syncMarketsUseCase.startSync(correlationId);
      if (!started) {
        incrementFailure(EventTypes.SYNC_MARKETS_REQUEST);
      }
    };
  }

  /**
   * Handles FetchMarketsResponse: upserts asset metadata, then fans in sync completion.
   *
   * @return a consumer for CloudEvents
   */
  @Bean
  public Consumer<CloudEvent> fetchMarketsResponseConsumer() {
    return event -> {
      if (!EventTypes.FETCH_MARKETS_RESPONSE.equals(event.getType())) {
        log.warn(
            "Skipping event type {} (expected {})",
            event.getType(),
            EventTypes.FETCH_MARKETS_RESPONSE
        );
        return;
      }

      final FetchMarketsResponse response;
      try {
        response = parseFetchResponse(event);
      } catch (InvalidProtocolBufferException ex) {
        log.error("Failed to parse FetchMarketsResponse for event {}", event.getId(), ex);
        incrementFailure(EventTypes.FETCH_MARKETS_RESPONSE);
        return;
      }

      if (response.hasMarketMetaDatas()) {
        marketMetaDatasEventService.process(response.getMarketMetaDatas());
      }

      final String correlationId = eventSender.resolveCorrelationId(event);
      final String dataProvider = extractDataProvider(response);
      final Optional<SyncMarketsResult> completed = syncMarketsUseCase.onFetchResponse(
          correlationId,
          dataProvider,
          response.getPageIndex(),
          response.getSkipped(),
          response.getIsLast()
      );

      completed.ifPresent(result -> {
        final boolean sent = syncMarketsResponsePublisher.publish(correlationId, result);
        if (!sent) {
          incrementFailure(EventTypes.SYNC_MARKETS_RESPONSE);
        }
      });
    };
  }

  private FetchMarketsResponse parseFetchResponse(CloudEvent event)
      throws InvalidProtocolBufferException {
    final CloudEventData data = event.getData();
    if (data == null) {
      return FetchMarketsResponse.getDefaultInstance();
    }
    return FetchMarketsResponse.parseFrom(data.toBytes());
  }

  private String extractDataProvider(FetchMarketsResponse response) {
    if (response.hasDataProvider()) {
      final Value value = response.getDataProvider();
      if (value.hasStringValue()) {
        return value.getStringValue();
      }
      if (value.hasNumberValue()) {
        return Double.toString(value.getNumberValue());
      }
    }
    if (response.hasMarketMetaDatas() && response.getMarketMetaDatas().hasDataProviderId()) {
      final Value value = response.getMarketMetaDatas().getDataProviderId();
      if (value.hasStringValue()) {
        return value.getStringValue();
      }
    }
    return "unknown";
  }

  private void incrementFailure(String eventType) {
    final MeterRegistry registry = meterRegistryProvider.getIfAvailable();
    if (registry != null) {
      registry.counter("maze.events.reply.failed", "eventType", eventType).increment();
    }
  }
}
