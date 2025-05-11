package com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluateWalletRequest {

  private List<EvaluateWalletAsset> assets;

  private LocalDate date;
}
