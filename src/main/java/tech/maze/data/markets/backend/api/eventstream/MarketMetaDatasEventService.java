package tech.maze.data.markets.backend.api.eventstream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.maze.commons.mappers.ProtobufValueMapper;
import tech.maze.data.markets.backend.api.mappers.MarketTypeDtoMapper;
import tech.maze.data.markets.backend.domain.models.MarketType;
import tech.maze.data.markets.backend.domain.models.PerpetualSpecificData;
import tech.maze.data.markets.backend.domain.models.SpecificData;
import tech.maze.data.markets.backend.domain.models.SpotSpecificData;
import tech.maze.data.markets.backend.infrastructure.persistence.entities.MarketDataProviderMetaDataEntity;
import tech.maze.data.markets.backend.infrastructure.persistence.entities.MarketEntity;
import tech.maze.data.markets.backend.infrastructure.persistence.repositories.MarketDataProvidersMetadatasJpaRepository;
import tech.maze.data.markets.backend.infrastructure.persistence.repositories.MarketJpaRepository;
import tech.maze.dtos.markets.models.MarketMetaDatas;
import tech.maze.dtos.markets.models.MarketMetaDatasMarket;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles single-market metadata events and persists/upserts related records.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MarketMetaDatasEventService {
  MarketJpaRepository marketJpaRepository;
  MarketDataProvidersMetadatasJpaRepository marketDataProvidersMetadatasJpaRepository;
  ProtobufValueMapper protobufValueMapper;
  MarketTypeDtoMapper marketTypeDtoMapper;
  ObjectMapper objectMapper;

  /**
   * Processes one metadata payload (one market per event) and upserts persistence records.
   */
  @Transactional
  public void process(MarketMetaDatas payload) {
    final UUID dataProviderId = protobufValueMapper.toUuid(payload.getDataProviderId())
        .orElseThrow(() -> new IllegalArgumentException("dataProviderId must be a valid UUID"));
    if (!payload.hasMarket()) {
      throw new IllegalArgumentException("market is required");
    }

    final MarketMetaDatasMarket marketPayload = payload.getMarket();
    final MarketType marketType = marketTypeDtoMapper.toDomain(marketPayload.getType());
    final String exchange = protobufValueMapper.toString(marketPayload.getExchangeId())
        .orElseThrow(() -> new IllegalArgumentException("exchangeId must not be blank"));
    final String base = protobufValueMapper.toString(marketPayload.getBaseId())
        .orElseThrow(() -> new IllegalArgumentException("baseId must not be blank"));
    final String quote = protobufValueMapper.toString(marketPayload.getQuoteId())
        .orElseThrow(() -> new IllegalArgumentException("quoteId must not be blank"));

    final MarketEntity market = marketJpaRepository
        .findFirstByTypeAndExchangeIgnoreCaseAndBaseIgnoreCaseAndQuoteIgnoreCase(
            marketType,
            exchange,
            base,
            quote
        )
        .orElseGet(() -> createMarket(marketType, exchange, base, quote, marketPayload));

    final MarketDataProviderMetaDataEntity metaData = marketDataProvidersMetadatasJpaRepository
        .findFirstByMarketIdAndDataProviderId(market.getId(), dataProviderId)
        .orElseGet(() -> createMetaData(market, dataProviderId));

    updateMetaDataIfNeeded(metaData, payload);
  }

  private MarketEntity createMarket(
      MarketType marketType,
      String exchange,
      String base,
      String quote,
      MarketMetaDatasMarket marketPayload
  ) {
    final MarketEntity market = new MarketEntity();
    market.setId(UUID.randomUUID());
    market.setType(marketType);
    market.setExchange(exchange);
    market.setBase(base);
    market.setQuote(quote);
    market.setSpecificData(resolveSpecificData(marketType, marketPayload));
    market.setCreatedAt(Instant.now());
    return marketJpaRepository.save(market);
  }

  private MarketDataProviderMetaDataEntity createMetaData(
      MarketEntity market,
      UUID dataProviderId
  ) {
    final MarketDataProviderMetaDataEntity metaData = new MarketDataProviderMetaDataEntity();
    metaData.setMarket(market);
    metaData.setDataProviderId(dataProviderId);
    metaData.setCreatedAt(Instant.now());
    return metaData;
  }

  private void updateMetaDataIfNeeded(
      MarketDataProviderMetaDataEntity metaData,
      MarketMetaDatas payload
  ) {
    boolean changed = false;
    if (!Objects.equals(metaData.getExtraDatas(), payload.getExtraDatasMap())) {
      metaData.setExtraDatas(payload.getExtraDatasMap());
      changed = true;
    }
    if (!Objects.equals(metaData.getToolBox(), payload.getToolBoxMap())) {
      metaData.setToolBox(payload.getToolBoxMap());
      changed = true;
    }

    if (changed || metaData.getId() == null) {
      marketDataProvidersMetadatasJpaRepository.save(metaData);
    }
  }

  private SpecificData resolveSpecificData(
      MarketType marketType,
      MarketMetaDatasMarket marketPayload
  ) {
    if (marketPayload.hasSpecificData()) {
      final var value = marketPayload.getSpecificData();
      try {
        if (value.hasStringValue() && !value.getStringValue().isBlank()) {
          return objectMapper.readValue(value.getStringValue(), SpecificData.class);
        }
        if (value.hasStructValue()) {
          final String json = JsonFormat.printer().print(value.getStructValue());
          return objectMapper.readValue(json, SpecificData.class);
        }
      } catch (InvalidProtocolBufferException | RuntimeException ex) {
        throw new IllegalArgumentException("Invalid market.specificData payload", ex);
      }
    }

    return switch (marketType) {
      case SPOT -> new SpotSpecificData();
      case PERP -> new PerpetualSpecificData();
      case FUTURES, OPTION -> null;
    };
  }
}
