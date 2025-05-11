package com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class GetWalletAssetResponse {

  private String symbol;

  private Double quantity;

  private BigDecimal price;

  private BigDecimal value;
}
