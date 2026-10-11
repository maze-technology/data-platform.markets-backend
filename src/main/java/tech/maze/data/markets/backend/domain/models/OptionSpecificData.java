package tech.maze.data.markets.backend.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Option market specific data.
 *
 * <p>JSON property names are single letters to reduce jsonb storage size.
 *
 * @param strike option strike
 * @param expiredAt option expiry
 * @param type call/put
 * @param style american/european
 * @param underlying underlying market identifier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptionSpecificData(
    @JsonProperty("s") double strike,
    @JsonProperty("e") Instant expiredAt,
    @JsonProperty("t") OptionType type,
    @JsonProperty("o") OptionStyle style,
    @JsonProperty("u") String underlying
) implements SpecificData {
  @Override
  public MarketType marketType() {
    return MarketType.OPTION;
  }
}
