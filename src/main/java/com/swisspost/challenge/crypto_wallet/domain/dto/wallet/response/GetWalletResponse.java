package com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetWalletResponse {

  private UUID id;

  private BigDecimal total;

  private List<GetWalletAssetResponse> assets;
}
