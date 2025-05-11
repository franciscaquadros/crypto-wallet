package com.swisspost.challenge.crypto_wallet.service.wallet;

import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.EvaluateWalletResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.GetWalletResponse;

public interface WalletService {

  void createWallet(final String email);

  GetWalletResponse getWallet(final String email);

  EvaluateWalletResponse evaluateWallet(final EvaluateWalletRequest request);
}
