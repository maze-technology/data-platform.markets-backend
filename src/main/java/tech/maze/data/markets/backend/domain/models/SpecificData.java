package tech.maze.data.markets.backend.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polymorphic market-type payload stored as jsonb ({@code specific_data}).
 *
 * <p>Discriminator property {@code m} uses compact numeric names to match the
 * historical DCY layout and keep JSON small in Postgres.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "m", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SpotSpecificData.class, name = "0"),
    @JsonSubTypes.Type(value = PerpetualSpecificData.class, name = "1"),
    @JsonSubTypes.Type(value = FutureSpecificData.class, name = "2"),
    @JsonSubTypes.Type(value = OptionSpecificData.class, name = "3"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface SpecificData
    permits SpotSpecificData, PerpetualSpecificData, FutureSpecificData, OptionSpecificData {
  /**
   * Domain market type for this payload.
   */
  MarketType marketType();
}
