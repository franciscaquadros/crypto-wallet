package com.swisspost.challenge.crypto_wallet.controller;

import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.AddAssetRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.CreateWalletRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.EvaluateWalletResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.GetWalletResponse;
import com.swisspost.challenge.crypto_wallet.service.asset.AssetService;
import com.swisspost.challenge.crypto_wallet.service.wallet.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crypto-wallet")
public class CryptoWalletController implements CryptoWalletAPI {

  private final WalletService walletService;

  private final AssetService assetService;

  public CryptoWalletController(final WalletService walletService, final AssetService assetService) {
    this.walletService = walletService;
    this.assetService = assetService;
  }

  @Override
  @PostMapping(value = "/wallet", consumes = {"application/json"})
  public ResponseEntity<Void> createWallet(@RequestBody final CreateWalletRequest body) {
      walletService.createWallet(body.getEmail());
      return ResponseEntity.ok().build();
  }

  @Override
  @PostMapping(value = "/asset/{email}", consumes = {"application/json"}, produces = {"application/json"})
  public ResponseEntity<Void> addAsset(@RequestBody final AddAssetRequest body, @PathVariable final String email) {
      assetService.addAsset(body, email);
      return ResponseEntity.ok().build();
  }

  @Override
  @GetMapping(value = "/wallet/{email}", produces = {"application/json"})
  public ResponseEntity<GetWalletResponse> showWallet(@PathVariable final String email) {
      return ResponseEntity.ok(walletService.getWallet(email));
  }

  @Override
  @PostMapping(value = "/wallet/evaluate", consumes = {"application/json"}, produces = {"application/json"})
  public ResponseEntity<EvaluateWalletResponse> evaluateWallet(@RequestBody final EvaluateWalletRequest request) {
    return ResponseEntity.ok(walletService.evaluateWallet(request));
  }
}
