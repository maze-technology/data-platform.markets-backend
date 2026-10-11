package tech.maze.data.markets.backend.api.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tech.maze.data.markets.backend.domain.models.Market;
import tech.maze.data.markets.backend.domain.models.MarketType;

class MarketDtoMapperTest {
  private final MarketTypeDtoMapper marketTypeDtoMapper = new MarketTypeDtoMapper();
  private final MarketDtoMapper mapper = new MarketDtoMapper() {
    @Override
    public tech.maze.dtos.markets.models.Market toDto(Market market) {
      return null;
    }
  };

  @Test
  void marketTypeToDtoMapsValues() {
    assertThatThrownBy(() -> marketTypeDtoMapper.toDto(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("marketType must not be null");
    assertThat(marketTypeDtoMapper.toDto(MarketType.SPOT)).isEqualTo(tech.maze.dtos.markets.enums.Type.SPOT);
    assertThat(marketTypeDtoMapper.toDto(MarketType.PERP)).isEqualTo(tech.maze.dtos.markets.enums.Type.PERPETUAL);
    assertThat(marketTypeDtoMapper.toDto(MarketType.FUTURES)).isEqualTo(tech.maze.dtos.markets.enums.Type.FUTURE);
    assertThat(marketTypeDtoMapper.toDto(MarketType.OPTION)).isEqualTo(tech.maze.dtos.markets.enums.Type.OPTION);
  }

  @Test
  void marketTypeToDomainMapsValuesAndRejectsInvalid() {
    assertThatThrownBy(() -> marketTypeDtoMapper.toDomain(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("marketType must not be null");
    assertThatThrownBy(() -> marketTypeDtoMapper.toDomain(tech.maze.dtos.markets.enums.Type.UNRECOGNIZED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("marketType must be defined");
    assertThat(marketTypeDtoMapper.toDomain(tech.maze.dtos.markets.enums.Type.SPOT)).isEqualTo(MarketType.SPOT);
    assertThat(marketTypeDtoMapper.toDomain(tech.maze.dtos.markets.enums.Type.PERPETUAL)).isEqualTo(MarketType.PERP);
    assertThat(marketTypeDtoMapper.toDomain(tech.maze.dtos.markets.enums.Type.FUTURE)).isEqualTo(MarketType.FUTURES);
    assertThat(marketTypeDtoMapper.toDomain(tech.maze.dtos.markets.enums.Type.OPTION)).isEqualTo(MarketType.OPTION);
  }
}
