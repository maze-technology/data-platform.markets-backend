package tech.maze.data.markets.backend.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tech.maze.data.markets.backend.domain.models.MarketType;
import tech.maze.data.markets.backend.domain.models.SpecificData;

/**
 * JPA entity for markets.
 */
@Entity
@Table(name = "markets")
@Getter
@Setter
@NoArgsConstructor
public class MarketEntity {
  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MarketType type;

  @Column(nullable = false)
  private String exchange;

  @Column(nullable = false)
  private String base;

  @Column(nullable = false)
  private String quote;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "specific_data", columnDefinition = "jsonb")
  private SpecificData specificData;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "market")
  private List<MarketDataProviderMetaDataEntity> dataProvidersMetaDatas;
}
