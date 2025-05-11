package com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CoinCapGetAssetsResponse {

  private List<CoinCapAssetData> data;

}
