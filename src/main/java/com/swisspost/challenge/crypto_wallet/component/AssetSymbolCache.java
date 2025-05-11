package com.swisspost.challenge.crypto_wallet.component;

import com.swisspost.challenge.crypto_wallet.client.CoinCapClient;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapGetAssetsResponse;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AssetSymbolCache {

  private final CoinCapClient coinCapClient;
  private final Map<String, String> symbolToSlug = new ConcurrentHashMap<>();

  @Autowired
  public AssetSymbolCache(final CoinCapClient coinCapClient) {
    this.coinCapClient = coinCapClient;
  }


  @PostConstruct
  public void init() {
    ResponseEntity<CoinCapGetAssetsResponse> response = coinCapClient.getAssets();
    if (response.getStatusCode().is2xxSuccessful()) {
      response.getBody().getData().forEach(asset -> {
        if(!containsSymbol(asset.getSymbol().toUpperCase())) {
          log.info("Loading asset symbol to cache: {} -> {}", asset.getSymbol(), asset.getId());
          symbolToSlug.put(asset.getSymbol().toUpperCase(), asset.getId());
        }
      });
    } else {
      log.error("Failed to load asset symbol to cache from CoinCap: {}", response.getStatusCode());
    }
  }

  public String getSlugBySymbol(final String symbol) {
    return Optional.ofNullable(symbolToSlug.get(symbol.toUpperCase()))
        .orElseThrow(() -> {
          log.error("Unknown symbol: {}", symbol);
          return new IllegalArgumentException("Unknown symbol: " + symbol);
        });
  }

  public boolean containsSymbol(final String symbol) {
    return symbolToSlug.containsKey(symbol.toUpperCase());
  }
}
