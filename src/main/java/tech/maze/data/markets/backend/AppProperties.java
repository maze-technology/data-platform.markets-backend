package tech.maze.data.markets.backend;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the application.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = AppProperties.PREFIX)
public class AppProperties {
  public static final String PREFIX = "maze";

  String name;
  int marketsPerPage = 50;

  /**
   * Sync/fetch fan-out configuration.
   */
  Sync sync = new Sync();

  /**
   * Sync/fetch properties.
   */
  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Sync {
    /**
     * Fixed Kafka topic for FetchMarketsRequest fan-out to data providers.
     */
    String fetchRequestTopic = "markets.fetch.request";

    /**
     * Fixed Kafka topic where data providers publish FetchMarketsResponse.
     */
    String fetchResponseTopic = "markets.fetch.response";

    /**
     * Expected data-provider identifiers that must each emit {@code isLast=true}
     * before a SyncMarketsRequest is considered complete. When empty, the first
     * distinct provider that reports {@code isLast} completes the sync (dev mode).
     */
    List<String> expectedDataProviders = new ArrayList<>();

    /**
     * Maximum time to wait for fan-in before failing the sync session.
     */
    Duration timeout = Duration.ofMinutes(5);
  }
}
