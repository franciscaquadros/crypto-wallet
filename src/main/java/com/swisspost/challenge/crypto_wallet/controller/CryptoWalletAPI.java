package com.swisspost.challenge.crypto_wallet.controller;

import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.AddAssetRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.CreateWalletRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.EvaluateWalletResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.GetWalletResponse;
import org.springframework.http.ResponseEntity;

public interface CryptoWalletAPI {

  ResponseEntity<Void> createWallet(final CreateWalletRequest request);

  ResponseEntity<Void> addAsset(final AddAssetRequest request, final String email);

  ResponseEntity<GetWalletResponse> showWallet(final String email);

  ResponseEntity<EvaluateWalletResponse> evaluateWallet(final EvaluateWalletRequest request);
}
