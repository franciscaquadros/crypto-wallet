package com.swisspost.challenge.crypto_wallet.component;

import com.swisspost.challenge.crypto_wallet.client.CoinCapClient;
import com.swisspost.challenge.crypto_wallet.config.TokenFetchConfig;
import com.swisspost.challenge.crypto_wallet.repository.WalletAssetRepository;
import com.swisspost.challenge.crypto_wallet.service.asset.AssetService;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class TokenPriceFetcher {

  private final CoinCapClient coinCapClient;
  private final TokenFetchConfig config;
  private final ExecutorService threadPool;
  private final WalletAssetRepository walletAssetRepository;
  private final AssetService assetService;

  private final AssetSymbolCache assetSymbolCache;

  @Autowired
  public TokenPriceFetcher(final CoinCapClient coinCapClient, final TokenFetchConfig config, final WalletAssetRepository walletAssetRepository,
      final AssetService assetService, final AssetSymbolCache assetSymbolCache, final ExecutorService threadPool) {
    this.coinCapClient = coinCapClient;
    this.config = config;
    this.walletAssetRepository = walletAssetRepository;
    this.assetService = assetService;
    this.assetSymbolCache = assetSymbolCache;
    this.threadPool = threadPool;
    startScheduler();
  }

  private void startScheduler() {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::fetchPrices, 0, config.getIntervalSeconds(), TimeUnit.SECONDS);
  }

  protected void fetchPrices() {
    final var usersAssetsSymbols = walletAssetRepository.findDistinctByAssetIsNotNull().stream()
        .map(walletAsset -> walletAsset.getAsset().getSymbol())
        .toList();

    for (final String token : usersAssetsSymbols) {
      threadPool.submit(() -> {
        try {
          final var slugId = assetSymbolCache.getSlugBySymbol(token);
          final var tokenDetailResponse = coinCapClient.getAssetDetailsBySlugId(slugId);
          if (tokenDetailResponse.getStatusCode().is2xxSuccessful()) {
            final var tokenDetailBody = tokenDetailResponse.getBody();
            final var symbol = tokenDetailBody.getData().getSymbol();
            final var price = tokenDetailBody.getData().getPriceUsd();
            assetService.updateAssetPrice(symbol, price);
            log.info("Updated Token: {}, Price: {}", token, price);
          } else {
            log.error("Failed to fetch price for token: {}", token);
          }
        } catch (Exception e) {
          log.error("Error fetching price for token: {}", token, e);
        }
      });
    }
  }

  @PreDestroy
  public void shutdownThreadPool() {
    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (final InterruptedException e) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
