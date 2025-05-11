package com.swisspost.challenge.crypto_wallet.service.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

  @Mock
  private AssetRepository assetRepository;

  @Mock
  private WalletRepository walletRepository;

  @Mock
  private WalletAssetRepository walletAssetRepository;

  @Mock
  private CoinCapClient coinCapClient;

  @Mock
  private AssetSymbolCache assetSymbolCache;

  @Mock
  private LockManager lockManager;

  @Mock
  private ReentrantReadWriteLock.WriteLock writeLock;

  @InjectMocks
  private AssetServiceImpl assetService;

  @Captor
  private ArgumentCaptor<WalletAsset> walletAssetArgumentCaptor;

  LogCaptor logCaptor = LogCaptor.forClass(AssetServiceImpl.class);

  @BeforeEach
  void setUp() {
    when(lockManager.getWriteLock()).thenReturn(writeLock);
  }

  @Test
  void shouldUpdateAssetPriceSuccessfully() {

    var asset = new Asset();
    asset.setSymbol("BTC");
    asset.setPrice(BigDecimal.valueOf(50000));

    when(assetRepository.findBySymbolIgnoreCase("BTC")).thenReturn(Optional.of(asset));

    assetService.updateAssetPrice("BTC", BigDecimal.valueOf(60000));

    // Assert
    assertEquals(BigDecimal.valueOf(60000), asset.getPrice());
    verify(assetRepository, times(1)).save(asset);
    verify(writeLock).lock();
    verify(writeLock).unlock();
  }

  @Test
  void shouldThrowExceptionWhenAssetNotFound() {

    when(assetRepository.findBySymbolIgnoreCase("ETH")).thenReturn(Optional.empty());

    final var ex = assertThrows(AssetNotFoundException.class,
        () -> assetService.updateAssetPrice("ETH", BigDecimal.valueOf(2000)));

    verify(assetRepository, never()).save(any());
    assertEquals("Asset not found: ETH", ex.getMessage());
    assertTrue(logCaptor.getErrorLogs().stream()
        .anyMatch(log -> log.contains("Asset not found: ETH")));
    verify(writeLock).lock();
    verify(writeLock).unlock();
  }

  @Test
  void shouldAddExistingAssetToWallet() {

    final var request = new AddAssetRequest("BTC", 1.5, BigDecimal.valueOf(10000));
    final var wallet = Wallet.builder().walletId(UUID.randomUUID()).build();
    final var asset = Asset.builder().id(UUID.randomUUID()).symbol("BTC").price(BigDecimal.valueOf(500)).build();
    final var assetData = new CoinCapAssetData("1", "BTC", new BigDecimal("50000"));
    final var coinCapResponse = new CoinCapAssetDetailResponse(assetData, "2025-01-01");

    when(walletRepository.findByCustomerEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(wallet));
    when(assetSymbolCache.getSlugBySymbol("BTC")).thenReturn("bitcoin");
    when(coinCapClient.getAssetDetailsBySlugId("bitcoin")).thenReturn(ResponseEntity.ok(coinCapResponse));
    when(assetRepository.findBySymbolIgnoreCase("BTC")).thenReturn(Optional.of(asset));

    assetService.addAsset(request, "test@example.com");

    final var updatedAsset = Asset.builder().id(asset.getId()).symbol("BTC").price(BigDecimal.valueOf(50000)).build();
    verify(assetRepository, times(1)).save(updatedAsset);
    verify(walletAssetRepository, times(1)).save(walletAssetArgumentCaptor.capture());
    assertEquals(1.5, walletAssetArgumentCaptor.getValue().getQuantity());
    assertEquals(updatedAsset, walletAssetArgumentCaptor.getValue().getAsset());
    verify(writeLock).lock();
    verify(writeLock).unlock();
  }

  @Test
  void shouldThrowIfWalletNotFound() {
    when(walletRepository.findByCustomerEmailIgnoreCase("user@noexist.com"))
        .thenReturn(Optional.empty());

    final var request = new AddAssetRequest("ETH", 2.0, BigDecimal.valueOf(2000));

    assertThrows(WalletNotFoundException.class, () ->
        assetService.addAsset(request, "user@noexist.com"));

    verify(walletRepository, times(1)).findByCustomerEmailIgnoreCase("user@noexist.com");
    verify(assetRepository, times(0)).save(any());
    assertTrue(logCaptor.getErrorLogs().stream()
        .anyMatch(log -> log.contains("Wallet not found for email: user@noexist.com")));
    verify(writeLock).lock();
    verify(writeLock).unlock();
  }

  @Test
  void shouldThrowIfAssetDataIsNull() {
    var wallet = Wallet.builder().walletId(UUID.randomUUID()).build();

    when(walletRepository.findByCustomerEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(wallet));
    when(assetSymbolCache.getSlugBySymbol("DOGE")).thenReturn("dogecoin");
    when(coinCapClient.getAssetDetailsBySlugId("dogecoin")).thenReturn(ResponseEntity.ok(null));

    final var request = new AddAssetRequest("DOGE", 3.0, BigDecimal.valueOf(100));

    final var ex = assertThrows(AssetNotFoundException.class, () ->
        assetService.addAsset(request, "test@example.com"));

    verify(walletRepository, times(1)).findByCustomerEmailIgnoreCase("test@example.com");
    verify(assetRepository, times(0)).save(any());
    assertEquals("Asset data is not available: dogecoin", ex.getMessage());
    assertTrue(logCaptor.getErrorLogs().stream()
        .anyMatch(log -> log.contains("Asset data is not available for slug: dogecoin")));
    verify(writeLock).lock();
    verify(writeLock).unlock();
  }

  @Test
  void shouldThrowOn404FromCoinCap() {
    var wallet = Wallet.builder().walletId(UUID.randomUUID()).build();

    when(walletRepository.findByCustomerEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(wallet));
    when(assetSymbolCache.getSlugBySymbol("SOL")).thenReturn("solana");
    when(coinCapClient.getAssetDetailsBySlugId("solana")).thenThrow(FeignException.NotFound.class);

    final var request = new AddAssetRequest("SOL", 1.0, BigDecimal.valueOf(171));

    final var ex = assertThrows(AssetNotFoundException.class, () ->
        assetService.addAsset(request, "test@example.com"));

    verify(walletRepository, times(1)).findByCustomerEmailIgnoreCase("test@example.com");
    verify(assetRepository, times(0)).save(any());
    assertEquals("Error fetching asset data: SOL", ex.getMessage());
    assertTrue(logCaptor.getErrorLogs().stream()
        .anyMatch(log -> log.contains("Asset not found (404): SOL")));
    verify(writeLock).lock();
    verify(writeLock).unlock();
  }
}