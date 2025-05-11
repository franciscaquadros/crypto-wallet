package com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluateWalletResponse {

  private BigDecimal total;

  @JsonProperty("best_asset")
  private String bestAsset;

  @JsonProperty("best_performance")
  private BigDecimal bestPerformance;

  @JsonProperty("worst_asset")
  private String worstAsset;

  @JsonProperty("worst_performance")
  private BigDecimal worstPerformance;
}
