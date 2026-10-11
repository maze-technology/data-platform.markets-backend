package tech.maze.data.markets.backend.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Perpetual market specific data (no extra fields).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerpetualSpecificData() implements SpecificData {
  @Override
  public MarketType marketType() {
    return MarketType.PERP;
  }
}
