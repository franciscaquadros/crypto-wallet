package com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CoinCapAssetPrice {

  private BigDecimal priceUsd;

  private LocalDateTime date;
}
