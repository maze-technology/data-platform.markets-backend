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
import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;
import tech.maze.data.markets.backend.domain.ports.in.SyncMarketsUseCase;
import tech.maze.dtos.markets.events.EventTypes;
import tech.maze.dtos.markets.payloads.FetchMarketsResponse;
import tech.maze.dtos.markets.payloads.SyncMarketsResponse;

/**
 * Event stream configuration for markets Sync/Fetch orchestration.
 *
 * <p>Flow: SyncMarketsRequest → fan-out FetchMarketsRequest on a fixed topic →
 * correlate FetchMarketsResponse by {@code correlationid} → SyncMarketsResponse.
 */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EnableConfigurationProperties(MazeEventProperties.class)
@Slf4j
public class MarketsEventStreamConfiguration {
  EventSender eventSender;
  ObjectProvider<MeterRegistry> meterRegistryProvider;
  SyncMarketsUseCase syncMarketsUseCase;

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
      final String replyTo = eventSender.resolveReplyTo(event);

      final boolean started = syncMarketsUseCase.startSync(correlationId, replyTo);
      if (!started) {
        incrementFailure(EventTypes.SYNC_MARKETS_REQUEST);
      }
    };
  }

  /**
   * Handles FetchMarketsResponse fan-in for in-flight sync sessions.
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

      final String correlationId = eventSender.resolveCorrelationId(event);
      final String dataProvider = extractDataProvider(response);
      final Optional<SyncMarketsResult> completed = syncMarketsUseCase.onFetchResponse(
          correlationId,
          dataProvider,
          response.getSkipped(),
          response.getIsLast()
      );

      completed.ifPresent(result -> {
        final String replyTo = result.replyTo();
        if (replyTo == null || replyTo.isBlank()) {
          log.info(
              "Sync {} completed with skipped={} (no replyto)",
              correlationId,
              result.skipped()
          );
          return;
        }

        final SyncMarketsResponse syncResponse = SyncMarketsResponse.newBuilder()
            .setSkipped(result.skipped())
            .build();
        final boolean sent = eventSender.send(
            replyTo,
            syncResponse,
            EventTypes.SYNC_MARKETS_RESPONSE
        );
        if (!sent) {
          log.error("Failed to dispatch SyncMarketsResponse for correlation {}", correlationId);
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
