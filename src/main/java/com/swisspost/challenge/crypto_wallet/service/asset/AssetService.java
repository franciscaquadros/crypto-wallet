package com.swisspost.challenge.crypto_wallet.service.asset;

import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.AddAssetRequest;
import java.math.BigDecimal;

public interface AssetService {

  void updateAssetPrice(final String assetSymbol, final BigDecimal priceUsd);

  void addAsset(final AddAssetRequest body, final String email);

}
