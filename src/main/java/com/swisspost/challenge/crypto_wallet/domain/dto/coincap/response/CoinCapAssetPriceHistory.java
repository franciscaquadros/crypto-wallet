package com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response;

import java.util.List;
import lombok.Data;

@Data
public class CoinCapAssetPriceHistory {

  private List<CoinCapAssetPrice> data;
}
