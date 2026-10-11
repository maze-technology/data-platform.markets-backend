package tech.maze.data.markets.backend.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tech.maze.data.markets.backend.domain.models.Market;
import tech.maze.data.markets.backend.infrastructure.persistence.entities.MarketEntity;

/**
 * Maps market entities to domain models and back.
 */
@Mapper(componentModel = "spring")
public interface MarketEntityMapper {
  /**
   * Maps a market entity into its domain model.
   */
  Market toDomain(MarketEntity entity);

  /**
   * Maps a market domain model into its entity representation.
   */
  @Mapping(target = "dataProvidersMetaDatas", ignore = true)
  MarketEntity toEntity(Market market);
}
