package tech.maze.data.markets.backend.domain.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.maze.commons.eventstream.EventSender;
import tech.maze.data.markets.backend.AppProperties;
import tech.maze.data.markets.backend.domain.models.SyncMarketsResult;
import tech.maze.data.markets.backend.infrastructure.sync.InMemorySyncSessionStore;
import tech.maze.dtos.markets.enums.SyncStatus;
import tech.maze.dtos.markets.events.EventTypes;
import tech.maze.dtos.markets.payloads.FetchMarketsRequest;

@ExtendWith(MockitoExtension.class)
class SyncMarketsUseCaseImplTest {
  @Mock
  private EventSender eventSender;

  private AppProperties appProperties;
  private SyncMarketsUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    appProperties = new AppProperties();
    appProperties.getSync().setExpectedDataProviders(List.of("dp-a", "dp-b"));
    appProperties.getSync().setTimeout(Duration.ofMinutes(1));
    useCase = new SyncMarketsUseCaseImpl(
        eventSender,
        appProperties,
        new InMemorySyncSessionStore(appProperties)
    );
  }

  @Test
  void startSyncFansOutFetchRequest() {
    when(eventSender.send(
        eq("markets.fetch.request"),
        any(FetchMarketsRequest.class),
        eq(EventTypes.FETCH_MARKETS_REQUEST),
        anyMap()
    )).thenReturn(true);

    assertThat(useCase.startSync("corr-1")).isTrue();
    verify(eventSender).send(
        eq("markets.fetch.request"),
        any(FetchMarketsRequest.class),
        eq(EventTypes.FETCH_MARKETS_REQUEST),
        anyMap()
    );
  }

  @Test
  void completesOnlyAfterContiguousPagesAndAllProviders() {
    when(eventSender.send(any(), any(), any(), anyMap())).thenReturn(true);
    useCase.startSync("corr-1");

    assertThat(useCase.onFetchResponse("corr-1", "dp-a", 0, 1, false)).isEmpty();
    assertThat(useCase.onFetchResponse("corr-1", "dp-a", 1, 0, true)).isEmpty();
    assertThat(useCase.onFetchResponse("corr-1", "dp-b", 0, 2, true)).isPresent().get()
        .satisfies(result -> {
          assertThat(result.skipped()).isEqualTo(3);
          assertThat(result.status()).isEqualTo(SyncStatus.COMPLETED);
          assertThat(result.incompleteProviders()).isEmpty();
        });
  }

  @Test
  void doesNotCompleteWhenLastArrivesBeforeMissingPages() {
    when(eventSender.send(any(), any(), any(), anyMap())).thenReturn(true);
    useCase.startSync("corr-1");

    // isLast on page 1 but page 0 missing — barrier holds
    assertThat(useCase.onFetchResponse("corr-1", "dp-a", 1, 0, true)).isEmpty();
    assertThat(useCase.onFetchResponse("corr-1", "dp-b", 0, 0, true)).isEmpty();

    // fill gap
    Optional<SyncMarketsResult> result = useCase.onFetchResponse("corr-1", "dp-a", 0, 0, false);
    assertThat(result).isPresent();
    assertThat(result.get().status()).isEqualTo(SyncStatus.COMPLETED);
  }
}
