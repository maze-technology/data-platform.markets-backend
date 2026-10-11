package tech.maze.data.markets.backend.domain.usecases;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.maze.commons.eventstream.EventSender;
import tech.maze.data.markets.backend.AppProperties;
import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;
import tech.maze.data.markets.backend.domain.ports.in.SyncMarketsUseCase;
import tech.maze.dtos.markets.events.EventTypes;
import tech.maze.dtos.markets.payloads.FetchMarketsRequest;

/**
 * In-memory correlation fan-in for SyncMarkets over fixed Kafka topics.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class SyncMarketsUseCaseImpl implements SyncMarketsUseCase {
  EventSender eventSender;
  AppProperties appProperties;

  Map<String, SyncSession> sessions = new ConcurrentHashMap<>();

  @Override
  public boolean startSync(String correlationId, String replyTo) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId is required");
    }

    final SyncSession session = new SyncSession(replyTo, Instant.now());
    sessions.put(correlationId, session);

    final String topic = appProperties.getSync().getFetchRequestTopic();
    final boolean sent = eventSender.send(
        topic,
        FetchMarketsRequest.getDefaultInstance(),
        EventTypes.FETCH_MARKETS_REQUEST,
        Map.of(
            EventSender.CORRELATION_ID_EXTENSION, correlationId,
            EventSender.REPLY_TO_EXTENSION, appProperties.getSync().getFetchResponseTopic()
        )
    );

    if (!sent) {
      sessions.remove(correlationId);
      log.error("Failed to fan-out FetchMarketsRequest for correlation {}", correlationId);
    }
    return sent;
  }

  @Override
  public Optional<SyncMarketsResult> onFetchResponse(
      String correlationId,
      String dataProvider,
      long skipped,
      boolean last
  ) {
    if (correlationId == null || correlationId.isBlank()) {
      return Optional.empty();
    }

    final SyncSession session = sessions.get(correlationId);
    if (session == null) {
      log.warn("No sync session for correlation {}", correlationId);
      return Optional.empty();
    }

    if (isTimedOut(session)) {
      sessions.remove(correlationId);
      log.warn(
          "Sync session {} timed out after {}",
          correlationId,
          appProperties.getSync().getTimeout()
      );
      return Optional.empty();
    }

    if (skipped > 0) {
      session.getSkipped().addAndGet(skipped);
    }

    if (last && dataProvider != null && !dataProvider.isBlank()) {
      session.getCompletedProviders().add(dataProvider);
    }

    if (!isComplete(session)) {
      return Optional.empty();
    }

    sessions.remove(correlationId);
    return Optional.of(new SyncMarketsResult(session.getSkipped().get(), session.getReplyTo()));
  }

  private boolean isComplete(SyncSession session) {
    final var expected = appProperties.getSync().getExpectedDataProviders();
    if (expected == null || expected.isEmpty()) {
      return !session.getCompletedProviders().isEmpty();
    }
    return session.getCompletedProviders().containsAll(expected);
  }

  private boolean isTimedOut(SyncSession session) {
    final Duration timeout = appProperties.getSync().getTimeout();
    if (timeout == null || session.getStartedAt() == null) {
      return false;
    }
    return Instant.now().isAfter(session.getStartedAt().plus(timeout));
  }

  @Getter
  @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
  private static final class SyncSession {
    String replyTo;
    Instant startedAt;
    AtomicLong skipped = new AtomicLong();
    Set<String> completedProviders = ConcurrentHashMap.newKeySet();

    SyncSession(String replyTo, Instant startedAt) {
      this.replyTo = replyTo;
      this.startedAt = startedAt;
    }
  }
}
