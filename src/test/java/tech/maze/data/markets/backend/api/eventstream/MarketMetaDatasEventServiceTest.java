package tech.maze.data.markets.backend.api.eventstream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Value;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.maze.commons.mappers.ProtobufValueMapper;
import tech.maze.data.markets.backend.api.mappers.MarketTypeDtoMapper;
import tech.maze.data.markets.backend.domain.models.MarketType;
import tech.maze.data.markets.backend.domain.models.SpotSpecificData;
import tech.maze.data.markets.backend.infrastructure.persistence.entities.MarketDataProviderMetaDataEntity;
import tech.maze.data.markets.backend.infrastructure.persistence.entities.MarketEntity;
import tech.maze.data.markets.backend.infrastructure.persistence.repositories.MarketDataProvidersMetadatasJpaRepository;
import tech.maze.data.markets.backend.infrastructure.persistence.repositories.MarketJpaRepository;
import tech.maze.dtos.markets.enums.Type;
import tech.maze.dtos.markets.models.MarketMetaDatas;
import tech.maze.dtos.markets.models.MarketMetaDatasMarket;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MarketMetaDatasEventServiceTest {
  @Mock
  private MarketJpaRepository marketJpaRepository;
  @Mock
  private MarketDataProvidersMetadatasJpaRepository marketDataProvidersMetadatasJpaRepository;

  private final ProtobufValueMapper protobufValueMapper =
      Mappers.getMapper(ProtobufValueMapper.class);
  private final MarketTypeDtoMapper marketTypeDtoMapper = new MarketTypeDtoMapper();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void createsMarketAndMetaDataWhenUnknown() {
    final MarketMetaDatasEventService service = new MarketMetaDatasEventService(
        marketJpaRepository,
        marketDataProvidersMetadatasJpaRepository,
        protobufValueMapper,
        marketTypeDtoMapper,
        objectMapper
    );
    final UUID dataProviderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    final MarketMetaDatas payload = MarketMetaDatas.newBuilder()
        .setDataProviderId(
            Value.newBuilder().setStringValue(dataProviderId.toString()).build()
        )
        .setMarket(
            MarketMetaDatasMarket.newBuilder()
                .setType(Type.SPOT)
                .setExchangeId(Value.newBuilder().setStringValue("binance").build())
                .setBaseId(Value.newBuilder().setStringValue("BTC").build())
                .setQuoteId(Value.newBuilder().setStringValue("USDT").build())
                .build()
        )
        .putExtraDatas("k", "v")
        .putToolBox("k", "v")
        .build();

    when(
        marketJpaRepository
            .findFirstByTypeAndExchangeIgnoreCaseAndBaseIgnoreCaseAndQuoteIgnoreCase(
                MarketType.SPOT,
                "binance",
                "BTC",
                "USDT"
            )
    ).thenReturn(Optional.empty());
    when(marketJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(
        marketDataProvidersMetadatasJpaRepository.findFirstByMarketIdAndDataProviderId(
            any(),
            eq(dataProviderId)
        )
    ).thenReturn(Optional.empty());
    when(marketDataProvidersMetadatasJpaRepository.save(any())).thenAnswer(invocation -> {
      MarketDataProviderMetaDataEntity entity = invocation.getArgument(0);
      if (entity.getId() == null) {
        entity.setId(1L);
      }
      return entity;
    });

    service.process(payload);

    final ArgumentCaptor<MarketEntity> marketCaptor = ArgumentCaptor.forClass(MarketEntity.class);
    verify(marketJpaRepository).save(marketCaptor.capture());
    assertThat(marketCaptor.getValue().getType()).isEqualTo(MarketType.SPOT);
    assertThat(marketCaptor.getValue().getExchange()).isEqualTo("binance");
    assertThat(marketCaptor.getValue().getBase()).isEqualTo("BTC");
    assertThat(marketCaptor.getValue().getQuote()).isEqualTo("USDT");
    assertThat(marketCaptor.getValue().getSpecificData()).isInstanceOf(SpotSpecificData.class);

    final ArgumentCaptor<MarketDataProviderMetaDataEntity> metaCaptor =
        ArgumentCaptor.forClass(MarketDataProviderMetaDataEntity.class);
    verify(marketDataProvidersMetadatasJpaRepository).save(metaCaptor.capture());
    assertThat(metaCaptor.getValue().getDataProviderId()).isEqualTo(dataProviderId);
    assertThat(metaCaptor.getValue().getExtraDatas()).isEqualTo(Map.of("k", "v"));
    assertThat(metaCaptor.getValue().getToolBox()).isEqualTo(Map.of("k", "v"));
  }

  @Test
  void updatesExistingMetaDataWhenChanged() {
    final MarketMetaDatasEventService service = new MarketMetaDatasEventService(
        marketJpaRepository,
        marketDataProvidersMetadatasJpaRepository,
        protobufValueMapper,
        marketTypeDtoMapper,
        objectMapper
    );
    final UUID dataProviderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    final MarketEntity existingMarket = new MarketEntity();
    existingMarket.setId(UUID.randomUUID());
    existingMarket.setType(MarketType.SPOT);
    existingMarket.setExchange("binance");
    existingMarket.setBase("BTC");
    existingMarket.setQuote("USDT");
    existingMarket.setCreatedAt(Instant.now());

    final MarketDataProviderMetaDataEntity existingMeta = new MarketDataProviderMetaDataEntity();
    existingMeta.setId(42L);
    existingMeta.setMarket(existingMarket);
    existingMeta.setDataProviderId(dataProviderId);
    existingMeta.setExtraDatas(Map.of("old", "1"));
    existingMeta.setToolBox(Map.of("old", "1"));

    final MarketMetaDatas payload = MarketMetaDatas.newBuilder()
        .setDataProviderId(
            Value.newBuilder().setStringValue(dataProviderId.toString()).build()
        )
        .setMarket(
            MarketMetaDatasMarket.newBuilder()
                .setType(Type.SPOT)
                .setExchangeId(Value.newBuilder().setStringValue("binance").build())
                .setBaseId(Value.newBuilder().setStringValue("BTC").build())
                .setQuoteId(Value.newBuilder().setStringValue("USDT").build())
                .build()
        )
        .putExtraDatas("k", "v")
        .putToolBox("k", "v")
        .build();

    when(
        marketJpaRepository
            .findFirstByTypeAndExchangeIgnoreCaseAndBaseIgnoreCaseAndQuoteIgnoreCase(
                MarketType.SPOT,
                "binance",
                "BTC",
                "USDT"
            )
    ).thenReturn(Optional.of(existingMarket));
    when(
        marketDataProvidersMetadatasJpaRepository.findFirstByMarketIdAndDataProviderId(
            existingMarket.getId(),
            dataProviderId
        )
    ).thenReturn(Optional.of(existingMeta));
    when(marketDataProvidersMetadatasJpaRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.process(payload);

    verify(marketDataProvidersMetadatasJpaRepository).save(eq(existingMeta));
    assertThat(existingMeta.getExtraDatas()).isEqualTo(Map.of("k", "v"));
    assertThat(existingMeta.getToolBox()).isEqualTo(Map.of("k", "v"));
  }
}
