package com.swisspost.challenge.crypto_wallet.exception;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {

  private int status;
  private String error;
  private String message;
  private String path;
  private LocalDateTime timestamp;
}
