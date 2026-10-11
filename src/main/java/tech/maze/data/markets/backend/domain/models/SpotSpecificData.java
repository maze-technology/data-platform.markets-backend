package tech.maze.data.markets.backend.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Spot market specific data (no extra fields).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotSpecificData() implements SpecificData {
  @Override
  public MarketType marketType() {
    return MarketType.SPOT;
  }
}
