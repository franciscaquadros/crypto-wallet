package com.swisspost.challenge.crypto_wallet.client;

import com.swisspost.challenge.crypto_wallet.config.CoinCapFeignConfig;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetDetailResponse;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapAssetPriceHistory;
import com.swisspost.challenge.crypto_wallet.domain.dto.coincap.response.CoinCapGetAssetsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "coinCapClient", url = "${coinCap.url}", configuration = CoinCapFeignConfig.class)
public interface CoinCapClient {

  @GetMapping(value = "/assets/{slug}", produces = "application/json")
  ResponseEntity<CoinCapAssetDetailResponse> getAssetDetailsBySlugId(@PathVariable("slug") String slugId);

  @GetMapping(value = "/assets", produces = "application/json")
  ResponseEntity<CoinCapGetAssetsResponse> getAssets();
  @GetMapping(value = "/assets/{slug}/history", produces = "application/json")
  ResponseEntity<CoinCapAssetPriceHistory> getAssetPriceHistory(@PathVariable("slug") String slugId, @RequestParam (value = "interval") String interval, @RequestParam(value = "start") Long start, @RequestParam(value = "end") Long end);
}
