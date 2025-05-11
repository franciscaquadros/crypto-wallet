package com.swisspost.challenge.crypto_wallet.service.asset;

import com.swisspost.challenge.crypto_wallet.client.CoinCapClient;
import com.swisspost.challenge.crypto_wallet.component.AssetSymbolCache;
import com.swisspost.challenge.crypto_wallet.component.LockManager;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetData;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetDetailResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.AddAssetRequest;
import com.swisspost.challenge.crypto_wallet.repository.entity.Asset;
import com.swisspost.challenge.crypto_wallet.repository.entity.Wallet;
import com.swisspost.challenge.crypto_wallet.repository.entity.WalletAsset;
import com.swisspost.challenge.crypto_wallet.exception.AssetNotFoundException;
import com.swisspost.challenge.crypto_wallet.exception.WalletNotFoundException;
import com.swisspost.challenge.crypto_wallet.repository.AssetRepository;
import com.swisspost.challenge.crypto_wallet.repository.WalletAssetRepository;
import com.swisspost.challenge.crypto_wallet.repository.WalletRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AssetServiceImpl implements AssetService {

  private final AssetRepository assetRepository;

  private final WalletRepository walletRepository;

  private final WalletAssetRepository walletAssetRepository;

  private final CoinCapClient coinCapClient;

  private final AssetSymbolCache assetSymbolCache;

  private final LockManager lockManager;


  @Autowired
  public AssetServiceImpl(final AssetRepository assetRepository, final WalletRepository walletRepository,
      final WalletAssetRepository walletAssetRepository, final CoinCapClient coinCapClient,
      final AssetSymbolCache assetSymbolCache, final LockManager lockManager) {
    this.assetRepository = assetRepository;
    this.walletRepository = walletRepository;
    this.walletAssetRepository = walletAssetRepository;
    this.coinCapClient = coinCapClient;
    this.assetSymbolCache = assetSymbolCache;
    this.lockManager = lockManager;
  }

  @Override
  @Transactional
  public void updateAssetPrice(final String assetSymbol, final BigDecimal priceUsd) {

    final var writeLock = lockManager.getWriteLock();
    writeLock.lock();
    try {
      final var asset = assetRepository.findBySymbolIgnoreCase(assetSymbol)
          .orElseThrow(() -> {
            log.error("Asset not found: {}", assetSymbol);
            return new AssetNotFoundException("Asset not found: " + assetSymbol);
          });

      asset.setPrice(priceUsd);
      assetRepository.save(asset);
    } finally {
      writeLock.unlock(); // Release the write lock
    }
  }

  @Override
  public void addAsset(final AddAssetRequest request, final String email) {

    final var writeLock = lockManager.getWriteLock();
    writeLock.lock();
    try {
      //check if user wallet exists
      final var wallet = walletRepository.findByCustomerEmailIgnoreCase(email)
          .orElseThrow(() -> {
            log.error("Wallet not found for email: {}", email);
            return new WalletNotFoundException("Wallet not found for email: " + email);
          });

      //get price from CoinCap
      final var slugId = assetSymbolCache.getSlugBySymbol(request.getSymbol());
      final var assetDetail = coinCapClient.getAssetDetailsBySlugId(slugId);

      final var assetData = Optional.ofNullable(assetDetail)
          .map(ResponseEntity::getBody)
          .map(CoinCapAssetDetailResponse::getData)
          .orElseThrow(() -> {
            log.error("Asset data is not available for slug: {}", slugId);
            return new AssetNotFoundException("Asset data is not available: " + slugId);
          });


      assetRepository.findBySymbolIgnoreCase(request.getSymbol())
          .ifPresentOrElse(existingAsset -> {
            existingAsset.setPrice(assetData.getPriceUsd());
            assetRepository.save(existingAsset);
            handleWalletAsset(wallet, existingAsset, request.getQuantity());
          }, () ->
            createAssetAndWalletAsset(request, wallet, assetData));

    } catch (FeignException.NotFound e) {
      log.error("Asset not found (404): {}", request.getSymbol(), e);
      throw new AssetNotFoundException("Error fetching asset data: " + request.getSymbol());
    } finally {
      writeLock.unlock();
    }
  }

  private void handleWalletAsset(final Wallet wallet, final Asset asset, final Double quantityToAdd) {
    walletAssetRepository.findByWalletAndAsset(wallet, asset)
        .ifPresentOrElse(walletAsset ->
          updateWalletAssetQuantity(walletAsset, quantityToAdd),
        () ->
          createNewWalletAsset(wallet, asset, quantityToAdd));
  }

  private void updateWalletAssetQuantity(final WalletAsset walletAsset, final Double quantityToAdd) {
    walletAsset.setQuantity(walletAsset.getQuantity() + quantityToAdd);
    walletAssetRepository.save(walletAsset);
  }

  private void createNewWalletAsset(final Wallet wallet, final Asset asset, final Double quantity) {
    WalletAsset newWalletAsset = WalletAsset.builder()
        .wallet(wallet)
        .asset(asset)
        .quantity(quantity)
        .build();
    walletAssetRepository.save(newWalletAsset);
  }

  private void createAssetAndWalletAsset(final AddAssetRequest request, final Wallet wallet, final CoinCapAssetData assetData) {
    final var newAsset = Asset.builder()
        .symbol(request.getSymbol().toUpperCase())
        .price(assetData.getPriceUsd())
        .build();
    assetRepository.save(newAsset);
    final var walletAsset = WalletAsset.builder()
        .asset(newAsset)
        .wallet(wallet)
        .quantity(request.getQuantity())
        .build();
    walletAssetRepository.save(walletAsset);
  }
}
