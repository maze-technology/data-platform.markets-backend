package tech.maze.data.markets.backend.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SpecificDataJsonTest {
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @Test
  void roundTripsOptionSpecificDataWithShortKeys() throws Exception {
    final OptionSpecificData option = new OptionSpecificData(
        42000.5,
        Instant.parse("2024-06-01T00:00:00Z"),
        OptionType.CALL,
        OptionStyle.EUROPEAN,
        "BTC-USDT"
    );

    final String json = mapper.writeValueAsString(option);
    assertThat(json).contains("\"m\":\"3\"");
    assertThat(json).contains("\"s\":42000.5");
    assertThat(json).contains("\"t\":");

    final SpecificData decoded = mapper.readValue(json, SpecificData.class);
    assertThat(decoded).isInstanceOf(OptionSpecificData.class);
    assertThat(decoded.marketType()).isEqualTo(MarketType.OPTION);
    assertThat(((OptionSpecificData) decoded).strike()).isEqualTo(42000.5);
  }

  @Test
  void roundTripsSpotAndFuture() throws Exception {
    assertThat(mapper.readValue("{\"m\":\"0\"}", SpecificData.class))
        .isInstanceOf(SpotSpecificData.class);
    assertThat(mapper.readValue("{\"m\":\"1\"}", SpecificData.class))
        .isInstanceOf(PerpetualSpecificData.class);

    final FutureSpecificData future = new FutureSpecificData(Instant.parse("2024-12-31T00:00:00Z"));
    final SpecificData decoded = mapper.readValue(mapper.writeValueAsString(future), SpecificData.class);
    assertThat(decoded).isInstanceOf(FutureSpecificData.class);
    assertThat(((FutureSpecificData) decoded).expiredAt()).isEqualTo(future.expiredAt());
  }
}
