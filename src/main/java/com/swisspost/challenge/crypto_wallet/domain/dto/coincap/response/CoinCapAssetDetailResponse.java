package com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinCapAssetDetailResponse {

  private CoinCapAssetData data;
  private String timestamp;
}
