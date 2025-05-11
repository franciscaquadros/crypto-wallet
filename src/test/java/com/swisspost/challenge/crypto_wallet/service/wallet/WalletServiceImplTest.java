package com.swisspost.challenge.crypto_wallet.service.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swisspost.challenge.crypto_wallet.client.CoinCapClient;
import com.swisspost.challenge.crypto_wallet.component.AssetSymbolCache;
import com.swisspost.challenge.crypto_wallet.component.LockManager;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetPrice;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletAsset;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletRequest;
import com.swisspost.challenge.crypto_wallet.repository.entity.Asset;
import com.swisspost.challenge.crypto_wallet.repository.entity.Customer;
import com.swisspost.challenge.crypto_wallet.repository.entity.Wallet;
import com.swisspost.challenge.crypto_wallet.repository.entity.WalletAsset;
import com.swisspost.challenge.crypto_wallet.exception.WalletNotFoundException;
import com.swisspost.challenge.crypto_wallet.repository.CustomerRepository;
import com.swisspost.challenge.crypto_wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

  @Mock
  private CustomerRepository customerRepository;
  @Mock
  private WalletRepository walletRepository;
  @Mock
  private CoinCapClient coinCapClient;
  @Mock
  private AssetSymbolCache assetSymbolCache;
  @Mock
  private LockManager lockManager;
  @Mock
  private ReentrantReadWriteLock.ReadLock readLock;
  @Captor
  private ArgumentCaptor<Wallet> walletArgumentCaptor;
  @Captor
  private ArgumentCaptor<Customer> customerArgumentCaptor;

  private WalletServiceImpl walletService;

  LogCaptor logCaptor = LogCaptor.forClass(WalletServiceImpl.class);

  private final LocalDate testDate = LocalDate.of(2025, 1, 7);


  @BeforeEach
  void setUp() {
    walletService = Mockito.spy(new WalletServiceImpl(customerRepository, walletRepository, coinCapClient, assetSymbolCache, lockManager));
  }

  @Test
  void shouldHandleExistingCustomer() {

    final var email = "test@example.com";
    final var customer = new Customer();
    customer.setEmail(email);
    when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

    walletService.createWallet(email);

    verify(walletService, times(1)).handleExistingCustomer(email, customer);
    verify(walletService, never()).createCustomerAndWallet(any());
    verify(walletRepository, times(1)).findByCustomerEmailIgnoreCase(email);
    verify(walletRepository, times(1)).save(walletArgumentCaptor.capture());
    assertEquals(customer, walletArgumentCaptor.getValue().getCustomer());
  }

  @Test
  void shouldCreateCustomerAndWalletWhenCustomerNotFound() {

    final var email = "newuser@example.com";
    when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());
    final var customerId = UUID.randomUUID();
    final var customer = Customer.builder().email(email).id(customerId).build();
    when(customerRepository.save(Customer.builder().email(email).build())).thenReturn(customer);

    walletService.createWallet(email);

    verify(walletService, times(1)).createCustomerAndWallet(email);
    verify(walletService, never()).handleExistingCustomer(any(), any());
    verify(customerRepository, times(1)).save(customerArgumentCaptor.capture());
    assertEquals(email, customerArgumentCaptor.getValue().getEmail());
    verify(walletRepository, times(1)).save(walletArgumentCaptor.capture());
    assertEquals(customer, walletArgumentCaptor.getValue().getCustomer());
  }

  @Test
  void shouldReturnWallet() {

    when(lockManager.getReadLock()).thenReturn(readLock);
    final var email = "test@example.com";
    final var asset = Asset.builder().symbol("BTC").price(new BigDecimal("70000")).build();
    final var walletAsset = WalletAsset.builder().asset(asset).quantity(0.5).build();
    final var walletID = UUID.randomUUID();
    final var wallet = Wallet.builder().walletId(walletID).walletAssets(List.of(walletAsset)).build();

    when(walletRepository.findByCustomerEmailIgnoreCase(email)).thenReturn(Optional.of(wallet));

    final var response = walletService.getWallet(email);

    assertEquals(walletID, response.getId());
    assertEquals(new BigDecimal("35000.00"), response.getTotal());
    assertEquals(1, response.getAssets().size());

    final var assetResponse = response.getAssets().get(0);
    assertEquals("BTC", assetResponse.getSymbol());
    assertEquals(0.5, assetResponse.getQuantity());
    assertEquals(new BigDecimal("70000.00"), assetResponse.getPrice());
    assertEquals(new BigDecimal("35000.00"), assetResponse.getValue());

    verify(readLock).lock();
    verify(readLock).unlock();
  }

  @Test
  void shouldThrowExceptionWhenWalletNotFound() {

    when(lockManager.getReadLock()).thenReturn(readLock);
    var email = "notfound@example.com";
    when(walletRepository.findByCustomerEmailIgnoreCase(email)).thenReturn(Optional.empty());

    // When & Then
    var exception = assertThrows(WalletNotFoundException.class, () -> walletService.getWallet(email));

    assertEquals("Wallet not found for email: " + email, exception.getMessage());
    assertTrue(logCaptor.getErrorLogs().stream()
        .anyMatch(log -> log.contains("Wallet not found for email: " + email)));

    verify(readLock).lock();
    verify(readLock).unlock();
  }

  @Test
  void shouldEvaluateWalletCorrectly() {
    // Given
    final var request = EvaluateWalletRequest.builder()
        .date(testDate)
        .assets(List.of(
            new EvaluateWalletAsset("BTC", new BigDecimal("0.5"), new BigDecimal("35000")),  // Implies 70,000 per BTC
            new EvaluateWalletAsset("ETH", new BigDecimal("4.25"), new BigDecimal("15310.71")) // Implies ~3602.52 per ETH
        ))
        .build();

    when(assetSymbolCache.getSlugBySymbol("BTC")).thenReturn("bitcoin");
    when(assetSymbolCache.getSlugBySymbol("ETH")).thenReturn("ethereum");

    // Mock BTC price history
    final var btcPrice = new CoinCapAssetPrice();
    btcPrice.setDate(LocalDateTime.of(2025,1,7,12,0,0));
    btcPrice.setPriceUsd(new BigDecimal("100872.33"));

    // Mock ETH price history
    final var ethPrice = new CoinCapAssetPrice();
    ethPrice.setDate(LocalDateTime.of(2025,1,7,12,0,0));
    ethPrice.setPriceUsd(new BigDecimal("3641.95"));

    doReturn(List.of(btcPrice)).when(walletService).getPriceHistory(any(), any(), eq("bitcoin"));
    doReturn(List.of(ethPrice)).when(walletService).getPriceHistory(any(), any(), eq("ethereum"));

    doReturn(new BigDecimal("100872.33")).when(walletService).getCurrentPrice(eq(testDate), eq("bitcoin"), any());
    doReturn(new BigDecimal("3641.95")).when(walletService).getCurrentPrice(eq(testDate), eq("ethereum"), any());

    // When
    final var response = walletService.evaluateWallet(request);

    // Then
    assertEquals(new BigDecimal("65914.45"), response.getTotal());
    assertEquals("BTC", response.getBestAsset());
    assertEquals("ETH", response.getWorstAsset());

    assertEquals(BigDecimal.valueOf(44.10).setScale(2, RoundingMode.HALF_UP), response.getBestPerformance().setScale(2, RoundingMode.HALF_UP));
    assertEquals(BigDecimal.valueOf(1.09).setScale(2, RoundingMode.HALF_UP), response.getWorstPerformance().setScale(2, RoundingMode.HALF_UP));
  }
}