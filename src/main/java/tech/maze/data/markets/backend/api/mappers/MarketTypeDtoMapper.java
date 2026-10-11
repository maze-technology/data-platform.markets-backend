package tech.maze.data.markets.backend.api.mappers;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import tech.maze.data.markets.backend.domain.models.MarketType;

/**
 * Maps between market type DTO and domain enums.
 */
@Component
public class MarketTypeDtoMapper {
  /**
   * Converts a domain market type to a DTO market type.
   */
  @Named("marketTypeToDto")
  public tech.maze.dtos.markets.enums.Type toDto(MarketType value) {
    if (value == null) {
      throw new IllegalArgumentException("marketType must not be null");
    }

    return switch (value) {
      case SPOT -> tech.maze.dtos.markets.enums.Type.SPOT;
      case PERP -> tech.maze.dtos.markets.enums.Type.PERPETUAL;
      case FUTURES -> tech.maze.dtos.markets.enums.Type.FUTURE;
      case OPTION -> tech.maze.dtos.markets.enums.Type.OPTION;
      default -> throw new IllegalArgumentException("marketType must be defined");
    };
  }

  /**
   * Converts a DTO market type to a domain market type.
   */
  public MarketType toDomain(tech.maze.dtos.markets.enums.Type value) {
    if (value == null) {
      throw new IllegalArgumentException("marketType must not be null");
    }

    return switch (value) {
      case SPOT -> MarketType.SPOT;
      case PERPETUAL -> MarketType.PERP;
      case FUTURE -> MarketType.FUTURES;
      case OPTION -> MarketType.OPTION;
      case UNRECOGNIZED -> throw new IllegalArgumentException("marketType must be defined");
      default -> throw new IllegalArgumentException("marketType must be defined");
    };
  }
}
