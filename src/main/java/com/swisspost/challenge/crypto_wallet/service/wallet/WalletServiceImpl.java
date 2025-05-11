package com.swisspost.challenge.crypto_wallet.service.wallet;

import com.swisspost.challenge.crypto_wallet.client.CoinCapClient;
import com.swisspost.challenge.crypto_wallet.component.AssetSymbolCache;
import com.swisspost.challenge.crypto_wallet.component.LockManager;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetPrice;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetPriceHistory;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletAsset;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.request.EvaluateWalletRequest;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.EvaluateWalletResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.GetWalletAssetResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.wallet.response.GetWalletResponse;
import com.swisspost.challenge.crypto_wallet.repository.entity.Customer;
import com.swisspost.challenge.crypto_wallet.repository.entity.Wallet;
import com.swisspost.challenge.crypto_wallet.repository.entity.WalletAsset;
import com.swisspost.challenge.crypto_wallet.exception.AssetNotFoundException;
import com.swisspost.challenge.crypto_wallet.exception.WalletNotFoundException;
import com.swisspost.challenge.crypto_wallet.repository.CustomerRepository;
import com.swisspost.challenge.crypto_wallet.repository.WalletRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

  public static final String H_12 = "h12";
  private final CustomerRepository customerRepository;

  private final WalletRepository walletRepository;

  private final CoinCapClient coinCapClient;

  private final AssetSymbolCache assetSymbolCache;

  private final LockManager lockManager;

  @Autowired
  public WalletServiceImpl(final CustomerRepository customerRepository, final WalletRepository walletRepository,
      final CoinCapClient coinCapClient, final AssetSymbolCache assetSymbolCache, final LockManager lockManager) {
    this.customerRepository = customerRepository;
    this.walletRepository = walletRepository;
    this.coinCapClient = coinCapClient;
    this.assetSymbolCache = assetSymbolCache;
    this.lockManager = lockManager;
  }

  @Override
  public void createWallet(final String email) {

    customerRepository.findByEmail(email)
        .ifPresentOrElse(
            customer ->
              handleExistingCustomer(email, customer),
            () -> createCustomerAndWallet(email)
        );
  }

  protected void handleExistingCustomer(final String email, final Customer customer) {
    final var walletDB = walletRepository.findByCustomerEmailIgnoreCase(email);
    if(walletDB.isPresent()) {
      log.error("Wallet already exists for this customer: {}", email);
      throw new IllegalArgumentException("Wallet already exists for this customer");
    }
    final var wallet = Wallet.builder()
        .walletId(UUID.randomUUID())
        .customer(customer)
        .build();
    walletRepository.save(wallet);
  }

  @Override
  public GetWalletResponse getWallet(final String email) {

    final var readLock = lockManager.getReadLock();
    try {
      readLock.lock();
      final var wallet = walletRepository.findByCustomerEmailIgnoreCase(email)
          .orElseThrow(() -> {
            log.error("Wallet not found for email: {}", email);
            return new WalletNotFoundException("Wallet not found for email: " + email);
          });

      final List<GetWalletAssetResponse> walletAssetsList = new ArrayList<>();
      BigDecimal total = BigDecimal.ZERO;
      for(final WalletAsset walletAsset : wallet.getWalletAssets()) {
        final var asset = walletAsset.getAsset();
        final var walletAssetResponse = new GetWalletAssetResponse();
        walletAssetResponse.setSymbol(asset.getSymbol());
        walletAssetResponse.setQuantity(walletAsset.getQuantity());
        final var price = walletAsset.getAsset().getPrice();
        final var value = price.multiply(BigDecimal.valueOf(walletAsset.getQuantity()));
        walletAssetResponse.setValue(value.setScale(2, RoundingMode.HALF_UP));
        walletAssetResponse.setPrice(price.setScale(2, RoundingMode.HALF_UP));

        total = total.add(value);
        walletAssetsList.add(walletAssetResponse);
      }

      return GetWalletResponse.builder()
          .id(wallet.getWalletId())
          .total(total.setScale(2, RoundingMode.HALF_UP))
          .assets(walletAssetsList)
          .build();

    } finally {
      readLock.unlock();
    }
  }

  @Override
  public EvaluateWalletResponse evaluateWallet(final EvaluateWalletRequest request) {

    var total = BigDecimal.ZERO;
    BigDecimal bestPerformance = null;
    BigDecimal worstPerformance = null;
    String bestAsset = null;
    String worstAsset = null;
    final var isDateToday = request.getDate() == null;
    final var date = isDateToday ? LocalDate.now() : request.getDate();
    for(final EvaluateWalletAsset walletAsset: request.getAssets()) {
      final var initialPrice = walletAsset.getValue().divide(walletAsset.getQuantity(), RoundingMode.HALF_UP);
      final var slugId = assetSymbolCache.getSlugBySymbol(walletAsset.getSymbol());
      final var start = getStartDate(date);
      final var end = getEndDate(date);
      final List<CoinCapAssetPrice> assetPriceHistoryData = getPriceHistory(start, end, slugId);
      final var currentPrice = getCurrentPrice(date, slugId, assetPriceHistoryData);

      final var newValue = walletAsset.getQuantity().multiply(currentPrice);
      final var percentageChange = getPercentageChange(initialPrice, currentPrice);

      if(bestPerformance == null || percentageChange.compareTo(bestPerformance) > 0) {
        bestPerformance = percentageChange;
        bestAsset = walletAsset.getSymbol();
      }

      if(worstPerformance == null || percentageChange.compareTo(worstPerformance) < 0) {
        worstPerformance = percentageChange;
        worstAsset = walletAsset.getSymbol();
      }

      total = total.add(newValue);
    }
    return EvaluateWalletResponse.builder()
        .total(total.setScale(2, RoundingMode.HALF_UP))
        .bestAsset(bestAsset)
        .bestPerformance(bestPerformance)
        .worstAsset(worstAsset)
        .worstPerformance(worstPerformance)
        .build();
  }

  protected BigDecimal getCurrentPrice(final LocalDate date, final String slugId, final List<CoinCapAssetPrice> assetPriceHistoryData) {
    return assetPriceHistoryData.stream()
        .filter(dataPoint -> dataPoint.getDate().toLocalDate().isEqual(date))
        .max(Comparator.comparing(CoinCapAssetPrice::getDate))
        .map(CoinCapAssetPrice::getPriceUsd)
        .orElseThrow(() -> {
          log.error("No price found for slug: {} on date: {}", slugId, date);
          return new IllegalArgumentException("No price found for slug " + slugId + " on date: " + date);
        });
  }

  private BigDecimal getPercentageChange(final BigDecimal initialPrice, final BigDecimal currentPrice) {
    if(initialPrice.compareTo(BigDecimal.ZERO) == 0) {
      log.error("Initial price is zero, cannot calculate percentage change");
      throw new IllegalArgumentException("Initial price cannot be zero");
    }

    return (currentPrice.subtract(initialPrice))
        .divide(initialPrice, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP);
  }

  protected List<CoinCapAssetPrice> getPriceHistory(final Long start, final Long end, String slugId) {
    try {
      final ResponseEntity<CoinCapAssetPriceHistory> assetPriceHistoryResponse = coinCapClient.getAssetPriceHistory(slugId, H_12, start,
          end);
      if (!assetPriceHistoryResponse.getStatusCode().is2xxSuccessful()) {
        log.error("Error fetching asset price history for slug: {}", slugId);
        throw new IllegalArgumentException("Error fetching asset price history for slug: " + slugId);
      }
      return Optional.of(assetPriceHistoryResponse.getBody())
          .map(CoinCapAssetPriceHistory::getData)
          .orElseThrow(() -> {
            log.error("Asset price history data is not available for slug: {}", slugId);
            return new IllegalArgumentException("Asset price history data is not available");
          });
    } catch (final FeignException e) {
      log.error("Error fetching asset price history for slug: {}", slugId, e);
      throw new AssetNotFoundException("Error fetching asset price history for slug: " + slugId);
    }
  }

  private long getEndDate(final LocalDate date) {
    return date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC) * 1000;
  }

  private long getStartDate(final LocalDate date) {
    return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
  }

  protected void createCustomerAndWallet(final String email) {
    final var customer = customerRepository.save(Customer.builder()
        .email(email)
        .build());
    final var wallet =  Wallet.builder()
        .walletId(UUID.randomUUID())
        .customer(customer)
        .build();
    walletRepository.save(wallet);
  }
}
