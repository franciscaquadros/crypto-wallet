package com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinCapAssetData {

  private String id;
  private String symbol;
  private BigDecimal priceUsd;
}
