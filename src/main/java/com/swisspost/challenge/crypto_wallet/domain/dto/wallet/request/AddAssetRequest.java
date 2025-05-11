package com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddAssetRequest {

  private String symbol;

  private Double quantity;

  private BigDecimal price;
}
