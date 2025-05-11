package com.swisspost.challenge.crypto_wallet.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class TokenFetchConfig {

  @Value("${app.token-fetch.interval-seconds}")
  private int intervalSeconds;

  @Value("${app.token-fetch.max-threads-number}")
  private int maxNumberThreads;

  @Bean
  public ExecutorService tokenFetcherThreadPool(final TokenFetchConfig config) {
    return Executors.newFixedThreadPool(config.getMaxNumberThreads());
  }
}
