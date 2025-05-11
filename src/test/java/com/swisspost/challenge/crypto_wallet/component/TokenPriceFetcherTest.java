package com.swisspost.challenge.crypto_wallet.component;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swisspost.challenge.crypto_wallet.client.CoinCapClient;
import com.swisspost.challenge.crypto_wallet.config.TokenFetchConfig;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetData;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetDetailResponse;
import com.swisspost.challenge.crypto_wallet.repository.entity.Asset;
import com.swisspost.challenge.crypto_wallet.repository.entity.WalletAsset;
import com.swisspost.challenge.crypto_wallet.repository.WalletAssetRepository;
import com.swisspost.challenge.crypto_wallet.service.asset.AssetService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TokenPriceFetcherTest {

  @Mock
  private WalletAssetRepository walletAssetRepository;

  @Mock
  private CoinCapClient coinCapClient;

  @Mock
  private TokenFetchConfig tokenFetchConfig;

  @Mock
  private AssetSymbolCache assetSymbolCache;

  @Mock
  private AssetService assetService;

  @Mock
  private ExecutorService threadPool;

  private TokenPriceFetcher priceFetcher;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    threadPool = Executors.newFixedThreadPool(3);
    when(tokenFetchConfig.getIntervalSeconds()).thenReturn(3600);

    priceFetcher = new TokenPriceFetcher(
        coinCapClient,
        tokenFetchConfig,
        walletAssetRepository,
        assetService,
        assetSymbolCache,
        threadPool
    );
  }

  @Test
  void shouldUpdatePriceForValidToken() throws InterruptedException {
    var btcAsset = mock(WalletAsset.class);
    var btcEntity = mock(Asset.class);

    when(walletAssetRepository.findDistinctByAssetIsNotNull()).thenReturn(List.of(btcAsset));
    when(btcAsset.getAsset()).thenReturn(btcEntity);
    when(btcEntity.getSymbol()).thenReturn("BTC");

    when(assetSymbolCache.getSlugBySymbol("BTC")).thenReturn("bitcoin");

    var detailData = new CoinCapAssetData("1", "BTC", new BigDecimal("100000"));
    var detailResponse = new CoinCapAssetDetailResponse(detailData, LocalDateTime.now().toString());

    when(coinCapClient.getAssetDetailsBySlugId("bitcoin"))
        .thenReturn(ResponseEntity.ok(detailResponse));

    priceFetcher.fetchPrices();

    threadPool.shutdown();
    threadPool.awaitTermination(1, TimeUnit.SECONDS);

    // Verify update call
    verify(assetSymbolCache, times(1)).getSlugBySymbol("BTC");
    verify(coinCapClient, times(1)).getAssetDetailsBySlugId("bitcoin");
    verify(assetService).updateAssetPrice("BTC", new BigDecimal("100000"));
  }

  @Test
  void shouldNotUpdatePriceIfApiFails() throws InterruptedException {
    var ethAsset = mock(WalletAsset.class);
    var ethEntity = mock(Asset.class);

    when(walletAssetRepository.findDistinctByAssetIsNotNull()).thenReturn(List.of(ethAsset));
    when(ethAsset.getAsset()).thenReturn(ethEntity);
    when(ethEntity.getSymbol()).thenReturn("ETH");

    when(assetSymbolCache.getSlugBySymbol("ETH")).thenReturn("ethereum");

    when(coinCapClient.getAssetDetailsBySlugId("ethereum"))
        .thenReturn(ResponseEntity.status(500).build());

    LogCaptor logCaptor = LogCaptor.forClass(TokenPriceFetcher.class);
    priceFetcher.fetchPrices();

    threadPool.shutdown();
    threadPool.awaitTermination(1, TimeUnit.SECONDS);

    verify(assetSymbolCache, times(1)).getSlugBySymbol("ETH");
    verify(coinCapClient, times(1)).getAssetDetailsBySlugId("ethereum");
    verify(assetService, never()).updateAssetPrice(any(), any());

    assertTrue(logCaptor.getErrorLogs().stream()
        .anyMatch(log -> log.contains("Failed to fetch price for token: ETH")));
  }

  @AfterEach
  void tearDown() {
    threadPool.shutdownNow();
  }
}