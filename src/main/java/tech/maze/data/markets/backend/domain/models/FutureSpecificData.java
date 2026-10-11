package tech.maze.data.markets.backend.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Dated future market specific data.
 *
 * @param expiredAt contract expiry
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FutureSpecificData(
    @JsonProperty("e") Instant expiredAt
) implements SpecificData {
  @Override
  public MarketType marketType() {
    return MarketType.FUTURES;
  }
}
