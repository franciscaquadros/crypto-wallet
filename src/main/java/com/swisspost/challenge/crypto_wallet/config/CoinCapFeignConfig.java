package com.swisspost.challenge.crypto_wallet.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoinCapFeignConfig {

  @Value("${coinCap.apiKey}")
  private String apiKey;

  @Bean
  public RequestInterceptor requestInterceptor() {
    return requestTemplate -> requestTemplate.header("Authorization", "Bearer " + apiKey);
  }
}
